package cc.skysparkle.fogwave.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.player.PlayerState
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import kotlin.math.roundToInt

private val TABLET_BREAKPOINT_DP = 600.dp
private val TABLET_MINI_PLAYER_MAX_WIDTH_DP = 640.dp

@Composable
fun MiniPlayer(
    station: RadioStation?,
    playerState: PlayerState,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (station == null) return

    val isPlaying = playerState is PlayerState.Playing
    val isConnecting = playerState is PlayerState.Connecting
    val isError = playerState is PlayerState.Error

    val subText = when {
        isError -> playerState.message
        isConnecting -> stringResource(R.string.player_connecting)
        isPlaying -> stringResource(R.string.player_on_air)
        else -> stationDescription(station)
    }

    val density = LocalDensity.current
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(density) { 44.dp.toPx() }
    val flingVelocityThresholdPx = with(density) { 1200.dp.toPx() }
    val hideTargetPx = with(density) { 160.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val isTablet = maxWidth > TABLET_BREAKPOINT_DP

        Box(
            modifier = (if (isTablet) Modifier.widthIn(max = TABLET_MINI_PLAYER_MAX_WIDTH_DP) else Modifier.fillMaxWidth())
                .offset { IntOffset(0, dragOffsetPx.roundToInt()) }
                .graphicsLayer {
                    alpha = 1f - (dragOffsetPx / hideTargetPx).coerceIn(0f, 1f)
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(LocalGeoColors.current.surfaceContainer)
                    .border(1.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(32.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) {
                        onOpenNowPlaying()
                    }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            dragOffsetPx = (dragOffsetPx + delta).coerceAtLeast(0f)
                        },
                        onDragStopped = { velocity ->
                            val shouldHide =
                                dragOffsetPx > dismissThresholdPx || velocity > flingVelocityThresholdPx
                            if (shouldHide) {
                                animate(
                                    initialValue = dragOffsetPx,
                                    targetValue = hideTargetPx,
                                    initialVelocity = velocity,
                                    animationSpec = tween(
                                        durationMillis = 180,
                                        easing = FastOutLinearInEasing
                                    )
                                ) { value, _ -> dragOffsetPx = value }
                                onHide()
                                dragOffsetPx = 0f
                            } else {
                                animate(
                                    initialValue = dragOffsetPx,
                                    targetValue = 0f,
                                    initialVelocity = velocity,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) { value, _ -> dragOffsetPx = value }
                            }
                        }
                    )
                    .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                    .testTag("mini_player"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(stationTileColor(station))
                        .border(1.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(14.dp))
                ) {
                    Image(
                        painter = painterResource(id = station.iconResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = LocalGeoColors.current.deepPurple
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = if (isError) LocalGeoColors.current.redFavorite else (if (isPlaying) LocalGeoColors.current.liveGreen else LocalGeoColors.current.onSurfaceVariant)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("mini_prev_btn")
                    ) {
                        AppIcon(
                            resId = R.drawable.ic_chevron_left,
                            contentDescription = stringResource(R.string.previous_station_content_desc),
                            tint = LocalGeoColors.current.onSurfaceVariant,
                            size = 24.dp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(LocalGeoColors.current.accentLavender)
                            .clickable { onTogglePlay() }
                            .testTag("mini_play_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = LocalGeoColors.current.onAccentLavender,
                                strokeWidth = 2.dp
                            )
                        } else {
                            AppIcon(
                                resId = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                                contentDescription = if (isPlaying) stringResource(R.string.pause_content_desc) else stringResource(R.string.play_content_desc),
                                tint = LocalGeoColors.current.onAccentLavender,
                                size = 22.dp
                            )
                        }
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("mini_next_btn")
                    ) {
                        AppIcon(
                            resId = R.drawable.ic_chevron_right,
                            contentDescription = stringResource(R.string.next_station_content_desc),
                            tint = LocalGeoColors.current.onSurfaceVariant,
                            size = 24.dp
                        )
                    }
                }
            }
        }
    }
}
