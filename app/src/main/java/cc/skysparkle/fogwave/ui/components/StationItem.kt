package cc.skysparkle.fogwave.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.data.model.IconSizeStep
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.data.model.ViewMode
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import coil.compose.AsyncImage

@Composable
fun LiveOnAirDot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_pulse"
    )

    Box(
        modifier = modifier
            .size(7.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(LocalGeoColors.current.liveGreen)
    )
}

private val GridTextShadow = Shadow(color = Color.Black.copy(alpha = 0.55f), blurRadius = 8f)

@Composable
fun StationItem(
    station: RadioStation,
    isCurrent: Boolean,
    isPlaying: Boolean,
    viewMode: ViewMode,
    sizeStep: IconSizeStep,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "station_press"
    )
    val cardModifier = modifier.graphicsLayer {
        scaleX = pressScale
        scaleY = pressScale
    }

    when (viewMode) {
        ViewMode.ICONS -> {
            StationIconsCard(
                station = station,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                sizeDp = sizeStep.sizeDp.dp,
                onClick = onClick,
                interactionSource = interactionSource,
                modifier = cardModifier
            )
        }
        ViewMode.TILES -> {
            StationTilesCard(
                station = station,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                sizeDp = (sizeStep.sizeDp * 0.7f).dp,
                onClick = onClick,
                interactionSource = interactionSource,
                modifier = cardModifier
            )
        }
        ViewMode.LIST -> {
            StationListRow(
                station = station,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                sizeDp = (sizeStep.sizeDp * 0.45f).coerceAtLeast(36f).dp,
                onClick = onClick,
                interactionSource = interactionSource,
                modifier = cardModifier
            )
        }
        ViewMode.DETAILS -> {
            StationDetailsRow(
                station = station,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                sizeDp = (sizeStep.sizeDp * 0.45f).coerceAtLeast(36f).dp,
                onClick = onClick,
                interactionSource = interactionSource,
                modifier = cardModifier
            )
        }
    }
}

@Composable
private fun StationIconsCard(
    station: RadioStation,
    isCurrent: Boolean,
    isPlaying: Boolean,
    sizeDp: Dp,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(sizeDp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .padding(4.dp)
            .testTag("station_${station.id}"),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(if (isCurrent) 4.dp else 1.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(stationTileColor(station))
                .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = if (isCurrent) LocalGeoColors.current.primary else LocalGeoColors.current.outlineVariant,
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            AsyncImage(
                model = station.iconResId,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isCurrent && isPlaying) {
                LiveOnAirDot()
            }
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    lineHeight = 17.sp,
                    color = if (isCurrent) LocalGeoColors.current.deepPurple else LocalGeoColors.current.onSurface,
                    shadow = GridTextShadow
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = stationDescription(station),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                color = LocalGeoColors.current.onSurface.copy(alpha = 0.82f),
                shadow = GridTextShadow
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StationTilesCard(
    station: RadioStation,
    isCurrent: Boolean,
    isPlaying: Boolean,
    sizeDp: Dp,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isCurrent) LocalGeoColors.current.primaryContainer.copy(alpha = 0.7f) else LocalGeoColors.current.surfaceContainer.copy(alpha = 0.9f))
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) LocalGeoColors.current.primary else LocalGeoColors.current.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .padding(10.dp)
            .testTag("station_${station.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp)
                .clip(RoundedCornerShape(14.dp))
                .background(stationTileColor(station))
                .border(0.8.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = station.iconResId,
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
                if (isCurrent && isPlaying) {
                    LiveOnAirDot()
                }
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isCurrent) LocalGeoColors.current.deepPurple else LocalGeoColors.current.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stationDescription(station),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = LocalGeoColors.current.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StationListRow(
    station: RadioStation,
    isCurrent: Boolean,
    isPlaying: Boolean,
    sizeDp: Dp,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) LocalGeoColors.current.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("station_${station.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp)
                .clip(RoundedCornerShape(10.dp))
                .background(stationTileColor(station))
                .border(0.8.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = station.iconResId,
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
                if (isCurrent && isPlaying) {
                    LiveOnAirDot()
                }
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) LocalGeoColors.current.deepPurple else LocalGeoColors.current.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StationDetailsRow(
    station: RadioStation,
    isCurrent: Boolean,
    isPlaying: Boolean,
    sizeDp: Dp,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) LocalGeoColors.current.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
            .border(
                width = 0.8.dp,
                color = if (isCurrent) LocalGeoColors.current.primary else LocalGeoColors.current.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("station_${station.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp)
                .clip(RoundedCornerShape(10.dp))
                .background(stationTileColor(station))
                .border(0.8.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = station.iconResId,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        Column(modifier = Modifier.weight(1.3f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isCurrent && isPlaying) {
                    LiveOnAirDot()
                }
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) LocalGeoColors.current.deepPurple else LocalGeoColors.current.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stationDescription(station),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    color = LocalGeoColors.current.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = stationGenres(station),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                color = LocalGeoColors.current.primary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.9f)
        )
    }
}
