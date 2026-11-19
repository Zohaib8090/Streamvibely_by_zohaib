package app.streamvibely.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.streamvibely.domain.Track
import app.streamvibely.theme.engine.ColorEngine
import app.streamvibely.ui.common.LocalArtworkLoader

/**
 * The dominant color of [track]'s cover as ARGB, or null when there is no track or no cover.
 * Keeps the previous seed while the next cover loads so the theme does not flicker.
 */
@Composable
fun rememberArtSeed(track: Track?): Int? {
    val loader = LocalArtworkLoader.current
    var seed by remember { mutableStateOf<Int?>(null) }
    val id = track?.id
    LaunchedEffect(id, loader) {
        if (track == null || loader == null) {
            seed = null
            return@LaunchedEffect
        }
        val bitmap = loader.load(track, 64)
        seed = if (bitmap == null) null else ColorEngine.seedFromBitmap(bitmap)
    }
    return seed
}

/** A small copy of [track]'s cover for the blurred Glass backdrop, or null (only loaded while [enabled]). */
@Composable
fun rememberArtBackdrop(track: Track?, enabled: Boolean): androidx.compose.ui.graphics.ImageBitmap? {
    val loader = LocalArtworkLoader.current
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val id = track?.id
    LaunchedEffect(id, loader, enabled) {
        bitmap = if (!enabled || track == null || loader == null) null else loader.load(track, 96)
    }
    return bitmap
}
