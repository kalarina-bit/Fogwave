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

private val LightLogoBackdrop = Color(0xFFECE8F1)

// Dark station logos are unreadable on the dark tile, so they get a light backdrop.
@Composable
@ReadOnlyComposable
fun stationTileColor(station: RadioStation): Color =
    if (station.lightBackdrop) LightLogoBackdrop else LocalGeoColors.current.surfaceContainer

// Use the same explicit Android resource URI as the Media3 session metadata. This avoids
// relying on Coil's implicit Int model handling for drawable-nodpi assets, while still letting
// Coil decode at the actual target size (important for the mini player and full-size artwork).
@Composable
fun StationLogoImage(station: RadioStation, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val artworkUri = remember(context.packageName, station.iconResId) {
        "android.resource://${context.packageName}/${station.iconResId}"
    }

    AsyncImage(
        model = artworkUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
fun StationArtwork(station: RadioStation, modifier: Modifier = Modifier) {
    StationLogoImage(station, modifier)
}
