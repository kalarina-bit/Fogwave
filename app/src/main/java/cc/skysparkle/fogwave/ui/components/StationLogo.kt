package cc.skysparkle.fogwave.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size

private val LightLogoBackdrop = Color(0xFFECE8F1)

// Dark station logos are unreadable on the dark tile, so they get a light backdrop.
@Composable
@ReadOnlyComposable
fun stationTileColor(station: RadioStation): Color =
    if (station.lightBackdrop) LightLogoBackdrop else LocalGeoColors.current.surfaceContainer

// Decoded off the main thread and kept in Coil's memory cache, so no logo is decoded during a frame.
@Composable
fun StationLogoImage(station: RadioStation, modifier: Modifier = Modifier) {
    AsyncImage(
        model = station.iconResId,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

// Player artwork is decoded at full size under its own cache key: the mini player and the
// now-playing sheet share one decode, which the station list never evicts, and show it without a fade.
@Composable
fun StationArtwork(station: RadioStation, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(context, station.iconResId) {
        ImageRequest.Builder(context)
            .data(station.iconResId)
            .size(Size.ORIGINAL)
            .memoryCacheKey("artwork_${station.iconResId}")
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
