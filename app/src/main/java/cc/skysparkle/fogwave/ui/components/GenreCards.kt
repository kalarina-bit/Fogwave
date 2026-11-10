package cc.skysparkle.fogwave.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.Genre
import cc.skysparkle.fogwave.data.model.RadioStation
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors

/** Two glow colors per card; null is the "All stations" card. */
private fun genrePalette(genre: Genre?): Pair<Color, Color> = when (genre) {
    null -> Color(0xFF3466F6) to Color(0xFF8E5CFF)
    Genre.HITS -> Color(0xFFFF3D8B) to Color(0xFF7A3CFF)
    Genre.POP -> Color(0xFFB04DFF) to Color(0xFF3FA9FF)
    Genre.DANCE -> Color(0xFF00D1FF) to Color(0xFFB13CFF)
    Genre.OLDIES -> Color(0xFFFFB13D) to Color(0xFFD9488A)
    Genre.ROCK -> Color(0xFFFF5640) to Color(0xFF5B1FA8)
    Genre.EASY_LISTENING -> Color(0xFF3DDCB0) to Color(0xFF3466F6)
    Genre.LITHUANIAN -> Color(0xFFFDB913) to Color(0xFF006A44)
    Genre.NEWS_TALK -> Color(0xFF8FA6C8) to Color(0xFF2F4B8C)
    Genre.LATVIAN -> Color(0xFFC8102E) to Color(0xFFE8E8F0)
}

/**
 * Horizontal row of genre cards in the spirit of the design's smoky category art.
 * The first card shows every station; tapping the selected genre again clears the filter.
 */
@Composable
fun GenreCarousel(
    stations: List<RadioStation>,
    selectedGenre: Genre?,
    onSelectGenre: (Genre?) -> Unit,
    modifier: Modifier = Modifier
) {
    val genres = Genre.entries.filter { g -> stations.any { g in it.genres } }

    LazyRow(
        modifier = modifier.testTag("genre_carousel"),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "all") {
            GenreCard(
                genre = null,
                title = stringResource(R.string.genre_all),
                count = stations.size,
                selected = selectedGenre == null,
                onClick = { onSelectGenre(null) }
            )
        }
        items(genres, key = { it.name }) { genre ->
            GenreCard(
                genre = genre,
                title = stringResource(genre.labelRes),
                count = stations.count { genre in it.genres },
                selected = selectedGenre == genre,
                onClick = { onSelectGenre(if (selectedGenre == genre) null else genre) }
            )
        }
    }
}

@Composable
private fun GenreCard(
    genre: Genre?,
    title: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalGeoColors.current
    val (glowA, glowB) = genrePalette(genre)
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, label = "genre_border")
    val dim by animateFloatAsState(if (selected) 1f else 0.82f, label = "genre_dim")
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .size(width = 124.dp, height = 156.dp)
            .clip(shape)
            .border(borderWidth, if (selected) colors.primary else colors.outlineVariant, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .testTag("genre_${genre?.name?.lowercase() ?: "all"}")
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = dim }
        ) {
            drawRect(Color(0xFF0B0D14))
            // Soft overlapping glows read as colored smoke on the dark card.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowA.copy(alpha = 0.9f), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.3f),
                    radius = size.width * 0.85f
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * 0.25f, size.height * 0.3f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowB.copy(alpha = 0.85f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.62f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.85f, size.height * 0.62f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.55f, size.height * 0.42f),
                    radius = size.width * 0.35f
                ),
                radius = size.width * 0.35f,
                center = Offset(size.width * 0.55f, size.height * 0.42f)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                    startY = size.height * 0.45f,
                    endY = size.height
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 8f)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    color = Color.White,
                    shadow = shadow
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pluralStringResource(R.plurals.station_count, count, count),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    shadow = shadow
                ),
                maxLines = 1
            )
        }
    }
}
