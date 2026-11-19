package app.streamvibely.ui.usage

import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.DayUsage
import app.streamvibely.domain.TrackUsage
import app.streamvibely.domain.UsageSummary
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.scaled
import app.streamvibely.ui.account.AccountPageHeader
import app.streamvibely.ui.common.AppIcon

@Composable
fun ListeningUsageScreen(
    summary: UsageSummary,
    onBack: () -> Unit = {},
    onClear: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val empty = allTimeMs(summary) == 0L && summary.days.all { it.offlineMs + it.onlineMs == 0L }
    var confirmClear by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 720.dp).padding(horizontal = 16.dp)) {
            AccountPageHeader("Listening usage", onBack)
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (empty) {
                    item(key = "empty") { EmptyUsage() }
                } else {
                    item(key = "totals") { TotalsCard(summary) }
                    item(key = "split") { SplitCard(summary) }
                    item(key = "chart") { WeekChartCard(lastSevenDays(summary), summary.todayEpochDay) }
                    if (summary.topTracks.isNotEmpty()) {
                        item(key = "mostPlayedTitle") {
                            Text(
                                "Most played",
                                Modifier.padding(start = 4.dp, top = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                        itemsIndexed(summary.topTracks.take(5), key = { i, t -> "$i-${t.title}-${t.artist}" }) { i, t ->
                            TopTrackRow(i + 1, t)
                        }
                    }
                    item(key = "clear") { ClearButton { confirmClear = true } }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear listening data?") },
            text = { Text("This removes your listening history and statistics. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClear() }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun UsageCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun EmptyUsage() {
    UsageCard {
        Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AppIcon(AppIcons.BarChart, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 40.dp)
            Text(
                "Nothing listened yet",
                Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Play some music and your listening time will show up here.",
                Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TotalsCard(summary: UsageSummary) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Today",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            formatListening(todayMs(summary)),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.headlineLarge,
        )
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TotalsStat("Last 7 days", formatListening(weekMs(summary)), Modifier.weight(1f))
            TotalsStat("All time (last 30 days)", formatListening(allTimeMs(summary)), Modifier.weight(1f))
        }
    }
}

@Composable
private fun TotalsStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            value,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun SplitCard(summary: UsageSummary) {
    val offline = summary.totalOfflineMs
    val online = summary.totalOnlineMs
    val total = offline + online
    val offlineColor = MaterialTheme.colorScheme.primary
    val onlineColor = MaterialTheme.colorScheme.tertiary
    UsageCard {
        Row(
            Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (offline > 0) Box(Modifier.weight(offline.toFloat()).fillMaxSize().background(offlineColor))
            if (online > 0) Box(Modifier.weight(online.toFloat()).fillMaxSize().background(onlineColor))
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SplitStat("Offline (local files)", offline, total, offlineColor, Modifier.weight(1f))
            SplitStat("Online (streaming)", online, total, onlineColor, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SplitStat(label: String, ms: Long, total: Long, color: Color, modifier: Modifier = Modifier) {
    val percent = if (total > 0) (ms * 100 / total).toInt() else 0
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Text(
                label,
                Modifier.padding(start = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            formatListening(ms),
            Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "$percent%",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun WeekChartCard(days: List<DayUsage>, today: Long) {
    val offlineColor = MaterialTheme.colorScheme.primary
    val onlineColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val maxMs = (days.maxOfOrNull { it.offlineMs + it.onlineMs } ?: 0L).coerceAtLeast(1L)
    UsageCard {
        Text(
            "Last 7 days",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
        )
        Canvas(Modifier.fillMaxWidth().padding(top = 12.dp).height(140.dp)) {
            val slot = size.width / days.size
            val barWidth = slot * 0.5f
            val radius = CornerRadius(4.dp.toPx())
            val minHeight = 3.dp.toPx()
            days.forEachIndexed { i, day ->
                val highlight = day.epochDay == today
                val x = i * slot + (slot - barWidth) / 2f
                drawRoundRect(trackColor, Offset(x, 0f), Size(barWidth, size.height), radius)
                val offlineH = size.height * day.offlineMs / maxMs
                val onlineH = size.height * day.onlineMs / maxMs
                val fade = if (highlight) 1f else 0.5f
                var bottom = size.height
                if (day.offlineMs > 0) {
                    val h = offlineH.coerceAtLeast(minHeight)
                    drawRoundRect(offlineColor.copy(alpha = fade), Offset(x, bottom - h), Size(barWidth, h), radius)
                    bottom -= h
                }
                if (day.onlineMs > 0) {
                    val h = onlineH.coerceAtLeast(minHeight)
                    drawRoundRect(onlineColor.copy(alpha = fade), Offset(x, bottom - h), Size(barWidth, h), radius)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            days.forEach { day ->
                val highlight = day.epochDay == today
                Text(
                    weekdayLetter(day.epochDay),
                    Modifier.weight(1f),
                    color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = if (highlight) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TopTrackRow(rank: Int, track: TrackUsage) {
    Row(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$rank",
            Modifier.padding(end = 14.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
        )
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.End) {
            Text(
                formatListening(track.listenedMs),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                if (track.plays == 1) "1 play" else "${track.plays} plays",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .appSurface(SurfaceKind.CardHigh, RoundedCornerShape(Tokens.radiusChip.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusChip.dp.scaled()))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(AppIcons.Delete, null, tint = MaterialTheme.colorScheme.error, size = 20.dp)
        Text(
            "Clear listening data",
            Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
