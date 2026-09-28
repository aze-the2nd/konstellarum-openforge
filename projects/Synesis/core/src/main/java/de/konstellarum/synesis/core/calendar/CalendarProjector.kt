package de.konstellarum.synesis.core.calendar

import de.konstellarum.synesis.core.domain.CalendarDayEntry
import de.konstellarum.synesis.core.domain.CalendarEvent
import de.konstellarum.synesis.core.domain.Note

/**
 * Projects calendar events and calendar-linked notes onto a single day view.
 * Events come first, then notes; order within each group follows the input order.
 */
object CalendarProjector {

    fun entriesFor(
        date: String,
        events: List<CalendarEvent>,
        notes: List<Note>,
    ): List<CalendarDayEntry> = buildList {
        events.filter { it.date == date }.forEach { event ->
            add(CalendarDayEntry(kind = CalendarDayEntry.Kind.EVENT, id = event.id, title = event.title))
        }
        notes.filter { it.linkedDate == date }.forEach { note ->
            add(CalendarDayEntry(kind = CalendarDayEntry.Kind.NOTE, id = note.id, title = note.title))
        }
    }

    fun datesWithEntries(events: List<CalendarEvent>, notes: List<Note>): Set<String> = buildSet {
        events.forEach { add(it.date) }
        notes.forEach { if (it.linkedDate != null) add(it.linkedDate) }
    }
}
