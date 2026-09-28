package de.konstellarum.synesis.core.calendar

import de.konstellarum.synesis.core.domain.CalendarDayEntry
import de.konstellarum.synesis.core.domain.CalendarEvent
import de.konstellarum.synesis.core.domain.TodoItem

/**
 * Projects calendar events and calendar-linked todos onto a single day view.
 * Events come first, then todos; order within each group follows the input order.
 */
object CalendarProjector {

    fun entriesFor(
        date: String,
        events: List<CalendarEvent>,
        todos: List<TodoItem>,
    ): List<CalendarDayEntry> = buildList {
        events.filter { it.date == date }.forEach { event ->
            add(CalendarDayEntry(kind = CalendarDayEntry.Kind.EVENT, id = event.id, title = event.title))
        }
        todos.filter { it.linkedDate == date }.forEach { todo ->
            add(CalendarDayEntry(kind = CalendarDayEntry.Kind.TODO, id = todo.id, title = todo.title))
        }
    }

    fun datesWithEntries(events: List<CalendarEvent>, todos: List<TodoItem>): Set<String> = buildSet {
        events.forEach { add(it.date) }
        todos.forEach { if (it.linkedDate != null) add(it.linkedDate) }
    }
}
