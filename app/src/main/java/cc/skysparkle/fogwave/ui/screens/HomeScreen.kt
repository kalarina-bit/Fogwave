package cc.skysparkle.fogwave.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.AppTab
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.player.PlayerState
import cc.skysparkle.fogwave.ui.components.AboutPanel
import cc.skysparkle.fogwave.ui.components.BatteryOptimizationDialog
import cc.skysparkle.fogwave.ui.components.EqualizerSheet
import cc.skysparkle.fogwave.ui.components.FavoritesPanel
import cc.skysparkle.fogwave.ui.components.HomeBackdrop
import cc.skysparkle.fogwave.ui.components.MiniPlayer
import cc.skysparkle.fogwave.ui.components.NavigationTabs
import cc.skysparkle.fogwave.ui.components.NowPlayingSheet
import cc.skysparkle.fogwave.ui.components.SectionHeader
import cc.skysparkle.fogwave.ui.components.StationRow
import cc.skysparkle.fogwave.ui.components.TopBrandBar
import cc.skysparkle.fogwave.ui.components.accentOr
import cc.skysparkle.fogwave.ui.components.rememberStationArt
import cc.skysparkle.fogwave.ui.components.stationDescription
import cc.skysparkle.fogwave.ui.theme.AppThemeState
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors
import cc.skysparkle.fogwave.ui.viewmodel.RadioViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: RadioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPlaying = uiState.playerState is PlayerState.Playing

    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val favoriteStations = remember(uiState.stations, uiState.favorites) {
        uiState.stations.filter { it.id in uiState.favorites }
    }
    val similarStations = remember(uiState.stations, uiState.currentStation) {
        viewModel.getSimilarStations()
    }
    val recentStations = remember(uiState.recents, uiState.currentStation) {
        uiState.recents.filter { it.id != uiState.currentStation?.id }
    }

    val closeSearch = {
        isSearchOpen = false
        query = ""
    }

    BackHandler(enabled = uiState.isEqualizerPopOpen) { viewModel.closeEqualizerPop() }
    BackHandler(enabled = uiState.isNowPlayingOpen && !uiState.isEqualizerPopOpen) { viewModel.closeNowPlaying() }
    BackHandler(enabled = isSearchOpen && !uiState.isNowPlayingOpen) { closeSearch() }
    BackHandler(
        enabled = uiState.selectedTab != AppTab.RADIO && !uiState.isNowPlayingOpen && !isSearchOpen
    ) { viewModel.setSelectedTab(AppTab.RADIO) }
    val showBatteryOptimizationPrompt by viewModel.showBatteryOptimizationPrompt.collectAsStateWithLifecycle()

    if (showBatteryOptimizationPrompt) {
        BatteryOptimizationDialog(onDismiss = { viewModel.dismissBatteryOptimizationPrompt() })
    }

    val radioListState = rememberLazyListState()
    val accent by animateColorAsState(
        // The style's colour unless the user asked for each station's own
        targetValue = uiState.currentStation?.let { rememberStationArt(it) }
            .takeIf { AppThemeState.stationFog }
            .accentOr(LocalGeoColors.current.accentLavender),
        animationSpec = tween(700),
        label = "home_accent"
    )

    val density = LocalDensity.current
    var dockHeight by remember { mutableStateOf(160.dp) }
    val scrollBottomPadding = dockHeight + 16.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalGeoColors.current.background)
    ) {
        HomeBackdrop(
            tint = accent,
            // Covered by the open player; no need to keep animating fog nobody sees.
            animate = !uiState.isNowPlayingOpen,
            visibility = {
                if (uiState.selectedTab != AppTab.RADIO) {
                    0.55f
                } else if (radioListState.firstVisibleItemIndex > 0) {
                    0.3f
                } else {
                    1f - radioListState.firstVisibleItemScrollOffset / with(density) { 420.dp.toPx() } * 0.7f
                }
            }
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TopBrandBar(
                showSearch = uiState.selectedTab == AppTab.RADIO,
                isSearchOpen = isSearchOpen,
                query = query,
                onQueryChange = { query = it },
                onOpenSearch = { isSearchOpen = true },
                onCloseSearch = closeSearch
            )

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (fadeIn(tween(220, delayMillis = 50)) +
                            slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 10 * dir })
                            .togetherWith(
                                fadeOut(tween(140)) +
                                    slideOutHorizontally(tween(200)) { -it / 10 * dir }
                            )
                    },
                    label = "tabs"
                ) { tab ->
                    when (tab) {
                        AppTab.RADIO -> RadioTab(
                            listState = radioListState,
                            stations = uiState.stations,
                            currentStation = uiState.currentStation,
                            isPlaying = isPlaying,
                            query = query.trim(),
                            onSelectStation = { viewModel.onStationClick(it) },
                            bottomContentPadding = scrollBottomPadding
                        )

                        AppTab.FAVORITES -> FavoritesPanel(
                            favorites = favoriteStations,
                            currentStation = uiState.currentStation,
                            isPlaying = isPlaying,
                            onSelectStation = { viewModel.onStationClick(it) },
                            onBrowseStations = { viewModel.setSelectedTab(AppTab.RADIO) },
                            bottomContentPadding = scrollBottomPadding,
                            modifier = Modifier.fillMaxSize()
                        )

                        AppTab.ABOUT -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = scrollBottomPadding)
                        ) {
                            AboutPanel()
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Measured before the inset padding so list content clears the system bar too.
                .onGloballyPositioned { coordinates ->
                    val measured = with(density) { coordinates.size.height.toDp() }
                    if (measured > 0.dp && measured != dockHeight) {
                        dockHeight = measured
                    }
                }
                // Rows (and the current station's highlight) fade out under the dock
                // instead of peeking through the gaps around the mini player and tabs.
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.22f to LocalGeoColors.current.background.copy(alpha = 0.94f),
                        1f to LocalGeoColors.current.background
                    )
                )
                // Insets live on the dock itself so the tabs stay clear of the gesture bar
                // even when the mini player is swiped away.
                .navigationBarsPadding()
        ) {
            AnimatedVisibility(
                visible = !uiState.isMiniPlayerHidden,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = ExitTransition.None
            ) {
                MiniPlayer(
                    station = uiState.currentStation,
                    playerState = uiState.playerState,
                    trackTitle = uiState.nowPlayingTitle,
                    onTogglePlay = { viewModel.togglePlay() },
                    onNext = { viewModel.nextStation() },
                    onOpenNowPlaying = { viewModel.openNowPlaying() },
                    onHide = { viewModel.hideMiniPlayer() }
                )
            }

            NavigationTabs(
                selectedTab = uiState.selectedTab,
                favoritesCount = uiState.unseenFavoritesCount,
                onTabSelected = {
                    closeSearch()
                    viewModel.setSelectedTab(it)
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        AnimatedVisibility(
            visible = uiState.isNowPlayingOpen,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(220))
        ) {
            val isCurFav = uiState.currentStation?.let { uiState.favorites.contains(it.id) } == true
            NowPlayingSheet(
                station = uiState.currentStation,
                playerState = uiState.playerState,
                trackTitle = uiState.nowPlayingTitle,
                isFavorite = isCurFav,
                onToggleFavorite = { viewModel.toggleFavorite() },
                onTogglePlay = { viewModel.togglePlay() },
                onPrevious = { viewModel.previousStation() },
                onNext = { viewModel.nextStation() },
                onClose = { viewModel.closeNowPlaying() },
                onOpenEqualizer = { viewModel.toggleEqualizerPop() },
                audioEffects = uiState.audioEffects,
                activeExtraTab = uiState.activeExtraTab,
                onSelectExtraTab = { viewModel.setActiveExtraTab(it) },
                recents = recentStations,
                similarStations = similarStations,
                onSelectStation = { viewModel.selectStation(it) },
                sheetModifier = Modifier.animateEnterExit(
                    enter = slideInVertically(
                        animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
                    ) { it },
                    exit = slideOutVertically(
                        animationSpec = tween(220, easing = FastOutLinearInEasing)
                    ) { it }
                )
            )
        }

        AnimatedVisibility(
            visible = uiState.isEqualizerPopOpen,
            enter = fadeIn(tween(200)) + slideInVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
            ) { it / 3 },
            exit = fadeOut(tween(180)) + slideOutVertically(
                animationSpec = tween(200, easing = FastOutLinearInEasing)
            ) { it / 3 }
        ) {
            EqualizerSheet(
                audioEffects = uiState.audioEffects,
                onClose = { viewModel.closeEqualizerPop() },
                onSetEqualizerEnabled = { viewModel.setEqualizerEnabled(it) },
                onSetEqualizerBand = { band, level -> viewModel.setEqualizerBand(band, level) },
                onSetPreset = { viewModel.setEqualizerPreset(it) },
                onResetBands = { viewModel.resetEqualizerBands() },
                onSetLoudnessEnabled = { viewModel.setLoudnessEnabled(it) },
                onSetDynamicsProcessingEnabled = { viewModel.setDynamicsProcessingEnabled(it) }
            )
        }
    }
}

