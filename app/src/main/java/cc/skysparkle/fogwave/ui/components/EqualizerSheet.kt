package cc.skysparkle.fogwave.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.player.AudioEffectsState
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun EqualizerSheet(
    audioEffects: AudioEffectsState,
    onClose: () -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit,
    onSetEqualizerBand: (Int, Int) -> Unit,
    onSetPreset: (Int) -> Unit,
    onResetBands: () -> Unit,
    onSetLoudnessEnabled: (Boolean) -> Unit,
    onSetDynamicsProcessingEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalGeoColors.current.surfaceContainer)
            // Full-screen overlay: keep taps on empty areas from reaching the player underneath.
            .pointerInput(Unit) {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("eq_back_btn")) {
                    AppIcon(
                        resId = R.drawable.ic_chevron_left,
                        contentDescription = stringResource(R.string.back_content_desc),
                        tint = LocalGeoColors.current.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.equalizer_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = LocalGeoColors.current.deepPurple
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (audioEffects.equalizerEnabled) stringResource(R.string.equalizer_on) else stringResource(R.string.equalizer_off),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (audioEffects.equalizerEnabled) LocalGeoColors.current.liveGreen else LocalGeoColors.current.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
                Switch(
                    checked = audioEffects.equalizerEnabled,
                    onCheckedChange = onSetEqualizerEnabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = LocalGeoColors.current.primary),
                    modifier = Modifier.testTag("eq_enable_switch")
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(LocalGeoColors.current.outlineVariant)
                    )
                    Text(
                        text = stringResource(R.string.equalizer_sound_section),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = LocalGeoColors.current.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(LocalGeoColors.current.outlineVariant)
                    )
                }

                if (audioEffects.bands.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .alpha(if (audioEffects.equalizerEnabled) 1f else 0.45f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(LocalGeoColors.current.outlineVariant)
                        )

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            audioEffects.bands.forEach { band ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    val db = (band.levelMillibel / 100f).roundToInt()
                                    Text(
                                        text = if (db > 0) "+$db dB" else "$db dB",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (db == 0) LocalGeoColors.current.onSurfaceVariant
                                            else LocalGeoColors.current.primary
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .width(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Slider(
                                            value = band.levelMillibel.toFloat(),
                                            onValueChange = { onSetEqualizerBand(band.index, it.roundToInt()) },
                                            valueRange = band.minLevelMillibel.toFloat()..band.maxLevelMillibel.toFloat(),
                                            enabled = audioEffects.equalizerEnabled,
                                            colors = SliderDefaults.colors(
                                                thumbColor = LocalGeoColors.current.primary,
                                                activeTrackColor = LocalGeoColors.current.primary,
                                                inactiveTrackColor = LocalGeoColors.current.outlineVariant
                                            ),
                                            modifier = Modifier
                                                // requiredWidth (not width): the parent is narrower, and width() would be clamped before rotation.
                                                .requiredWidth(160.dp)
                                                .graphicsLayer {
                                                    rotationZ = 270f
                                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                                }
                                        )
                                    }
                                    Text(
                                        text = frequencyLabel(band.centerFreqHz),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            color = LocalGeoColors.current.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppIcon(
                            resId = R.drawable.ic_alert,
                            tint = LocalGeoColors.current.onSurfaceVariant,
                            size = 18.dp
                        )
                        Text(
                            text = stringResource(R.string.equalizer_unavailable),
                            style = MaterialTheme.typography.bodyMedium.copy(color = LocalGeoColors.current.onSurfaceVariant)
                        )
                    }
                }

                if (audioEffects.presets.isNotEmpty()) {
                    val currentPresetName = audioEffects.presets.getOrNull(audioEffects.currentPreset)
                        ?.let { presetLabel(it) }
                        ?: stringResource(R.string.equalizer_custom_preset)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppIcon(
                            resId = R.drawable.ic_sparkle,
                            contentDescription = null,
                            tint = LocalGeoColors.current.primary,
                            size = 18.dp
                        )
                        Text(
                            text = currentPresetName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = LocalGeoColors.current.onSurface
                            )
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(audioEffects.presets) { index, name ->
                            val selected = audioEffects.currentPreset == index
                            Text(
                                text = presetLabel(name),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) LocalGeoColors.current.deepPurple else LocalGeoColors.current.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) LocalGeoColors.current.primaryContainer else LocalGeoColors.current.surfaceContainerHigh)
                                    .clickable { onSetPreset(index) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (audioEffects.loudnessAvailable) {
                    ToggleRow(
                        label = stringResource(R.string.loudness_row_label),
                        checked = audioEffects.loudnessEnabled,
                        onCheckedChange = onSetLoudnessEnabled
                    )
                }
                if (audioEffects.dynamicsProcessingAvailable) {
                    ToggleRow(
                        label = stringResource(R.string.dynamics_row_label),
                        checked = audioEffects.dynamicsProcessingEnabled,
                        onCheckedChange = onSetDynamicsProcessingEnabled
                    )
                }

                if (audioEffects.bands.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = onResetBands,
                        modifier = Modifier.testTag("eq_reset_btn")
                    ) {
                        AppIcon(
                            resId = R.drawable.ic_refresh,
                            contentDescription = null,
                            tint = LocalGeoColors.current.primary,
                            size = 18.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.equalizer_reset))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // The whole row is the switch, so the label is a bigger touch target and is read with it.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LocalGeoColors.current.surfaceContainerHigh)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = LocalGeoColors.current.onSurface
            ),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = LocalGeoColors.current.primary)
        )
    }
}

private fun frequencyLabel(hz: Int): String = when {
    hz < 1000 -> "$hz Hz"
    hz % 1000 == 0 -> "${hz / 1000} kHz"
    else -> String.format(Locale.getDefault(), "%.1f kHz", hz / 1000f)
}

// Preset names come from the device's audio framework in English; translate the standard ones.
@Composable
@ReadOnlyComposable
private fun presetLabel(name: String): String {
    val res = when (name.trim().lowercase(Locale.ROOT)) {
        "normal" -> R.string.eq_preset_normal
        "classical" -> R.string.eq_preset_classical
        "dance" -> R.string.eq_preset_dance
        "flat" -> R.string.eq_preset_flat
        "folk" -> R.string.eq_preset_folk
        "heavy metal" -> R.string.eq_preset_heavy_metal
        "hip hop" -> R.string.eq_preset_hip_hop
        "jazz" -> R.string.eq_preset_jazz
        "pop" -> R.string.eq_preset_pop
        "rock" -> R.string.eq_preset_rock
        else -> return name
    }
    return stringResource(res)
}
