package app.streamvibely.ui.nowplaying

import app.streamvibely.ui.motion.StreamvibelyMotion
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import app.streamvibely.ui.common.AppIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.Lyrics
import app.streamvibely.domain.PlayerUiState
import app.streamvibely.domain.SampleData
import app.streamvibely.domain.Track
import app.streamvibely.lyrics.LyricsView
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.ArtworkImage
import app.streamvibely.ui.motion.MorphingPlayButton
import app.streamvibely.ui.motion.bounceClickable
import app.streamvibely.ui.motion.fadeThroughTransition

/** Playlist the queue came from. The player state does not carry it yet, so sample data stands in. */

@Composable
fun NowPlayingScreen(
    state: PlayerUiState = PlayerUiState(
        current = SampleData.tracks.first(),
        isPlaying = true,
        positionMs = 154_000,
        queue = SampleData.tracks,
    ),
    source: String = "YouTube",
    quality: String = "High quality",
    onBack: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onShuffle: () -> Unit = {},
    onRepeat: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    modifier: Modifier = Modifier,
    lyrics: Lyrics? = null,
    showLyrics: Boolean = false,
    onToggleLyrics: () -> Unit = {},
    upgradeOfferText: String? = null,
    onUpgradeQuality: () -> Unit = {},
    onDismissUpgrade: () -> Unit = {},
    onDownload: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
) {
    val track = state.current
    // No scrolling: the cover/lyrics area takes whatever height the controls leave free.
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxSize().padding(horizontal = 24.dp)) {
            TopBar(source, onBack)
            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = fadeThroughTransition(),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp),
                label = "coverOrLyrics",
            ) { lyricsShown ->
                if (lyricsShown) {
                    LyricsView(lyrics, state.positionMs, onSeek)
                } else {
                    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ArtworkImage(
                            track,
                            Modifier.size(minOf(maxWidth, maxHeight, 360.dp))
                                .clip(RoundedCornerShape(Tokens.radiusPanel.dp.scaled())),
                            sizePx = 600,
                        )
                    }
                }
            }
            Column(Modifier.padding(top = 12.dp)) {
                Text(track?.title.orEmpty(), style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track?.artist.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(source)
                InfoChip(quality)
            }
            if (upgradeOfferText != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        upgradeOfferText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Upgrade",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onUpgradeQuality).padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Text(
                            "✕",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.clickable(onClick = onDismissUpgrade).padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            val duration = track?.durationMs ?: 0L
            SeekBar(state.positionMs, duration, onSeek, Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TimeLabel(formatTime(state.positionMs))
                TimeLabel(formatTime(duration))
            }
            Transport(state.isPlaying, state.shuffle, state.repeat, onShuffle, onPrevious, onPlayPause, onNext, onRepeat)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (onDownload != null) {
                    ActionButton(if (isDownloaded) AppIcons.DownloadDone else AppIcons.Download, if (isDownloaded) "Downloaded" else "Download", onDownload)
                } else {
                    ActionButton(AppIcons.Devices, "This device", {})
                }
                ActionButton(AppIcons.Equalizer, "Equalizer", onOpenEqualizer)
                ActionButton(AppIcons.Lyrics, "Lyrics", onToggleLyrics, active = showLyrics)
                ActionButton(AppIcons.QueueMusic, "Queue", onOpenQueue)
            }
        }
    }
}

/** Shared by the Now playing and Queue screens: back, title, more. */
@Composable
internal fun ScreenHeader(onBack: () -> Unit, modifier: Modifier = Modifier, title: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIconButton(AppIcons.ArrowBack, "Back", onBack, size = 40.dp)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { title() }
        AppIconButton(AppIcons.MoreVert, "More options", {}, size = 40.dp)
    }
}

@Composable
private fun TopBar(source: String, onBack: () -> Unit) {
    ScreenHeader(onBack) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NOW PLAYING", style = MaterialTheme.typography.labelMedium)
            Text(source.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Transport(
    isPlaying: Boolean,
    shuffle: Boolean,
    repeat: app.streamvibely.domain.RepeatMode,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        AppIconButton(AppIcons.Shuffle, "Shuffle", onShuffle, active = shuffle)
        AppIconButton(AppIcons.SkipPrevious, "Previous", onPrevious, size = 48.dp)
        MorphingPlayButton(isPlaying, onPlayPause, size = 64.dp)
        AppIconButton(AppIcons.SkipNext, "Next", onNext, size = 48.dp)
        AppIconButton(
            if (repeat == app.streamvibely.domain.RepeatMode.One) AppIcons.RepeatOne else AppIcons.Repeat,
            when (repeat) { app.streamvibely.domain.RepeatMode.Off -> "Repeat off"; app.streamvibely.domain.RepeatMode.All -> "Repeat all"; else -> "Repeat one" },
            onRepeat,
            active = repeat != app.streamvibely.domain.RepeatMode.Off,
        )
    }
}

@Composable
private fun InfoChip(label: String) {
    Box(
        Modifier.appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusChipLarge.dp.scaled())).clip(RoundedCornerShape(Tokens.radiusChipLarge.dp.scaled()))
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TimeLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit, active: Boolean = false) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.clip(RoundedCornerShape(Tokens.radiusSm.dp.scaled())).bounceClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(icon, null, tint = if (active) color else MaterialTheme.colorScheme.onSurface, size = 24.dp)
        Text(label, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Round icon button. Icons are 28dp in 48dp-or-larger targets and 24dp otherwise.
 * When [active] the button morphs from a circle into a rounded tile with an accent container,
 * and pops with a spring each time the state flips, so shuffle/repeat clicks feel expressive.
 */
@Composable
internal fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
    size: Dp = 44.dp,
) {
    val iconSize = if (size >= 48.dp) 28.dp else 24.dp
    val colors = app.streamvibely.theme.AppTheme.colors
    val corner by androidx.compose.animation.core.animateFloatAsState(
        if (active) 32f else 50f, StreamvibelyMotion.spatialDefault(), label = "toggleCorner",
    )
    val fill by androidx.compose.animation.animateColorAsState(
        if (active) colors.accentContainer else androidx.compose.ui.graphics.Color.Transparent,
        StreamvibelyMotion.effectsDefault(), label = "toggleFill",
    )
    val pop = remember { androidx.compose.animation.core.Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(active, icon) {
        if (first) { first = false; return@LaunchedEffect }
        pop.snapTo(0.72f)
        pop.animateTo(1f, StreamvibelyMotion.spatialFast())
    }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(corner.toInt())
    Box(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .clip(shape)
            .background(fill, shape)
            .bounceClickable(role = Role.Button, pressedScale = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            icon,
            contentDescription,
            tint = if (active) colors.onAccentContainer else MaterialTheme.colorScheme.onSurface,
            size = iconSize,
        )
    }
}
