package app.streamvibely.widgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionSendBroadcast
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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.streamvibely.R

/** What the now playing widget displays. */
data class WidgetPlayerState(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
)

private val TitleKey = stringPreferencesKey("now_playing_title")
private val ArtistKey = stringPreferencesKey("now_playing_artist")
private val IsPlayingKey = booleanPreferencesKey("now_playing_is_playing")

/** Pushes a new player state to every placed now playing widget. */
suspend fun updateNowPlayingWidget(context: Context, state: WidgetPlayerState) {
    val ids = GlanceAppWidgetManager(context).getGlanceIds(NowPlayingWidget::class.java)
    ids.forEach { id ->
        updateAppWidgetState(context, id) { prefs ->
            prefs[TitleKey] = state.title
            prefs[ArtistKey] = state.artist
            prefs[IsPlayingKey] = state.isPlaying
        }
        NowPlayingWidget().update(context, id)
    }
}

class NowPlayingWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val title = prefs[TitleKey]
            val state = WidgetPlayerState(
                title = title ?: context.getString(R.string.widget_nothing_playing),
                artist = prefs[ArtistKey].orEmpty(),
                isPlaying = prefs[IsPlayingKey] ?: false,
            )
            NowPlayingContent(context, state)
        }
    }
}

class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}

@Composable
private fun NowPlayingContent(context: Context, state: WidgetPlayerState) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(R.drawable.widget_bg_card))
            .clickable(openAppAction(context))
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(72.dp)
                .background(ImageProvider(R.drawable.widget_bg_cover)),
            content = {},
        )
        Spacer(GlanceModifier.width(12.dp))
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = state.title,
                maxLines = 1,
                style = TextStyle(
                    color = WidgetColors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = state.artist,
                maxLines = 1,
                style = TextStyle(color = WidgetColors.textSecondary, fontSize = 13.sp),
            )
            Spacer(GlanceModifier.size(8.dp))
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                ControlButton(
                    context, PlaybackActions.PREVIOUS,
                    R.drawable.widget_ic_previous, R.string.widget_cd_previous,
                )
                Spacer(GlanceModifier.width(12.dp))
                PlayPauseButton(context, state.isPlaying)
                Spacer(GlanceModifier.width(12.dp))
                ControlButton(
                    context, PlaybackActions.NEXT,
                    R.drawable.widget_ic_next, R.string.widget_cd_next,
                )
            }
        }
    }
}

@Composable
private fun ControlButton(context: Context, action: String, icon: Int, description: Int) {
    Box(
        modifier = GlanceModifier
            .size(40.dp)
            .clickable(broadcastAction(context, action)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = context.getString(description),
            modifier = GlanceModifier.size(26.dp),
            colorFilter = ColorFilter.tint(WidgetColors.text),
        )
    }
}

@Composable
private fun PlayPauseButton(context: Context, isPlaying: Boolean) {
    Box(
        modifier = GlanceModifier
            .size(48.dp)
            .background(ImageProvider(R.drawable.widget_bg_play))
            .clickable(broadcastAction(context, PlaybackActions.PLAY_PAUSE)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(
                if (isPlaying) R.drawable.widget_ic_pause else R.drawable.widget_ic_play,
            ),
            contentDescription = context.getString(
                if (isPlaying) R.string.widget_cd_pause else R.string.widget_cd_play,
            ),
            modifier = GlanceModifier.size(28.dp),
            colorFilter = ColorFilter.tint(WidgetColors.onPrimary),
        )
    }
}

private fun broadcastAction(context: Context, action: String): Action =
    actionSendBroadcast(
        Intent(action).setComponent(ComponentName(context, PlaybackActionReceiver::class.java)),
    )

private fun openAppAction(context: Context): Action =
    actionStartActivity(
        Intent().setComponent(ComponentName(context.packageName, "app.streamvibely.MainActivity")),
    )
