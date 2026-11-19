package app.streamvibely.ui.usage

import app.streamvibely.domain.DayUsage
import app.streamvibely.domain.UsageSummary

private const val MINUTE_MS = 60_000L
private const val WEEKDAY_LETTERS = "MTWTFSS"

/** "2 h 15 min", "40 min", "< 1 min" or "0 min". */
internal fun formatListening(ms: Long): String {
    if (ms <= 0) return "0 min"
    val minutes = ms / MINUTE_MS
    if (minutes < 1) return "< 1 min"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours == 0L) "$rest min" else "$hours h $rest min"
}

/** First letter of the weekday of [epochDay] (1970-01-01 was a Thursday). */
internal fun weekdayLetter(epochDay: Long): String {
    val index = (((epochDay + 3) % 7) + 7) % 7
    return WEEKDAY_LETTERS[index.toInt()].toString()
}

private val DayUsage.totalMs get() = offlineMs + onlineMs

/** The 7 days ending today, oldest first, with zero entries for missing days. */
internal fun lastSevenDays(summary: UsageSummary): List<DayUsage> {
    val today = summary.todayEpochDay
    val byDay = summary.days.associateBy { it.epochDay }
    return (today - 6..today).map { byDay[it] ?: DayUsage(it) }
}

internal fun todayMs(summary: UsageSummary): Long =
    summary.days.firstOrNull { it.epochDay == summary.todayEpochDay }?.totalMs ?: 0L

internal fun weekMs(summary: UsageSummary): Long = lastSevenDays(summary).sumOf { it.totalMs }

internal fun allTimeMs(summary: UsageSummary): Long = summary.totalOfflineMs + summary.totalOnlineMs
