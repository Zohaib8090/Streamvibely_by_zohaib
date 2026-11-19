package app.streamvibely.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Runtime permissions the app asks for, each only when the feature that needs it is used. */
enum class AppPermission {
    Notifications,
    AudioLibrary;

    /**
     * The Android permission string this feature needs on [sdk], or null when no permission is needed.
     */
    fun androidPermissionName(sdk: Int = Build.VERSION.SDK_INT): String? = when (this) {
        Notifications ->
            if (sdk >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
        AudioLibrary ->
            if (sdk >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

/** True when the permission is granted, or when this Android version does not need it. */
fun isGranted(context: Context, permission: AppPermission): Boolean {
    val name = permission.androidPermissionName() ?: return true
    return context.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED
}

/** Opens this app's page in system settings, for when the user denied a permission and wants it back. */
fun openAppSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
