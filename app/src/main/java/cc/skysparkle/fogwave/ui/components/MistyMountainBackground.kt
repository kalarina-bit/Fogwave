package cc.skysparkle.fogwave.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors

@Composable
fun MistyMountainBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(context) {
        ImageRequest.Builder(context)
            .data(R.drawable.bg_misty_mountain)
            .scale(Scale.FILL)
            .crossfade(200)
            .build()
    }

    Box(modifier = modifier.fillMaxSize().background(LocalGeoColors.current.background)) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        val scrimBase = LocalGeoColors.current.background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scrimBase.copy(alpha = 0.62f),
                            scrimBase.copy(alpha = 0.48f),
                            scrimBase.copy(alpha = 0.52f),
                            scrimBase.copy(alpha = 0.78f)
                        )
                    )
                )
        )
    }
}
