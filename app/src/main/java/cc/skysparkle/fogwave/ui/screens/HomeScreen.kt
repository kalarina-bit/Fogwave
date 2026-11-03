package cc.skysparkle.fogwave.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.AppTab
import cc.skysparkle.fogwave.player.PlayerState
import cc.skysparkle.fogwave.ui.components.AboutPanel
import cc.skysparkle.fogwave.ui.components.BatteryOptimizationDialog
import cc.skysparkle.fogwave.ui.components.EqualizerSheet
import cc.skysparkle.fogwave.ui.components.FavoritesPanel
import cc.skysparkle.fogwave.ui.components.MiniPlayer
import cc.skysparkle.fogwave.ui.components.MistyMountainBackground
import cc.skysparkle.fogwave.ui.components.NavigationTabs
import cc.skysparkle.fogwave.ui.components.NowPlayingSheet
import cc.skysparkle.fogwave.ui.components.StationCollection
import cc.skysparkle.fogwave.ui.components.StationsToolbar
import cc.skysparkle.fogwave.ui.components.TopBrandBar
import cc.skysparkle.fogwave.ui.viewmodel.RadioViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: RadioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPlaying = uiState.playerState is PlayerState.Playing

    val favoriteStations = remember(uiState.stations, uiState.favorites) {
        uiState.stations.filter { it.id in uiState.favorites }
    }
    val similarStations = remember(uiState.stations, uiState.currentStation) {
        viewModel.getSimilarStations()
    }
    val recentStations = remember(uiState.recents, uiState.currentStation) {
        uiState.recents.filter { it.id != uiState.currentStation?.id }
    }

    BackHandler(enabled = uiState.isEqualizerPopOpen) { viewModel.closeEqualizerPop() }
    BackHandler(enabled = uiState.isNowPlayingOpen && !uiState.isEqualizerPopOpen) { viewModel.closeNowPlaying() }
    BackHandler(enabled = uiState.isViewPopOpen && !uiState.isNowPlayingOpen) { viewModel.closeViewPop() }
    BackHandler(
        enabled = uiState.selectedTab != AppTab.RADIO && !uiState.isNowPlayingOpen && !uiState.isViewPopOpen
    ) { viewModel.setSelectedTab(AppTab.RADIO) }
    val showBatteryOptimizationPrompt by viewModel.showBatteryOptimizationPrompt.collectAsStateWithLifecycle()

    if (showBatteryOptimizationPrompt) {
        BatteryOptimizationDialog(onDismiss = { viewModel.dismissBatteryOptimizationPrompt() })
    }

    val density = LocalDensity.current
    var dockHeight by remember { mutableStateOf(140.dp) }
    val scrollBottomPadding = dockHeight + 16.dp

    Box(modifier = Modifier.fillMaxSize()) {
        MistyMountainBackground()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBrandBar()

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
                        AppTab.RADIO, AppTab.FAVORITES -> {
                            val isFavorites = tab == AppTab.FAVORITES
                            Column(modifier = Modifier.fillMaxSize()) {
                                StationsToolbar(
                                    title = stringResource(
                                        if (isFavorites) R.string.favorite_stations_title else R.string.stations_title
                                    ),
                                    isOpen = uiState.isViewPopOpen,
                                    onToggleOpen = { viewModel.toggleViewPop() },
                                    viewMode = uiState.viewMode,
                                    onSelectViewMode = { viewModel.setViewMode(it) },
                                    sizeStep = uiState.sizeStep,
                                    onSelectSizeStep = { viewModel.setSizeStep(it) }
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    AnimatedContent(
                                        targetState = uiState.viewMode,
                                        transitionSpec = {
                                            (fadeIn(tween(220, delayMillis = 60)) +
                                                scaleIn(initialScale = 0.985f, animationSpec = tween(260)))
                                                .togetherWith(fadeOut(tween(120)))
                                        },
                                        label = "view_mode"
                                    ) { mode ->
                                        if (isFavorites) {
                                            FavoritesPanel(
                                                favorites = favoriteStations,
                                                currentStation = uiState.currentStation,
                                                isPlaying = isPlaying,
                                                viewMode = mode,
                                                sizeStep = uiState.sizeStep,
                                                onSelectStation = { viewModel.onStationClick(it) },
                                                onBrowseStations = { viewModel.setSelectedTab(AppTab.RADIO) },
                                                bottomContentPadding = scrollBottomPadding,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            StationCollection(
                                                stations = uiState.stations,
                                                currentStation = uiState.currentStation,
                                                isPlaying = isPlaying,
                                                viewMode = mode,
                                                sizeStep = uiState.sizeStep,
                                                onSelectStation = { viewModel.onStationClick(it) },
                                                bottomContentPadding = scrollBottomPadding,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    PopoverScrim(
                                        visible = uiState.isViewPopOpen,
                                        onDismiss = { viewModel.closeViewPop() }
                                    )
                                }
                            }
                        }

                        AppTab.ABOUT -> {
                            Column(
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
                // Insets live on the dock itself so the tabs stay clear of the gesture bar
                // even when the mini player is swiped away.
                .navigationBarsPadding()
        ) {
            NavigationTabs(
                selectedTab = uiState.selectedTab,
                favoritesCount = uiState.unseenFavoritesCount,
                onTabSelected = { viewModel.setSelectedTab(it) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            AnimatedVisibility(
                visible = !uiState.isMiniPlayerHidden,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = ExitTransition.None
            ) {
                MiniPlayer(
                    station = uiState.currentStation,
                    playerState = uiState.playerState,
                    onTogglePlay = { viewModel.togglePlay() },
                    onPrevious = { viewModel.previousStation() },
                    onNext = { viewModel.nextStation() },
                    onOpenNowPlaying = { viewModel.openNowPlaying() },
                    onHide = { viewModel.hideMiniPlayer() }
                )
            }
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

@Composable
private fun PopoverScrim(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(160))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
}
