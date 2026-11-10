package cc.skysparkle.fogwave.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
    trackTitle: String?,
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

    val colors = LocalGeoColors.current
    val context = LocalContext.current
    val isPlaying = playerState is PlayerState.Playing
    val isConnecting = playerState is PlayerState.Connecting
    val isError = playerState is PlayerState.Error

    val density = LocalDensity.current
    val currentOnClose by rememberUpdatedState(onClose)
    val drag = remember(density) {
        SheetDragState(
            dismissThresholdPx = with(density) { 110.dp.toPx() },
            flingThresholdPx = with(density) { 1400.dp.toPx() },
            offscreenPx = with(density) { 1200.dp.toPx() },
            onDismiss = { currentOnClose() }
        )
    }
    val accent by animateColorAsState(
        targetValue = rememberStationArt(station).accentOr(colors.accentLavender),
        animationSpec = tween(700),
        label = "page_accent"
    )
    val discScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "disc_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            // Read in the draw phase so pulling the page does not recompose the whole player.
            .drawBehind {
                val progress = (drag.offsetPx / drag.dismissThresholdPx).coerceIn(0f, 1f)
                drawRect(Color.Black.copy(alpha = 0.55f * (1f - progress)))
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth > 600.dp
            val pageWidth = if (isTablet) Modifier.widthIn(max = 560.dp) else Modifier.fillMaxWidth()

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .then(sheetModifier)
                    .offset { IntOffset(0, drag.offsetPx.roundToInt()) }
                    .then(pageWidth)
                    .fillMaxSize()
                    .background(colors.background)
                    // Swallow taps so nothing underneath reacts to them.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .testTag("now_playing_sheet")
            ) {
                // Fog in the station's color drifts behind the disc and fades into the page.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(620.dp)
                        .fadeOutBottom(from = 0.5f)
                ) {
                    FogLayer(
                        tint = accent,
                        intensity = if (isPlaying) 0.9f else 0.45f,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to colors.background.copy(alpha = 0.35f),
                                    0.3f to Color.Transparent
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // The page scrolls; drags the content cannot use pull it down to close.
                        .nestedScroll(drag.nestedScrollConnection)
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.testTag("np_close_btn")) {
                            AppIcon(
                                resId = R.drawable.ic_chevron_left,
                                contentDescription = stringResource(R.string.back_content_desc),
                                tint = colors.onSurface,
                                size = 26.dp
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onOpenEqualizer, modifier = Modifier.testTag("np_equalizer_toggle")) {
                            AppIcon(
                                resId = R.drawable.ic_equalizer,
                                contentDescription = stringResource(R.string.equalizer_content_desc),
                                tint = if (audioEffects.equalizerEnabled) colors.primary else colors.onSurface,
                                size = 22.dp
                            )
                        }
                    }

                    StationDisc(
                        station = station,
                        isPlaying = isPlaying,
                        glow = false,
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 20.dp)
                            .fillMaxWidth(if (isTablet) 0.6f else 0.74f)
                            .scale(discScale)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            LiveLabel(active = isPlaying)
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    letterSpacing = (-0.4).sp,
                                    color = colors.deepPurple
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${station.bitrate} • ${stationGenres(station)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = colors.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { context.shareStation(station, trackTitle) },
                            modifier = Modifier.testTag("np_share_btn")
                        ) {
                            AppIcon(
                                resId = R.drawable.ic_share,
                                contentDescription = stringResource(R.string.share_content_desc),
                                tint = colors.onSurface,
                                size = 21.dp
                            )
                        }

                        val favScale by animateFloatAsState(
                            targetValue = if (isFavorite) 1.15f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "fav_scale"
                        )
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .scale(favScale)
                                .testTag("np_favorite_toggle")
                        ) {
                            AppIcon(
                                resId = if (isFavorite) R.drawable.ic_heart_fill else R.drawable.ic_heart_outline,
                                contentDescription = stringResource(
                                    if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites
                                ),
                                tint = if (isFavorite) colors.redFavorite else colors.onSurface,
                                size = 22.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppIcon(resId = R.drawable.ic_music_note, tint = colors.onSurfaceVariant, size = 13.dp)
                        Text(
                            text = stringResource(R.string.now_playing_label),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp,
                                letterSpacing = 1.2.sp,
                                color = colors.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when {
                            isError -> playerState.message
                            isConnecting -> stringResource(R.string.player_connecting)
                            !isPlaying -> stringResource(R.string.player_stopped)
                            else -> trackTitle ?: stationDescription(station)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 21.sp,
                            textAlign = TextAlign.Center,
                            color = if (isError) colors.redFavorite else colors.onSurface
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .heightIn(min = 42.dp)
                            .testTag("np_track_title")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("np_prev_btn")
                        ) {
                            AppIcon(
                                resId = R.drawable.ic_skip_previous,
                                contentDescription = stringResource(R.string.previous_station_content_desc),
                                tint = colors.onSurface,
                                size = 28.dp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .shadow(14.dp, CircleShape, ambientColor = colors.accentLavender, spotColor = colors.accentLavender)
                                .clip(CircleShape)
                                .background(colors.accentLavender)
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
                                    color = colors.onAccentLavender,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                AppIcon(
                                    resId = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                                    contentDescription = stringResource(
                                        if (isPlaying) R.string.pause_content_desc else R.string.play_content_desc
                                    ),
                                    tint = colors.onAccentLavender,
                                    size = 32.dp
                                )
                            }
                        }

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("np_next_btn")
                        ) {
                            AppIcon(
                                resId = R.drawable.ic_skip_next,
                                contentDescription = stringResource(R.string.next_station_content_desc),
                                tint = colors.onSurface,
                                size = 28.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    ExtraPanel(
                        station = station,
                        playerState = playerState,
                        activeExtraTab = activeExtraTab,
                        onSelectExtraTab = onSelectExtraTab,
                        recents = recents,
                        similarStations = similarStations,
                        onSelectStation = onSelectStation
                    )
                }
            }
        }
    }
}

