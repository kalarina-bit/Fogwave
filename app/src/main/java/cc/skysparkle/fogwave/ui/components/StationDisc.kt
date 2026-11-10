package cc.skysparkle.fogwave.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val ARC_SWEEP = 280f
private const val TURN_MILLIS = 14_000

/**
 * Round station artwork wrapped in a ring. Live radio has no track progress, so the arc slowly
 * circles while the station plays and rests where it was when playback stops.
 */
@Composable
fun StationDisc(
    station: RadioStation,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 4.dp,
    ringGap: Dp = 10.dp,
    glow: Boolean = true
) {
    val colors = LocalGeoColors.current
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            rotation.snapTo(rotation.value % 360f)
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(TURN_MILLIS, easing = LinearEasing)
            )
        }
    }

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = ringWidth.toPx()
            val radius = min(size.width, size.height) / 2f - stroke
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            if (glow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.accentLavender.copy(alpha = 0.28f), Color.Transparent),
                        center = center,
                        radius = radius * 1.25f
                    ),
                    radius = radius * 1.25f,
                    center = center
                )
            }

            drawCircle(
                color = colors.outlineVariant,
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )

            val start = -90f + rotation.value
            drawArc(
                color = colors.primary,
                startAngle = start,
                sweepAngle = ARC_SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            val end = Math.toRadians((start + ARC_SWEEP).toDouble())
            val knob = Offset(
                center.x + radius * cos(end).toFloat(),
                center.y + radius * sin(end).toFloat()
            )
            drawCircle(color = colors.accentLavender, radius = stroke * 1.9f, center = knob)
            drawCircle(color = Color.White, radius = stroke * 0.8f, center = knob)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(ringWidth * 2 + ringGap)
                .clip(CircleShape)
                .background(stationTileColor(station))
                .border(1.dp, colors.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Slightly larger than the inscribed square, so only the logo's corners get clipped.
            Image(
                painter = painterResource(id = station.iconResId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(0.78f)
            )
        }
    }
}