/** Station list, filtered by the search query. */
@Composable
private fun RadioTab(
    listState: LazyListState,
    stations: List<RadioStation>,
    currentStation: RadioStation?,
    isPlaying: Boolean,
    query: String,
    onSelectStation: (RadioStation) -> Unit,
    bottomContentPadding: Dp
) {
    val descriptions = stations.associate { it.id to stationDescription(it) }
    val visible = remember(stations, query, descriptions) {
        stations.filter { station ->
            query.isEmpty() ||
                station.name.contains(query, ignoreCase = true) ||
                descriptions[station.id].orEmpty().contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 12.dp, bottom = bottomContentPadding),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "header") {
            SectionHeader(
                title = stringResource(
                    if (query.isNotEmpty()) R.string.search_results_title else R.string.stations_title
                ),
                trailing = pluralStringResource(R.plurals.station_count, visible.size, visible.size),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
            )
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.search_no_results, query),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LocalGeoColors.current.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 32.dp)
                )
            }
        }

        items(visible, key = { it.id }) { station ->
            val isCur = station.id == currentStation?.id
            StationRow(
                station = station,
                isCurrent = isCur,
                isPlaying = isCur && isPlaying,
                onClick = { onSelectStation(station) },
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 1.dp)
                    .animateItem()
            )
        }

        item(key = "spacer") { Spacer(modifier = Modifier.height(8.dp)) }
    }
}
