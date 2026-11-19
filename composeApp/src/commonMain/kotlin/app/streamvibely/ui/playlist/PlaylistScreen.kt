package app.streamvibely.ui.playlist

import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.Playlist
import app.streamvibely.domain.SampleData
import app.streamvibely.domain.Track
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.ArtworkImage
import app.streamvibely.ui.motion.bounceClickable
import app.streamvibely.ui.motion.staggeredEntrance

/** Only the first rows animate in, so long lists stay cheap. */
private const val StaggerRows = 8

/**
 * Playlist detail: header, cover, title and summary, actions, sorting controls, then the track list.
 * Content is capped at 720dp and centered on wide screens (>= 840dp).
 */
@Composable
fun PlaylistScreen(
    playlist: Playlist = SampleData.playlists.first(),
    currentTrackId: String? = null,
    onBack: () -> Unit = {},
    onPlayAll: (List<Track>) -> Unit = {},
    onShuffle: (List<Track>) -> Unit = {},
    onTrackClick: (Track, List<Track>) -> Unit = { _, _ -> },
    onDownloadAll: () -> Unit = {},
    onDownloadTrack: ((Track) -> Unit)? = null,
    onMore: (Track) -> Unit = {},
    modifier: Modifier = Modifier,
    onAddToPlaylist: (Track) -> Unit = {},
    onPlayNext: (Track) -> Unit = {},
    onRemoveDownload: ((Track) -> Unit)? = null,
    notice: String? = null,
    downloadProgress: Map<String, Float> = emptyMap(),
    repeat: app.streamvibely.domain.RepeatMode = app.streamvibely.domain.RepeatMode.Off,
    shuffleOn: Boolean = false,
    onRepeat: () -> Unit = {},
) {
    var sortOrder by remember(playlist.id) { mutableStateOf(PlaylistSortOrder.Custom) }
    var showSortSheet by remember { mutableStateOf(false) }

    val sortedTracks = remember(playlist.tracks, sortOrder) {
        when (sortOrder) {
            PlaylistSortOrder.Custom -> playlist.tracks
            PlaylistSortOrder.Title -> playlist.tracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            PlaylistSortOrder.Artist -> playlist.tracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist })
            PlaylistSortOrder.Album -> playlist.tracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.album.orEmpty() })
            PlaylistSortOrder.RecentlyAdded -> playlist.tracks.reversed()
            PlaylistSortOrder.ReleaseDate -> playlist.tracks.reversed()
            PlaylistSortOrder.Duration -> playlist.tracks.sortedByDescending { it.durationMs }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val columnWidth = if (maxWidth >= 840.dp) 720.dp else maxWidth
        LazyColumn(Modifier.width(columnWidth).fillMaxHeight()) {
            item { Header(onBack, onOpenSort = { showSortSheet = true }) }
            item { Cover(playlist) }
            item { Details(playlist) }
            item {
                ActionRow(
                    playlist = playlist,
                    progress = downloadProgress,
                    onDownloadAll = onDownloadAll,
                    onShuffle = { onShuffle(sortedTracks) },
                    onPlayAll = { onPlayAll(sortedTracks) },
                    repeat = repeat,
                    shuffleOn = shuffleOn,
                    onRepeat = onRepeat,
                )
            }
            if (notice != null) item {
                Text(
                    notice,
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            // Sorter chip and track counter row
            if (playlist.tracks.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showSortSheet = true }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            AppIcon(AppIcons.Sort, null, tint = Color(0xFF1ED760), size = 18.dp)
                            Text(
                                sortOrder.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            "${sortedTracks.size} songs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (playlist.tracks.isEmpty()) {
                item {
                    Text(
                        "No songs yet",
                        Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            itemsIndexed(items = sortedTracks, key = { _, t -> t.id }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    onClick = { onTrackClick(track, sortedTracks) },
                    onMore = { onMore(track) },
                    modifier = if (index < StaggerRows) Modifier.staggeredEntrance(index) else Modifier,
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onPlayNext = { onPlayNext(track) },
                    onDownload = if (!track.isDownloaded && track.source != "local" && onDownloadTrack != null) ({ onDownloadTrack(track) }) else null,
                    downloadProgress = downloadProgress[track.id],
                    onRemoveDownload = if (onRemoveDownload != null && track.isDownloaded && track.source != "local") ({ onRemoveDownload(track) }) else null,
                )
            }
        }

        // Spotify-style Sort Bottom Sheet
        PlaylistSortSheet(
            visible = showSortSheet,
            currentSort = sortOrder,
            onSelectSort = { sortOrder = it },
            onDismiss = { showSortSheet = false },
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit, onOpenSort: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(AppIcons.ArrowBack, "Back", onBack)
        Text(
            "PLAYLIST",
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
        AppIconButton(AppIcons.Sort, "Sort by", onOpenSort)
    }
}

@Composable
private fun Cover(playlist: Playlist) {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        ArtworkImage(
            playlist.tracks.firstOrNull(),
            Modifier.size(width = 190.dp, height = 180.dp).clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled())),
            sizePx = 600,
        )
    }
}

@Composable
private fun Details(playlist: Playlist) {
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            playlist.name,
            style = MaterialTheme.typography.headlineLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        playlist.description?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "Streamvibely  ·  ${summary(playlist.tracks)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ActionRow(
    playlist: Playlist,
    progress: Map<String, Float>,
    onDownloadAll: () -> Unit,
    onShuffle: () -> Unit,
    onPlayAll: () -> Unit,
    repeat: app.streamvibely.domain.RepeatMode,
    shuffleOn: Boolean,
    onRepeat: () -> Unit,
) {
    val saveable = playlist.tracks.filter { it.source != "local" || it.isDownloaded }
    val finished = saveable.count { it.isDownloaded }
    val running = saveable.filter { !it.isDownloaded && progress[it.id] != null }
    val percent = if (running.isEmpty() || saveable.isEmpty()) null
    else (((finished + running.sumOf { progress[it.id]!!.toDouble() }) / saveable.size) * 100).toInt().coerceIn(0, 99)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (percent != null) {
            androidx.compose.material3.CircularProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.size(28.dp).padding(2.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
            )
            Text(
                "  Downloading $percent%",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        } else {
            AppIconButton(
                if (saveable.isNotEmpty() && finished == saveable.size) AppIcons.DownloadDone else AppIcons.Download,
                "Download all",
                onDownloadAll,
            )
            if (saveable.isNotEmpty() && finished > 0) {
                Text(
                    "$finished/${saveable.size} saved",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        app.streamvibely.ui.nowplaying.AppIconButton(
            if (repeat == app.streamvibely.domain.RepeatMode.One) AppIcons.RepeatOne else AppIcons.Repeat,
            when (repeat) {
                app.streamvibely.domain.RepeatMode.Off -> "Repeat off"
                app.streamvibely.domain.RepeatMode.All -> "Repeat playlist"
                else -> "Repeat one song"
            },
            onRepeat,
            active = repeat != app.streamvibely.domain.RepeatMode.Off,
        )
        app.streamvibely.ui.nowplaying.AppIconButton(AppIcons.Shuffle, "Shuffle", onShuffle, active = shuffleOn)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .bounceClickable(role = Role.Button, onClick = onPlayAll)
                .semantics { contentDescription = "Play playlist" },
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(AppIcons.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, size = 32.dp)
        }
    }
}

/** "24 songs, 1 hr 48 min". Total length is floored to whole minutes. */
private fun summary(tracks: List<Track>): String {
    val count = if (tracks.size == 1) "1 song" else "${tracks.size} songs"
    val totalMinutes = tracks.sumOf { it.durationMs } / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val length = if (hours > 0) "$hours hr $minutes min" else "$minutes min"
    return "$count, $length"
}
