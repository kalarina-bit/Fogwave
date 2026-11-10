package cc.skysparkle.fogwave.ui.components

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import cc.skysparkle.fogwave.data.model.RadioStation
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What the UI needs to know about a station logo: its brand color (for the fog and the ring)
 * and where the visible part of the logo sits, so wide wordmarks can be enlarged inside a circle.
 */
@Immutable
data class StationArt(
    val accent: Color?,
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    /** Logos with their own square background are shown edge to edge instead of being shrunk. */
    val isFullBleed: Boolean get() = right - left > 0.97f && bottom - top > 0.97f

    /** Scale that fits the visible part of the logo into a circle of [fill] of its diameter. */
    fun circleScale(fill: Float = 0.86f): Float {
        if (isFullBleed) return 1f
        val w = right - left
        val h = bottom - top
        return (fill / sqrt(w * w + h * h)).coerceIn(0.5f, 1.7f)
    }

    /** Scale that fits the visible part of the logo into [fill] of a square tile. */
    fun squareScale(fill: Float = 0.84f): Float {
        if (isFullBleed) return 1f
        return minOf(fill / (right - left), fill / (bottom - top)).coerceIn(1f, 1.5f)
    }

    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

private val cache = ConcurrentHashMap<Int, StationArt>()

@Composable
fun rememberStationArt(station: RadioStation): StationArt? {
    val resources = LocalContext.current.resources
    val art by produceState(initialValue = cache[station.iconResId], station.iconResId) {
        value = cache[station.iconResId] ?: withContext(Dispatchers.Default) {
            analyzeLogo(resources, station.iconResId).also { cache[station.iconResId] = it }
        }
    }
    return art
}

private fun analyzeLogo(resources: Resources, resId: Int): StationArt {
    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
    val bitmap = BitmapFactory.decodeResource(resources, resId, options) ?: return StationArt(accent = null)
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    bitmap.recycle()

    var minX = w
    var minY = h
    var maxX = -1
    var maxY = -1
    // Hue is averaged on the circle, weighted by how vivid each pixel is.
    var hueX = 0.0
    var hueY = 0.0
    var weight = 0.0
    var opaque = 0
    val hsl = FloatArray(3)

    for (y in 0 until h) {
        for (x in 0 until w) {
            val c = pixels[y * w + x]
            val alpha = c ushr 24
            if (alpha <= 24) continue
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
            opaque++

            ColorUtils.colorToHSL(c, hsl)
            val vivid = hsl[1] * (1f - abs(hsl[2] - 0.5f) * 2f)
            if (vivid < 0.25f) continue
            val wgt = vivid.toDouble() * (alpha / 255.0)
            val rad = Math.toRadians(hsl[0].toDouble())
            hueX += cos(rad) * wgt
            hueY += sin(rad) * wgt
            weight += wgt
        }
    }

    if (maxX < 0) return StationArt(accent = null)

    // Mostly black, white or grey logos keep the app's own blue.
    val accent = if (opaque > 0 && weight / opaque > 0.08) {
        var hue = Math.toDegrees(atan2(hueY, hueX)).toFloat()
        if (hue < 0f) hue += 360f
        // Same saturation and lightness for every station, so only the hue changes.
        Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.78f, 0.6f)))
    } else {
        null
    }

    return StationArt(
        accent = accent,
        left = minX / w.toFloat(),
        top = minY / h.toFloat(),
        right = (maxX + 1) / w.toFloat(),
        bottom = (maxY + 1) / h.toFloat()
    )
}

/** Station color, or the app accent when the logo has none. */
fun StationArt?.accentOr(fallback: Color): Color = this?.accent ?: fallback
