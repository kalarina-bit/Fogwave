package cc.skysparkle.fogwave.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.BuildConfig
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.ui.theme.AppStyle
import cc.skysparkle.fogwave.ui.theme.AppThemeState
import cc.skysparkle.fogwave.ui.theme.GeoColors
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors

private const val REPOSITORY_URL = "https://git.skysparkle.cc/kalarina/Fogwave"
private const val PRIVACY_POLICY_URL = "https://skysparkle.cc/privacy?lang=en"

@Composable
fun AboutPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showLicenses by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppHeader()

        SectionLabel(stringResource(R.string.about_appearance))
        GroupCard {
            StylePicker()
            CardDivider()
            StationFogRow()
        }

        SectionLabel(stringResource(R.string.about_more))
        GroupCard {
            LinkRow(
                iconRes = R.drawable.ic_mc_code,
                title = stringResource(R.string.about_my_repository),
                subtitle = REPOSITORY_URL.removePrefix("https://"),
                external = true,
                onClick = { context.openUrl(REPOSITORY_URL) },
                modifier = Modifier.testTag("github_repo_card")
            )
            CardDivider()
            LinkRow(
                iconRes = R.drawable.ic_mc_shield,
                title = stringResource(R.string.privacy_title),
                subtitle = stringResource(R.string.privacy_subtitle),
                external = true,
                onClick = { context.openUrl(PRIVACY_POLICY_URL) },
                modifier = Modifier.testTag("privacy_card")
            )
            CardDivider()
            LinkRow(
                iconRes = R.drawable.ic_mc_copyright,
                title = stringResource(R.string.licenses_title),
                subtitle = stringResource(R.string.licenses_subtitle),
                external = false,
                onClick = { showLicenses = true },
                modifier = Modifier.testTag("licenses_card")
            )
        }
    }

    if (showLicenses) {
        LicensesDialog(onDismiss = { showLicenses = false })
    }
}

/** Logo, name and version in one row, the description underneath. */
@Composable
private fun AppHeader() {
    val colors = LocalGeoColors.current
    GroupCard(contentPadding = 16.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    resId = R.drawable.ic_fogwave_logo,
                    contentDescription = stringResource(R.string.fogwave_logo_content_desc),
                    size = 40.dp
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        letterSpacing = (-0.4).sp,
                        color = colors.deepPurple
                    )
                )
                // Comes straight from versionName in app/build.gradle.kts, so it can never drift.
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = colors.primary
                    ),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.primaryContainer)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                        .testTag("about_version")
                )
            }
        }
        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = colors.onSurfaceVariant
            ),
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun StylePicker() {
    val context = LocalContext.current
    val colors = LocalGeoColors.current
    val selected = AppThemeState.style
    val styles = remember { AppStyle.available }

    Column(modifier = Modifier.padding(16.dp)) {
        RowHeader(iconRes = R.drawable.ic_mc_palette, title = stringResource(R.string.app_style_title))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            styles.forEach { style ->
                StyleSwatch(
                    style = style,
                    preview = remember(style) { AppThemeState.colorsFor(context, style) },
                    selected = style == selected,
                    onClick = { AppThemeState.select(context, style) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        AnimatedVisibility(visible = selected == AppStyle.DEVICE) {
            Text(
                text = stringResource(R.string.app_style_device_hint),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = colors.onSurfaceVariant
                ),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun StyleSwatch(
    style: AppStyle,
    preview: GeoColors,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGeoColors.current
    val ring by animateColorAsState(if (selected) colors.deepPurple else Color.Transparent, label = "swatch_ring")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag("style_${style.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // The outer ring shows the selection; inside, the style's accent over its background
        Box(
            modifier = Modifier
                .size(42.dp)
                .border(2.dp, ring, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(preview.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(preview.accentLavender),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    AppIcon(resId = R.drawable.ic_mc_check, tint = preview.onAccentLavender, size = 15.dp)
                }
            }
        }
        Text(
            text = stringResource(style.titleRes),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colors.deepPurple else colors.onSurfaceVariant
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StationFogRow() {
    val context = LocalContext.current
    val colors = LocalGeoColors.current
    val checked = AppThemeState.stationFog

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { AppThemeState.setStationFog(context, it) }
            )
            .padding(16.dp)
            .testTag("station_fog_toggle"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RowIcon(R.drawable.ic_mc_fog)
        Column(modifier = Modifier.weight(1f)) {
            RowTitle(stringResource(R.string.station_fog_title))
            RowSubtitle(stringResource(R.string.station_fog_subtitle), maxLines = 3)
        }
        // The row handles the tap, so the switch itself only shows the state
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccentLavender,
                checkedTrackColor = colors.accentLavender,
                checkedBorderColor = colors.accentLavender,
                uncheckedThumbColor = colors.onSurfaceVariant,
                uncheckedTrackColor = colors.surfaceContainerHigh,
                uncheckedBorderColor = colors.outline
            )
        )
    }
}

@Composable
private fun LinkRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    external: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = if (external) stringResource(R.string.about_open_in_browser) else null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RowIcon(iconRes)
        Column(modifier = Modifier.weight(1f)) {
            RowTitle(title)
            RowSubtitle(subtitle)
        }
        AppIcon(
            resId = if (external) R.drawable.ic_mc_external else R.drawable.ic_mc_right,
            tint = LocalGeoColors.current.onSurfaceVariant,
            size = 18.dp
        )
    }
}

@Composable
private fun RowHeader(@DrawableRes iconRes: Int, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RowIcon(iconRes)
        RowTitle(title)
    }
}

@Composable
private fun RowIcon(@DrawableRes iconRes: Int) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(LocalGeoColors.current.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(resId = iconRes, tint = LocalGeoColors.current.primary, size = 20.dp)
    }
}

@Composable
private fun RowTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            color = LocalGeoColors.current.deepPurple
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RowSubtitle(text: String, maxLines: Int = 1) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = LocalGeoColors.current.onSurfaceVariant
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            color = LocalGeoColors.current.onSurfaceVariant
        ),
        modifier = Modifier.padding(start = 6.dp, top = 8.dp)
    )
}

/** Rows grouped on one rounded card, like a settings list. */
@Composable
private fun GroupCard(
    contentPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(LocalGeoColors.current.surfaceContainer)
            .border(1.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(22.dp))
            .padding(contentPadding),
        content = content
    )
}

@Composable
private fun CardDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp)
            .height(1.dp)
            .background(LocalGeoColors.current.outlineVariant)
    )
}
