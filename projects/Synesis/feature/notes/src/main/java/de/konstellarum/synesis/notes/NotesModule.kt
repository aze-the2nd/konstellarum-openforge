package de.konstellarum.synesis.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.konstellarum.synesis.core.domain.Note
import de.konstellarum.synesis.core.domain.NoteRepository

@Composable
fun NotesModule(repository: NoteRepository) {
    val notes by repository.notes.collectAsState()
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    editorOpen = true
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Notiz hinzufügen")
            }
        },
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Noch keine Notizen.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onEdit = {
                            editing = note
                            editorOpen = true
                        },
                        onDelete = { repository.removeNote(note.id) },
                    )
                }
            }
        }
    }

    if (editorOpen) {
        NoteEditorDialog(
            existing = editing,
            onDismiss = { editorOpen = false },
            onSave = { title, body ->
                val existing = editing
                if (existing == null) {
                    repository.addNote(title, body)
                } else {
                    repository.updateNote(existing.copy(title = title.trim(), body = body))
                }
                editorOpen = false
            },
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.body.isNotBlank()) {
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Notiz löschen")
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(
    existing: Note?,
    onDismiss: () -> Unit,
    onSave: (title: String, body: String) -> Unit,
) {
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var body by remember(existing) { mutableStateOf(existing?.body ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Neue Notiz" else "Notiz bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Inhalt") },
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onSave(title, body) },
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
