package cc.skysparkle.fogwave.player

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log
import androidx.core.content.edit

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMillibel: Int,
    val minLevelMillibel: Int,
    val maxLevelMillibel: Int
)

data class AudioEffectsState(
    val equalizerAvailable: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val presets: List<String> = emptyList(),
    val currentPreset: Int = -1,
    val loudnessAvailable: Boolean = false,
    val loudnessEnabled: Boolean = false,
    val dynamicsProcessingAvailable: Boolean = false,
    val dynamicsProcessingEnabled: Boolean = false
)

// Effect parameters are cached here: every getter on an AudioEffect is a call into audioserver,
// and the state is rebuilt on every frame while an equalizer slider is dragged.
class AudioEffectsEngine(context: Context, audioSessionId: Int) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("fogwave_audio_effects", Context.MODE_PRIVATE)

    // Tracked manually: Equalizer.currentPreset is unreliable after bands are moved by hand.
    private var currentPresetIndex: Int = prefs.getInt(KEY_PRESET, -1)

    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null

    private var presetNames: List<String> = emptyList()
    private var centerFreqsHz = IntArray(0)
    private var bandLevels = IntArray(0)
    private var minBandLevel = 0
    private var maxBandLevel = 0

    private var equalizerEnabled = prefs.getBoolean(KEY_EQ_ENABLED, false)
    private var loudnessEnabled = prefs.getBoolean(KEY_LOUDNESS_ENABLED, false)
    // There is no gain slider, so the switch applies a fixed, moderate boost. A gain of 0 (the old
    // default) made the switch do nothing.
    private val loudnessGain = prefs.getInt(KEY_LOUDNESS_GAIN, DEFAULT_LOUDNESS_GAIN)
        .takeIf { it > 0 }?.coerceAtMost(MAX_LOUDNESS_GAIN) ?: DEFAULT_LOUDNESS_GAIN
    private var dynamicsEnabled = prefs.getBoolean(KEY_DYNAMICS_ENABLED, false)

    init {
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                presetNames = (0 until numberOfPresets).map { getPresetName(it.toShort()) }

                val bandCount = numberOfBands.toInt()
                centerFreqsHz = IntArray(bandCount) { getCenterFreq(it.toShort()) / 1000 }
                val range = bandLevelRange
                minBandLevel = range[0].toInt()
                maxBandLevel = range[1].toInt()

                val preset = currentPresetIndex
                if (preset in 0 until numberOfPresets) {
                    usePreset(preset.toShort())
                } else {
                    for (b in 0 until bandCount) {
                        val saved = prefs.getInt(KEY_BAND_PREFIX + b, Int.MIN_VALUE)
                        if (saved != Int.MIN_VALUE) setBandLevel(b.toShort(), saved.toShort())
                    }
                }
                bandLevels = IntArray(bandCount) { getBandLevel(it.toShort()).toInt() }
                enabled = equalizerEnabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Equalizer unavailable", e)
            equalizer = null
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(loudnessGain)
                enabled = loudnessEnabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "LoudnessEnhancer unavailable", e)
            loudnessEnhancer = null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                dynamicsProcessing = buildDynamicsProcessing(audioSessionId)
            } catch (e: Exception) {
                Log.e(TAG, "DynamicsProcessing unavailable", e)
            }
        }
    }

    private fun buildDynamicsProcessing(audioSessionId: Int): DynamicsProcessing {
        // Upper edge of each multiband-compressor band: bass / mids / highs.
        val crossovers = floatArrayOf(200f, 4_000f, 20_000f)
        val bandCount = crossovers.size

        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            1,
            false, 0,
            true, bandCount,
            false, 0,
            true
        ).build()

        val dp = DynamicsProcessing(0, audioSessionId, config)
        dp.setInputGainAllChannelsTo(3.0f)

        // The effect expands the config to the real channel count of the session (usually stereo),
        // so every channel has to be tuned, not only the first one.
        for (ch in 0 until dp.channelCount) {
            val mbc = dp.getMbcByChannelIndex(ch) ?: continue
            mbc.isEnabled = true
            for (b in 0 until bandCount) {
                val band = DynamicsProcessing.MbcBand(
                    true,
                    crossovers[b],
                    8f,
                    60f,
                    2.0f,
                    -24f,
                    6f,
                    -90f,
                    1f,
                    0f,
                    0f
                )
                mbc.setBand(b, band)
            }
            dp.setMbcByChannelIndex(ch, mbc)

            val limiter = dp.getLimiterByChannelIndex(ch) ?: continue
            limiter.isEnabled = true
            limiter.attackTime = 3f
            limiter.releaseTime = 60f
            limiter.ratio = 10f
            limiter.threshold = -2f
            limiter.postGain = 0f
            dp.setLimiterByChannelIndex(ch, limiter)
        }

        dp.enabled = dynamicsEnabled
        return dp
    }

    fun currentState(): AudioEffectsState {
        val eqAvailable = equalizer != null
        val bands = if (eqAvailable) {
            bandLevels.indices.map { i ->
                EqualizerBand(
                    index = i,
                    centerFreqHz = centerFreqsHz[i],
                    levelMillibel = bandLevels[i],
                    minLevelMillibel = minBandLevel,
                    maxLevelMillibel = maxBandLevel
                )
            }
        } else emptyList()

        return AudioEffectsState(
            equalizerAvailable = eqAvailable,
            equalizerEnabled = eqAvailable && equalizerEnabled,
            bands = bands,
            presets = presetNames,
            currentPreset = if (eqAvailable) currentPresetIndex else -1,
            loudnessAvailable = loudnessEnhancer != null,
            loudnessEnabled = loudnessEnhancer != null && loudnessEnabled,
            dynamicsProcessingAvailable = dynamicsProcessing != null,
            dynamicsProcessingEnabled = dynamicsProcessing != null && dynamicsEnabled
        )
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        val eq = equalizer ?: return
        try {
            eq.enabled = enabled
            equalizerEnabled = enabled
            prefs.edit { putBoolean(KEY_EQ_ENABLED, enabled) }
        } catch (_: Exception) {}
    }

    fun setBandLevel(band: Int, levelMillibel: Int) {
        val eq = equalizer ?: return
        if (band !in bandLevels.indices) return
        val level = levelMillibel.coerceIn(minBandLevel, maxBandLevel)
        if (bandLevels[band] == level && currentPresetIndex == -1) return
        try {
            eq.setBandLevel(band.toShort(), level.toShort())
            bandLevels[band] = level
            currentPresetIndex = -1
            prefs.edit {
                putInt(KEY_BAND_PREFIX + band, level)
                putInt(KEY_PRESET, -1)
            }
        } catch (_: Exception) {}
    }

    fun setPreset(preset: Int) {
        val eq = equalizer ?: return
        try {
            eq.usePreset(preset.toShort())
            currentPresetIndex = preset
            for (b in bandLevels.indices) {
                bandLevels[b] = eq.getBandLevel(b.toShort()).toInt()
            }
            prefs.edit {
                putInt(KEY_PRESET, preset)
                for (b in bandLevels.indices) putInt(KEY_BAND_PREFIX + b, bandLevels[b])
            }
        } catch (_: Exception) {}
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        val le = loudnessEnhancer ?: return
        try {
            le.enabled = enabled
            loudnessEnabled = enabled
            prefs.edit { putBoolean(KEY_LOUDNESS_ENABLED, enabled) }
        } catch (_: Exception) {}
    }

    fun setDynamicsProcessingEnabled(enabled: Boolean) {
        val dp = dynamicsProcessing ?: return
        try {
            dp.enabled = enabled
            dynamicsEnabled = enabled
            prefs.edit { putBoolean(KEY_DYNAMICS_ENABLED, enabled) }
        } catch (_: Exception) {}
    }

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        try { dynamicsProcessing?.release() } catch (_: Exception) {}
        equalizer = null
        loudnessEnhancer = null
        dynamicsProcessing = null
    }

    private companion object {
        const val TAG = "AudioEffectsEngine"
        const val MAX_LOUDNESS_GAIN = 2000
        const val DEFAULT_LOUDNESS_GAIN = 600
        const val KEY_EQ_ENABLED = "eq_enabled"
        const val KEY_PRESET = "eq_preset"
        const val KEY_BAND_PREFIX = "eq_band_"
        const val KEY_LOUDNESS_ENABLED = "loudness_enabled"
        const val KEY_LOUDNESS_GAIN = "loudness_gain"
        const val KEY_DYNAMICS_ENABLED = "dynamics_enabled"
    }
}
