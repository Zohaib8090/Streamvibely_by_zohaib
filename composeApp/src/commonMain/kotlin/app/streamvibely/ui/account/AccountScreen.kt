package app.streamvibely.ui.account

import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.Account
import app.streamvibely.domain.PlaylistImporter
import app.streamvibely.domain.RemotePlaylist
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.scaled
import app.streamvibely.ui.common.AppIcon

@Composable
fun AccountScreen(
    account: Account?,
    message: String? = null,
    onBack: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    importer: PlaylistImporter? = null,
    onImportPlaylist: (RemotePlaylist, (String) -> Unit) -> Unit = { _, _ -> },
    spotifyConnected: Boolean = false,
    spotifySyncState: app.streamvibely.domain.SpotifySyncState = app.streamvibely.domain.SpotifySyncState.Idle,
    spotifyStatusMessage: String? = null,
    onConnectSpotify: () -> Unit = {},
    onSyncSpotify: () -> Unit = {},
    onDisconnectSpotify: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxSize().widthIn(max = 720.dp).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccountPageHeader("Account", onBack)

            // Google / YouTube Account Section
            Text("Google / YouTube", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            if (account == null) {
                SignedOutContent(onSignIn)
            } else {
                SignedInContent(account, onSignOut)
                if (importer != null) ImportSection(importer, onImportPlaylist)
            }

            // Spotify Account Section
            Text("Spotify", Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            SpotifyAccountContent(
                isConnected = spotifyConnected,
                syncState = spotifySyncState,
                statusMessage = spotifyStatusMessage,
                onConnect = onConnectSpotify,
                onSync = onSyncSpotify,
                onDisconnect = onDisconnectSpotify,
            )

            if (message != null) {
                Text(
                    message,
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Box(Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun SpotifyAccountContent(
    isConnected: Boolean,
    syncState: app.streamvibely.domain.SpotifySyncState,
    statusMessage: String?,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (isConnected) "Spotify (Connected)" else "Spotify (Not connected)",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (isConnected) "Web Player session active (sp_dc)"
                    else "Connect to sync your Liked Songs and playlists directly into your library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (statusMessage != null) {
            Text(
                statusMessage,
                color = when (syncState) {
                    app.streamvibely.domain.SpotifySyncState.Error -> MaterialTheme.colorScheme.error
                    app.streamvibely.domain.SpotifySyncState.Done -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (!isConnected) {
            AccountActionButton("Connect Spotify", AppIcons.Login, onConnect, primary = true)
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(Tokens.radiusChip.dp.scaled()))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(enabled = syncState != app.streamvibely.domain.SpotifySyncState.Syncing, onClick = onSync)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(AppIcons.Download, null, tint = MaterialTheme.colorScheme.onPrimary, size = 18.dp)
                    Text(
                        if (syncState == app.streamvibely.domain.SpotifySyncState.Syncing) "Syncing..." else "Sync Spotify Library",
                        Modifier.padding(start = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                Row(
                    Modifier.clip(RoundedCornerShape(Tokens.radiusChip.dp.scaled()))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(onClick = onDisconnect)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
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

@Composable
private fun SignedOutContent(onSignIn: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AccountAvatar(null, 56.dp)
        Text(
            "Not signed in",
            Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Sign in with YouTube to import your playlists and keep them in sync.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    AccountActionButton("Sign in with YouTube", AppIcons.Login, onSignIn, primary = true)
}

@Composable
private fun SignedInContent(account: Account, onSignOut: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AccountAvatar(account, 72.dp)
        Text(
            account.displayName,
            Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
        )
        if (account.email != null) {
            Text(
                account.email,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            "Signed in with ${account.provider}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    AccountActionButton("Sign out", AppIcons.Logout, onSignOut, primary = false)
}

@Composable
private fun AccountActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    primary: Boolean,
) {
    val container = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.radiusChip.dp.scaled()))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon, null, tint = content, size = 20.dp)
        Text(label, Modifier.padding(start = 8.dp), color = content, style = MaterialTheme.typography.labelMedium)
    }
}


/** Lists the YouTube playlists and imports the songs the user already has on this device. */
@Composable
private fun ImportSection(importer: PlaylistImporter, onImport: (RemotePlaylist, (String) -> Unit) -> Unit) {
    var lists by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<RemotePlaylist>?>(null) }
    var status by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var loading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(importer) {
        loading = true
        lists = runCatching { importer.playlists() }.getOrNull()
        if (lists == null) status = "Could not load your YouTube playlists."
        loading = false
    }
    Column(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(vertical = 12.dp),
    ) {
        Text(
            "Import from YouTube",
            Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            "Adds the songs you already have on this device. Nothing is downloaded from YouTube.",
            Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        if (loading) Text("Loading playlists…", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        lists?.forEach { p ->
            Row(
                Modifier.fillMaxWidth().clickable { status = "Importing \"${p.title}\"…"; onImport(p) { status = it } }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    Text("${p.itemCount} videos", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                AppIcon(AppIcons.Download, "Import ${p.title}", tint = MaterialTheme.colorScheme.primary, size = 22.dp)
            }
        }
        if (lists?.isEmpty() == true) Text("No playlists found on this account.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        status?.let { Text(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium) }
    }
}
