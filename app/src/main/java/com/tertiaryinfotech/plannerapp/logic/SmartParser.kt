package com.tertiaryinfotech.plannerapp.logic

import com.tertiaryinfotech.plannerapp.data.PlannerItem
import com.tertiaryinfotech.plannerapp.data.PlannerKind
import java.util.Calendar
import java.util.Locale

/** The structured result of interpreting a free-text / spoken phrase. */
data class ParsedEntry(
    val title: String,
    val kind: PlannerKind,
    val date: Long?,
) {
    fun makeItem(notes: String = ""): PlannerItem =
        PlannerItem(title = title, notes = notes, kindRaw = kind.raw, date = date)
}

/**
 * On-device intelligence that turns a natural-language phrase (typed or dictated) into a
 * structured planner entry. Mirrors the iOS `SmartParser`: detects dates/times, classifies
 * task vs. appointment, and cleans the leftover phrase into a tidy title. Fully offline.
 */
object SmartParser {

    /** Words that strongly imply a scheduled event. */
    private val appointmentCues = setOf(
        "meeting", "meet", "appointment", "appt", "call", "lunch", "dinner", "breakfast",
        "interview", "flight", "doctor", "dentist", "visit", "session", "class", "event",
        "reservation", "booking", "conference", "standup", "sync", "demo", "presentation",
        "party", "pickup", "drop", "deadline", "due"
    )

    /** Words that imply a plain to-do. */
    private val taskCues = setOf(
        "buy", "remember", "remind", "todo", "task", "finish", "complete", "email",
        "text", "read", "write", "pay", "clean", "wash", "fix", "review", "send", "get",
        "pick", "order", "renew", "check"
    )

    /** Leading filler that dictation tends to produce ("add a reminder to …"). */
    private val leadingFiller = listOf(
        "add a new", "add new", "add a", "add an", "add", "create a", "create an", "create",
        "new", "please", "can you", "i need to", "i have to", "i want to", "remind me to",
        "reminder to", "remind me", "note to", "schedule a", "schedule an", "schedule",
        "set up a", "set up", "make a", "to"
    )

    fun parse(raw: String): ParsedEntry {
        val text = raw.trim()
        if (text.isEmpty()) return ParsedEntry("", PlannerKind.TASK, null)

        // 1. Detect a date/time anywhere in the phrase.
        val detection = DateDetector.detect(text)

        // 2. Strip the matched date/time phrases so they don't clutter the title.
        var remainder = text
        detection?.ranges?.sortedByDescending { it.first }?.forEach { range ->
            remainder = remainder.removeRange(range)
        }
        remainder = cleanTitle(remainder)

        // 3. Classify.
        val words = text.lowercase(Locale.US).split(Regex("[^a-z]+")).filter { it.isNotEmpty() }.toSet()
        val hasAppointmentCue = words.any { it in appointmentCues }
        val hasTaskCue = words.any { it in taskCues }

        val kind = when {
            detection?.hasTime == true || hasAppointmentCue -> PlannerKind.APPOINTMENT
            hasTaskCue -> PlannerKind.TASK
            detection != null -> PlannerKind.APPOINTMENT   // a bare date still reads as scheduled
            else -> PlannerKind.TASK
        }

        val title = remainder.ifEmpty { text }
        // Tasks keep an optional due date; appointments require one (default to soon).
        val date = detection?.dateMillis
            ?: if (kind == PlannerKind.APPOINTMENT) defaultAppointmentDate() else null

        return ParsedEntry(title.replaceFirstChar { it.uppercase() }, kind, date)
    }

    /** Next top of the hour, today. */
    private fun defaultAppointmentDate(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.HOUR_OF_DAY, 1)
        return cal.timeInMillis
    }

    // MARK: - Title cleanup

    private fun cleanTitle(input: String): String {
        var s = input.trim().replace(Regex("\\s+"), " ")

        var changed = true
        while (changed) {
            changed = false
            val lower = s.lowercase(Locale.US)
            for (filler in leadingFiller) {
                if (lower == filler) { s = ""; changed = true; break }
                if (lower.startsWith("$filler ")) {
                    s = s.substring(filler.length + 1)
                    changed = true
                    break
                }
            }
            s = s.trim()
        }

        // Trim dangling connector words left over after the date was removed.
        var trimmedMore = true
        while (trimmedMore) {
            trimmedMore = false
            val lower = s.lowercase(Locale.US)
            for (trailing in listOf(" at", " on", " by", " for", " to", " the", " a", " -", ",")) {
                if (lower.endsWith(trailing)) {
                    s = s.dropLast(trailing.length)
                    trimmedMore = true
                    break
                }
            }
        }
        return s.trim().trim(',').trim()
    }
}

