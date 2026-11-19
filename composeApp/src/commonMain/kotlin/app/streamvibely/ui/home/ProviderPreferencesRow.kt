package app.streamvibely.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.PlaybackQualityPreference
import app.streamvibely.domain.ProviderOption
import app.streamvibely.domain.SpotifySyncState
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.AppIcons

@Composable
fun ProviderPreferencesRow(
    providers: List<ProviderOption>,
    qualityPreferences: Map<String, PlaybackQualityPreference>,
    onQualityCycle: (String) -> Unit,
    spotifyConnected: Boolean,
    spotifySyncState: SpotifySyncState,
    onSyncSpotify: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Streaming & Provider Preferences",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                "Tap to change quality",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        LazyRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. Streaming providers with their quality ladder
            items(providers.filter { it.enabled }, key = { "pref-" + it.id }) { provider ->
                val currentQuality = qualityPreferences[provider.id] ?: PlaybackQualityPreference.Auto
                ProviderQualityPill(
                    provider = provider,
                    quality = currentQuality,
                    onClick = { onQualityCycle(provider.id) },
                )
            }

            // 2. Spotify Sync & Connection status pill
            item(key = "pref-spotify") {
                SpotifyStatusPill(
                    isConnected = spotifyConnected,
                    syncState = spotifySyncState,
                    onClick = onSyncSpotify,
                )
            }

            // 3. Settings shortcut pill
            item(key = "pref-settings") {
                Row(
                    Modifier.appSurface(SurfaceKind.Chip, RoundedCornerShape(Tokens.radiusChip.dp.scaled()))
                        .clip(RoundedCornerShape(Tokens.radiusChip.dp.scaled()))
                        .clickable(onClick = onOpenSettings)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppIcon(AppIcons.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
                    Text(
                        "More",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderQualityPill(
    provider: ProviderOption,
    quality: PlaybackQualityPreference,
    onClick: () -> Unit,
) {
    val qualityBadgeColor = when (quality) {
        PlaybackQualityPreference.High -> MaterialTheme.colorScheme.primary
        PlaybackQualityPreference.Standard -> MaterialTheme.colorScheme.secondary
        PlaybackQualityPreference.DataSaver -> MaterialTheme.colorScheme.tertiary
        PlaybackQualityPreference.Auto -> MaterialTheme.colorScheme.outline
    }

    Row(
        Modifier.appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIcon(AppIcons.Tune, null, tint = MaterialTheme.colorScheme.primary, size = 16.dp)

        Column {
            Text(
                provider.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(qualityBadgeColor),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    quality.displayName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SpotifyStatusPill(
    isConnected: Boolean,
    syncState: SpotifySyncState,
    onClick: () -> Unit,
) {
    val isSyncing = syncState == SpotifySyncState.Syncing

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    Row(
        Modifier.appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIcon(
            AppIcons.Refresh,
            null,
            modifier = if (isSyncing) Modifier.rotate(rotation) else Modifier,
            tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            size = 16.dp,
        )

        Column {
            Text(
                "Spotify",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                when {
                    isSyncing -> "Syncing..."
                    isConnected -> "Connected · Tap to sync"
                    else -> "Tap to connect"
                },
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

