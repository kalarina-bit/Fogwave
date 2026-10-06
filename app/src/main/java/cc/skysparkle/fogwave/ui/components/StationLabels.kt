package cc.skysparkle.fogwave.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import cc.skysparkle.fogwave.data.model.RadioStation

@Composable
@ReadOnlyComposable
fun stationDescription(station: RadioStation): String = stringResource(station.descriptionRes)

@Composable
@ReadOnlyComposable
fun stationGenres(station: RadioStation): String =
    station.genres.map { stringResource(it.labelRes) }.joinToString(", ")

/** Opens [url] in the user's browser; silently does nothing when no browser is installed. */
fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
    }
}
