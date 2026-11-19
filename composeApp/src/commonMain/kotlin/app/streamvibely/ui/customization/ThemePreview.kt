package app.streamvibely.ui.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.Tokens
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.AppIcons

/** A small mock of the app drawn with the real theme colors, so it reflects every change live. */
@Composable
internal fun ThemePreview(cornerScale: Float, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    fun r(base: Int) = RoundedCornerShape((base * cornerScale).dp)
    Column(
        modifier.fillMaxWidth().clip(r(Tokens.radiusTile)).background(c.background),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Good evening", color = c.onBackground, style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth().clip(r(Tokens.radiusCard)).background(c.card).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(r(Tokens.radiusArtwork)).background(c.accentContainer), contentAlignment = Alignment.Center) {
                    AppIcon(AppIcons.MusicNote, null, tint = c.onAccentContainer, size = 22.dp)
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Liked songs", color = c.onCard, style = MaterialTheme.typography.titleSmall)
                    Text("128 songs", color = c.muted, style = MaterialTheme.typography.bodySmall)
                }
                AppIcon(AppIcons.ChevronRight, null, tint = c.muted, size = 22.dp)
            }
            Row(
                Modifier.fillMaxWidth().clip(r(Tokens.radiusCard)).background(c.miniPlayer).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).clip(r(Tokens.radiusArtworkSmall)).background(c.accent))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Midnight City", color = c.onMiniPlayer, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text("Now playing", color = c.onMiniPlayer.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
                Box(Modifier.size(36.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                    AppIcon(AppIcons.PlayArrow, "Play", tint = c.onAccent, size = 22.dp)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(52.dp).background(c.navigation),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(AppIcons.Home, "Home", tint = c.accent, size = 24.dp)
            AppIcon(AppIcons.Search, "Search", tint = c.onNavigation.copy(alpha = 0.7f), size = 24.dp)
            AppIcon(AppIcons.LibraryMusic, "Library", tint = c.onNavigation.copy(alpha = 0.7f), size = 24.dp)
        }
    }
}
