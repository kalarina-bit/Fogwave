package cc.skysparkle.fogwave.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import cc.skysparkle.fogwave.R

/** Colour styles, the same set as in Matewave. Every style is dark. */
enum class AppStyle(@param:StringRes val titleRes: Int) {
    FOGWAVE(R.string.app_style_fogwave),
    LAVENDER(R.string.app_style_lavender),
    PEACH(R.string.app_style_peach),
    MINT(R.string.app_style_mint),
    SKY(R.string.app_style_sky),
    DEVICE(R.string.app_style_device);

    companion object {
        /** Wallpaper colours exist from Android 12. */
        val deviceSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        val available: List<AppStyle> get() = entries.filter { it != DEVICE || deviceSupported }
    }
}

/** The selected style, kept in compose state so every screen recolours at once. */
object AppThemeState {
    private const val PREFS = "app_theme"
    private const val KEY_STYLE = "style"
    private const val KEY_STATION_FOG = "station_fog"

    var style by mutableStateOf(AppStyle.FOGWAVE)
        private set

    var colors by mutableStateOf(DarkGeoColors)
        private set

    /** Fog and the disc ring take the playing station's colour instead of the style's. */
    var stationFog by mutableStateOf(false)
        private set

    /** Reads the saved settings; call before the first frame. */
    fun load(context: Context) {
        val prefs = prefs(context)
        val saved = prefs.getString(KEY_STYLE, null)
        stationFog = prefs.getBoolean(KEY_STATION_FOG, false)
        apply(context, AppStyle.entries.firstOrNull { it.name == saved } ?: AppStyle.FOGWAVE)
    }

    fun select(context: Context, style: AppStyle) {
        prefs(context).edit().putString(KEY_STYLE, style.name).apply()
        apply(context, style)
    }

    fun setStationFog(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_STATION_FOG, enabled).apply()
        stationFog = enabled
    }

    /** Colours of a style, also used for the previews in the picker. */
    fun colorsFor(context: Context, style: AppStyle): GeoColors = when (style) {
        AppStyle.FOGWAVE -> DarkGeoColors
        AppStyle.LAVENDER -> styleColors(Color(0xFFC8B6FF), Color(0xFF1E1630), Color(0xFF0F0E14), Color(0xFF1A1822), Color(0xFF25222F))
        AppStyle.PEACH -> styleColors(Color(0xFFFFB59E), Color(0xFF2A1710), Color(0xFF12100E), Color(0xFF1D1A17), Color(0xFF28231F))
        AppStyle.MINT -> styleColors(Color(0xFF9EE7C8), Color(0xFF0F231B), Color(0xFF0D110F), Color(0xFF171C1A), Color(0xFF202724))
        AppStyle.SKY -> styleColors(Color(0xFFA5CBFF), Color(0xFF0F1A2A), Color(0xFF0D1014), Color(0xFF171B21), Color(0xFF20252D))
        AppStyle.DEVICE -> deviceColors(context)
    }

    private fun apply(context: Context, requested: AppStyle) {
        val style = if (requested == AppStyle.DEVICE && !AppStyle.deviceSupported) AppStyle.FOGWAVE else requested
        this.style = style
        colors = colorsFor(context, style)
    }

    private fun deviceColors(context: Context): GeoColors {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return DarkGeoColors
        val scheme = dynamicDarkColorScheme(context)
        return styleColors(
            accent = scheme.primary,
            onAccent = scheme.onPrimary,
            background = scheme.background,
            surface1 = scheme.surfaceContainer,
            surface2 = scheme.surfaceContainerHigh
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/**
 * Builds the full set from a style's few colours. Pastel styles use their accent for buttons too,
 * with dark text on top, like in Matewave. The live dot and the favourite heart keep their own red.
 */
private fun styleColors(
    accent: Color,
    onAccent: Color,
    background: Color,
    surface1: Color,
    surface2: Color
): GeoColors = DarkGeoColors.copy(
    background = background,
    surface = background,
    surfaceContainer = surface1,
    surfaceContainerHigh = surface2,
    onSurfaceVariant = lerp(Color(0xFF9AA0AA), accent, 0.12f),
    outline = lerp(surface2, Color.White, 0.3f),
    outlineVariant = lerp(surface2, Color.White, 0.06f),
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = lerp(background, accent, 0.22f),
    onPrimaryContainer = lerp(accent, Color.White, 0.75f),
    secondary = lerp(accent, Color.White, 0.4f),
    onSecondary = onAccent,
    secondaryContainer = lerp(surface2, accent, 0.18f),
    onSecondaryContainer = lerp(accent, Color.White, 0.75f),
    accentLavender = accent,
    onAccentLavender = onAccent,
    deepPurple = lerp(Color.White, accent, 0.06f)
)
