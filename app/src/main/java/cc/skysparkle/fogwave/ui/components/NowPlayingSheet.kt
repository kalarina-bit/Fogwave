package cc.skysparkle.fogwave.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.ExtraPlayerTab
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.player.AudioEffectsState
import cc.skysparkle.fogwave.player.PlayerState
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import kotlin.math.roundToInt

@Composable
fun NowPlayingSheet(
    station: RadioStation?,
    playerState: PlayerState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onOpenEqualizer: () -> Unit,
    audioEffects: AudioEffectsState,
    activeExtraTab: ExtraPlayerTab,
    onSelectExtraTab: (ExtraPlayerTab) -> Unit,
    recents: List<RadioStation>,
    similarStations: List<RadioStation>,
    onSelectStation: (RadioStation) -> Unit,
    modifier: Modifier = Modifier,
    sheetModifier: Modifier = Modifier
) {
    if (station == null) return

    val isPlaying = playerState is PlayerState.Playing
    val isConnecting = playerState is PlayerState.Connecting
    val isError = playerState is PlayerState.Error

    val density = LocalDensity.current
    val currentOnClose by rememberUpdatedState(onClose)
    val drag = remember(density) {
        SheetDragState(
            dismissThresholdPx = with(density) { 110.dp.toPx() },
            flingThresholdPx = with(density) { 1400.dp.toPx() },
            offscreenPx = with(density) { 900.dp.toPx() },
            onDismiss = { currentOnClose() }
        )
    }
    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.02f else 0.98f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "art_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            // Read in the draw phase so pulling the sheet does not recompose the whole player.
            .drawBehind {
                val progress = (drag.offsetPx / drag.dismissThresholdPx).coerceIn(0f, 1f)
                drawRect(Color.Black.copy(alpha = 0.55f * (1f - progress)))
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClose()
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth > 600.dp
            val sheetWidthModifier = if (isTablet) Modifier.widthIn(max = 560.dp) else Modifier.fillMaxWidth()

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(sheetModifier)
                    .offset { IntOffset(0, drag.offsetPx.roundToInt()) }
                    .then(sheetWidthModifier)
                    .fillMaxHeight(0.94f)
                    .shadow(16.dp, RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    .background(LocalGeoColors.current.background)
                    .border(1.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    // Swallow taps so they do not fall through to the scrim (which closes the sheet),
                    // without exposing the sheet itself as a button to accessibility services.
                    .pointerInput(Unit) { detectTapGestures { } }
                    // The whole sheet scrolls; drags that the content cannot use pull the sheet down.
                    .nestedScroll(drag.nestedScrollConnection)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 28.dp)
                    .testTag("now_playing_sheet"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .size(width = 48.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(LocalGeoColors.current.outlineVariant)
                )

                // The sheet closes by swiping down, tapping outside it or pressing Back.
                Text(
                    text = stringResource(R.string.now_playing_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        letterSpacing = 1.2.sp,
                        color = LocalGeoColors.current.primary
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 44.dp, vertical = 10.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .scale(artScale)
                        .shadow(8.dp, RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp))
                        .background(stationTileColor(station))
                        .border(1.5.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(32.dp))
                ) {
                    Image(
                        painter = painterResource(id = station.iconResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 25.sp,
                            letterSpacing = (-0.5).sp,
                            color = LocalGeoColors.current.deepPurple,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stationDescription(station),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            color = LocalGeoColors.current.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isPlaying) LocalGeoColors.current.primaryContainer else LocalGeoColors.current.surfaceContainer)
                            .border(
                                width = 1.dp,
                                color = if (isPlaying) LocalGeoColors.current.primary.copy(alpha = 0.4f) else LocalGeoColors.current.outlineVariant,
                                shape = CircleShape
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when {
                            isPlaying -> LiveOnAirDot()
                            isError -> AppIcon(
                                resId = R.drawable.ic_alert,
                                tint = LocalGeoColors.current.redFavorite,
                                size = 12.dp
                            )
                            !isConnecting -> AppIcon(
                                resId = R.drawable.ic_sleep,
                                tint = LocalGeoColors.current.onSurfaceVariant,
                                size = 12.dp
                            )
                            else -> Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(LocalGeoColors.current.onSurfaceVariant)
                            )
                        }
                        Text(
                            text = when {
                                isError -> playerState.message
                                isConnecting -> stringResource(R.string.player_connecting)
                                isPlaying -> stringResource(R.string.now_playing_live)
                                else -> stringResource(R.string.player_stopped)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (isPlaying) LocalGeoColors.current.deepPurple else (if (isError) LocalGeoColors.current.redFavorite else LocalGeoColors.current.onSurfaceVariant)
                            )
                        )
                    }
                }

                LiveBars(
                    active = isPlaying,
                    color = LocalGeoColors.current.primary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 10.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(LocalGeoColors.current.surfaceContainer)
                                .border(1.dp, LocalGeoColors.current.outlineVariant, CircleShape)
                                .clickable { onOpenEqualizer() }
                                .testTag("np_equalizer_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(
                                resId = R.drawable.ic_equalizer,
                                contentDescription = stringResource(R.string.equalizer_content_desc),
                                tint = if (audioEffects.equalizerEnabled) LocalGeoColors.current.deepPurple else LocalGeoColors.current.onSurfaceVariant.copy(alpha = 0.5f),
                                size = 20.dp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(LocalGeoColors.current.surfaceContainer)
                                .border(1.dp, LocalGeoColors.current.outlineVariant, CircleShape)
                                .clickable { onPrevious() }
                                .testTag("np_prev_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(
                                resId = R.drawable.ic_skip_previous,
                                contentDescription = stringResource(R.string.previous_station_content_desc),
                                tint = LocalGeoColors.current.deepPurple,
                                size = 24.dp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(4.dp, RoundedCornerShape(26.dp))
                                .clip(RoundedCornerShape(26.dp))
                                .background(LocalGeoColors.current.accentLavender)
                                .clickable { onTogglePlay() }
                                .testTag("np_play_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isConnecting) {
                                val connecting = stringResource(R.string.player_connecting)
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .semantics { contentDescription = connecting },
                                    color = LocalGeoColors.current.onAccentLavender,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                AppIcon(
                                    resId = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                                    contentDescription = if (isPlaying) stringResource(R.string.pause_content_desc) else stringResource(R.string.play_content_desc),
                                    tint = LocalGeoColors.current.onAccentLavender,
                                    size = 34.dp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(LocalGeoColors.current.surfaceContainer)
                                .border(1.dp, LocalGeoColors.current.outlineVariant, CircleShape)
                                .clickable { onNext() }
                                .testTag("np_next_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(
                                resId = R.drawable.ic_skip_next,
                                contentDescription = stringResource(R.string.next_station_content_desc),
                                tint = LocalGeoColors.current.deepPurple,
                                size = 24.dp
                            )
                        }

                        val favScale by animateFloatAsState(
                            targetValue = if (isFavorite) 1.12f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "fav_scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isFavorite) LocalGeoColors.current.redFavorite.copy(alpha = 0.15f) else LocalGeoColors.current.surfaceContainer)
                                .border(
                                    width = 1.2.dp,
                                    color = if (isFavorite) LocalGeoColors.current.redFavorite else LocalGeoColors.current.outlineVariant,
                                    shape = CircleShape
                                )
                                .scale(favScale)
                                .clickable { onToggleFavorite() }
                                .testTag("np_favorite_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(
                                resId = if (isFavorite) R.drawable.ic_heart_fill else R.drawable.ic_heart_outline,
                                contentDescription = if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                                tint = if (isFavorite) LocalGeoColors.current.redFavorite else LocalGeoColors.current.deepPurple,
                                size = 22.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LocalGeoColors.current.surfaceContainer)
                            .border(1.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ExtraPlayerTab.entries.forEach { tab ->
                            val isSelected = tab == activeExtraTab
                            val bg by animateColorAsState(
                                targetValue = if (isSelected) LocalGeoColors.current.primaryContainer else Color.Transparent,
                                label = "extra_tab_bg"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) LocalGeoColors.current.deepPurple else LocalGeoColors.current.onSurfaceVariant,
                                label = "extra_tab_text"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .clickable { onSelectExtraTab(tab) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(tab.titleRes),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = textColor
                                    )
                                )
                            }
                        }
                    }

                    when (activeExtraTab) {
                        ExtraPlayerTab.RECENT -> {
                            if (recents.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.recent_empty),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = LocalGeoColors.current.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    recents.forEach { s ->
                                        val isCur = s.id == station.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isCur) LocalGeoColors.current.primaryContainer.copy(alpha = 0.6f) else LocalGeoColors.current.surfaceContainer)
                                                .border(0.8.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(14.dp))
                                                .clickable { onSelectStation(s) }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(stationTileColor(s))
                                            ) {
                                                Image(
                                                    painter = painterResource(id = s.iconResId),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.matchParentSize()
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (isCur && isPlaying) {
                                                        LiveOnAirDot()
                                                    }
                                                    Text(
                                                        text = s.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isCur) LocalGeoColors.current.deepPurple else LocalGeoColors.current.onSurface
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = stationDescription(s),
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 11.5.sp,
                                                        color = LocalGeoColors.current.onSurfaceVariant
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            AppIcon(
                                                resId = R.drawable.ic_play,
                                                contentDescription = stringResource(R.string.play_content_desc),
                                                tint = LocalGeoColors.current.deepPurple,
                                                size = 18.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ExtraPlayerTab.SIMILAR -> {
                            if (similarStations.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.similar_empty),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = LocalGeoColors.current.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    similarStations.forEach { s ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(LocalGeoColors.current.surfaceContainer)
                                                .border(0.8.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(14.dp))
                                                .clickable { onSelectStation(s) }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(stationTileColor(s))
                                            ) {
                                                Image(
                                                    painter = painterResource(id = s.iconResId),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.matchParentSize()
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = s.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = LocalGeoColors.current.deepPurple
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${stationDescription(s)} • ${stationGenres(s)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 11.5.sp,
                                                        color = LocalGeoColors.current.onSurfaceVariant
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            AppIcon(
                                                resId = R.drawable.ic_play,
                                                contentDescription = stringResource(R.string.play_content_desc),
                                                tint = LocalGeoColors.current.deepPurple,
                                                size = 18.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ExtraPlayerTab.INFO -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(LocalGeoColors.current.surfaceContainer)
                                    .border(1.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                InfoRow(key = stringResource(R.string.info_station), value = station.name)
                                InfoRow(key = stringResource(R.string.info_description), value = stationDescription(station))
                                InfoRow(key = stringResource(R.string.info_genre), value = stationGenres(station))
                                InfoRow(key = stringResource(R.string.info_quality), value = station.bitrate)
                                InfoRow(
                                    key = stringResource(R.string.info_status),
                                    value = when (playerState) {
                                        is PlayerState.Playing -> stringResource(R.string.status_live)
                                        is PlayerState.Connecting -> stringResource(R.string.player_connecting)
                                        is PlayerState.Error -> playerState.message
                                        else -> stringResource(R.string.player_stopped)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = LocalGeoColors.current.onSurfaceVariant
            )
        )
        // Long values (descriptions, genre lists) wrap on the right instead of pushing the key out.
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = LocalGeoColors.current.deepPurple,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Offset of the now-playing sheet while it is being pulled down. Driven through nested scroll so
 * the gesture works anywhere on the (scrollable) sheet once its content is scrolled to the top.
 */
@Stable
private class SheetDragState(
    val dismissThresholdPx: Float,
    private val flingThresholdPx: Float,
    private val offscreenPx: Float,
    private val onDismiss: () -> Unit
) {
    var offsetPx by mutableFloatStateOf(0f)
        private set

    private fun dragBy(delta: Float): Float {
        val old = offsetPx
        offsetPx = (offsetPx + delta).coerceAtLeast(0f)
        return offsetPx - old
    }

    private suspend fun settle(velocity: Float) {
        if (offsetPx > dismissThresholdPx || velocity > flingThresholdPx) {
            animate(
                initialValue = offsetPx,
                targetValue = offscreenPx,
                initialVelocity = velocity,
                animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
            ) { value, _ -> offsetPx = value }
            onDismiss()
        } else {
            animate(
                initialValue = offsetPx,
                targetValue = 0f,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) { value, _ -> offsetPx = value }
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        // Dragging back up first collapses the pull-down offset before the content scrolls.
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0f && offsetPx > 0f) Offset(0f, dragBy(available.y)) else Offset.Zero

        // Whatever downward drag the content could not consume (it is at the top) moves the sheet.
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (available.y > 0f && source == NestedScrollSource.UserInput) {
                Offset(0f, dragBy(available.y))
            } else {
                Offset.Zero
            }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offsetPx <= 0f) return Velocity.Zero
            settle(available.y)
            return available
        }
    }
}
