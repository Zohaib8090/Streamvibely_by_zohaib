package app.streamvibely.permissions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Explains why [permission] is needed before the system prompt. [onContinue] runs the request;
 * [onDismiss] (the "Not now" button, or tapping outside) leaves the feature working without it.
 */
@Composable
fun PermissionRationaleDialog(
    permission: AppPermission,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, reason) = when (permission) {
        AppPermission.Notifications ->
            "Allow notifications" to "Streamvibely shows download progress and playback controls in notifications."
        AppPermission.AudioLibrary ->
            "Allow access to music" to "Streamvibely needs to read your audio files to show songs already on this device."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(reason) },
        confirmButton = { TextButton(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
