package app.streamvibely.ui.settings

import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.unit.dp
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon

/** Hero card at the top: the listening space and how many songs the library holds. */
@Composable
internal fun SettingsHero(trackCount: Int, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Your listening space",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "$trackCount songs on this device",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier.padding(start = 4.dp, top = 12.dp),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge,
    )
}

/** A surface row with an optional leading icon, a title and a trailing chevron. */
@Composable
internal fun SettingsNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            AppIcon(icon, null, Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 24.dp)
        }
        Text(
            title,
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
        )
        AppIcon(
            AppIcons.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 24.dp,
        )
    }
}

/** A radio-style option: the selected one gets a primary ring and dot. */
@Composable
internal fun SettingsRadioRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(20.dp).border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                title,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * Pill button. [primary] uses the accent; the secondary one is smaller text on a raised
 * container. Disabled buttons keep their label visible but ignore clicks.
 */
@Composable
internal fun SettingsButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainer
        primary -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        primary -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier
            .clip(RoundedCornerShape(Tokens.radiusChip.dp.scaled()))
            .background(container)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = content,
            style = if (primary) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
        )
    }
}
