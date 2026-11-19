package app.streamvibely.ui.playlist

import app.streamvibely.theme.AppTheme
import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.Track
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.ArtworkImage
import app.streamvibely.ui.common.AppMenu
import app.streamvibely.ui.common.AppMenuItem

/** A round 40dp touch target holding one icon. */
@Composable
internal fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(icon, contentDescription, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
    }
}

@Composable
internal fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToPlaylist: () -> Unit = {},
    onPlayNext: () -> Unit = {},
    onDownload: (() -> Unit)? = null,
    onRemoveDownload: (() -> Unit)? = null,
    downloadProgress: Float? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .background(if (isCurrent) AppTheme.colors.accentContainer.copy(alpha = 0.6f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(
            track,
            Modifier.size(50.dp).clip(RoundedCornerShape(Tokens.radiusArtwork.dp.scaled())),
            sizePx = 150,
        )
        Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
            Text(
                track.title,
                color = if (isCurrent) scheme.primary else scheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle(track),
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (downloadProgress != null && !track.isDownloaded) {
            androidx.compose.material3.CircularProgressIndicator(
                progress = { downloadProgress.coerceIn(0.02f, 1f) },
                modifier = Modifier.padding(end = 6.dp).size(20.dp),
                strokeWidth = 2.5.dp,
                color = scheme.primary,
                trackColor = scheme.onSurfaceVariant.copy(alpha = 0.25f),
            )
            Text("${(downloadProgress * 100).toInt().coerceIn(0, 99)}%", Modifier.padding(end = 4.dp), color = scheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        } else if (track.isDownloaded) {
            AppIcon(
                AppIcons.DownloadDone,
                "Downloaded",
                Modifier.padding(end = 4.dp),
                tint = scheme.primary,
                size = 18.dp,
            )
        }
        Box {
            AppIconButton(AppIcons.MoreVert, "More options for ${track.title}", { menuOpen = true; onMore() })
            AppMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                AppMenuItem("Add to playlist", { menuOpen = false; onAddToPlaylist() })
                AppMenuItem("Play next", { menuOpen = false; onPlayNext() })
                if (!track.isDownloaded && track.source != "local" && onDownload != null) {
                    AppMenuItem("Download song", { menuOpen = false; onDownload() })
                }
                if (onRemoveDownload != null) AppMenuItem("Remove download", { menuOpen = false; onRemoveDownload() })
            }
        }
    }
}

private fun subtitle(track: Track): String =
    listOf(track.artist, sourceLabel(track.source), track.license).filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")

private fun sourceLabel(source: String): String = when (source.lowercase()) {
    "youtube" -> "YouTube"
    "spotify" -> "Spotify"
    "local" -> "Local"
    "jamendo" -> "Jamendo"
    else -> source.replaceFirstChar { it.uppercase() }
}
