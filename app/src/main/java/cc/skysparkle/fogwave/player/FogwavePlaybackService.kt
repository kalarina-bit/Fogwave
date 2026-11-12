package cc.skysparkle.fogwave.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import cc.skysparkle.fogwave.BuildConfig
import cc.skysparkle.fogwave.MainActivity
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.data.repository.RadioRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class FogwavePlaybackService : MediaLibraryService() {
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var repository: RadioRepository
    private lateinit var player: ExoPlayer

    // Station order for next/previous from the notification, a headset or the car; sorted once,
    // like the grid in the app.
    private var stationOrder: List<RadioStation> = emptyList()
    private var audioEffectsEngine: AudioEffectsEngine? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val retryDelaysMs = longArrayOf(1_000, 2_000, 4_000, 8_000, 15_000, 30_000)
    private var retryAttempt = 0
    private var retryJob: Job? = null

    // Elapsed-realtime timestamp of the last pause, used to jump back to the live edge on resume.
    private var pausedAtMs = 0L

    private var lastRequestedItem: MediaItem? = null

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var defaultNetwork: Network? = null
    private var hasNetwork = true

    // Set when a reconnect is due but there is no network: no attempts are spent while offline,
    // the stream is reopened from onAvailable instead.
    private var waitingForNetwork = false

    // ExoPlayer drops its own wake and Wi-Fi locks once the player is idle after an error, so the
    // backoff delay would stall while the CPU sleeps. These are held only while a reconnect is pending.
    private var reconnectWakeLock: PowerManager.WakeLock? = null
    private var reconnectWifiLock: WifiManager.WifiLock? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var destroyed = false

    private val httpDataSourceFactory: DefaultHttpDataSource.Factory by lazy {
        DefaultHttpDataSource.Factory()
            .setUserAgent("FogwaveRadio/${BuildConfig.VERSION_NAME} (Android; Icecast/Shoutcast Client)")
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)
            .setAllowCrossProtocolRedirects(true)
    }

    private val loadControl = DefaultLoadControl.Builder()
        // Quick start (0.5 s), but after a stall wait for 2.5 s of audio so a weak network does not
        // turn into a play-stall-play stutter.
        .setBufferDurationsMs(15_000, 50_000, 500, 2_500)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    private val librarySessionCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(R.string.app_name))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            if (parentId != ROOT_ID) {
                return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            }
            val items = repository.stations.map { it.toMediaItem(this@FogwavePlaybackService) }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val station = repository.stations.find { it.id == mediaId }
                ?: return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            return Futures.immediateFuture(LibraryResult.ofItem(station.toMediaItem(this@FogwavePlaybackService), null))
        }

        // Items coming from a controller are never trusted: only the mediaId (or a voice search
        // query) is read, and the stream URI always comes from our own station list. Without this
        // an item without a URI (a voice request from Assistant or Android Auto) crashed the
        // player, and any app on the device could make Fogwave play an arbitrary stream.
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { resolveStation(it).toMediaItem(this@FogwavePlaybackService) }
                .toMutableList()
            resolved.firstOrNull()?.let { onNewRequest(it) }
            return Futures.immediateFuture(resolved)
        }
    }

    private fun resolveStation(item: MediaItem): RadioStation {
        val stations = repository.stations
        val query = item.requestMetadata.searchQuery?.trim().orEmpty()
        return stations.find { it.id == item.mediaId }
            ?: query.takeIf { it.isNotEmpty() }?.let { q ->
                stations.find { it.name.contains(q, ignoreCase = true) }
                    ?: stations.find { q.contains(it.name, ignoreCase = true) }
            }
            ?: stations.find { it.id == repository.getLastStationId() }
            ?: stations.first()
    }

    private fun onNewRequest(item: MediaItem) {
        Log.d(TAG, "MediaItem resolved: mediaId=${item.mediaId}")
        lastRequestedItem = item
        retryJob?.cancel()
        retryAttempt = 0
        waitingForNetwork = false
    }

    private fun switchStation(step: Int) {
        val order = stationOrder.ifEmpty { repository.stations }
        val currentId = player.currentMediaItem?.mediaId ?: lastRequestedItem?.mediaId
        val index = order.indexOfFirst { it.id == currentId }
        val target = if (index < 0) order.first() else order[(index + step).mod(order.size)]
        Log.d(TAG, "Switching station: ${target.id}")
        val item = target.toMediaItem(this)
        onNewRequest(item)
        repository.setLastStationId(target.id)
        // playWhenReady is kept: a paused player switches the station and stays paused.
        player.setMediaItem(item)
        player.prepare()
    }

    /**
     * The player only ever holds the station that is on air: with the whole list in ExoPlayer every
     * stream would be prepared at once, and the HLS ones would keep refreshing their playlists in
     * the background. Instead next/previous are always offered to the session (notification,
     * headset buttons, Android Auto) and switch the station here, wrapping around like the app.
     */
    private inner class StationSwitchingPlayer(player: Player) : ForwardingSimpleBasePlayer(player) {
        override fun getState(): State {
            val state = super.getState()
            if (state.timeline.isEmpty()) return state
            val commands = state.availableCommands.buildUpon()
                .addAll(
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
                )
                .remove(Player.COMMAND_SET_REPEAT_MODE)
                .build()
            // REPEAT_MODE_ALL makes next/previous resolve to the single item, which lands in
            // handleSeek below with the original seek command.
            return state.buildUpon()
                .setAvailableCommands(commands)
                .setRepeatMode(Player.REPEAT_MODE_ALL)
                .build()
        }

        override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
            when (seekCommand) {
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> switchStation(+1)
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> switchStation(-1)
                else -> return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
            }
            return Futures.immediateVoidFuture()
        }

        // The stream's own repeat mode stays off; a dropped stream is handled by scheduleReconnect.
        override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> = Futures.immediateVoidFuture()
    }

    private val resiliencePlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateName = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN($playbackState)"
            }
            Log.d(TAG, "Player state changed: $stateName (playWhenReady=${player.playWhenReady})")

            // A live stream never really ends: STATE_ENDED means the server dropped the connection.
            if (playbackState == Player.STATE_ENDED && player.playWhenReady) {
                scheduleReconnect()
            }

            if (playbackState == Player.STATE_READY && player.playWhenReady) {
                if (retryAttempt != 0) Log.d(TAG, "Reconnect succeeded - resetting backoff")
                retryAttempt = 0
                retryJob?.cancel()
                releaseReconnectLocks()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, if (isPlaying) "Playing" else "Not playing (paused/buffering/stopped)")
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) {
                if (retryJob?.isActive == true) {
                    Log.d(TAG, "Reconnect cancelled - playWhenReady=false (reason=$reason)")
                }
                retryJob?.cancel()
                waitingForNetwork = false
                releaseReconnectLocks()
                pausedAtMs = SystemClock.elapsedRealtime()
                return
            }
            val pausedFor = SystemClock.elapsedRealtime() - pausedAtMs
            val resumingOldBuffer = pausedAtMs != 0L && pausedFor > LIVE_RESYNC_AFTER_MS &&
                player.playbackState == Player.STATE_READY
            pausedAtMs = 0L
            if (resumingOldBuffer) {
                // After a long pause the buffer is stale and the server has usually dropped the
                // connection, so reopen the stream at the live edge instead of replaying old audio.
                Log.d(TAG, "Resumed after ${pausedFor}ms - reloading live stream")
                // Posted (not immediate) so the player is not re-entered from its own callback.
                serviceScope.launch(Dispatchers.Main) { restartStream() }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(
                TAG,
                "Playback error: errorCode=${error.errorCode} errorCodeName=${error.errorCodeName} " +
                    "message=${error.message} cause=${error.cause} " +
                    "mediaItem=${lastRequestedItem?.localConfiguration?.uri}",
                error
            )
            scheduleReconnect(maxAttempts = maxRetryAttemptsFor(error))
        }
    }

    private fun scheduleReconnect(immediate: Boolean = false, maxAttempts: Int = MAX_RETRY_ATTEMPTS) {
        retryJob?.cancel()
        if (!immediate && !hasNetwork) {
            // Attempts would only fail and run out while offline; wait for onAvailable instead.
            Log.d(TAG, "No network - waiting for it before reconnecting")
            waitingForNetwork = true
            releaseReconnectLocks()
            return
        }
        waitingForNetwork = false
        if (!immediate && retryAttempt >= maxAttempts) {
            // Give up instead of keeping the device busy forever; the error stays visible and a tap
            // on play starts over.
            Log.w(TAG, "Reconnect abandoned after $retryAttempt attempts")
            retryAttempt = 0
            releaseReconnectLocks()
            player.pause()
            return
        }
        acquireReconnectLocks()
        val delayMs = if (immediate) 0L else retryDelaysMs.getOrElse(retryAttempt) { retryDelaysMs.last() }
        retryAttempt++
        Log.d(TAG, "Reconnect scheduled in ${delayMs}ms (attempt=$retryAttempt)")
        retryJob = serviceScope.launch {
            delay(delayMs)
            if (!player.playWhenReady) {
                Log.d(TAG, "Reconnect skipped - no longer playWhenReady")
                releaseReconnectLocks()
                return@launch
            }
            restartStream()
        }
    }

    private fun restartStream() {
        val item = player.currentMediaItem ?: lastRequestedItem
        if (item == null) {
            Log.d(TAG, "Restart skipped - nothing to play")
            return
        }
        Log.d(TAG, "Restarting stream: uri=${item.localConfiguration?.uri}")
        try {
            player.stop()
            player.setMediaItem(item)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "Restart failed", e)
        }
    }

    @Suppress("DEPRECATION") // WIFI_MODE_FULL_HIGH_PERF is what ExoPlayer itself uses for streaming.
    private fun acquireReconnectLocks() {
        try {
            if (reconnectWakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                reconnectWakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Fogwave:reconnect")
                    ?.apply { setReferenceCounted(false) }
            }
            // The timeout is a safety net; the lock is released as soon as the stream is back.
            reconnectWakeLock?.acquire(RECONNECT_LOCK_TIMEOUT_MS)

            if (reconnectWifiLock == null) {
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                reconnectWifiLock = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Fogwave:reconnect")
                    ?.apply { setReferenceCounted(false) }
            }
            val wifiLock = reconnectWifiLock
            if (wifiLock != null && !wifiLock.isHeld) wifiLock.acquire()
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire reconnect locks", e)
        }
    }

    private fun releaseReconnectLocks() {
        try { reconnectWakeLock?.takeIf { it.isHeld }?.release() } catch (_: Exception) {}
        try { reconnectWifiLock?.takeIf { it.isHeld }?.release() } catch (_: Exception) {}
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        connectivityManager = cm
        hasNetwork = cm.activeNetwork != null
        val callback = object : ConnectivityManager.NetworkCallback() {
            // Called on a ConnectivityManager thread; ExoPlayer must only be accessed on the main thread.
            override fun onAvailable(network: Network) {
                serviceScope.launch {
                    val previous = defaultNetwork
                    defaultNetwork = network
                    hasNetwork = true
                    val wasWaiting = waitingForNetwork
                    waitingForNetwork = false
                    if (!player.playWhenReady || player.playbackState == Player.STATE_READY) return@launch
                    if (wasWaiting) {
                        Log.d(TAG, "Network is back - reconnecting")
                        retryAttempt = 0
                        scheduleReconnect(immediate = true)
                        return@launch
                    }
                    // After a switch (e.g. Wi-Fi to mobile) a stalled load can sit on a dead socket until
                    // the read timeout, so reopen right away instead of waiting for the error.
                    val switchedWhileStalled = previous != null && previous != network &&
                        player.playbackState == Player.STATE_BUFFERING
                    if (retryJob?.isActive == true || switchedWhileStalled) {
                        Log.d(TAG, "Network available - reconnecting immediately")
                        scheduleReconnect(immediate = true)
                    }
                }
            }

            override fun onLost(network: Network) {
                serviceScope.launch {
                    // A switch reports the new network first; only the loss of the current one counts.
                    if (network == defaultNetwork) hasNetwork = false
                }
            }
        }
        networkCallback = callback
        try {
            cm.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = RadioRepository(applicationContext)
        stationOrder = repository.stationsSortedByPopularity()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val audioSessionId = audioManager?.generateAudioSessionId()?.takeIf { it != 0 }
            ?: C.AUDIO_SESSION_ID_UNSET

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(httpDataSourceFactory))
            .setLoadControl(loadControl)
            .build()
            .apply {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    setAudioSessionId(audioSessionId)
                }

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                setHandleAudioBecomingNoisy(true)

                // Keeps CPU and Wi-Fi awake while streaming; without it playback stops with the screen off.
                setWakeMode(C.WAKE_MODE_NETWORK)
                addListener(resiliencePlayerListener)
            }

        Log.d(TAG, "Audio session id for effects: ${player.audioSessionId}")
        createAudioEffectsAsync(player.audioSessionId)

        registerNetworkCallback()

        // Tapping the media notification brings the app back to the front.
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, StationSwitchingPlayer(player), librarySessionCallback)
            .setSessionActivity(sessionActivity)
            .build()
    }

    // Creating the effects means several calls into audioserver; done off the main thread so the
    // service, which starts together with the UI, does not delay the first frames.
    private fun createAudioEffectsAsync(audioSessionId: Int) {
        Thread({
            try {
                val engine = AudioEffectsEngine(applicationContext, audioSessionId)
                mainHandler.post {
                    if (destroyed) {
                        engine.release()
                    } else {
                        audioEffectsEngine = engine
                        AudioEffectsController.attach(engine)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio effects init failed", e)
            }
        }, "FogwaveEffectsInit").start()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    // Swiping the app away keeps only real playback alive (a short rebuffer counts): a failed
    // stream, a pending reconnect or a pause stop the service. The Media3 default would also stop
    // during a rebuffer, because isPlaying is false while buffering.
    override fun onTaskRemoved(rootIntent: Intent?) {
        val reconnecting = retryJob?.isActive == true || waitingForNetwork || player.playerError != null
        if (!isPlaybackOngoing() || !player.playWhenReady || player.mediaItemCount == 0 || reconnecting) {
            pauseAllPlayersAndStopSelf()
        }
    }

    override fun onDestroy() {
        destroyed = true
        retryJob?.cancel()
        releaseReconnectLocks()
        serviceScope.cancel()
        networkCallback?.let { cb ->
            try { connectivityManager?.unregisterNetworkCallback(cb) } catch (_: Exception) {}
        }
        audioEffectsEngine?.let {
            AudioEffectsController.detach(it)
            it.release()
        }
        player.removeListener(resiliencePlayerListener)
        player.release()
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        super.onDestroy()
    }

    companion object {
        private const val ROOT_ID = "fogwave_root"
        private const val TAG = "FogwavePlaybackService"
        private const val RECONNECT_LOCK_TIMEOUT_MS = 2 * 60_000L
        private const val LIVE_RESYNC_AFTER_MS = 30_000L
    }
}
