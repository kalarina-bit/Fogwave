package cc.skysparkle.fogwave.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.ui.theme.LocalGeoColors

/**
 * Header with the logo on the left, the app name centered and a search action on the right.
 * While searching, the title turns into a search field.
 */
@Composable
fun TopBrandBar(
    modifier: Modifier = Modifier,
    showSearch: Boolean = false,
    isSearchOpen: Boolean = false,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onCloseSearch: () -> Unit = {}
) {
    val colors = LocalGeoColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
            .height(48.dp)
            .testTag("top_brand_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppIcon(
            resId = R.drawable.ic_fogwave_logo,
            contentDescription = stringResource(R.string.fogwave_logo_content_desc),
            size = 30.dp
        )

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = showSearch && isSearchOpen,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "header_search"
            ) { searching ->
                if (searching) {
                    SearchField(query = query, onQueryChange = onQueryChange)
                } else {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = 0.sp,
                            color = colors.deepPurple
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { heading() },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showSearch) {
            IconButton(
                onClick = if (isSearchOpen) onCloseSearch else onOpenSearch,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("search_toggle")
            ) {
                AppIcon(
                    resId = if (isSearchOpen) R.drawable.ic_close else R.drawable.ic_search,
                    contentDescription = stringResource(
                        if (isSearchOpen) R.string.close_content_desc else R.string.search_content_desc
                    ),
                    tint = colors.onSurface,
                    size = 22.dp
                )
            }
        } else {
            Box(modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = LocalGeoColors.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, fontSize = 15.sp),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("search_field"),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppIcon(resId = R.drawable.ic_search, tint = colors.onSurfaceVariant, size = 18.dp)
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = colors.onSurfaceVariant,
                                fontSize = 15.sp
                            ),
                            maxLines = 1
                        )
                    }
                    inner()
                }
            }
        }
    )
}
