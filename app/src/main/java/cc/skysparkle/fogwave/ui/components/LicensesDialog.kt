package cc.skysparkle.fogwave.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cc.skysparkle.fogwave.BuildConfig
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors

private data class LicenseRow(
    val name: String,
    val attribution: String,
    val license: String,
    val url: String,
    val note: String? = null
)

private const val APACHE = "Apache License 2.0"
private const val AOSP = "The Android Open Source Project"
private const val BROADCASTER = "\u00A9 broadcaster"

// Versions come from gradle/libs.versions.toml through BuildConfig, so this list cannot go stale.
private val LIBRARIES = listOf(
    LicenseRow("Jetpack Compose (UI, Foundation, Material 3)", AOSP, APACHE, "https://developer.android.com/jetpack/androidx/releases/compose", "BOM ${BuildConfig.COMPOSE_BOM_VERSION}"),
    LicenseRow("AndroidX Activity Compose", AOSP, APACHE, "https://developer.android.com/jetpack/androidx/releases/activity", BuildConfig.ACTIVITY_VERSION),
    LicenseRow("AndroidX Core KTX", AOSP, APACHE, "https://developer.android.com/jetpack/androidx/releases/core", BuildConfig.CORE_KTX_VERSION),
    LicenseRow("AndroidX Lifecycle (ViewModel + Runtime Compose)", AOSP, APACHE, "https://developer.android.com/jetpack/androidx/releases/lifecycle", BuildConfig.LIFECYCLE_VERSION),
    LicenseRow("AndroidX Media3 (ExoPlayer, MediaSession)", AOSP, APACHE, "https://developer.android.com/jetpack/androidx/releases/media3", BuildConfig.MEDIA3_VERSION),
    LicenseRow("AndroidX \u2014 other libraries pulled in by the ones above", AOSP, APACHE, "https://developer.android.com/jetpack/androidx"),
    LicenseRow("Guava (used by Media3)", "Google", APACHE, "https://github.com/google/guava"),
    LicenseRow("kotlinx.coroutines", "JetBrains", APACHE, "https://github.com/Kotlin/kotlinx.coroutines", BuildConfig.COROUTINES_VERSION),
    LicenseRow("Kotlin Standard Library", "JetBrains", APACHE, "https://github.com/JetBrains/kotlin", BuildConfig.KOTLIN_VERSION),
    LicenseRow("Coil", "Coil Contributors", APACHE, "https://github.com/coil-kt/coil", BuildConfig.COIL_VERSION),
    LicenseRow("OkHttp & Okio (used by Coil)", "Square, Inc.", APACHE, "https://square.github.io/okhttp/")
)

private val ASSETS = listOf(
    LicenseRow("MingCute Icon (interface icons)", "MingCute Design, via IconBuddy", APACHE, "https://iconbuddy.com/mingcute")
)

private val STATIONS = listOf(
    LicenseRow("ZIP FM", "Radiocentras group", BROADCASTER, "https://www.zipfm.lt/"),
    LicenseRow("Power Hit Radio", "TV3 Group", BROADCASTER, "https://powerhitradio.tv3.lt/"),
    LicenseRow("Power Hit Radio Gold", "TV3 Group", BROADCASTER, "https://powerhitradio.tv3.lt/"),
    LicenseRow("Rock FM", "Radiocentras group", BROADCASTER, "https://www.radijas.lt/rockfm"),
    LicenseRow("Relax FM", "Radiocentras group", BROADCASTER, "https://www.radijas.lt/relaxfm"),
    LicenseRow("Lietus", "M-1 group", BROADCASTER, "https://www.m-1.fm/"),
    LicenseRow("M-1", "M-1 group", BROADCASTER, "https://www.m-1.fm/"),
    LicenseRow("M-1 Plius", "M-1 group", BROADCASTER, "https://www.m-1.fm/"),
    LicenseRow("Gold FM", "Gold FM", BROADCASTER, "https://www.goldfm.lt/"),
    LicenseRow("RC", "Radiocentras group", BROADCASTER, "https://www.rc.lt/")
)

@Composable
fun LicensesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(LocalGeoColors.current.surfaceContainer)
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.licenses_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = LocalGeoColors.current.deepPurple
                )
            )
            Text(
                text = stringResource(R.string.licenses_intro),
                style = MaterialTheme.typography.bodySmall.copy(color = LocalGeoColors.current.onSurfaceVariant),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LicenseGroup(stringResource(R.string.licenses_group_libraries), LIBRARIES, context)
                LicenseGroup(stringResource(R.string.licenses_group_assets), ASSETS, context)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LicenseGroup(stringResource(R.string.licenses_group_stations), STATIONS, context)
                    Text(
                        text = stringResource(R.string.licenses_stations_hint),
                        style = MaterialTheme.typography.labelSmall.copy(color = LocalGeoColors.current.onSurfaceVariant),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(R.string.close_content_desc),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = LocalGeoColors.current.deepPurple
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun LicenseGroup(title: String, rows: List<LicenseRow>, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = LocalGeoColors.current.primary
            )
        )
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { context.openUrl(row.url) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    val meta = listOfNotNull(row.attribution, row.note).joinToString(" \u00B7 ")
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall.copy(color = LocalGeoColors.current.onSurfaceVariant)
                    )
                    Text(
                        text = row.license,
                        style = MaterialTheme.typography.labelSmall.copy(color = LocalGeoColors.current.primary)
                    )
                }
                AppIcon(
                    resId = R.drawable.ic_open_in_new,
                    tint = LocalGeoColors.current.onSurfaceVariant,
                    size = 14.dp
                )
            }
        }
    }
}
