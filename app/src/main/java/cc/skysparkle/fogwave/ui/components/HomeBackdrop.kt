package cc.skysparkle.fogwave.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors

/**
 * Misty peak at the top of the home screen with drifting fog in the playing station's color.
 * [animate] is turned off while the home screen is covered, so the hidden fog does not keep drawing.
 * [visibility] is read in the draw phase (0 = gone, 1 = fully shown) so scrolling never recomposes it.
 */
@Composable
fun HomeBackdrop(
    tint: Color,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    visibility: () -> Float = { 1f }
) {
    val background = LocalGeoColors.current.background
    // Cool, slightly desaturated photo so the colored fog and the UI stay in front.
    val muted = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.35f) })
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
            .graphicsLayer { alpha = visibility().coerceIn(0f, 1f) }
            // Everything (photo and fog) melts into the page instead of ending at a hard edge.
            .fadeOutBottom(from = 0.45f)
    ) {
        Image(
            painter = painterResource(R.drawable.bg_misty_mountain),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            colorFilter = muted,
            alpha = 0.5f,
            modifier = Modifier.fillMaxSize()
        )
        FogLayer(tint = tint, intensity = 0.6f, animate = animate, modifier = Modifier.fillMaxSize())
        // Darker top keeps the header readable over the photo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to background.copy(alpha = 0.45f),
                        0.35f to Color.Transparent
                    )
                )
        )
    }
}
