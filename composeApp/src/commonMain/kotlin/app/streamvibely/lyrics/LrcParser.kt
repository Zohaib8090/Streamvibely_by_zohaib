package app.streamvibely.lyrics

import app.streamvibely.domain.LyricLine

/** Parses LRC text. A positive `[offset:ms]` makes lyrics appear sooner (timestamps are reduced). */
object LrcParser {
    private val timestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val leadingStamps = Regex("""^(?:\s*\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?])+""")
    private val offsetTag = Regex("""\[\s*offset\s*:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)
    private val wordTime = Regex("""<\d{1,3}:\d{2}(?:[.:]\d{1,3})?>""")

    fun parse(raw: String): List<LyricLine> {
        val offset = offsetTag.find(raw)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val lines = ArrayList<LyricLine>()
        for (rawLine in raw.lineSequence()) {
            val line = rawLine.trim()
            val prefix = leadingStamps.find(line)?.value ?: continue
            val text = line.substring(prefix.length).replace(wordTime, "").trim()
            if (text.isEmpty()) continue
            for (m in timestamp.findAll(prefix)) {
                val (min, sec, frac) = m.destructured
                val ms = min.toLong() * 60_000 + sec.toLong() * 1000 + fractionToMs(frac)
                lines += LyricLine((ms - offset).coerceAtLeast(0), text)
            }
        }
        return lines.sortedBy { it.startMs }
    }

    fun isSynced(raw: String): Boolean = raw.lineSequence().any { leadingStamps.containsMatchIn(it.trim()) }

    private fun fractionToMs(frac: String): Long = when (frac.length) {
        0 -> 0L
        1 -> frac.toLong() * 100
        2 -> frac.toLong() * 10
        else -> frac.toLong()
    }
}
