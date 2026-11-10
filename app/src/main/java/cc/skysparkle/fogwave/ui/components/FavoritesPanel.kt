package cc.skysparkle.fogwave.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors

@Composable
fun FavoritesPanel(
    favorites: List<RadioStation>,
    currentStation: RadioStation?,
    isPlaying: Boolean,
    onSelectStation: (RadioStation) -> Unit,
    onBrowseStations: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 140.dp
) {
    if (favorites.isEmpty()) {
        EmptyFavorites(
            onBrowseStations = onBrowseStations,
            bottomContentPadding = bottomContentPadding,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        item(key = "header") {
            SectionHeader(
                title = stringResource(R.string.tab_favorites),
                trailing = pluralStringResource(R.plurals.station_count, favorites.size, favorites.size),
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
            )
        }
        items(favorites, key = { it.id }) { station ->
            val isCur = station.id == currentStation?.id
            StationRow(
                station = station,
                isCurrent = isCur,
                isPlaying = isCur && isPlaying,
                onClick = { onSelectStation(station) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun EmptyFavorites(
    onBrowseStations: () -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    // Scrollable and at least as tall as the screen, so the card stays centered on phones and is
    // still fully reachable in landscape.
    BoxWithConstraints(modifier = modifier) {
        Box(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = bottomContentPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(LocalGeoColors.current.surfaceContainer)
                    .border(1.dp, LocalGeoColors.current.outlineVariant, RoundedCornerShape(24.dp))
                    .padding(24.dp)
                    .testTag("empty_favorites_card"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(LocalGeoColors.current.redFavorite.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        resId = R.drawable.ic_favorite,
                        tint = LocalGeoColors.current.redFavorite,
                        size = 28.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.favorites_empty_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp,
                        color = LocalGeoColors.current.deepPurple,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.favorites_empty_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = LocalGeoColors.current.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onBrowseStations,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalGeoColors.current.primaryContainer,
                        contentColor = LocalGeoColors.current.onPrimaryContainer
                    )
                ) {
                    AppIcon(
                        resId = R.drawable.ic_radio,
                        tint = LocalGeoColors.current.onPrimaryContainer,
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.browse_stations))
                }
            }
        }
    }
}
