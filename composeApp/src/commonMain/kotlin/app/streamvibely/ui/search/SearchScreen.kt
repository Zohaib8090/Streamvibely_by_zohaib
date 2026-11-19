package app.streamvibely.ui.search

import app.streamvibely.theme.chipSurface
import app.streamvibely.theme.engine.ColorEngine
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.AppTheme
import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.Track
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.playlist.AppIconButton
import app.streamvibely.ui.common.ArtworkImage
import app.streamvibely.ui.motion.staggeredEntrance

/** Only the first rows animate in, so long result lists stay cheap. */
private const val StaggerRows = 8

private data class Category(val name: String, val color: Color)

@Composable
private fun rememberCategories(): List<Category> {
    val tiles = AppTheme.colors.tiles
    return categoryNames.mapIndexed { i, name -> Category(name, tiles[i % tiles.size]) }
}

private val categoryNames = listOf("Made for you", "Electronic", "Chill", "Indie", "Focus", "Podcasts")

@Composable
fun SearchScreen(
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    selectedSource: String = "All sources",
    sources: List<String> = listOf("All sources", "YouTube", "Spotify"),
    onSourceSelected: (String) -> Unit = {},
    results: List<Track> = emptyList(),
    loading: Boolean = false,
    notice: String? = null,
    downloadProgress: Map<String, Float> = emptyMap(),
    onDownload: (Track) -> Unit = {},
    onTrackClick: (Track) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SearchField(query, onQueryChange, Modifier.padding(top = 8.dp))

        LazyRow(
            Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sources, key = { it }) { source ->
                val selected = source == selectedSource
                Box(
                    Modifier.chipSurface(selected)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSourceSelected(source) })
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        source,
                        color = if (selected) AppTheme.colors.onAccent else AppTheme.colors.onCard,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        // Only the blank/non-blank flip matters below, so typing more letters does not
        // recompose the list; the results list itself only changes when `results` does.
        val showResults = query.isNotBlank()
        if (!showResults) {
            CategoryGrid(onCategoryClick, Modifier.weight(1f).padding(top = 4.dp))
        } else if (notice != null && results.isEmpty()) {
            Text(
                notice,
                Modifier.padding(top = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (results.isEmpty()) {
            Text(
                if (loading) "Searching…" else "No results for \"$query\".",
                Modifier.padding(top = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Column(Modifier.weight(1f)) {
                if (notice != null) Text(
                    notice,
                    Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                ResultsList(results, onTrackClick, downloadProgress, onDownload, Modifier.weight(1f).padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier) {
    val ink = AppTheme.colors.onCard
    val hint = AppTheme.colors.muted
    Row(
        modifier.fillMaxWidth().height(54.dp)
            .appSurface(SurfaceKind.Field, RoundedCornerShape(Tokens.radiusTile.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusTile.dp.scaled()))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(AppIcons.Search, null, tint = hint, size = 24.dp)
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
            cursorBrush = SolidColor(ink),
            modifier = Modifier.weight(1f).padding(start = 10.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Songs, artists, albums or a link",
                            color = hint,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun CategoryGrid(onCategoryClick: (String) -> Unit, modifier: Modifier) {
    val categories = rememberCategories()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Browse your world",
                Modifier.padding(top = 12.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        gridItems(categories, key = { it.name }) { category ->
            CategoryTile(category, onClick = { onCategoryClick(category.name) })
        }
    }
}

@Composable
private fun CategoryTile(category: Category, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(120.dp)
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .background(category.color)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
    ) {
        Text(category.name, color = ColorEngine.onColorFor(category.color), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ResultsList(
    results: List<Track>,
    onTrackClick: (Track) -> Unit,
    downloadProgress: Map<String, Float>,
    onDownload: (Track) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        itemsIndexed(results, key = { _, track -> track.id }) { index, track ->
            TrackRow(
                track,
                onClick = { onTrackClick(track) },
                progress = downloadProgress[track.id],
                onDownload = { onDownload(track) },
                modifier = if (index < StaggerRows) Modifier.staggeredEntrance(index) else Modifier,
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    progress: Float?,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourceLabel = when (track.source) {
        "youtube" -> "YouTube"
        "youtube-music" -> "YouTube Music"
        "archive" -> "Internet Archive"
        else -> track.source.replaceFirstChar { it.uppercase() }
    }
    val subtitle = "${track.artist} · $sourceLabel" + if (track.isDownloaded) " · Downloaded" else ""
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.radiusSm.dp.scaled())).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(track, Modifier.size(44.dp).clip(RoundedCornerShape(Tokens.radiusArtwork.dp.scaled())), sizePx = 150)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                track.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Local files are "downloaded" by definition; only online tracks get a download control.
        if (track.source != "local") when {
            progress != null -> androidx.compose.material3.CircularProgressIndicator(
                progress = { progress.coerceIn(0.02f, 1f) },
                modifier = Modifier.padding(end = 12.dp).size(22.dp),
                strokeWidth = 2.5.dp,
            )
            track.isDownloaded -> AppIcon(AppIcons.DownloadDone, "Downloaded", Modifier.padding(end = 12.dp), tint = AppTheme.colors.accent, size = 22.dp)
            track.canDownload -> AppIconButton(AppIcons.Download, "Download ${track.title}", onDownload)
        }
    }
}
