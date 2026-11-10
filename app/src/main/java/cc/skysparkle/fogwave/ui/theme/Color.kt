package cc.skysparkle.fogwave.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class GeoColors(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val accentLavender: Color,
    val onAccentLavender: Color,
    val deepPurple: Color,
    val liveRed: Color,
    val redFavorite: Color
)

// Dark take on the "Rad.io" layout: deep navy surfaces with an electric-blue accent.
val DarkGeoColors = GeoColors(
    background = Color(0xFF0D1016),
    onBackground = Color(0xFFEEF1F7),
    surface = Color(0xFF0D1016),
    onSurface = Color(0xFFEEF1F7),
    surfaceContainer = Color(0xFF161A23),
    surfaceContainerHigh = Color(0xFF1F2430),
    onSurfaceVariant = Color(0xFF98A1B3),
    outline = Color(0xFF5C6577),
    outlineVariant = Color(0xFF272D3A),
    primary = Color(0xFF7C9DFF),
    onPrimary = Color(0xFF0B1A47),
    primaryContainer = Color(0xFF1F2D5C),
    onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = Color(0xFFB2C1E6),
    onSecondary = Color(0xFF18233F),
    secondaryContainer = Color(0xFF283350),
    onSecondaryContainer = Color(0xFFDCE4FF),
    accentLavender = Color(0xFF3466F6),
    onAccentLavender = Color(0xFFFFFFFF),
    deepPurple = Color(0xFFF3F6FF),
    liveRed = Color(0xFFFF4D5E),
    redFavorite = Color(0xFFFF6B81)
)

val LocalGeoColors = staticCompositionLocalOf { DarkGeoColors }
