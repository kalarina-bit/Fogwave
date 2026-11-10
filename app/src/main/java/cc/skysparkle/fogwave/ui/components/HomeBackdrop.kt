package cc.skysparkle.fogwave.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale

/**
 * Misty peak at the top of the home screen with drifting fog in the playing station's color.
 * [visibility] is read in the draw phase (0 = gone, 1 = fully shown) so scrolling never recomposes it.
 */
@Composable
fun HomeBackdrop(
    tint: Color,
    modifier: Modifier = Modifier,
    visibility: () -> Float = { 1f }
) {
    val context = LocalContext.current
    val background = LocalGeoColors.current.background
    val request = remember(context) {
        ImageRequest.Builder(context)
            .data(R.drawable.bg_misty_mountain)
            .scale(Scale.FILL)
            .crossfade(300)
            .build()
    }
    // Cool, slightly desaturated photo so the colored fog and the UI stay in front.
    val muted = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.35f) })
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
            .graphicsLayer { alpha = visibility().coerceIn(0f, 1f) }
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            colorFilter = muted,
            alpha = 0.5f,
            modifier = Modifier.fillMaxSize()
        )
        FogLayer(tint = tint, intensity = 0.6f, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to background.copy(alpha = 0.45f),
                        0.35f to background.copy(alpha = 0.15f),
                        0.7f to background.copy(alpha = 0.7f),
                        1f to background
                    )
                )
        )
    }
}
