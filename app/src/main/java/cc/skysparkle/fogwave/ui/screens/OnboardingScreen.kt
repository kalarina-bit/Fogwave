package cc.skysparkle.fogwave.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.repository.RadioRepository
import cc.skysparkle.fogwave.ui.components.AppIcon
import cc.skysparkle.fogwave.ui.components.HomeBackdrop
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import kotlinx.coroutines.launch

/**
 * First-launch guide: shown once to new users, and again from About → «Show the guide».
 * People who already used the app before this version (they have favourites, recents or a
 * last station) are treated as having seen it.
 */
object Onboarding {
    private const val PREFS = "fogwave_prefs"
    private const val KEY_DONE = "onboarding_done"

    /** Whether the guide is on screen. */
    var visible by mutableStateOf(false)
        private set

    /** Opened by the user from About: the back button closes it from the first page too. */
    var replay by mutableStateOf(false)
        private set

    /** Call before the first frame. */
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return
        val usedBefore = prefs.contains("last_station_id") || prefs.contains("play_counts") ||
            prefs.contains("favorites") || prefs.contains("recents")
        if (usedBefore) {
            prefs.edit().putBoolean(KEY_DONE, true).apply()
        } else {
            replay = false
            visible = true
        }
    }

    fun show() {
        replay = true
        visible = true
    }

    fun finish(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()
        visible = false
    }
}

private data class GuidePage(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val textRes: Int,
    /** Icon is a full-colour picture (the logo), not a glyph to tint. */
    val keepColors: Boolean = false
)

private val PAGES = listOf(
    GuidePage(R.drawable.ic_fogwave_logo, R.string.guide_welcome_title, R.string.guide_welcome_text, keepColors = true),
    GuidePage(R.drawable.ic_radio, R.string.guide_listen_title, R.string.guide_listen_text),
    GuidePage(R.drawable.ic_info, R.string.guide_info_title, R.string.guide_info_text),
    GuidePage(R.drawable.ic_play, R.string.guide_background_title, R.string.guide_background_text)
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val colors = LocalGeoColors.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val last = pager.currentPage == PAGES.lastIndex

    // Back goes to the previous page; on the first page it closes a guide opened from About.
    BackHandler(enabled = pager.currentPage > 0 || Onboarding.replay) {
        if (pager.currentPage > 0) scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } else onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("onboarding")
    ) {
        HomeBackdrop(tint = colors.accentLavender)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // «Skip» on every page but the last
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (!last) {
                    Text(
                        text = stringResource(R.string.guide_skip),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = colors.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onDone)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("onboarding_skip")
                    )
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                GuidePageContent(page = PAGES[index], showBattery = index == PAGES.lastIndex)
            }

            Dots(count = PAGES.size, current = pager.currentPage)

            val accent by animateColorAsState(colors.accentLavender, label = "guide_button")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(if (last) R.string.guide_start else R.string.guide_next),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = colors.onAccentLavender,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(role = Role.Button) {
                            if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        }
                        .padding(vertical = 16.dp)
                        .testTag("onboarding_next")
                )
            }
        }
    }
}

@Composable
private fun GuidePageContent(page: GuidePage, showBattery: Boolean) {
    val colors = LocalGeoColors.current
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(colors.surfaceContainer.copy(alpha = 0.85f))
                .border(1.dp, colors.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                resId = page.iconRes,
                tint = if (page.keepColors) Color.Unspecified else colors.accentLavender,
                size = if (page.keepColors) 88.dp else 56.dp
            )
        }
        Spacer(Modifier.height(36.dp))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineSmall.copy(
                color = colors.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.widthIn(max = 420.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(page.textRes),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            ),
            modifier = Modifier.widthIn(max = 420.dp)
        )
        if (showBattery) {
            Spacer(Modifier.height(24.dp))
            BatteryButton(context)
        }
    }
}

/** Opens the battery optimisation list; afterwards the reminder dialog isn't needed any more. */
@Composable
private fun BatteryButton(context: Context) {
    val colors = LocalGeoColors.current
    Text(
        text = stringResource(R.string.guide_battery_button),
        style = MaterialTheme.typography.labelLarge.copy(
            color = colors.onBackground,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .clickable(role = Role.Button) {
                RadioRepository(context).markBatteryOptimizationPromptSeen()
                try {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (_: Exception) {
                }
            }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("onboarding_battery")
    )
}

@Composable
private fun Dots(count: Int, current: Int) {
    val colors = LocalGeoColors.current
    val label = stringResource(R.string.guide_page_of, current + 1, count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            val selected = i == current
            val width by animateDpAsState(if (selected) 22.dp else 8.dp, label = "guide_dot")
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.accentLavender else colors.outlineVariant)
            )
        }
    }
}
