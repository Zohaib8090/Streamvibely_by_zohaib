package app.streamvibely.ui.account

import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.appSurface
import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.Account
import app.streamvibely.ui.common.AppIcon

/**
 * Round avatar: the account's initial on a colored circle (images are not loaded), or a person
 * icon on a neutral circle when nobody is signed in.
 */
@Composable
internal fun AccountAvatar(account: Account?, size: Dp, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    if (account == null) {
        Box(
            modifier.size(size).clip(CircleShape).background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(AppIcons.Person, null, tint = scheme.onSurfaceVariant, size = size * 0.6f)
        }
        return
    }
    val index = account.displayName.sumOf { it.code } % 3
    val container = when (index) {
        0 -> scheme.primaryContainer
        1 -> scheme.secondaryContainer
        else -> scheme.tertiaryContainer
    }
    val content = when (index) {
        0 -> scheme.onPrimaryContainer
        1 -> scheme.onSecondaryContainer
        else -> scheme.onTertiaryContainer
    }
    val initial = account.displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier.size(size).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial,
            color = content,
            style = if (size >= 56.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
        )
    }
}

/** Page header with a round back button and the page title. */
@Composable
internal fun AccountPageHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).appSurface(SurfaceKind.Chip, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .semantics { contentDescription = "Back" },
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(AppIcons.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            title,
            Modifier.padding(start = 12.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
