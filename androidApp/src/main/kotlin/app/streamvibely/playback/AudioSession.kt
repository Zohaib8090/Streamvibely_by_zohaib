package app.streamvibely.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The playback service publishes the audio session id of the active player here; the equalizer
 * attaches its effects to that id. Null when nothing has been prepared yet.
 */
object AudioSession {
    private val _id = MutableStateFlow<Int?>(null)
    val id: StateFlow<Int?> = _id
    fun publish(sessionId: Int?) { _id.value = sessionId }
}
