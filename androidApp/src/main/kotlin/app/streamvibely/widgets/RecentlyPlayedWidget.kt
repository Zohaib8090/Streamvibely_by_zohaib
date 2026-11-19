package app.streamvibely.widgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.streamvibely.R

/** One tile in the recently played widget. */
data class RecentTrack(val id: String, val title: String)

private const val MAX_TILES = 4
private const val FIELD_SEP = '\u001F'
private const val ITEM_SEP = '\u001E'
private val TracksKey = stringPreferencesKey("recent_tracks")

/** Pushes the recently played list (first four are shown) to every placed widget. */
suspend fun updateRecentlyPlayedWidget(context: Context, tracks: List<RecentTrack>) {
    val encoded = tracks.take(MAX_TILES).joinToString(ITEM_SEP.toString()) {
        "${it.id}$FIELD_SEP${it.title}"
    }
    val ids = GlanceAppWidgetManager(context).getGlanceIds(RecentlyPlayedWidget::class.java)
    ids.forEach { id ->
        updateAppWidgetState(context, id) { prefs -> prefs[TracksKey] = encoded }
        RecentlyPlayedWidget().update(context, id)
    }
}

private fun decode(encoded: String?): List<RecentTrack> =
    encoded.orEmpty()
        .split(ITEM_SEP)
        .mapNotNull { item ->
            val parts = item.split(FIELD_SEP, limit = 2)
            if (parts.size == 2 && parts[0].isNotEmpty()) RecentTrack(parts[0], parts[1]) else null
        }
        .take(MAX_TILES)

class RecentlyPlayedWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val tracks = decode(currentState<Preferences>()[TracksKey])
            RecentlyPlayedContent(context, tracks)
        }
    }
}

class RecentlyPlayedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentlyPlayedWidget()
}

@Composable
private fun RecentlyPlayedContent(context: Context, tracks: List<RecentTrack>) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(R.drawable.widget_bg_card))
            .padding(8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        if (tracks.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxSize()
                    .clickable(openTrackAction(context, null)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = context.getString(R.string.widget_recent_empty),
                    maxLines = 1,
                    style = TextStyle(color = WidgetColors.textSecondary, fontSize = 13.sp),
                )
            }
        } else {
            tracks.forEach { track ->
                Tile(context, track, GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
private fun Tile(context: Context, track: RecentTrack, modifier: GlanceModifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(openTrackAction(context, track.id)),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(40.dp)
                .background(ImageProvider(R.drawable.widget_bg_cover)),
            content = {},
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = track.title,
            maxLines = 1,
            style = TextStyle(color = WidgetColors.text, fontSize = 11.sp),
        )
    }
}

/** Opens MainActivity; with a [trackId] the intent carries the `track_id` extra. */
private fun openTrackAction(context: Context, trackId: String?) =
    actionStartActivity(
        Intent().apply {
            component = ComponentName(context.packageName, "app.streamvibely.MainActivity")
            if (trackId != null) {
                putExtra("track_id", trackId)
                // A distinct data URI keeps each tile's PendingIntent unique.
                data = Uri.parse("streamvibely://track/${Uri.encode(trackId)}")
            }
        },
    )
