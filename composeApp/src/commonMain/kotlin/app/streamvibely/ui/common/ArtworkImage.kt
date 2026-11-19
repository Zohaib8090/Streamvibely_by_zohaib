package app.streamvibely.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import app.streamvibely.domain.ArtworkLoader
import app.streamvibely.domain.Track
import app.streamvibely.theme.AppTheme

/** The platform supplies this. Null means "no covers available", so placeholders are shown. */
val LocalArtworkLoader = staticCompositionLocalOf<ArtworkLoader?> { null }

/** A stable placeholder color per track so lists are not a wall of one color. */
@Composable
fun placeholderTint(id: String): Color = AppTheme.colors.tiles[(id.hashCode() and 0x7fffffff) % 6]

/**
 * Cover art for [track], decoded at about [sizePx] pixels. Shows a colored placeholder until the
 * image is ready or when the track has no cover. Apply the shape with `Modifier.clip` outside.
 */
@Composable
fun ArtworkImage(track: Track?, modifier: Modifier = Modifier, sizePx: Int = 300) {
    val loader = LocalArtworkLoader.current
    val bitmap by produceState<ImageBitmap?>(null, track?.id, loader, sizePx) {
        value = if (track != null && loader != null) loader.load(track, sizePx) else null
    }
    Box(modifier.background(placeholderTint(track?.id ?: "none"))) {
        val art = bitmap
        if (art != null) {
            Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            // No cover: a music note on the tinted placeholder.
            Icon(
                AppIcons.MusicNote, null,
                Modifier.align(Alignment.Center).fillMaxSize(0.46f),
                tint = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}
