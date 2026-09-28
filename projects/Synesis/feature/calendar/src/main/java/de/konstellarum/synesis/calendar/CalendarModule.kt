package de.konstellarum.synesis.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.konstellarum.synesis.core.calendar.CalendarProjector
import de.konstellarum.synesis.core.calendar.DisplayFormats
import de.konstellarum.synesis.core.calendar.MonthCell
import de.konstellarum.synesis.core.calendar.MonthGrid
import de.konstellarum.synesis.core.domain.CalendarDayEntry
import de.konstellarum.synesis.core.domain.EventRepository
import de.konstellarum.synesis.core.domain.NoteRepository
import java.time.YearMonth

@Composable
fun CalendarModule(eventRepository: EventRepository, noteRepository: NoteRepository) {
    val events by eventRepository.events.collectAsState()
    val notes by noteRepository.notes.collectAsState()

    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var addEventOpen by remember { mutableStateOf(false) }

    val cells = remember(yearMonth) { MonthGrid.cellsFor(yearMonth) }
    val markedDates = remember(events, notes) { CalendarProjector.datesWithEntries(events, notes) }
    val selectedEntries = remember(selectedDate, events, notes) {
        selectedDate?.let { CalendarProjector.entriesFor(it, events, notes) } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { yearMonth = yearMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Vorheriger Monat")
            }
            Text(
                text = DisplayFormats.monthTitle(yearMonth),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
            IconButton(onClick = { yearMonth = yearMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Nächster Monat")
            }
        }

        Row {
            DisplayFormats.weekdayShortLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    DayCell(
                        cell = cell,
                        selected = cell.date != null && cell.date == selectedDate,
                        marked = cell.date != null && cell.date in markedDates,
                        onClick = { cell.date?.let { selectedDate = it } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        selectedDate?.let { date ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = date,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = { addEventOpen = true }) {
                    Text("Termin hinzufügen")
                }
            }
            if (selectedEntries.isEmpty()) {
                Text("Keine Einträge an diesem Tag.")
            } else {
                selectedEntries.forEach { entry ->
                    EntryRow(
                        entry = entry,
                        onDeleteEvent = { eventRepository.removeEvent(entry.id) },
                    )
                }
            }
        }
    }

    if (addEventOpen && selectedDate != null) {
        AddEventDialog(
            date = selectedDate.orEmpty(),
            onDismiss = { addEventOpen = false },
            onSave = { title ->
                eventRepository.addEvent(title, selectedDate.orEmpty())
                addEventOpen = false
            },
        )
    }
}

@Composable
private fun DayCell(
    cell: MonthCell,
    selected: Boolean,
    marked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        if (cell.dayOfMonth != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = cell.dayOfMonth.toString(),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClick)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape,
                        )
                        .padding(6.dp),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (marked) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: CalendarDayEntry,
    onDeleteEvent: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (entry.kind == CalendarDayEntry.Kind.EVENT) "Termin" else "Notiz",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = entry.title,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (entry.kind == CalendarDayEntry.Kind.EVENT) {
            IconButton(onClick = onDeleteEvent) {
                Icon(Icons.Default.Delete, contentDescription = "Termin löschen")
            }
        }
    }
}

@Composable
private fun AddEventDialog(
    date: String,
    onDismiss: () -> Unit,
    onSave: (title: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Termin hinzufügen") },
        text = {
            Column {
                Text(date, style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onSave(title.trim()) }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
