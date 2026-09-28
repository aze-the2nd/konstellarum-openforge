package de.konstellarum.synesis.core.domain

import kotlinx.serialization.Serializable

/**
 * A standalone note. Notes stay out of the calendar; dated planning belongs to todos/events.
 * [linkedDate] is a legacy field kept only to preserve older notes created before task-calendar
 * links moved to todos.
 */
@Serializable
data class Note(
    val id: String,
    val title: String,
    val body: String = "",
    val linkedDate: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

/**
 * A todo. [linkedDate] is an ISO-8601 date (yyyy-MM-dd) when the task should be shown in the
 * calendar, null otherwise. The calendar projects linked todos directly; no copy is made.
 */
@Serializable
data class TodoItem(
    val id: String,
    val title: String,
    val done: Boolean = false,
    val linkedDate: String? = null,
    val createdAtEpochMillis: Long,
)

/**
 * A standalone calendar entry. [date] is an ISO-8601 date (yyyy-MM-dd).
 */
@Serializable
data class CalendarEvent(
    val id: String,
    val title: String,
    val date: String,
)

@Serializable
data class Transcript(
    val id: String,
    val text: String,
    val createdAtEpochMillis: Long,
)

/**
 * A projected entry on one calendar day, composed from events and linked todos.
 */
data class CalendarDayEntry(
    val kind: Kind,
    val id: String,
    val title: String,
) {
    enum class Kind { EVENT, TODO }
}
