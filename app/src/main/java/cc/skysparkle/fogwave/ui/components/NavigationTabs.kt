package cc.skysparkle.fogwave.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.AppTab
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors

@Composable
fun NavigationTabs(
    selectedTab: AppTab,
    favoritesCount: Int,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(LocalGeoColors.current.surfaceContainer.copy(alpha = 0.96f))
            .border(
                width = 1.dp,
                color = LocalGeoColors.current.outlineVariant,
                shape = RoundedCornerShape(26.dp)
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        FitTabsRow(modifier = Modifier.fillMaxWidth()) {
            AppTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) LocalGeoColors.current.primaryContainer else Color.Transparent,
                    label = "tab_bg"
                )
                val tintColor by animateColorAsState(
                    targetValue = if (isSelected) LocalGeoColors.current.deepPurple else LocalGeoColors.current.onSurfaceVariant,
                    label = "tab_tint"
                )

                val tabIcon: Int = when (tab) {
                    AppTab.RADIO -> R.drawable.ic_radio
                    AppTab.FAVORITES -> R.drawable.ic_favorite
                    AppTab.ABOUT -> R.drawable.ic_info
                }

                Row(
                    modifier = Modifier
                        .testTag("tab_${tab.name.lowercase()}")
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .selectable(
                            selected = isSelected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            role = Role.Tab,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    AppIcon(
                        resId = tabIcon,
                        tint = if (tab == AppTab.FAVORITES && isSelected) LocalGeoColors.current.redFavorite else tintColor,
                        size = 17.dp
                    )

                    Text(
                        text = stringResource(tab.titleRes),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = tintColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // Weighted so the icon and badge keep their size when a long label is squeezed.
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (tab == AppTab.FAVORITES && favoritesCount > 0 && !isSelected) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(LocalGeoColors.current.primary.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (favoritesCount > 9) "9+" else "$favoritesCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = LocalGeoColors.current.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lays tabs out like `Arrangement.SpaceEvenly`, but when the labels are too long for the screen
 * (narrow phones, long translations) it shrinks only the widest tabs until everything fits.
 */
@Composable
private fun FitTabsRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val natural = measurables.map { it.maxIntrinsicWidth(constraints.maxHeight) }
        val available = if (constraints.hasBoundedWidth) constraints.maxWidth else natural.sum()

        val widths = if (natural.sum() <= available) {
            natural
        } else {
            // Water-filling: find the largest cap so that sum(min(width, cap)) fits.
            val sorted = natural.sorted()
            var remaining = available
            var cap = 0
            for ((index, width) in sorted.withIndex()) {
                val left = sorted.size - index
                if (width * left <= remaining) {
                    remaining -= width
                } else {
                    cap = remaining / left
                    break
                }
            }
            natural.map { minOf(it, cap) }
        }

        val placeables = measurables.mapIndexed { i, measurable ->
            measurable.measure(Constraints.fixedWidth(widths[i]).copy(maxHeight = constraints.maxHeight))
        }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        val gap = ((available - placeables.sumOf { it.width }) / (placeables.size + 1)).coerceAtLeast(0)

        layout(available, height) {
            var x = gap
            placeables.forEach { placeable ->
                placeable.placeRelative(x, (height - placeable.height) / 2)
                x += placeable.width + gap
            }
        }
    }
}
