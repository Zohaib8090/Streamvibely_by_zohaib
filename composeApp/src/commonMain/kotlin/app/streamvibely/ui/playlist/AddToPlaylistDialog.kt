package app.streamvibely.ui.playlist

import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.Playlist
import app.streamvibely.ui.common.AppIcon

/** Lists playlists to add a track to, with a "New playlist" row on top. */
@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onPick: (Playlist) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            LazyColumn {
                item(key = "new") {
                    DialogRow("New playlist", MaterialTheme.colorScheme.primary, AppIcons.Add, onCreateNew)
                }
                items(playlists, key = { it.id }) { playlist ->
                    DialogRow(playlist.name, MaterialTheme.colorScheme.onSurface) { onPick(playlist) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DialogRow(label: String, color: Color, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            AppIcon(icon, null, tint = color, size = 20.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(label, color = color, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
    }
}
