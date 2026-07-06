package com.tertiaryinfotech.plannerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** The kind of entry — a plain to-do task or a scheduled appointment. */
enum class PlannerKind(val raw: String, val title: String) {
    TASK("task", "To-Do"),
    APPOINTMENT("appointment", "Appointment");

    companion object {
        fun from(raw: String): PlannerKind = entries.firstOrNull { it.raw == raw } ?: TASK
    }
}

/**
 * A single entry in the planner — either a to-do task or a calendar appointment.
 * Mirrors the iOS `PlannerItem` SwiftData model.
 */
@Entity(tableName = "planner_items")
data class PlannerItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val notes: String = "",
    /** `PlannerKind.raw` — "task" or "appointment". */
    val kindRaw: String = PlannerKind.TASK.raw,
    /** Epoch millis when the appointment occurs. Null for plain tasks with no due time. */
    val date: Long? = null,
    /** Checked-off state. Checking an item auto-archives it. */
    val isDone: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
) {
    val kind: PlannerKind get() = PlannerKind.from(kindRaw)
    val isAppointment: Boolean get() = kind == PlannerKind.APPOINTMENT

    /** Toggle completion. Checking auto-archives (per app spec); unchecking restores. */
    fun toggledDone(): PlannerItem =
        if (!isDone) copy(isDone = true, completedAt = System.currentTimeMillis(), isArchived = true)
        else copy(isDone = false, completedAt = null, isArchived = false)
}
