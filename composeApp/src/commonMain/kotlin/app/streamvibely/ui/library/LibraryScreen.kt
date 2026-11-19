package app.streamvibely.ui.library

import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.chipSurface
import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.Playlist
import app.streamvibely.domain.SampleData
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.ArtworkImage
import app.streamvibely.ui.motion.staggeredEntrance

enum class LibraryTab { Playlists, Albums, Artists, Downloads }

/** Only the first rows animate in, so long lists stay cheap. */
private const val StaggerRows = 8

@Composable
fun LibraryScreen(
    playlists: List<Playlist> = SampleData.playlists,
    selectedTab: LibraryTab = LibraryTab.Playlists,
    onTabSelected: (LibraryTab) -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit = {},
    onCreatePlaylist: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Albums and Artists have no data in this screen's contract yet, so they show an empty state.
    val visible = remember(playlists, selectedTab) {
        when (selectedTab) {
            LibraryTab.Playlists -> playlists
            LibraryTab.Downloads -> playlists.filter { p -> p.tracks.any { it.isDownloaded && it.source != "local" } }
            LibraryTab.Albums, LibraryTab.Artists -> emptyList()
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Your library",
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
            )
            Box(
                Modifier.size(40.dp).appSurface(SurfaceKind.Chip, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onCreatePlaylist)
                    .semantics { contentDescription = "Create playlist" },
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(AppIcons.Add, null, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LibraryTab.entries, key = { it.name }) { tab ->
                val selected = tab == selectedTab
                Box(
                    Modifier.chipSurface(selected)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onTabSelected(tab) })
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        tab.name,
                        color = if (selected) AppTheme.colors.onAccent else AppTheme.colors.onCard,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Recently played",
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "${playlists.size} collections",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (visible.isEmpty()) {
            Text(
                "Nothing in ${selectedTab.name.lowercase()} yet.",
                Modifier.padding(top = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(visible, key = { _, p -> p.id }) { index, playlist ->
                    PlaylistRow(
                        playlist,
                        onClick = { onPlaylistClick(playlist) },
                        modifier = if (index < StaggerRows) Modifier.staggeredEntrance(index) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val downloaded = playlist.tracks.any { it.isDownloaded }
    val subtitle = "Playlist · ${playlist.tracks.size} songs" + if (downloaded) " · Downloaded" else ""

    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.radiusSm.dp.scaled())).clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(
            playlist.tracks.firstOrNull(),
            Modifier.size(50.dp).clip(RoundedCornerShape(Tokens.radiusArtwork.dp.scaled())),
            sizePx = 150,
        )
        Column(Modifier.weight(1f).padding(start = 13.dp)) {
            Text(playlist.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}
