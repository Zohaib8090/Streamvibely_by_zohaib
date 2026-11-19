package app.streamvibely.widgets

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/** Fixed dark Streamvibely colors for widgets (no Material You dynamic colors). */
internal object WidgetColors {
    val background = ColorProvider(Color(0xFF1B281D))
    val primary = ColorProvider(Color(0xFFC1F479))
    val onPrimary = ColorProvider(Color(0xFF1B281D))
    val text = ColorProvider(Color(0xFFF5F8F2))
    val textSecondary = ColorProvider(Color(0xFFA5AFA7))
}
