package cc.skysparkle.fogwave.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import coil.compose.AsyncImage

private val LightLogoBackdrop = Color(0xFFECE8F1)

// Dark station logos are unreadable on the dark tile, so they get a light backdrop.
@Composable
@ReadOnlyComposable
fun stationTileColor(station: RadioStation): Color =
    if (station.lightBackdrop) LightLogoBackdrop else LocalGeoColors.current.surfaceContainer

// Every logo goes through Coil with the size of the box it sits in. Logos live in drawable-nodpi, and a
// full-size decode (painterResource, or Coil with Size.ORIGINAL) leaves them blank or a few pixels wide
// in release builds, so the mini player and the now-playing sheet load them exactly like the station list.
@Composable
fun StationLogoImage(station: RadioStation, modifier: Modifier = Modifier) {
    AsyncImage(
        model = station.iconResId,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
fun StationArtwork(station: RadioStation, modifier: Modifier = Modifier) {
    StationLogoImage(station, modifier)
}
