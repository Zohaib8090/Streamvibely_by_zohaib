package app.streamvibely.ui.nowplaying

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.PlayerUiState
import app.streamvibely.domain.SampleData
import app.streamvibely.domain.Track
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.ArtworkImage

@Composable
fun QueueScreen(
    state: PlayerUiState = PlayerUiState(
        current = SampleData.tracks.first(),
        isPlaying = true,
        positionMs = 154_000,
        queue = SampleData.tracks,
    ),
    onTrackClick: (Track) -> Unit = {},
    onClear: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val upNext = remember(state.queue, state.current) {
        val index = state.queue.indexOfFirst { it.id == state.current?.id }
        if (index >= 0) state.queue.drop(index + 1) else state.queue
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 640.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "header") {
                ScreenHeader(onBack) { Text("Play queue", style = MaterialTheme.typography.headlineSmall) }
            }
            item(key = "now-title") {
                Text("Now playing", Modifier.padding(top = 16.dp, bottom = 4.dp), style = MaterialTheme.typography.headlineSmall)
            }
            state.current?.let { current ->
                item(key = "now-card") { CurrentCard(current, state.isPlaying) { onTrackClick(current) } }
            }
            item(key = "next-title") {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Next in queue",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Clear",
                        Modifier.clip(RoundedCornerShape(Tokens.radiusSm.dp.scaled())).clickable(onClick = onClear).padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            items(upNext, key = { it.id }) { track -> QueueRow(track) { onTrackClick(track) } }
        }
    }
}

@Composable
private fun CurrentCard(track: Track, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled())).background(AppTheme.colors.accentContainer)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(track, 56)
        TrackText(track, Modifier.weight(1f).padding(horizontal = 12.dp))
        AppIcon(
            if (isPlaying) AppIcons.Pause else AppIcons.PlayArrow,
            if (isPlaying) "Pause" else "Play",
            tint = MaterialTheme.colorScheme.primary,
            size = 24.dp,
        )
    }
}

@Composable
private fun QueueRow(track: Track, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.radiusSm.dp.scaled())).clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(track, 48)
        TrackText(track, Modifier.weight(1f).padding(horizontal = 12.dp))
        Text(formatTime(track.durationMs), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        // Visual only: reordering is not implemented.
        AppIcon(AppIcons.DragHandle, null, Modifier.padding(start = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 24.dp)
    }
}

@Composable
private fun Cover(track: Track, sizeDp: Int) {
    ArtworkImage(track, Modifier.size(sizeDp.dp).clip(RoundedCornerShape(Tokens.radiusArtwork.dp.scaled())), sizePx = 150)
}

@Composable
private fun TrackText(track: Track, modifier: Modifier) {
    Column(modifier) {
        Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
