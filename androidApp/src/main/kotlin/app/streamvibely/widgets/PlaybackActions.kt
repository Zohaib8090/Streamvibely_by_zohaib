package app.streamvibely.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Broadcast action strings sent by the widget buttons. */
object PlaybackActions {
    const val PLAY_PAUSE = "app.streamvibely.action.PLAY_PAUSE"
    const val NEXT = "app.streamvibely.action.NEXT"
    const val PREVIOUS = "app.streamvibely.action.PREVIOUS"
}

/**
 * Receives the widget button broadcasts.
 *
 * For now this only logs the action. Real playback wiring (forwarding to the
 * player service or media session) comes later.
 */
class PlaybackActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Playback action received: ${intent.action}")
    }

    private companion object {
        const val TAG = "PlaybackActionReceiver"
    }
}
