package cc.skysparkle.fogwave.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
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

    private val httpDataSourceFactory: DefaultHttpDataSource.Factory by lazy {
        DefaultHttpDataSource.Factory()
            .setUserAgent("FogwaveRadio/${BuildConfig.VERSION_NAME} (Android; Icecast/Shoutcast Client)")
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)
            .setAllowCrossProtocolRedirects(true)
    }

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(5_000, 15_000, 500, 1_000)
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
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect(immediate: Boolean = false) {
        retryJob?.cancel()
        if (!immediate && retryAttempt >= MAX_RETRY_ATTEMPTS) {
            // Give up instead of keeping the device busy forever; the error stays visible and a tap
            // on play starts over.
            Log.w(TAG, "Reconnect abandoned after $retryAttempt attempts")
            retryAttempt = 0
            player.pause()
            return
        }
        val delayMs = if (immediate) 0L else retryDelaysMs.getOrElse(retryAttempt) { retryDelaysMs.last() }
        retryAttempt++
        Log.d(TAG, "Reconnect scheduled in ${delayMs}ms (attempt=$retryAttempt)")
        retryJob = serviceScope.launch {
            delay(delayMs)
            if (!player.playWhenReady) {
                Log.d(TAG, "Reconnect skipped - no longer playWhenReady")
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

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        connectivityManager = cm
        val callback = object : ConnectivityManager.NetworkCallback() {
            // Called on a ConnectivityManager thread; ExoPlayer must only be accessed on the main thread.
            override fun onAvailable(network: Network) {
                serviceScope.launch {
                    if (retryJob?.isActive == true && player.playWhenReady && player.playbackState != Player.STATE_READY) {
                        Log.d(TAG, "Network available again - reconnecting immediately")
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
        audioEffectsEngine = AudioEffectsEngine(applicationContext, player.audioSessionId).also {
            AudioEffectsController.attach(it)
        }

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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            player.pause()
            stopSelf()
        }
    }

    override fun onDestroy() {
        retryJob?.cancel()
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
        private const val LIVE_RESYNC_AFTER_MS = 30_000L
    }
}
