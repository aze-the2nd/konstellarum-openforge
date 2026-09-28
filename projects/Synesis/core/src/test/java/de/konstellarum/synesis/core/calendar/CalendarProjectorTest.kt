package de.konstellarum.synesis.core.calendar

import de.konstellarum.synesis.core.domain.CalendarDayEntry
import de.konstellarum.synesis.core.domain.CalendarEvent
import de.konstellarum.synesis.core.domain.TodoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalendarProjectorTest {

    private val eventOnDay = CalendarEvent(id = "e1", title = "Meeting", date = "2026-09-28")
    private val eventOtherDay = CalendarEvent(id = "e2", title = "Urlaub", date = "2026-09-29")
    private val linkedTodo = TodoItem(
        id = "t1",
        title = "Keller prüfen",
        done = false,
        linkedDate = "2026-09-28",
        createdAtEpochMillis = 1L,
    )
    private val unlinkedTodo = linkedTodo.copy(id = "t2", linkedDate = null)

    @Test
    fun `entries include events of that day only`() {
        val entries = CalendarProjector.entriesFor("2026-09-28", listOf(eventOnDay, eventOtherDay), emptyList())

        assertEquals(1, entries.size)
        assertEquals("e1", entries.single().id)
        assertEquals(CalendarDayEntry.Kind.EVENT, entries.single().kind)
    }

    @Test
    fun `entries include todos linked to that day only`() {
        val entries = CalendarProjector.entriesFor("2026-09-28", emptyList(), listOf(linkedTodo, unlinkedTodo))

        assertEquals(1, entries.size)
        assertEquals("t1", entries.single().id)
        assertEquals(CalendarDayEntry.Kind.TODO, entries.single().kind)
    }

    @Test
    fun `events come before todos when both match the day`() {
        val entries = CalendarProjector.entriesFor("2026-09-28", listOf(eventOnDay), listOf(linkedTodo))

        assertEquals(listOf("e1", "t1"), entries.map { it.id })
    }

    @Test
    fun `a day without matching entries yields an empty list`() {
        val entries = CalendarProjector.entriesFor("2026-01-01", listOf(eventOnDay), listOf(linkedTodo))

        assertTrue(entries.isEmpty())
    }

    @Test
    fun `datesWithEntries collects event dates and linked todo dates`() {
        val dates = CalendarProjector.datesWithEntries(listOf(eventOnDay, eventOtherDay), listOf(linkedTodo, unlinkedTodo))

        assertEquals(setOf("2026-09-28", "2026-09-29"), dates)
    }
}
