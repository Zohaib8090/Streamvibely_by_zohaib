package app.streamvibely.ui.settings

import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.LibrarySettings
import app.streamvibely.domain.MusicFolder
import app.streamvibely.domain.ScanMode
import app.streamvibely.domain.ScanState
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon

@Composable
fun SettingsScreen(
    library: LibrarySettings = LibrarySettings(),
    scanState: ScanState = ScanState.Idle,
    trackCount: Int = 0,
    hasAudioPermission: Boolean = true,
    onBack: () -> Unit = {},
    onScanModeChange: (ScanMode) -> Unit = {},
    onAddFolder: () -> Unit = {},
    onRemoveFolder: (MusicFolder) -> Unit = {},
    onRescan: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenCustomization: () -> Unit = {},
    providers: List<app.streamvibely.domain.ProviderOption> = emptyList(),
    onProviderToggle: (String, Boolean) -> Unit = { _, _ -> },
    notificationPlayer: app.streamvibely.domain.ProviderOption? = null,
    onEnableNotifications: () -> Unit = {},
    qualityPreferences: Map<String, app.streamvibely.domain.PlaybackQualityPreference> = emptyMap(),
    onQualityChange: (String, app.streamvibely.domain.PlaybackQualityPreference) -> Unit = { _, _ -> },
    spotifyConnected: Boolean = false,
    spotifySyncState: app.streamvibely.domain.SpotifySyncState = app.streamvibely.domain.SpotifySyncState.Idle,
    spotifyStatusMessage: String? = null,
    onConnectSpotify: () -> Unit = {},
    onSyncSpotify: () -> Unit = {},
    onDisconnectSpotify: () -> Unit = {},
    showSpotifyOnHome: Boolean = true,
    onToggleShowSpotifyOnHome: (Boolean) -> Unit = {},
    onOpenOnboarding: () -> Unit = {},
    mediaCacheSize: Long = 0L,
    onClearMediaCache: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val allDevice = library.scanMode == ScanMode.AllDeviceAudio

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 720.dp).padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).appSurface(SurfaceKind.Chip, CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(AppIcons.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                }
                Text(
                    "Settings",
                    Modifier.padding(start = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "customization") { SettingsNavRow("Customization", onOpenCustomization, icon = AppIcons.Palette) }
                item(key = "hero") { SettingsHero(trackCount) }

                item(key = "header-music") { SettingsSectionHeader("Your music") }
                item(key = "mode-all") {
                    SettingsRadioRow(
                        title = "All folders on this device",
                        subtitle = "Everything the system has indexed",
                        selected = allDevice,
                        onSelect = { onScanModeChange(ScanMode.AllDeviceAudio) },
                    )
                }
                item(key = "mode-selected") {
                    SettingsRadioRow(
                        title = "Only selected folders",
                        subtitle = "Choose exactly where Streamvibely looks",
                        selected = !allDevice,
                        onSelect = { onScanModeChange(ScanMode.SelectedFolders) },
                    )
                }

                if (allDevice) {
                    if (!hasAudioPermission) {
                        item(key = "permission") {
                            PermissionCard(onRequestPermission, onOpenAppSettings)
                        }
                    }
                } else {
                    items(library.folders, key = { "folder-${it.id}" }) { folder ->
                        FolderRow(folder, onRemove = { onRemoveFolder(folder) })
                    }
                    if (library.folders.isEmpty()) {
                        item(key = "folders-empty") {
                            Text(
                                "No folders yet. Add one to start.",
                                Modifier.padding(horizontal = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    item(key = "add-folder") {
                        SettingsButton("Add folder", onAddFolder, Modifier.fillMaxWidth(), primary = true)
                    }
                }

                item(key = "scan") { ScanRow(scanState, trackCount, onRescan) }

                if (providers.isNotEmpty()) {
                    item(key = "header-providers") { SettingsSectionHeader("Music Providers & Quality") }
                    item(key = "providers-note") {
                        Text(
                            "Configure which sources are searched and select their streaming quality.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    items(providers.size, key = { "provider-block-" + providers[it].id }) { i ->
                        val p = providers[i]
                        Column(
                            Modifier.fillMaxWidth()
                                .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
                                .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
                                .padding(vertical = 4.dp),
                        ) {
                            ProviderRow(p) { onProviderToggle(p.id, it) }
                            if (p.enabled) {
                                Box(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                                    ProviderQualityRow(
                                        provider = p,
                                        selected = qualityPreferences[p.id] ?: app.streamvibely.domain.PlaybackQualityPreference.Auto,
                                        onSelect = { onQualityChange(p.id, it) },
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "header-accounts") { SettingsSectionHeader("Accounts & Sync") }
                item(key = "account-nav") {
                    SettingsNavRow(
                        if (spotifyConnected) "Spotify & YouTube (Connected)" else "Manage Accounts (Spotify, YouTube)",
                        onOpenAccount,
                        icon = AppIcons.Headphones,
                    )
                }
                item(key = "spotify-home-toggle") {
                    ProviderRow(
                        app.streamvibely.domain.ProviderOption(
                            "home_spotify",
                            "Show Spotify on Home",
                            "Display recommendations and playlists from your synced Spotify on the Home screen",
                            showSpotifyOnHome,
                        )
                    ) { onToggleShowSpotifyOnHome(it) }
                }
                item(key = "re-run-onboarding") {
                    SettingsNavRow(
                        "Sync & Preferences Wizard",
                        onOpenOnboarding,
                        icon = AppIcons.Equalizer,
                    )
                }

                item(key = "header-lyrics") { SettingsSectionHeader("Lyrics") }
                item(key = "lyrics-card") {
                    Column(
                        Modifier.fillMaxWidth()
                            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
                            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Lyrics Provider", style = MaterialTheme.typography.titleMedium)
                            Text("LRCLIB (Time-synced)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            "Time-synced and plain text lyrics are fetched automatically from LRCLIB and embedded into offline downloads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Local LRC Sidecar Files", style = MaterialTheme.typography.bodyMedium)
                            Text("Active (.lrc)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item(key = "header-playback") { SettingsSectionHeader("Playback") }
                item(key = "equalizer") { SettingsNavRow("Equalizer", onOpenEqualizer, icon = AppIcons.Equalizer) }
                if (notificationPlayer != null) item(key = "notification-player") {
                    ProviderRow(notificationPlayer) { onEnableNotifications() }
                }

                item(key = "header-storage") { SettingsSectionHeader("Storage & Cache") }
                item(key = "media-cache") {
                    val cacheMb = if (mediaCacheSize <= 0) "0 MB" else "${(mediaCacheSize / (1024L * 1024L)).coerceAtLeast(1L)} MB"
                    Column(
                        Modifier.fillMaxWidth()
                            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
                            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Streamed audio cache", style = MaterialTheme.typography.titleMedium)
                            Text(cacheMb, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "Songs you play are cached locally in app storage for faster loading and offline replay.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SettingsButton("Clear audio cache", onClearMediaCache, Modifier.fillMaxWidth())
                    }
                }

                item(key = "header-about") { SettingsSectionHeader("About") }
                item(key = "about") { AboutCard() }
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit, onOpenAppSettings: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(AppIcons.Security, null, Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.primary, size = 20.dp)
            Text(
                "Allow access to your music",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Text(
            "Streamvibely needs permission to read the audio files on this device.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsButton("Allow access", onRequestPermission, primary = true)
            SettingsButton("Open app settings", onOpenAppSettings)
        }
    }
}

@Composable
private fun FolderRow(folder: MusicFolder, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(AppIcons.Folder, null, Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 24.dp)
        Text(
            folder.displayName,
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(AppIcons.Close, "Remove ${folder.displayName}", tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
        }
    }
}

@Composable
private fun ScanRow(state: ScanState, trackCount: Int, onRescan: () -> Unit) {
    val isProblem = state is ScanState.NeedsPermission || state is ScanState.Error
    Row(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(AppIcons.Folder, null, Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 24.dp)
        Text(
            scanStatusText(state, trackCount),
            Modifier.weight(1f),
            color = if (isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        SettingsButton(
            "Rescan now",
            onRescan,
            enabled = state !is ScanState.Scanning,
        )
    }
}

private fun scanStatusText(state: ScanState, trackCount: Int): String = when (state) {
    ScanState.Idle -> if (trackCount == 0) "Not scanned yet" else "$trackCount songs"
    is ScanState.Scanning -> "Scanning... ${state.found} found"
    is ScanState.Done -> "Found ${state.count} songs"
    ScanState.NeedsPermission -> "Permission needed"
    is ScanState.Error -> state.message
}

@Composable
private fun AboutCard() {
    Column(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(AppIcons.Info, null, Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.primary, size = 20.dp)
            Text("Streamvibely", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
        }
        Text(
            "Open source, GPL-3.0 license",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Plays music you own. Streamvibely does not host or distribute copyrighted media.",
            Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