/**
 * Deterministic natural-language date/time detection (the Android stand-in for
 * `NSDataDetector`). Handles: today / tonight / tomorrow, weekday names ("next friday"),
 * month-name dates ("july 12", "12 july"), numeric dates ("12/7"), and clock times
 * ("3pm", "3:30 pm", "15:00", "noon", "midnight"). Clock math never hallucinates.
 */
object DateDetector {

    data class Detection(
        val dateMillis: Long,
        /** Character ranges of every matched phrase, for stripping from the title. */
        val ranges: List<IntRange>,
        val hasTime: Boolean,
    )

    private val monthNames = listOf(
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december"
    )
    private val weekdayNames = listOf(
        "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"
    )

    private val monthPattern =
        monthNames.joinToString("|") { "${it.take(3)}(?:${it.drop(3)})?" }
    private val weekdayPattern =
        weekdayNames.joinToString("|") { "${it.take(3)}(?:${it.drop(3)})?" }

    // Date phrases
    private val reRelative = Regex(
        "\\b(day after tomorrow|tomorrow|tonight|today|this evening|this afternoon|this morning|next week)\\b",
        RegexOption.IGNORE_CASE
    )
    private val reWeekday = Regex(
        "\\b(?:(next|this)\\s+)?($weekdayPattern)\\b",
        RegexOption.IGNORE_CASE
    )
    private val reMonthDay = Regex(
        "\\b(?:on\\s+)?($monthPattern)\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b",
        RegexOption.IGNORE_CASE
    )
    private val reDayMonth = Regex(
        "\\b(?:on\\s+)?(\\d{1,2})(?:st|nd|rd|th)?\\s+(?:of\\s+)?($monthPattern)\\b",
        RegexOption.IGNORE_CASE
    )
    private val reNumericDate = Regex(
        "\\b(?:on\\s+)?(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b"
    )

    // Time phrases
    private val reClockTime = Regex(
        "\\b(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)\\b",
        RegexOption.IGNORE_CASE
    )
    private val re24hTime = Regex(
        "\\b(?:at\\s+)?([01]?\\d|2[0-3]):([0-5]\\d)\\b"
    )
    private val reNamedTime = Regex(
        "\\b(?:at\\s+)?(noon|midnight)\\b",
        RegexOption.IGNORE_CASE
    )
    private val reAtBareHour = Regex(
        "\\bat\\s+(\\d{1,2})\\b",
        RegexOption.IGNORE_CASE
    )

