package de.konstellarum.synesis.core.domain

import kotlinx.serialization.Serializable

/**
 * A note. [linkedDate] is an ISO-8601 date (yyyy-MM-dd) when the note is linked into the
 * calendar, null otherwise. The calendar projects linked notes directly; no copy is made.
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

@Serializable
data class TodoItem(
    val id: String,
    val title: String,
    val done: Boolean = false,
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

/**
 * A projected entry on one calendar day, composed from events and linked notes.
 */
data class CalendarDayEntry(
    val kind: Kind,
    val id: String,
    val title: String,
) {
    enum class Kind { EVENT, NOTE }
}
