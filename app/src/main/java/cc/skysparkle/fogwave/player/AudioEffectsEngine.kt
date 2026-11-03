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
    val loudnessGainMillibel: Float = 0f,
    val loudnessMaxGainMillibel: Float = 2000f,
    val dynamicsProcessingAvailable: Boolean = false,
    val dynamicsProcessingEnabled: Boolean = false
)

class AudioEffectsEngine(context: Context, audioSessionId: Int) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("fogwave_audio_effects", Context.MODE_PRIVATE)

    // Tracked manually: Equalizer.currentPreset is unreliable after bands are moved by hand.
    private var currentPresetIndex: Int = prefs.getInt(KEY_PRESET, -1)

    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null

    private val presetNames = mutableListOf<String>()

    init {
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                for (p in 0 until numberOfPresets) {
                    presetNames.add(getPresetName(p.toShort()))
                }

                val preset = currentPresetIndex
                if (preset in 0 until numberOfPresets) {
                    usePreset(preset.toShort())
                } else {
                    for (b in 0 until numberOfBands) {
                        val saved = prefs.getInt(KEY_BAND_PREFIX + b, Int.MIN_VALUE)
                        if (saved != Int.MIN_VALUE) setBandLevel(b.toShort(), saved.toShort())
                    }
                }
                enabled = prefs.getBoolean(KEY_EQ_ENABLED, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Equalizer unavailable", e)
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(prefs.getInt(KEY_LOUDNESS_GAIN, 0))
                enabled = prefs.getBoolean(KEY_LOUDNESS_ENABLED, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "LoudnessEnhancer unavailable", e)
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

        dp.enabled = prefs.getBoolean(KEY_DYNAMICS_ENABLED, false)
        return dp
    }

    fun currentState(): AudioEffectsState {
        val eq = equalizer
        val bands = if (eq != null) {
            (0 until eq.numberOfBands).map { i ->
                val idx = i.toShort()
                val range = eq.bandLevelRange
                EqualizerBand(
                    index = i,
                    centerFreqHz = eq.getCenterFreq(idx) / 1000,
                    levelMillibel = eq.getBandLevel(idx).toInt(),
                    minLevelMillibel = range[0].toInt(),
                    maxLevelMillibel = range[1].toInt()
                )
            }
        } else emptyList()

        return AudioEffectsState(
            equalizerAvailable = eq != null,
            equalizerEnabled = eq?.enabled ?: false,
            bands = bands,
            presets = presetNames,
            currentPreset = if (eq != null) currentPresetIndex else -1,
            loudnessAvailable = loudnessEnhancer != null,
            loudnessEnabled = loudnessEnhancer?.enabled ?: false,
            loudnessGainMillibel = try {
                loudnessEnhancer?.targetGain ?: 0f
            } catch (_: Exception) {
                0f
            },
            dynamicsProcessingAvailable = dynamicsProcessing != null,
            dynamicsProcessingEnabled = dynamicsProcessing?.enabled ?: false
        )
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            prefs.edit { putBoolean(KEY_EQ_ENABLED, enabled) }
        } catch (_: Exception) {}
    }

    fun setBandLevel(band: Int, levelMillibel: Int) {
        try {
            equalizer?.setBandLevel(band.toShort(), levelMillibel.toShort())
            currentPresetIndex = -1
            prefs.edit {
                putInt(KEY_BAND_PREFIX + band, levelMillibel)
                putInt(KEY_PRESET, -1)
            }
        } catch (_: Exception) {}
    }

    fun setPreset(preset: Int) {
        try {
            val eq = equalizer ?: return
            eq.usePreset(preset.toShort())
            currentPresetIndex = preset
            prefs.edit {
                putInt(KEY_PRESET, preset)
                for (b in 0 until eq.numberOfBands) {
                    putInt(KEY_BAND_PREFIX + b, eq.getBandLevel(b.toShort()).toInt())
                }
            }
        } catch (_: Exception) {}
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        try {
            loudnessEnhancer?.enabled = enabled
            prefs.edit { putBoolean(KEY_LOUDNESS_ENABLED, enabled) }
        } catch (_: Exception) {}
    }

    fun setLoudnessGain(gainMillibel: Float) {
        try {
            val gain = gainMillibel.coerceIn(0f, 2000f).toInt()
            loudnessEnhancer?.setTargetGain(gain)
            prefs.edit { putInt(KEY_LOUDNESS_GAIN, gain) }
        } catch (_: Exception) {}
    }

    fun setDynamicsProcessingEnabled(enabled: Boolean) {
        try {
            dynamicsProcessing?.enabled = enabled
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
        const val KEY_EQ_ENABLED = "eq_enabled"
        const val KEY_PRESET = "eq_preset"
        const val KEY_BAND_PREFIX = "eq_band_"
        const val KEY_LOUDNESS_ENABLED = "loudness_enabled"
        const val KEY_LOUDNESS_GAIN = "loudness_gain"
        const val KEY_DYNAMICS_ENABLED = "dynamics_enabled"
    }
}
