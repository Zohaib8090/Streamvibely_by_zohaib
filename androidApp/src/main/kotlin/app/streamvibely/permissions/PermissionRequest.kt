package app.streamvibely.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Returns a function that requests [permission] when called, typically from a button handler.
 *
 * [onResult] receives true when the feature may proceed: the permission was granted, or this
 * Android version does not need it. It receives false when the user denied the request.
 * Call the returned function only after showing a rationale (see PermissionRationaleDialog).
 */
@Composable
fun rememberPermissionRequester(permission: AppPermission, onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        currentOnResult(granted)
    }
    return remember(permission) {
        {
            val name = permission.androidPermissionName()
            if (name == null || isGranted(context, permission)) {
                currentOnResult(true)
            } else {
                launcher.launch(name)
            }
        }
    }
}
