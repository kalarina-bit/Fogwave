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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
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

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                if (item.localConfiguration != null) {
                    item
                } else {
                    repository.stations.find { it.id == item.mediaId }
                        ?.toMediaItem(this@FogwavePlaybackService)
                        ?: item
                }
            }.toMutableList()

            resolved.firstOrNull()?.let { item ->
                Log.d(TAG, "MediaItem resolved: mediaId=${item.mediaId} uri=${item.localConfiguration?.uri}")
                lastRequestedItem = item
                retryJob?.cancel()
                retryAttempt = 0
            }

            return Futures.immediateFuture(resolved)
        }
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

    // A missing stream or an unsupported format will not fix itself, so it gets only a couple of
    // attempts (a stale token or a short server outage can still recover); network errors get the full backoff.
    private fun maxRetryAttemptsFor(error: PlaybackException): Int {
        val httpCode = (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode
        val permanent = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                httpCode != null && httpCode in 400..499 && httpCode != 408 && httpCode != 429
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> true
            else -> false
        }
        return if (permanent) MAX_PERMANENT_ERROR_RETRY_ATTEMPTS else MAX_RETRY_ATTEMPTS
    }

    private fun scheduleReconnect(immediate: Boolean = false, maxAttempts: Int = MAX_RETRY_ATTEMPTS) {
        retryJob?.cancel()
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
        val callback = object : ConnectivityManager.NetworkCallback() {
            // Called on a ConnectivityManager thread; ExoPlayer must only be accessed on the main thread.
            override fun onAvailable(network: Network) {
                serviceScope.launch {
                    val previous = defaultNetwork
                    defaultNetwork = network
                    if (!player.playWhenReady || player.playbackState == Player.STATE_READY) return@launch
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

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, librarySessionCallback)
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            player.pause()
            stopSelf()
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
        private const val MAX_RETRY_ATTEMPTS = 12
        private const val MAX_PERMANENT_ERROR_RETRY_ATTEMPTS = 2
        private const val RECONNECT_LOCK_TIMEOUT_MS = 2 * 60_000L
        private const val LIVE_RESYNC_AFTER_MS = 30_000L
    }
}
