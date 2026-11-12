package cc.skysparkle.fogwave.player

import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackPolicyTest {

  private fun httpError(code: Int) = PlaybackException(
    "HTTP $code",
    HttpDataSource.InvalidResponseCodeException(
      code, null, null, emptyMap(), DataSpec(Uri.parse("https://example.com/stream")), ByteArray(0)
    ),
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
  )

  private fun error(code: Int) = PlaybackException("error", null, code)

  @Test
  fun clientErrorsGetOnlyAFewAttempts() {
    assertEquals(MAX_PERMANENT_ERROR_RETRY_ATTEMPTS, maxRetryAttemptsFor(httpError(404)))
    assertEquals(MAX_PERMANENT_ERROR_RETRY_ATTEMPTS, maxRetryAttemptsFor(httpError(403)))
  }

  @Test
  fun timeoutsRateLimitsAndServerErrorsGetTheFullBackoff() {
    assertEquals(MAX_RETRY_ATTEMPTS, maxRetryAttemptsFor(httpError(408)))
    assertEquals(MAX_RETRY_ATTEMPTS, maxRetryAttemptsFor(httpError(429)))
    assertEquals(MAX_RETRY_ATTEMPTS, maxRetryAttemptsFor(httpError(503)))
  }

  @Test
  fun unsupportedStreamsArePermanentAndNetworkErrorsAreNot() {
    assertEquals(
      MAX_PERMANENT_ERROR_RETRY_ATTEMPTS,
      maxRetryAttemptsFor(error(PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED))
    )
    assertEquals(
      MAX_PERMANENT_ERROR_RETRY_ATTEMPTS,
      maxRetryAttemptsFor(error(PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED))
    )
    assertEquals(
      MAX_RETRY_ATTEMPTS,
      maxRetryAttemptsFor(error(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
    )
    assertEquals(
      MAX_RETRY_ATTEMPTS,
      maxRetryAttemptsFor(error(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT))
    )
  }

  /** A player that only reports the given state. */
  private class FakePlayer(private val fixed: State) : SimpleBasePlayer(Looper.getMainLooper()) {
    override fun getState(): State = fixed
  }

  private fun player(
    playbackState: Int,
    playWhenReady: Boolean,
    hasItem: Boolean = true,
    error: PlaybackException? = null
  ): Player = FakePlayer(
    SimpleBasePlayer.State.Builder()
      .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
      .setPlaylist(if (hasItem) listOf(SimpleBasePlayer.MediaItemData.Builder("station").build()) else emptyList())
      .setPlaybackState(playbackState)
      .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
      .setPlayerError(error)
      .build()
  )

  private fun derive(p: Player) = deriveState(p) { "failed" }

  @Test
  fun readyMapsToPlayingOrPaused() {
    assertEquals(PlayerState.Playing, derive(player(Player.STATE_READY, playWhenReady = true)))
    assertEquals(PlayerState.Paused, derive(player(Player.STATE_READY, playWhenReady = false)))
  }

  @Test
  fun bufferingAndADroppedStreamShowConnectingWhilePlayRequested() {
    assertEquals(PlayerState.Connecting, derive(player(Player.STATE_BUFFERING, playWhenReady = true)))
    assertEquals(PlayerState.Connecting, derive(player(Player.STATE_ENDED, playWhenReady = true)))
    assertEquals(PlayerState.Paused, derive(player(Player.STATE_BUFFERING, playWhenReady = false)))
  }

  @Test
  fun idleDependsOnWhetherAStationIsLoaded() {
    assertEquals(PlayerState.Idle, derive(player(Player.STATE_IDLE, playWhenReady = false, hasItem = false)))
    assertEquals(PlayerState.Connecting, derive(player(Player.STATE_IDLE, playWhenReady = true)))
    assertEquals(PlayerState.Paused, derive(player(Player.STATE_IDLE, playWhenReady = false)))
  }

  @Test
  fun anErrorWinsOverThePlaybackState() {
    val state = derive(
      player(
        Player.STATE_IDLE,
        playWhenReady = true,
        error = error(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
      )
    )
    assertEquals(PlayerState.Error("failed"), state)
  }
}
