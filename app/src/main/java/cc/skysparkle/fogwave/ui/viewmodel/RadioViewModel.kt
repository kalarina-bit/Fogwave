package cc.skysparkle.fogwave.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.skysparkle.fogwave.data.model.AppTab
import cc.skysparkle.fogwave.data.model.ExtraPlayerTab
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.data.model.TrackEntry
import cc.skysparkle.fogwave.data.repository.RadioRepository
import cc.skysparkle.fogwave.player.AudioEffectsController
import cc.skysparkle.fogwave.player.AudioEffectsState
import cc.skysparkle.fogwave.player.PlayerState
import cc.skysparkle.fogwave.player.RadioPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class RadioUiState(
    val stations: List<RadioStation> = emptyList(),
    val currentStation: RadioStation? = null,
    val playerState: PlayerState = PlayerState.Idle,
    val favorites: Set<String> = emptySet(),
    val recents: List<RadioStation> = emptyList(),
    val selectedTab: AppTab = AppTab.RADIO,
    val isNowPlayingOpen: Boolean = false,
    val isEqualizerPopOpen: Boolean = false,
    val activeExtraTab: ExtraPlayerTab = ExtraPlayerTab.HISTORY,
    val audioEffects: AudioEffectsState = AudioEffectsState(),
    val isMiniPlayerHidden: Boolean = false,
    val unseenFavoritesCount: Int = 0,
    val nowPlayingTitle: String? = null,
    val trackHistory: List<TrackEntry> = emptyList()
)

class RadioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RadioRepository(application.applicationContext)
    private val playerManager = RadioPlayerManager(application.applicationContext)

    // Sorted once per launch so the grid and next/previous stay stable during a session.
    private val _stations = MutableStateFlow(repository.stationsSortedByPopularity())
    private val stations: List<RadioStation> get() = _stations.value

    private val initialStation: RadioStation? = run {
        val lastId = repository.getLastStationId()
        stations.find { it.id == lastId } ?: stations.firstOrNull()
    }

    private val _currentStation = MutableStateFlow(initialStation)
    private val _selectedTab = MutableStateFlow(AppTab.RADIO)
    private val _isNowPlayingOpen = MutableStateFlow(false)
    private val _isEqualizerPopOpen = MutableStateFlow(false)
    private val _activeExtraTab = MutableStateFlow(ExtraPlayerTab.HISTORY)
    private val _isMiniPlayerHidden = MutableStateFlow(false)

    // Newest first; kept for this session only.
    private val _trackHistory = MutableStateFlow<List<TrackEntry>>(emptyList())

    private val _showBatteryOptimizationPrompt = MutableStateFlow(false)
    val showBatteryOptimizationPrompt: StateFlow<Boolean> = _showBatteryOptimizationPrompt.asStateFlow()

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun dismissBatteryOptimizationPrompt() {
        repository.markBatteryOptimizationPromptSeen()
        _showBatteryOptimizationPrompt.value = false
    }

    val uiState: StateFlow<RadioUiState> = combine(
        _currentStation,
        playerManager.playerState,
        repository.favoritesFlow,
        repository.recentsFlow,
        _selectedTab,
        _isNowPlayingOpen,
        _isEqualizerPopOpen,
        _activeExtraTab,
        AudioEffectsController.state,
        _isMiniPlayerHidden,
        repository.seenFavoritesFlow,
        _stations,
        playerManager.nowPlayingTitle,
        _trackHistory
    ) { params ->
        val current = params[0] as? RadioStation
        val pState = params[1] as PlayerState
        @Suppress("UNCHECKED_CAST")
        val favs = params[2] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val recentsIds = params[3] as List<String>
        val tab = params[4] as AppTab
        val nowPlaying = params[5] as Boolean
        val eqPop = params[6] as Boolean
        val extraTab = params[7] as ExtraPlayerTab
        val effects = params[8] as AudioEffectsState
        val miniHidden = params[9] as Boolean
        @Suppress("UNCHECKED_CAST")
        val seenFavs = params[10] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val orderedStations = params[11] as List<RadioStation>
        val songTitle = params[12] as String?
        @Suppress("UNCHECKED_CAST")
        val history = params[13] as List<TrackEntry>

        val byId = orderedStations.associateBy { it.id }
        val recentsList = recentsIds.mapNotNull { byId[it] }

        RadioUiState(
            stations = orderedStations,
            currentStation = current,
            playerState = pState,
            favorites = favs,
            recents = recentsList,
            selectedTab = tab,
            isNowPlayingOpen = nowPlaying,
            isEqualizerPopOpen = eqPop,
            activeExtraTab = extraTab,
            audioEffects = effects,
            isMiniPlayerHidden = miniHidden,
            unseenFavoritesCount = favs.count { it !in seenFavs },
            nowPlayingTitle = songTitle,
            trackHistory = history
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RadioUiState(
            stations = stations,
            currentStation = initialStation,
            favorites = repository.favoritesFlow.value,
            audioEffects = AudioEffectsController.state.value,
            unseenFavoritesCount = repository.favoritesFlow.value.count { it !in repository.seenFavoritesFlow.value }
        )
    )

    init {
        viewModelScope.launch {
            playerManager.currentMediaId.collect { id ->
                if (id != null && id != _currentStation.value?.id) {
                    stations.find { it.id == id }?.let { onAir ->
                        _currentStation.value = onAir
                        repository.setLastStationId(onAir.id)
                    }
                }
            }
        }
        viewModelScope.launch {
            playerManager.nowPlayingTitle.collect { title ->
                val stationId = playerManager.currentMediaId.value ?: _currentStation.value?.id
                if (title != null && stationId != null) recordTrack(stationId, title)
            }
        }
    }

    private fun recordTrack(stationId: String, title: String) {
        val history = _trackHistory.value
        val last = history.firstOrNull { it.stationId == stationId }
        if (last?.title == title) return
        _trackHistory.value = (listOf(TrackEntry(stationId, title, System.currentTimeMillis())) + history)
            .take(MAX_TRACK_HISTORY)
    }

    /** Tap on a station tile: open the player for the station already on air, otherwise tune in. */
    fun onStationClick(station: RadioStation) {
        val state = playerManager.playerState.value
        val onAir = station.id == _currentStation.value?.id &&
            (state is PlayerState.Playing || state is PlayerState.Connecting)
        if (onAir) {
            _isMiniPlayerHidden.value = false
            openNowPlaying()
        } else {
            selectStation(station)
        }
    }

    fun selectStation(station: RadioStation) {
        _currentStation.value = station
        _isMiniPlayerHidden.value = false
        repository.setLastStationId(station.id)
        repository.pushRecent(station.id)
        repository.recordStationPlayed(station.id)

        if (!repository.hasSeenBatteryOptimizationPrompt() &&
            !isIgnoringBatteryOptimizations(getApplication<Application>())
        ) {
            _showBatteryOptimizationPrompt.value = true
        }
        playerManager.playStation(station)
    }

    fun hideMiniPlayer() {
        _isMiniPlayerHidden.value = true
    }

    fun togglePlay() {
        val current = _currentStation.value ?: return
        when (playerManager.playerState.value) {
            // Start from the station shown in the UI: it is kept in sync with whatever the
            // session is playing, including stations picked from Android Auto or the notification.
            is PlayerState.Idle, is PlayerState.Error -> playerManager.playStation(current)
            else -> playerManager.togglePlay()
        }
    }

    fun nextStation() {
        val list = stations
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentStation.value?.id }
        val nextIndex = if (currentIndex in 0 until list.size - 1) currentIndex + 1 else 0
        selectStation(list[nextIndex])
    }

    fun previousStation() {
        val list = stations
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentStation.value?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        selectStation(list[prevIndex])
    }

    fun toggleFavorite(stationId: String? = _currentStation.value?.id) {
        if (stationId != null) {
            repository.toggleFavorite(stationId)
            if (_selectedTab.value == AppTab.FAVORITES) {
                repository.markFavoritesSeen(repository.favoritesFlow.value)
            }
        }
    }

    fun getSimilarStations(): List<RadioStation> {
        val cur = _currentStation.value ?: return emptyList()
        val curGenres = cur.genres
        val others = stations.filter { it.id != cur.id }

        val similar = others
            .map { s -> s to s.genres.count { it in curGenres } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
        return similar.ifEmpty { others }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        AudioEffectsController.setEqualizerEnabled(enabled)
    }

    fun setEqualizerBand(band: Int, levelMillibel: Int) {
        AudioEffectsController.setBandLevel(band, levelMillibel)
    }

    fun setEqualizerPreset(preset: Int) {
        AudioEffectsController.setPreset(preset)
    }

    fun resetEqualizerBands() {
        AudioEffectsController.state.value.bands.forEach { band ->
            AudioEffectsController.setBandLevel(band.index, 0)
        }
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        AudioEffectsController.setLoudnessEnabled(enabled)
    }

    fun setLoudnessGain(gainMillibel: Float) {
        AudioEffectsController.setLoudnessGain(gainMillibel)
    }

    fun setDynamicsProcessingEnabled(enabled: Boolean) {
        AudioEffectsController.setDynamicsProcessingEnabled(enabled)
    }

    fun setSelectedTab(tab: AppTab) {
        _selectedTab.value = tab
        if (tab == AppTab.FAVORITES) {
            repository.markFavoritesSeen(repository.favoritesFlow.value)
        }
    }

    fun openNowPlaying() {
        _isNowPlayingOpen.value = true
    }

    fun closeNowPlaying() {
        _isNowPlayingOpen.value = false
        _isEqualizerPopOpen.value = false
    }

    fun toggleEqualizerPop() {
        _isEqualizerPopOpen.value = !_isEqualizerPopOpen.value
    }

    fun closeEqualizerPop() {
        _isEqualizerPopOpen.value = false
    }

    fun setActiveExtraTab(tab: ExtraPlayerTab) {
        _activeExtraTab.value = tab
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }

    private companion object {
        const val MAX_TRACK_HISTORY = 60
    }
}
