package app.streamvibely.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.SpotifySyncState
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.AppIcons

@Composable
internal fun SpotifySyncCard(
    isConnected: Boolean,
    syncState: SpotifySyncState,
    statusMessage: String?,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Spotify Web Sync",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (isConnected) "Session connected (sp_dc)" else "Sign in to sync your Spotify library",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (syncState == SpotifySyncState.Syncing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }

        if (statusMessage != null) {
            Text(
                statusMessage,
                color = when (syncState) {
                    SpotifySyncState.Error -> MaterialTheme.colorScheme.error
                    SpotifySyncState.Done -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!isConnected) {
                Row(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(Tokens.radiusChip.dp.scaled()))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onConnect)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(AppIcons.Login, null, tint = MaterialTheme.colorScheme.onPrimary, size = 18.dp)
                    Text(
                        "Connect Spotify",
                        Modifier.padding(start = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            } else {
                Row(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(Tokens.radiusChip.dp.scaled()))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = syncState != SpotifySyncState.Syncing, onClick = onSync)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(AppIcons.Download, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 18.dp)
                    Text(
                        if (syncState == SpotifySyncState.Syncing) "Syncing..." else "Sync Songs & Playlists",
                        Modifier.padding(start = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                Row(
                    Modifier.clip(RoundedCornerShape(Tokens.radiusChip.dp.scaled()))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(onClick = onDisconnect)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(AppIcons.Logout, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                    Text(
                        "Disconnect",
                        Modifier.padding(start = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

