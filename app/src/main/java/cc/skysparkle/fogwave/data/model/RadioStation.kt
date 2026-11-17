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
    val lightBackdrop: Boolean = false,
    /** FM frequencies in the biggest cities; empty for stations that are only online. */
    val frequencies: List<FmFrequency> = emptyList(),
    /** Station website with its shows and schedule. */
    val website: String? = null
)

/** One FM transmitter: the city and the frequency in MHz. */
@Immutable
data class FmFrequency(val city: City, val mhz: Double)

enum class City(@param:StringRes val labelRes: Int) {
    VILNIUS(R.string.city_vilnius),
    KAUNAS(R.string.city_kaunas),
    KLAIPEDA(R.string.city_klaipeda),
    RIGA(R.string.city_riga)
}

enum class Genre(@param:StringRes val labelRes: Int) {
    HITS(R.string.genre_hits),
    POP(R.string.genre_pop),
    DANCE(R.string.genre_dance),
    OLDIES(R.string.genre_oldies),
    ROCK(R.string.genre_rock),
    EASY_LISTENING(R.string.genre_easy_listening),
    LITHUANIAN(R.string.genre_lithuanian),
    NEWS_TALK(R.string.genre_news_talk),
    LATVIAN(R.string.genre_latvian)
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

