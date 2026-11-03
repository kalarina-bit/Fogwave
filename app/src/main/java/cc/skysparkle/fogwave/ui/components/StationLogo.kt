package cc.skysparkle.fogwave.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import coil.compose.AsyncImage

private val LightLogoBackdrop = Color(0xFFECE8F1)

// Dark station logos are unreadable on the dark tile, so they get a light backdrop.
@Composable
@ReadOnlyComposable
fun stationTileColor(station: RadioStation): Color =
    if (station.lightBackdrop) LightLogoBackdrop else LocalGeoColors.current.surfaceContainer

// Station list logos.
@Composable
fun StationLogoImage(station: RadioStation, modifier: Modifier = Modifier) {
    AsyncImage(
        model = station.iconResId,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

// Mini player, now-playing sheet and its station lists.
@Composable
fun StationArtwork(station: RadioStation, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = station.iconResId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
