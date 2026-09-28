package de.konstellarum.synesis.core.calendar

import de.konstellarum.synesis.core.domain.CalendarDayEntry
import de.konstellarum.synesis.core.domain.CalendarEvent
import de.konstellarum.synesis.core.domain.Note
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalendarProjectorTest {

    private val eventOnDay = CalendarEvent(id = "e1", title = "Meeting", date = "2026-09-28")
    private val eventOtherDay = CalendarEvent(id = "e2", title = "Urlaub", date = "2026-09-29")
    private val linkedNote = Note(
        id = "n1",
        title = "Einkaufsliste",
        body = "Milch",
        linkedDate = "2026-09-28",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )
    private val unlinkedNote = linkedNote.copy(id = "n2", linkedDate = null)

    @Test
    fun `entries include events of that day only`() {
        val entries = CalendarProjector.entriesFor("2026-09-28", listOf(eventOnDay, eventOtherDay), emptyList())

        assertEquals(1, entries.size)
        assertEquals("e1", entries.single().id)
        assertEquals(CalendarDayEntry.Kind.EVENT, entries.single().kind)
    }

    @Test
    fun `entries include notes linked to that day only`() {
        val entries = CalendarProjector.entriesFor("2026-09-28", emptyList(), listOf(linkedNote, unlinkedNote))

        assertEquals(1, entries.size)
        assertEquals("n1", entries.single().id)
        assertEquals(CalendarDayEntry.Kind.NOTE, entries.single().kind)
    }

    @Test
    fun `events come before notes when both match the day`() {
        val entries = CalendarProjector.entriesFor("2026-09-28", listOf(eventOnDay), listOf(linkedNote))

        assertEquals(listOf("e1", "n1"), entries.map { it.id })
    }

    @Test
    fun `a day without matching entries yields an empty list`() {
        val entries = CalendarProjector.entriesFor("2026-01-01", listOf(eventOnDay), listOf(linkedNote))

        assertTrue(entries.isEmpty())
    }

    @Test
    fun `datesWithEntries collects event dates and linked note dates`() {
        val dates = CalendarProjector.datesWithEntries(listOf(eventOnDay, eventOtherDay), listOf(linkedNote, unlinkedNote))

        assertEquals(setOf("2026-09-28", "2026-09-29"), dates)
    }
}