    fun detect(text: String, now: Calendar = Calendar.getInstance()): Detection? {
        val ranges = mutableListOf<IntRange>()
        val cal = now.clone() as Calendar
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        var dateFound = false
        var timeFound = false
        var impliedEvening = false

        // --- Date part ---
        reRelative.find(text)?.let { m ->
            dateFound = true
            ranges += m.range
            when (m.groupValues[1].lowercase(Locale.US)) {
                "today", "this morning", "this afternoon", "this evening" -> Unit
                "tonight" -> impliedEvening = true
                "tomorrow" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "day after tomorrow" -> cal.add(Calendar.DAY_OF_YEAR, 2)
                "next week" -> cal.add(Calendar.DAY_OF_YEAR, 7)
            }
            if (m.groupValues[1].lowercase(Locale.US) == "this afternoon") impliedEvening = false
        }

        if (!dateFound) {
            reMonthDay.find(text)?.let { m ->
                dateFound = true
                ranges += m.range
                applyMonthDay(cal, monthIndex(m.groupValues[1]), m.groupValues[2].toInt(), now)
            }
        }
        if (!dateFound) {
            reDayMonth.find(text)?.let { m ->
                dateFound = true
                ranges += m.range
                applyMonthDay(cal, monthIndex(m.groupValues[2]), m.groupValues[1].toInt(), now)
            }
        }
        if (!dateFound) {
            reNumericDate.find(text)?.let { m ->
                val day = m.groupValues[1].toInt()
                val month = m.groupValues[2].toInt()
                if (day in 1..31 && month in 1..12) {
                    dateFound = true
                    ranges += m.range
                    cal.set(Calendar.MONTH, month - 1)
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    val year = m.groupValues[3]
                    if (year.isNotEmpty()) {
                        cal.set(Calendar.YEAR, if (year.length == 2) 2000 + year.toInt() else year.toInt())
                    } else if (cal.before(now)) {
                        cal.add(Calendar.YEAR, 1)
                    }
                }
            }
        }
        if (!dateFound) {
            reWeekday.find(text)?.let { m ->
                dateFound = true
                ranges += m.range
                val target = weekdayIndex(m.groupValues[2]) + 1   // Calendar.SUNDAY == 1
                val isNext = m.groupValues[1].equals("next", ignoreCase = true)
                var diff = (target - now.get(Calendar.DAY_OF_WEEK) + 7) % 7
                if (diff == 0) diff = 7                            // "friday" on a Friday → next one
                if (isNext && diff < 7) diff += 0                  // "next friday" ≈ upcoming friday
                cal.add(Calendar.DAY_OF_YEAR, diff)
            }
        }

        // --- Time part ---
        reClockTime.find(text)?.let { m ->
            timeFound = true
            ranges += m.range
            var hour = m.groupValues[1].toInt() % 12
            if (m.groupValues[3].lowercase(Locale.US).startsWith("p")) hour += 12
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, m.groupValues[2].toIntOrNull() ?: 0)
        }
        if (!timeFound) {
            reNamedTime.find(text)?.let { m ->
                timeFound = true
                ranges += m.range
                val isNoon = m.groupValues[1].equals("noon", ignoreCase = true)
                cal.set(Calendar.HOUR_OF_DAY, if (isNoon) 12 else 0)
                cal.set(Calendar.MINUTE, 0)
            }
        }
        if (!timeFound) {
            re24hTime.find(text)?.let { m ->
                timeFound = true
                ranges += m.range
                cal.set(Calendar.HOUR_OF_DAY, m.groupValues[1].toInt())
                cal.set(Calendar.MINUTE, m.groupValues[2].toInt())
            }
        }
        if (!timeFound) {
            reAtBareHour.find(text)?.let { m ->
                val h = m.groupValues[1].toInt()
                if (h in 1..23) {
                    timeFound = true
                    ranges += m.range
                    // Bare "at 5" almost always means daytime/evening — assume pm for small hours.
                    cal.set(Calendar.HOUR_OF_DAY, if (h in 1..7) h + 12 else h)
                    cal.set(Calendar.MINUTE, 0)
                }
            }
        }

        if (!dateFound && !timeFound) return null

        if (!timeFound) {
            // Bare date: default 9am, or 8pm for "tonight".
            cal.set(Calendar.HOUR_OF_DAY, if (impliedEvening) 20 else 9)
            cal.set(Calendar.MINUTE, 0)
        } else if (impliedEvening && cal.get(Calendar.HOUR_OF_DAY) < 12) {
            cal.add(Calendar.HOUR_OF_DAY, 12)                     // "tonight at 8" → 8pm
        }

        if (!dateFound && timeFound && cal.timeInMillis < now.timeInMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)                      // "at 7am" said in the evening → tomorrow
        }

        return Detection(cal.timeInMillis, ranges, timeFound || impliedEvening)
    }

    private fun applyMonthDay(cal: Calendar, month: Int, day: Int, now: Calendar) {
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
        if (cal.get(Calendar.DAY_OF_YEAR) < now.get(Calendar.DAY_OF_YEAR) &&
            cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        ) {
            cal.add(Calendar.YEAR, 1)                             // past date this year → next year
        }
    }

    private fun monthIndex(name: String): Int {
        val prefix = name.lowercase(Locale.US).take(3)
        return monthNames.indexOfFirst { it.startsWith(prefix) }
    }

    private fun weekdayIndex(name: String): Int {
        val prefix = name.lowercase(Locale.US).take(3)
        return weekdayNames.indexOfFirst { it.startsWith(prefix) }
    }
}
