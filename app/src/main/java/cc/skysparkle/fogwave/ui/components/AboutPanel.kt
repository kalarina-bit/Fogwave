package cc.skysparkle.fogwave.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.BuildConfig
import cc.skysparkle.fogwave.R
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(LocalGeoColors.current.surfaceContainer)
                .border(1.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(24.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    resId = R.drawable.ic_fogwave_logo,
                    contentDescription = stringResource(R.string.fogwave_logo_content_desc),
                    size = 52.dp
                )
            }

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp,
                    color = LocalGeoColors.current.deepPurple
                )
            )

            // Comes straight from versionName in app/build.gradle.kts, so it can never drift.
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = LocalGeoColors.current.primary
                ),
                modifier = Modifier.testTag("about_version")
            )

            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = LocalGeoColors.current.onSurfaceVariant,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        AboutLinkCard(
            iconRes = R.drawable.ic_repo,
            iconTint = null,
            title = stringResource(R.string.about_my_repository),
            subtitle = REPOSITORY_URL.removePrefix("https://git.skysparkle.cc/"),
            trailingIconRes = R.drawable.ic_open_in_new,
            onClick = { context.openUrl(REPOSITORY_URL) },
            modifier = Modifier.testTag("github_repo_card")
        )

        AboutLinkCard(
            iconRes = R.drawable.ic_privacy,
            title = stringResource(R.string.privacy_title),
            subtitle = stringResource(R.string.privacy_subtitle),
            trailingIconRes = R.drawable.ic_open_in_new,
            onClick = { context.openUrl(PRIVACY_POLICY_URL) },
            modifier = Modifier.testTag("privacy_card")
        )

        AboutLinkCard(
            iconRes = R.drawable.ic_copyright,
            title = stringResource(R.string.licenses_title),
            subtitle = stringResource(R.string.licenses_subtitle),
            trailingIconRes = R.drawable.ic_chevron_right,
            onClick = { showLicenses = true },
            modifier = Modifier.testTag("licenses_card")
        )
    }

    if (showLicenses) {
        LicensesDialog(onDismiss = { showLicenses = false })
    }
}

@Composable
private fun AboutLinkCard(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    @DrawableRes trailingIconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = LocalGeoColors.current.deepPurple
) {
    val opensBrowser = trailingIconRes == R.drawable.ic_open_in_new
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(LocalGeoColors.current.surfaceContainer)
            .border(1.2.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(
                onClickLabel = if (opensBrowser) stringResource(R.string.about_open_in_browser) else null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LocalGeoColors.current.primaryContainer)
                .border(1.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                resId = iconRes,
                tint = iconTint ?: Color.Unspecified,
                size = if (iconTint == null) 32.dp else 26.dp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = LocalGeoColors.current.deepPurple
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = LocalGeoColors.current.primary
                )
            )
        }

        if (opensBrowser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(LocalGeoColors.current.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    resId = trailingIconRes,
                    tint = LocalGeoColors.current.deepPurple,
                    size = 18.dp
                )
            }
        } else {
            AppIcon(
                resId = trailingIconRes,
                tint = LocalGeoColors.current.onSurfaceVariant,
                size = 18.dp
            )
        }
    }
}
