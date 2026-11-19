package app.streamvibely.lyrics

import app.streamvibely.theme.AppTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.LyricLine
import app.streamvibely.domain.Lyrics

@Composable
fun LyricsView(
    lyrics: Lyrics?,
    positionMs: Long,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        when (lyrics) {
            null -> Text(
                "No lyrics found",
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.colors.lyricsInactive,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
            )
            is Lyrics.Plain -> PlainLyrics(lyrics, Modifier.weight(1f))
            is Lyrics.Synced -> SyncedLyrics(lyrics.lines, positionMs, onSeek, Modifier.weight(1f))
        }
        if (lyrics != null) {
            Text(
                "via ${lyrics.source}",
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.colors.muted,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun PlainLyrics(lyrics: Lyrics.Plain, modifier: Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Text(
            "Lyrics are not time-synced",
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.muted,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Text(lyrics.text, style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.onSurface.copy(alpha = 0.85f))
    }
}

@Composable
private fun SyncedLyrics(lines: List<LyricLine>, positionMs: Long, onSeek: (Long) -> Unit, modifier: Modifier) {
    val currentIndex by remember(lines) {
        derivedStateOf { lines.indexOfLast { it.startMs <= positionMs } }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(lines, currentIndex) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(currentIndex.coerceAtLeast(0))
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Padding of half the viewport lets the current line settle at the vertical center.
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = maxHeight / 2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(lines, key = { index, line -> "$index-${line.startMs}" }) { index, line ->
                val current = index == currentIndex
                val color by animateColorAsState(if (current) AppTheme.colors.onSurface else AppTheme.colors.lyricsInactive)
                Text(
                    line.text,
                    color = color,
                    style = if (current) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().clickable { onSeek(line.startMs) },
                )
            }
        }
    }
}
