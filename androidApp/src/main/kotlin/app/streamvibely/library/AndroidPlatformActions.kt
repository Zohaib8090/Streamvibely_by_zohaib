package app.streamvibely.library

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import app.streamvibely.domain.PlatformActions
import app.streamvibely.permissions.AppPermission
import app.streamvibely.permissions.isGranted
import app.streamvibely.permissions.openAppSettingsIntent

/**
 * Permission prompt, folder picker and settings shortcut for the library screens.
 * Construct it in `onCreate` before the activity starts: the result launchers are registered here.
 */
class AndroidPlatformActions(private val activity: ComponentActivity) : PlatformActions {
    private var permissionCallback: ((Boolean) -> Unit)? = null
    private var folderCallback: ((uri: String, displayName: String) -> Unit)? = null
    private var spotifyLoginCallback: ((String?) -> Unit)? = null

    private val spotifyLoginLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val cb = spotifyLoginCallback
        spotifyLoginCallback = null
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spDc = result.data?.getStringExtra(app.streamvibely.spotify.SpotifyLoginActivity.EXTRA_SP_DC)
            cb?.invoke(spDc)
        } else {
            cb?.invoke(null)
        }
    }

    override fun openSpotifyLogin(onResult: (String?) -> Unit) {
        spotifyLoginCallback = onResult
        try {
            val intent = Intent(activity, app.streamvibely.spotify.SpotifyLoginActivity::class.java)
            spotifyLoginLauncher.launch(intent)
        } catch (e: Exception) {
            spotifyLoginCallback = null
            onResult(null)
        }
    }

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val cb = permissionCallback
        permissionCallback = null
        cb?.invoke(granted)
    }

    private val folderLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        val cb = folderCallback
        folderCallback = null
        if (uri != null && cb != null) handlePickedFolder(uri, cb)
    }

    override fun hasAudioPermission(): Boolean = isGranted(activity, AppPermission.AudioLibrary)

    override fun requestAudioPermission(onResult: (Boolean) -> Unit) {
        val name = AppPermission.AudioLibrary.androidPermissionName()
        if (name == null || hasAudioPermission()) {
            onResult(true)
            return
        }
        permissionCallback = onResult
        permissionLauncher.launch(name)
    }

    override fun pickFolder(onPicked: (uri: String, displayName: String) -> Unit) {
        folderCallback = onPicked
        try {
            folderLauncher.launch(null)
        } catch (e: ActivityNotFoundException) {
            folderCallback = null // device has no document picker
        }
    }

    override fun notificationsEnabled(): Boolean =
        androidx.core.app.NotificationManagerCompat.from(activity).areNotificationsEnabled()

    override fun requestNotifications() {
        val name = AppPermission.Notifications.androidPermissionName()
        if (name != null && !isGranted(activity, AppPermission.Notifications) &&
            activity.shouldShowRequestPermissionRationale(name).not() && askedNotificationOnce
        ) {
            openNotificationSettings() // Android will not show the dialog again
        } else if (name != null && !isGranted(activity, AppPermission.Notifications)) {
            askedNotificationOnce = true
            permissionCallback = { granted -> if (!granted) openNotificationSettings() }
            permissionLauncher.launch(name)
        } else {
            openNotificationSettings() // allowed by permission but switched off in the system's app notification settings
        }
    }

    private var askedNotificationOnce = false

    private fun openNotificationSettings() {
        try {
            activity.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, activity.packageName)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: ActivityNotFoundException) {
            openAppSettings()
        }
    }

    override fun openAppSettings() {
        try {
            activity.startActivity(openAppSettingsIntent(activity))
        } catch (e: ActivityNotFoundException) {
            // nothing sensible to fall back to
        }
    }

    private fun handlePickedFolder(uri: Uri, onPicked: (String, String) -> Unit) {
        try {
            activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            return // without a persisted grant the folder would stop working after a restart
        }
        val name = try { DocumentFile.fromTreeUri(activity, uri)?.name } catch (e: Exception) { null }
        onPicked(uri.toString(), name?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: "Music folder")
    }

    override fun getMediaCacheSizeBytes(): Long = app.streamvibely.playback.MediaCache.getCacheSize(activity)

    override fun clearMediaCache() = app.streamvibely.playback.MediaCache.clear(activity)
}
