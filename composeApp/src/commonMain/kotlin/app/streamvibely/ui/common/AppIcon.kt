package app.streamvibely.ui.common

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

/**
 * The one way to draw an icon. Use the rounded Material set: `AppIcons.Search`,
 * `AppIcons.PlayArrow`, ... (import each from `androidx.compose.material.icons.rounded`).
 * Never use text glyphs or emoji as icons. Pass `contentDescription = null` for decorative icons
 * that sit next to a text label.
 */
@Composable
fun AppIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
) {
    Icon(icon, contentDescription, modifier.size(size), tint)
}
