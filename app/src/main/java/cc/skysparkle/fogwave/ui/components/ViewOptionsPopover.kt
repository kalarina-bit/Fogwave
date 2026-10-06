package cc.skysparkle.fogwave.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.IconSizeStep
import cc.skysparkle.fogwave.data.model.ViewMode
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import kotlin.math.roundToInt

@Composable
fun StationsToolbar(
    title: String,
    isOpen: Boolean,
    onToggleOpen: () -> Unit,
    viewMode: ViewMode,
    onSelectViewMode: (ViewMode) -> Unit,
    sizeStep: IconSizeStep,
    onSelectSizeStep: (IconSizeStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.4).sp,
                    color = LocalGeoColors.current.deepPurple
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            )

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isOpen) LocalGeoColors.current.primaryContainer else LocalGeoColors.current.surfaceContainer)
                    .border(1.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { onToggleOpen() }
                    .testTag("view_options_trigger"),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    resId = R.drawable.ic_table,
                    contentDescription = stringResource(R.string.view_mode_content_desc),
                    tint = if (isOpen) LocalGeoColors.current.deepPurple else LocalGeoColors.current.onSurfaceVariant,
                    size = 19.dp
                )
            }
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = expandVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                expandFrom = Alignment.Top
            ) + fadeIn(tween(200)) + scaleIn(
                initialScale = 0.96f,
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = shrinkVertically(
                animationSpec = tween(180, easing = FastOutLinearInEasing),
                shrinkTowards = Alignment.Top
            ) + fadeOut(tween(140)) + scaleOut(
                targetScale = 0.96f,
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = tween(180)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(LocalGeoColors.current.surfaceContainer)
                    .border(1.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.display_mode_label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.8.sp,
                            color = LocalGeoColors.current.primary
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LocalGeoColors.current.surface)
                            .border(0.8.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(3.dp)
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ViewModeSegment(
                            title = stringResource(R.string.view_mode_list),
                            icon = R.drawable.ic_list,
                            isSelected = viewMode == ViewMode.LIST,
                            onClick = { onSelectViewMode(ViewMode.LIST) },
                            modifier = Modifier.weight(1f)
                        )
                        ViewModeSegment(
                            title = stringResource(R.string.view_mode_table),
                            icon = R.drawable.ic_layout_columns,
                            isSelected = viewMode == ViewMode.DETAILS,
                            onClick = { onSelectViewMode(ViewMode.DETAILS) },
                            modifier = Modifier.weight(1f)
                        )
                        ViewModeSegment(
                            title = stringResource(R.string.view_mode_tiles),
                            icon = R.drawable.ic_layout_rows,
                            isSelected = viewMode == ViewMode.TILES,
                            onClick = { onSelectViewMode(ViewMode.TILES) },
                            modifier = Modifier.weight(1f)
                        )
                        ViewModeSegment(
                            title = stringResource(R.string.view_mode_icons),
                            icon = R.drawable.ic_grid,
                            isSelected = viewMode == ViewMode.ICONS,
                            onClick = { onSelectViewMode(ViewMode.ICONS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.size_label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.8.sp,
                            color = LocalGeoColors.current.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "S",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LocalGeoColors.current.onSurfaceVariant
                            )
                        )

                        val steps = IconSizeStep.entries
                        val currentIndex = steps.indexOf(sizeStep).toFloat()
                        val sizeLabel = stringResource(R.string.size_label)

                        Slider(
                            value = currentIndex,
                            onValueChange = { indexFloat ->
                                val step = steps[indexFloat.roundToInt().coerceIn(0, steps.size - 1)]
                                // The slider reports every drag frame; only persist real changes.
                                if (step != sizeStep) onSelectSizeStep(step)
                            },
                            valueRange = 0f..(steps.size - 1).toFloat(),
                            steps = steps.size - 2,
                            colors = SliderDefaults.colors(
                                thumbColor = LocalGeoColors.current.primary,
                                activeTrackColor = LocalGeoColors.current.primary,
                                inactiveTrackColor = LocalGeoColors.current.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = sizeLabel }
                        )

                        Text(
                            text = "XL",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LocalGeoColors.current.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeSegment(
    title: String,
    icon: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) LocalGeoColors.current.primaryContainer else Color.Transparent,
        animationSpec = tween(220),
        label = "segment_bg"
    )
    val tint by animateColorAsState(
        targetValue = if (isSelected) LocalGeoColors.current.deepPurple else LocalGeoColors.current.onSurfaceVariant,
        animationSpec = tween(220),
        label = "segment_tint"
    )
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(
            resId = icon,
            contentDescription = title,
            tint = tint,
            size = 18.dp
        )
    }
}
