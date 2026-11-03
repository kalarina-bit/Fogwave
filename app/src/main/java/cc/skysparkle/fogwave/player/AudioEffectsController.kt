package cc.skysparkle.fogwave.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioEffectsController {
    private val _state = MutableStateFlow(AudioEffectsState())
    val state: StateFlow<AudioEffectsState> = _state.asStateFlow()

    private var engine: AudioEffectsEngine? = null

    fun attach(engine: AudioEffectsEngine) {
        this.engine = engine
        refresh()
    }

    fun detach(engine: AudioEffectsEngine) {
        if (this.engine === engine) {
            this.engine = null
            _state.value = AudioEffectsState()
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        engine?.setEqualizerEnabled(enabled)
        refresh()
    }

    fun setBandLevel(band: Int, levelMillibel: Int) {
        engine?.setBandLevel(band, levelMillibel)
        refresh()
    }

    fun setPreset(preset: Int) {
        engine?.setPreset(preset)
        refresh()
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        engine?.setLoudnessEnabled(enabled)
        refresh()
    }

    fun setLoudnessGain(gainMillibel: Float) {
        engine?.setLoudnessGain(gainMillibel)
        refresh()
    }

    fun setDynamicsProcessingEnabled(enabled: Boolean) {
        engine?.setDynamicsProcessingEnabled(enabled)
        refresh()
    }

    private fun refresh() {
        engine?.let { _state.value = it.currentState() }
    }
}
