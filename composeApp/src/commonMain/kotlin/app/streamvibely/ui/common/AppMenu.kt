package app.streamvibely.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.ThemeStyle
import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.scaled

/**
 * A dropdown menu drawn with the current theme: panel color, theme corner radius, and the
 * style's own edge (Glass: light border, OLED: hairline, Clay/Material: none). Use this instead of
 * a bare Material DropdownMenu so menus follow Material, OLED, Glass and Clay.
 */
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = AppTheme.colors
    val container = when (c.style) {
        ThemeStyle.Glass -> c.cardHigh.copy(alpha = 0.94f)
        ThemeStyle.Oled -> c.card
        else -> c.cardHigh
    }
    val border = when (c.style) {
        ThemeStyle.Glass -> BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
        ThemeStyle.Oled -> BorderStroke(1.dp, c.outlineVariant)
        else -> null
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(Tokens.radiusCard.dp.scaled()),
        containerColor = container,
        tonalElevation = 0.dp,
        shadowElevation = if (c.style == ThemeStyle.Clay) 12.dp else 6.dp,
        border = border,
        content = { content() },
    )
}

/** A menu row whose text and icons use the theme's colors. */
@Composable
fun AppMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    destructive: Boolean = false,
) {
    val c = AppTheme.colors
    val color = when {
        destructive -> c.error
        selected -> c.accent
        else -> c.onCard
    }
    DropdownMenuItem(
        text = { Text(text, color = color, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick,
        modifier = modifier,
        trailingIcon = if (selected) ({ AppIcon(AppIcons.Check, null, tint = c.accent, size = 18.dp) }) else null,
        colors = MenuDefaults.itemColors(textColor = color, trailingIconColor = c.accent),
    )
}
