package cc.skysparkle.fogwave.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import cc.skysparkle.fogwave.R

@Immutable
data class RadioStation(
    val id: String,
    val name: String,
    @param:StringRes val descriptionRes: Int,
    val streamUrl: String,
    @param:DrawableRes val iconResId: Int,
    val genres: List<Genre>,
    val bitrate: String,
    val lightBackdrop: Boolean = false
)

enum class Genre(@param:StringRes val labelRes: Int) {
    HITS(R.string.genre_hits),
    POP(R.string.genre_pop),
    DANCE(R.string.genre_dance),
    OLDIES(R.string.genre_oldies),
    ROCK(R.string.genre_rock),
    EASY_LISTENING(R.string.genre_easy_listening),
    LITHUANIAN(R.string.genre_lithuanian)
}

enum class ViewMode {
    ICONS,
    TILES,
    LIST,
    DETAILS
}

enum class IconSizeStep(val sizeDp: Int) {
    S(64),
    M(96),
    L(132),
    XL(176)
}

enum class AppTab(@param:StringRes val titleRes: Int) {
    RADIO(R.string.tab_radio),
    FAVORITES(R.string.tab_favorites),
    ABOUT(R.string.tab_about)
}

enum class ExtraPlayerTab(@param:StringRes val titleRes: Int) {
    RECENT(R.string.extra_tab_recent),
    SIMILAR(R.string.extra_tab_similar),
    INFO(R.string.extra_tab_info)
}
