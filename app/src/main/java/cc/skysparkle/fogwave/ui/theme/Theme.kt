package cc.skysparkle.fogwave.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

private fun GeoColors.toColorScheme(): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = outlineVariant,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline
)

/** Always dark; the colours follow the style picked in About (see [AppThemeState]). */
@Composable
fun FogwaveTheme(content: @Composable () -> Unit) {
    val colors = AppThemeState.colors
    val scheme = remember(colors) { colors.toColorScheme() }
    CompositionLocalProvider(LocalGeoColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}