/** Lower card of the player: recent and similar stations, station info. */
@Composable
private fun ExtraPanel(
    station: RadioStation,
    playerState: PlayerState,
    activeExtraTab: ExtraPlayerTab,
    onSelectExtraTab: (ExtraPlayerTab) -> Unit,
    recents: List<RadioStation>,
    similarStations: List<RadioStation>,
    onSelectStation: (RadioStation) -> Unit
) {
    val colors = LocalGeoColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surfaceContainer)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(28.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(CircleShape)
                .background(colors.outlineVariant)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExtraPlayerTab.entries.forEach { tab ->
                val isSelected = tab == activeExtraTab
                val bg by animateColorAsState(
                    targetValue = if (isSelected) colors.accentLavender else colors.surfaceContainerHigh,
                    label = "extra_tab_bg"
                )
                val fg by animateColorAsState(
                    targetValue = if (isSelected) colors.onAccentLavender else colors.onSurfaceVariant,
                    label = "extra_tab_fg"
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bg)
                        .clickable { onSelectExtraTab(tab) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("extra_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(tab.titleRes),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = fg
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            when (activeExtraTab) {
                ExtraPlayerTab.RECENT -> {
                    if (recents.isEmpty()) {
                        EmptyHint(stringResource(R.string.recent_empty))
                    } else {
                        recents.forEach { s ->
                            StationRow(
                                station = s,
                                isCurrent = false,
                                isPlaying = false,
                                onClick = { onSelectStation(s) }
                            )
                        }
                    }
                }

                ExtraPlayerTab.SIMILAR -> {
                    if (similarStations.isEmpty()) {
                        EmptyHint(stringResource(R.string.similar_empty))
                    } else {
                        similarStations.forEach { s ->
                            StationRow(
                                station = s,
                                isCurrent = false,
                                isPlaying = false,
                                onClick = { onSelectStation(s) },
                                subtitle = "${stationDescription(s)} • ${stationGenres(s)}"
                            )
                        }
                    }
                }

                ExtraPlayerTab.INFO -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            color = LocalGeoColors.current.onSurfaceVariant,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    )
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

private fun Context.shareStation(station: RadioStation, trackTitle: String?) {
    val text = if (trackTitle != null) {
        getString(R.string.share_text_track, trackTitle, station.name)
    } else {
        getString(R.string.share_text, station.name)
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: android.content.ActivityNotFoundException) {
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
