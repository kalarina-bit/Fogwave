package cc.skysparkle.fogwave.ui.components

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LightLogoBackdrop = Color(0xFFECE8F1)

// Dark station logos are unreadable on the dark tile, so they get a light backdrop.
@Composable
@ReadOnlyComposable
fun stationTileColor(station: RadioStation): Color =
    if (station.lightBackdrop) LightLogoBackdrop else LocalGeoColors.current.surfaceContainer

// Station logos are small bundled files, so each is decoded once, at its full size and off the main
// thread, and the same bitmap is shared by the station list, the mini player and the now-playing sheet.
// They are read straight from the resource with density scaling off: going through an image loader's
// resource fetcher let release builds decode them a few pixels wide, which showed as a smear.
private object StationLogoCache {
    private val bitmaps = ConcurrentHashMap<Int, ImageBitmap>()

    fun get(@DrawableRes resId: Int): ImageBitmap? = bitmaps[resId]

    fun load(resources: Resources, @DrawableRes resId: Int): ImageBitmap? {
        bitmaps[resId]?.let { return it }
        val options = BitmapFactory.Options().apply { inScaled = false }
        val bitmap = resources.openRawResource(resId).use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return null
        return bitmap.asImageBitmap().also { bitmaps[resId] = it }
    }
}

@Composable
private fun rememberStationLogo(@DrawableRes resId: Int): ImageBitmap? {
    val resources = LocalContext.current.resources
    val logo by produceState(StationLogoCache.get(resId), resources, resId) {
        value = StationLogoCache.get(resId)
            ?: withContext(Dispatchers.IO) { StationLogoCache.load(resources, resId) }
    }
    return logo
}

@Composable
fun StationLogoImage(station: RadioStation, modifier: Modifier = Modifier) {
    val logo = rememberStationLogo(station.iconResId)
    if (logo != null) {
        Image(
            bitmap = logo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun StationArtwork(station: RadioStation, modifier: Modifier = Modifier) {
    StationLogoImage(station, modifier)
}
