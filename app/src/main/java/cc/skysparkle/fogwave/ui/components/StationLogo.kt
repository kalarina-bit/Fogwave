package cc.skysparkle.fogwave.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors

private val LightLogoBackdrop = Color(0xFFECE8F1)

// Dark station logos are unreadable on the dark tile, so they get a light backdrop.
@Composable
@ReadOnlyComposable
fun stationTileColor(station: RadioStation): Color =
    if (station.lightBackdrop) LightLogoBackdrop else LocalGeoColors.current.surfaceContainer
