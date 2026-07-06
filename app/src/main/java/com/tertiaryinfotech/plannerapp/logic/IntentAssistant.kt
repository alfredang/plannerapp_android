package com.tertiaryinfotech.plannerapp.logic

import com.tertiaryinfotech.plannerapp.data.PlannerKind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What the assistant produced for one user utterance: the structured entry plus a reply. */
data class AssistantDraft(
    val entry: ParsedEntry,
    val reply: String,
)

/**
 * Turns a free-text / spoken intent into a nicely worded planner entry with a friendly
 * confirmation, mirroring the iOS `IntentAssistant`. Everything runs on device — the
 * deterministic `SmartParser` classifies and words the entry, so nothing leaves the phone.
 */
object IntentAssistant {

    fun draft(raw: String): AssistantDraft {
        val entry = SmartParser.parse(raw.trim())
        return AssistantDraft(entry, confirmation(entry))
    }

    private fun confirmation(entry: ParsedEntry): String {
        if (entry.title.isEmpty()) {
            return "I couldn't make an item out of that — try something like " +
                "“Lunch with Sam tomorrow at 1pm”."
        }
        return when (entry.kind) {
            PlannerKind.APPOINTMENT -> {
                if (entry.date != null) {
                    val when_ = SimpleDateFormat("EEEE, MMM d 'at' h:mm a", Locale.getDefault())
                        .format(Date(entry.date))
                    "Scheduled “${entry.title}” for $when_."
                } else {
                    "Scheduled “${entry.title}”."
                }
            }
            PlannerKind.TASK -> {
                if (entry.date != null) {
                    val when_ = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                        .format(Date(entry.date))
                    "Added “${entry.title}” to your to-dos, due $when_."
                } else {
                    "Added “${entry.title}” to your to-dos."
                }
            }
        }
    }
}
