package cc.skysparkle.fogwave.player

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource

// Kept apart from the service and the UI controller so the decisions can be unit tested.

internal const val MAX_RETRY_ATTEMPTS = 12
internal const val MAX_PERMANENT_ERROR_RETRY_ATTEMPTS = 2

// A missing stream or an unsupported format will not fix itself, so it gets only a couple of
// attempts (a stale token or a short server outage can still recover); network errors get the full backoff.
@OptIn(UnstableApi::class)
internal fun maxRetryAttemptsFor(error: PlaybackException): Int {
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

/** What the UI shows for the session's player; [errorMessage] turns a playback error into text. */
internal fun deriveState(p: Player, errorMessage: (PlaybackException) -> String): PlayerState {
    p.playerError?.let { return PlayerState.Error(errorMessage(it)) }
    return when (p.playbackState) {
        Player.STATE_READY -> if (p.playWhenReady) PlayerState.Playing else PlayerState.Paused
        Player.STATE_BUFFERING, Player.STATE_ENDED ->
            if (p.playWhenReady) PlayerState.Connecting else PlayerState.Paused
        else -> when {
            p.mediaItemCount == 0 -> PlayerState.Idle
            p.playWhenReady -> PlayerState.Connecting
            else -> PlayerState.Paused
        }
    }
}
