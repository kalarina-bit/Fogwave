package cc.skysparkle.fogwave.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.RadioStation
import java.util.Locale

@Composable
@ReadOnlyComposable
fun stationDescription(station: RadioStation): String = stringResource(station.descriptionRes)

@Composable
@ReadOnlyComposable
fun stationGenres(station: RadioStation): String =
    station.genres.map { stringResource(it.labelRes) }.joinToString(", ")

/** FM frequencies one per line («Vilnius · 100.1 MHz»), or «Online only» for internet stations. */
@Composable
@ReadOnlyComposable
fun stationFrequencies(station: RadioStation): String {
    if (station.frequencies.isEmpty()) return stringResource(R.string.info_fm_online_only)
    return station.frequencies.map { f ->
        // Decimal separator of the current language: 100.1 in English, 100,1 in Lithuanian
        stringResource(R.string.info_fm_line, stringResource(f.city.labelRes), String.format(Locale.getDefault(), "%.1f", f.mhz))
    }.joinToString("\n")
}

/** Website address without the scheme and «www.», for display: «zipfm.lt». */
fun displayHost(url: String): String = url.substringAfter("://").removePrefix("www.").trimEnd('/')

/** Opens [url] in the user's browser; silently does nothing when no browser is installed. */
fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
    }
}
