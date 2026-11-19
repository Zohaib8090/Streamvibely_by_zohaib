package app.streamvibely.ui.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.Tokens
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.AppIcons

/**
 * Dialog that edits one color live. [color] is the current value (the generated color when no
 * override exists). [onColorChange] fires while picking; [onUseAutomatic] removes the override.
 */
@Composable
internal fun ColorPickerDialog(
    title: String,
    color: Int,
    onColorChange: (Int) -> Unit,
    onUseAutomatic: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.radiusCard.dp * 1.5f))
                .background(AppTheme.colors.surface)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppIcons.Colorize, null, Modifier.padding(end = 10.dp), tint = AppTheme.colors.accent, size = 22.dp)
                Text(title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
            }
            ColorPicker(color = color, onColorChange = onColorChange)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                DialogButton("Use automatic color", primary = false, onClick = {
                    onUseAutomatic()
                    onDismiss()
                })
                DialogButton("Done", primary = true, onClick = onDismiss)
            }
        }
    }
}

@Composable
internal fun DialogButton(label: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(Tokens.radiusChip.dp))
            .background(if (primary) AppTheme.colors.accent else AppTheme.colors.cardHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (primary) AppTheme.colors.onAccent else AppTheme.colors.onCard,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
