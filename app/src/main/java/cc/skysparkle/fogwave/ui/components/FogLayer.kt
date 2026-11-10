package cc.skysparkle.fogwave.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** One drifting patch of mist: where it rests, how far and how it wanders, and how big it is. */
private class Wisp(
    val x: Float,
    val y: Float,
    val dx: Float,
    val dy: Float,
    val phase: Float,
    val radius: Float,
    val alpha: Float,
    val mix: Float
)

private val Wisps = listOf(
    Wisp(x = 0.18f, y = 0.30f, dx = 0.10f, dy = 0.05f, phase = 0.0f, radius = 0.62f, alpha = 0.55f, mix = 0.0f),
    Wisp(x = 0.82f, y = 0.42f, dx = 0.08f, dy = 0.07f, phase = 1.9f, radius = 0.55f, alpha = 0.45f, mix = 0.35f),
    Wisp(x = 0.50f, y = 0.70f, dx = 0.14f, dy = 0.04f, phase = 3.7f, radius = 0.70f, alpha = 0.35f, mix = 0.15f),
    Wisp(x = 0.35f, y = 0.55f, dx = 0.06f, dy = 0.08f, phase = 5.1f, radius = 0.38f, alpha = 0.22f, mix = 0.8f)
)

/**
 * Slowly drifting colored fog — Fogwave's signature backdrop. [tint] is usually the playing
 * station's color. Only the draw phase reads the animation, so the fog never recomposes anything.
 */
@Composable
fun FogLayer(
    tint: Color,
    modifier: Modifier = Modifier,
    intensity: Float = 1f
) {
    val transition = rememberInfiniteTransition(label = "fog")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Restart),
        label = "fog_t"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_000), RepeatMode.Reverse),
        label = "fog_breathe"
    )

    // Wisps are larger than the layer; clipping keeps them from leaking past its edges.
    Canvas(modifier = modifier.clipToBounds()) {
        Wisps.forEach { wisp -> drawWisp(wisp, t, breathe * intensity, tint) }
    }
}

private fun DrawScope.drawWisp(wisp: Wisp, time: Float, intensity: Float, tint: Color) {
    val unit = max(size.width, size.height)
    val center = Offset(
        x = size.width * (wisp.x + wisp.dx * sin(time + wisp.phase)),
        y = size.height * (wisp.y + wisp.dy * cos(time * 2f + wisp.phase))
    )
    val radius = unit * wisp.radius
    // Part of the mist is paler, so the fog is not a flat single-color glow.
    val color = lerp(tint, Color(0xFFDDE6FF), wisp.mix)
    val a = (wisp.alpha * intensity).coerceIn(0f, 1f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = a),
            0.45f to color.copy(alpha = a * 0.35f),
            1f to Color.Transparent,
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/** Fades the content to transparent towards the bottom, starting at [from] of its height. */
fun Modifier.fadeOutBottom(from: Float = 0.55f): Modifier = this
    .clipToBounds()
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(from to Color.Black, 1f to Color.Transparent),
            blendMode = BlendMode.DstIn
        )
    }
