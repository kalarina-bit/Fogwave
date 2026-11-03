package cc.skysparkle.fogwave.data.repository

import android.content.Context
import androidx.core.content.edit
import cc.skysparkle.fogwave.data.model.IconSizeStep
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.data.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class RadioRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val stations: List<RadioStation> = RadioStations

    private val stationIds: Set<String> = RadioStations.mapTo(HashSet()) { it.id }

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

    fun markFavoritesSeen() {
        val copy = _favoritesFlow.value.toSet()
        _seenFavoritesFlow.value = copy
        prefs.edit { putStringSet("seen_favorites", copy) }
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

    fun toggleFavorite(stationId: String) {
        val current = _favoritesFlow.value.toMutableSet()
        if (!current.remove(stationId)) current.add(stationId)
        prefs.edit { putStringSet("favorites", current) }
        _favoritesFlow.value = current
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

    companion object {
        const val PREFS_NAME = "fogwave_prefs"
    }
}
