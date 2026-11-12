package cc.skysparkle.fogwave.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.RadioStation
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PlayerState {
    data object Idle : PlayerState()
    data object Connecting : PlayerState()
    data object Playing : PlayerState()
    data object Paused : PlayerState()
    data class Error(val message: String) : PlayerState()
}

class RadioPlayerManager(private val context: Context) {
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentMediaId = MutableStateFlow<String?>(null)
    val currentMediaId: StateFlow<String?> = _currentMediaId.asStateFlow()

    /** Song announced by the stream (ICY / ID3 metadata), or null when the station sends none. */
    private val _nowPlayingTitle = MutableStateFlow<String?>(null)
    val nowPlayingTitle: StateFlow<String?> = _nowPlayingTitle.asStateFlow()

    private var currentStation: RadioStation? = null
    private var pendingStation: RadioStation? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var released = false

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            _playerState.value = deriveState(player, ::errorMessage)
            _currentMediaId.value = player.currentMediaItem?.mediaId
            _nowPlayingTitle.value = streamTitle(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Playback error: ${error.errorCodeName} (${error.errorCode}) ${error.message}", error)
        }
    }

    // Without this a controller whose session went away stays non-null and every command is
    // silently dropped, leaving the UI stuck on "connecting".
    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            if (released || controller !== this@RadioPlayerManager.controller) return
            Log.w(TAG, "MediaController disconnected")
            dropController()
            _playerState.value = PlayerState.Idle
        }
    }

    // Declared after every property it touches so that nothing is read before initialization.
    init {
        connectToPlaybackService()
    }

    private fun connectToPlaybackService() {
        if (released || controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, FogwavePlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken)
            .setListener(controllerListener)
            .buildAsync()
        controllerFuture = future
        future.addListener({
            if (released) return@addListener
            try {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                Log.d(TAG, "MediaController connected")
                val station = pendingStation
                pendingStation = null
                if (station != null) {
                    playStation(station)
                } else {
                    _playerState.value = deriveState(c, ::errorMessage)
                    _currentMediaId.value = c.currentMediaItem?.mediaId
                    _nowPlayingTitle.value = streamTitle(c)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to FogwavePlaybackService", e)
                // Allow the next play request to try again.
                controllerFuture = null
                pendingStation = null
                _playerState.value = PlayerState.Error(context.getString(R.string.stream_unavailable))
            }
        }, MoreExecutors.directExecutor())
    }

    /** The controller if it is still connected; a stale one is dropped so the next call reconnects. */
    private fun connectedController(): MediaController? {
        val c = controller ?: return null
        if (c.isConnected) return c
        dropController()
        return null
    }

    private fun dropController() {
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    // Stream metadata replaces the item's own title; the station name itself is not a song.
    private fun streamTitle(p: Player): String? {
        val title = p.mediaMetadata.title?.toString()?.trim()
        if (title.isNullOrEmpty()) return null
        if (title == p.currentMediaItem?.mediaMetadata?.title?.toString()?.trim()) return null
        return title
    }

    private fun errorMessage(error: PlaybackException): String = context.getString(
        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> R.string.network_unavailable
            else -> R.string.stream_unavailable
        }
    )

    fun playStation(station: RadioStation) {
        Log.d(TAG, "Station requested: ${station.id}")
        currentStation = station
        val c = connectedController()
        if (c == null) {
            pendingStation = station
            _playerState.value = PlayerState.Connecting
            connectToPlaybackService()
            return
        }
        _playerState.value = PlayerState.Connecting
        _nowPlayingTitle.value = null
        try {
            c.setMediaItem(station.toMediaItem(context))
            c.prepare()
            c.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ${station.streamUrl}", e)
            _playerState.value = PlayerState.Error(context.getString(R.string.stream_unavailable))
        }
    }

    fun play() {
        val c = connectedController()
        when {
            c == null || c.mediaItemCount == 0 -> currentStation?.let { playStation(it) }
            // A failed, stopped or finished live stream has to be reopened rather than resumed.
            c.playerError != null || c.playbackState == Player.STATE_IDLE || c.playbackState == Player.STATE_ENDED -> {
                c.seekToDefaultPosition()
                c.prepare()
                c.play()
            }
            else -> c.play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun togglePlay() {
        when (_playerState.value) {
            is PlayerState.Playing, is PlayerState.Connecting -> pause()
            is PlayerState.Paused, is PlayerState.Idle, is PlayerState.Error -> play()
        }
    }

    fun release() {
        released = true
        pendingStation = null
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    private companion object {
        const val TAG = "RadioPlayerManager"
    }
}
