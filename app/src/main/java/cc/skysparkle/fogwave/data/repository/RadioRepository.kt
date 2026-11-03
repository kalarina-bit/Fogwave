package cc.skysparkle.fogwave.data.repository

import android.content.Context
import androidx.core.content.edit
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.Genre
import cc.skysparkle.fogwave.data.model.IconSizeStep
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.data.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class RadioRepository(context: Context) {
    private val prefs = context.getSharedPreferences("fogwave_prefs", Context.MODE_PRIVATE)

    val stations: List<RadioStation> = listOf(
        RadioStation(
            id = "zip",
            name = "ZIP FM",
            descriptionRes = R.string.station_desc_zip,
            streamUrl = "https://stream4.radijas.lt/zipfm",
            iconResId = R.drawable.ic_station_zipfm,
            genres = listOf(Genre.HITS, Genre.POP, Genre.LITHUANIAN),
            bitrate = "MP3"
        ),
        RadioStation(
            id = "powerhit",
            name = "Power Hit Radio",
            descriptionRes = R.string.station_desc_powerhit,
            // Stable revma entry point: it redirects to a fresh node URL with a short-lived token.
            streamUrl = "https://stream.rcs.revma.com/f31w7e0fveuvv",
            iconResId = R.drawable.ic_station_powerhit,
            genres = listOf(Genre.DANCE, Genre.POP, Genre.HITS),
            bitrate = "AAC"
        ),
        RadioStation(
            id = "powerhitgold",
            name = "Power Hit Radio Gold",
            descriptionRes = R.string.station_desc_powerhitgold,
            streamUrl = "https://stream.rcs.revma.com/asdpz3qfbhuvv",
            iconResId = R.drawable.ic_station_powerhitgold,
            genres = listOf(Genre.DANCE, Genre.OLDIES, Genre.HITS),
            bitrate = "AAC"
        ),
        RadioStation(
            id = "rock",
            name = "Rock FM",
            descriptionRes = R.string.station_desc_rock,
            streamUrl = "https://stream4.radijas.lt/rockfm",
            iconResId = R.drawable.ic_station_rockfm,
            genres = listOf(Genre.ROCK, Genre.LITHUANIAN),
            bitrate = "MP3",
            lightBackdrop = true
        ),
        RadioStation(
            id = "relax",
            name = "Relax FM",
            descriptionRes = R.string.station_desc_relax,
            streamUrl = "https://stream4.radijas.lt/relaxfm",
            iconResId = R.drawable.ic_station_relaxfm,
            genres = listOf(Genre.EASY_LISTENING),
            bitrate = "MP3"
        ),
        RadioStation(
            id = "lietus",
            name = "Lietus",
            descriptionRes = R.string.station_desc_lietus,
            streamUrl = "https://stream.m-1.fm/lietus/aacp64",
            iconResId = R.drawable.ic_station_lietus,
            genres = listOf(Genre.LITHUANIAN, Genre.POP),
            bitrate = "AAC 64 kbps",
            lightBackdrop = true
        ),
        RadioStation(
            id = "m1",
            name = "M-1",
            descriptionRes = R.string.station_desc_m1,
            streamUrl = "https://stream.m-1.fm/m1/aacp64",
            iconResId = R.drawable.ic_station_m1,
            genres = listOf(Genre.POP, Genre.HITS),
            bitrate = "AAC 64 kbps"
        ),
        RadioStation(
            id = "plus",
            name = "M-1 Plius",
            descriptionRes = R.string.station_desc_plus,
            streamUrl = "https://radio.m-1.fm/m1plius/aacp64",
            iconResId = R.drawable.ic_station_m1plus,
            genres = listOf(Genre.POP, Genre.OLDIES),
            bitrate = "AAC 64 kbps",
            lightBackdrop = true
        ),
        RadioStation(
            id = "goldfm",
            name = "Gold FM",
            descriptionRes = R.string.station_desc_goldfm,
            streamUrl = "https://stream.goldfm.lt/goldfm.aac",
            iconResId = R.drawable.ic_station_goldfm,
            genres = listOf(Genre.OLDIES, Genre.HITS, Genre.LITHUANIAN),
            bitrate = "AAC"
        ),
        RadioStation(
            id = "rc",
            name = "RC",
            descriptionRes = R.string.station_desc_rc,
            streamUrl = "https://stream1.rc.lt/rc128.mp3",
            iconResId = R.drawable.ic_station_rc,
            genres = listOf(Genre.HITS, Genre.LITHUANIAN),
            bitrate = "MP3 128 kbps",
            lightBackdrop = true
        )
    )

    private val stationIds: Set<String> = stations.mapTo(HashSet()) { it.id }

    private val _favoritesFlow = MutableStateFlow<Set<String>>(loadFavorites())
    val favoritesFlow: StateFlow<Set<String>> = _favoritesFlow.asStateFlow()

    private val _recentsFlow = MutableStateFlow<List<String>>(loadRecents())
    val recentsFlow: StateFlow<List<String>> = _recentsFlow.asStateFlow()

    private val _viewModeFlow = MutableStateFlow(loadViewMode())
    val viewModeFlow: StateFlow<ViewMode> = _viewModeFlow.asStateFlow()

    private val _sizeStepFlow = MutableStateFlow(loadSizeStep())
    val sizeStepFlow: StateFlow<IconSizeStep> = _sizeStepFlow.asStateFlow()

    private val _seenFavoritesFlow = MutableStateFlow(loadSeenFavorites())
    val seenFavoritesFlow: StateFlow<Set<String>> = _seenFavoritesFlow.asStateFlow()

    private fun loadSeenFavorites(): Set<String> {
        prefs.getStringSet("seen_favorites", null)?.let { return it.toSet() }
        return loadFavorites()
    }

    fun markFavoritesSeen(ids: Set<String>) {
        val copy = ids.toSet()
        _seenFavoritesFlow.value = copy
        prefs.edit { putStringSet("seen_favorites", copy); remove("last_seen_favorites_count") }
    }

    fun getLastStationId(): String? {
        return prefs.getString("last_station_id", null)
    }

    fun setLastStationId(stationId: String) {
        prefs.edit { putString("last_station_id", stationId) }
    }

    private fun loadPlayCounts(): MutableMap<String, Int> {
        val raw = prefs.getString("play_counts", null) ?: return mutableMapOf()
        val map = mutableMapOf<String, Int>()
        raw.split(",").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val count = parts[1].toIntOrNull()
                if (count != null) map[parts[0]] = count
            }
        }
        return map
    }

    private fun savePlayCounts(counts: Map<String, Int>) {
        val raw = counts.entries.joinToString(",") { (id, count) -> "$id:$count" }
        prefs.edit { putString("play_counts", raw) }
    }

    fun recordStationPlayed(stationId: String) {
        val counts = loadPlayCounts()
        counts[stationId] = (counts[stationId] ?: 0) + 1
        savePlayCounts(counts)
    }

    fun stationsSortedByPopularity(): List<RadioStation> {
        val counts = loadPlayCounts()
        if (counts.isEmpty()) return stations
        return stations.sortedByDescending { counts[it.id] ?: 0 }
    }

    private fun loadFavorites(): Set<String> {
        // SharedPreferences hands out its internal set, so always work on a copy.
        return prefs.getStringSet("favorites", null)?.filterTo(HashSet()) { it in stationIds } ?: emptySet()
    }

    fun toggleFavorite(stationId: String): Boolean {
        val current = _favoritesFlow.value.toMutableSet()
        val isNowFav = if (current.contains(stationId)) {
            current.remove(stationId)
            false
        } else {
            current.add(stationId)
            true
        }
        prefs.edit { putStringSet("favorites", current) }
        _favoritesFlow.value = current
        return isNowFav
    }

    private fun loadRecents(): List<String> {
        val raw = prefs.getString("recents", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val id = arr.getString(i)
                if (id in stationIds && id !in list) list.add(id)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun pushRecent(stationId: String) {
        val current = _recentsFlow.value.toMutableList()
        current.remove(stationId)
        current.add(0, stationId)
        val trimmed = current.take(10)
        _recentsFlow.value = trimmed

        val arr = JSONArray()
        trimmed.forEach { arr.put(it) }
        prefs.edit { putString("recents", arr.toString()) }
    }

    private fun loadViewMode(): ViewMode {
        val mode = prefs.getString("view_mode", ViewMode.ICONS.name) ?: ViewMode.ICONS.name
        return try {
            ViewMode.valueOf(mode)
        } catch (_: Exception) {
            ViewMode.ICONS
        }
    }

    fun setViewMode(mode: ViewMode) {
        _viewModeFlow.value = mode
        prefs.edit { putString("view_mode", mode.name) }
    }

    private fun loadSizeStep(): IconSizeStep {
        val step = prefs.getString("size_step", IconSizeStep.M.name) ?: IconSizeStep.M.name
        return try {
            IconSizeStep.valueOf(step)
        } catch (_: Exception) {
            IconSizeStep.M
        }
    }

    fun setSizeStep(step: IconSizeStep) {
        _sizeStepFlow.value = step
        prefs.edit { putString("size_step", step.name) }
    }

    fun hasSeenBatteryOptimizationPrompt(): Boolean {
        return prefs.getBoolean("seen_battery_optimization_prompt", false)
    }

    fun markBatteryOptimizationPromptSeen() {
        prefs.edit { putBoolean("seen_battery_optimization_prompt", true) }
    }
}
