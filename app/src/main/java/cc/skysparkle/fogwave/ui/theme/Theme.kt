package cc.skysparkle.fogwave.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = with(DarkGeoColors) {
    darkColorScheme(
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
}

@Composable
fun FogwaveTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGeoColors provides DarkGeoColors) {
        MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
    }
}
