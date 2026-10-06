package cc.skysparkle.fogwave.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BarDurations = intArrayOf(520, 740, 610, 830, 570)

private val IdleLevels = floatArrayOf(0.3f, 0.45f, 0.35f, 0.5f, 0.3f)

@Composable
fun LiveBars(active: Boolean, color: Color, modifier: Modifier = Modifier) {
    if (active) {
        val transition = rememberInfiniteTransition(label = "live_bars")
        val levels: List<State<Float>> = BarDurations.mapIndexed { i, d ->
            transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(d, delayMillis = i * 60, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )
        }
        Bars(modifier, color) { i -> levels[i].value }
    } else {
        Bars(modifier, color.copy(alpha = 0.45f)) { i -> IdleLevels[i] }
    }
}

@Composable
private fun Bars(modifier: Modifier, color: Color, level: (Int) -> Float) {
    Canvas(modifier = modifier.width(46.dp).height(22.dp)) {
        val count = BarDurations.size
        val gap = size.width * 0.08f
        val barW = (size.width - gap * (count - 1)) / count
        for (i in 0 until count) {
            val h = size.height * level(i)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }
    }
}
