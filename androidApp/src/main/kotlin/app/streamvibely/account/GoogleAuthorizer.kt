package app.streamvibely.account

import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Asks Google Play services for an access token with read-only YouTube access. Google matches the app by
 * package name and signing key (the Android OAuth client), so no secret or redirect scheme is involved.
 * Construct it in `onCreate`, before the activity starts, because it registers a result launcher.
 */
class GoogleAuthorizer(private val activity: ComponentActivity) {
    private var waiting: CompletableDeferred<AuthorizationResult?>? = null

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val parsed = try {
            Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(result.data)
        } catch (e: Exception) {
            null
        }
        waiting?.complete(parsed)
    }

    /** An access token, or null. With [interactive] false it never shows UI, so it only succeeds after consent was given once. */
    suspend fun accessToken(interactive: Boolean): String? {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(SCOPES.map { Scope(it) })
            .build()
        val first = suspendCancellableCoroutine<AuthorizationResult?> { cont ->
            Identity.getAuthorizationClient(activity).authorize(request)
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        } ?: return null
        if (!first.hasResolution()) return first.accessToken
        if (!interactive) return null
        val pending = first.pendingIntent ?: return null
        val deferred = CompletableDeferred<AuthorizationResult?>()
        waiting = deferred
        launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        val result = deferred.await()
        waiting = null
        return result?.accessToken
    }

    private companion object {
        val SCOPES = listOf("https://www.googleapis.com/auth/youtube.readonly", "profile", "email")
    }
}
