package de.konstellarum.synesis.todos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.konstellarum.synesis.core.calendar.IsoDates
import de.konstellarum.synesis.core.domain.TodoItem
import de.konstellarum.synesis.core.domain.TodoRepository
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosModule(repository: TodoRepository) {
    val todos by repository.todos.collectAsState()
    var newTitle by remember { mutableStateOf("") }
    var newLinkedDate by remember { mutableStateOf<String?>(null) }
    var datePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Neue Aufgabe") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            IconButton(
                enabled = newTitle.isNotBlank(),
                onClick = {
                    repository.addTodo(newTitle, newLinkedDate)
                    newTitle = ""
                    newLinkedDate = null
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aufgabe hinzufügen")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = newLinkedDate != null,
                onCheckedChange = { checked ->
                    newLinkedDate = if (checked) {
                        newLinkedDate ?: LocalDate.now().toString()
                    } else {
                        null
                    }
                },
            )
            Column {
                Text("Im Kalender anzeigen")
                newLinkedDate?.let { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable { datePickerTarget = DatePickerTarget.NewTodo },
                    )
                }
            }
        }

        if (todos.isEmpty()) {
            Text("Noch keine Aufgaben.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(todos, key = { it.id }) { todo ->
                    TodoRow(
                        todo = todo,
                        onDoneChange = { repository.setTodoDone(todo.id, it) },
                        onLinkedDateChange = { repository.setTodoLinkedDate(todo.id, it) },
                        onPickLinkedDate = { datePickerTarget = DatePickerTarget.ExistingTodo(todo.id) },
                        onDelete = { repository.removeTodo(todo.id) },
                    )
                }
            }
        }
    }

    val target = datePickerTarget
    if (target != null) {
        val currentDate = when (target) {
            DatePickerTarget.NewTodo -> newLinkedDate
            is DatePickerTarget.ExistingTodo -> todos.firstOrNull { it.id == target.id }?.linkedDate
        }
        val initialMillis = currentDate
            ?.let { IsoDates.parse(it) }
            ?.let { IsoDates.toEpochMillis(it) }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                            ?.let { IsoDates.fromEpochMillis(it).toString() }
                            ?: currentDate
                            ?: LocalDate.now().toString()
                        when (target) {
                            DatePickerTarget.NewTodo -> newLinkedDate = selected
                            is DatePickerTarget.ExistingTodo -> repository.setTodoLinkedDate(target.id, selected)
                        }
                        datePickerTarget = null
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerTarget = null }) { Text("Abbrechen") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TodoRow(
    todo: TodoItem,
    onDoneChange: (Boolean) -> Unit,
    onLinkedDateChange: (String?) -> Unit,
    onPickLinkedDate: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = todo.done,
                onCheckedChange = onDoneChange,
            )
            Text(
                text = todo.title,
                modifier = Modifier.weight(1f),
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Aufgabe löschen")
            }
        }
        Row(
            modifier = Modifier.padding(start = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = todo.linkedDate != null,
                onCheckedChange = { checked ->
                    onLinkedDateChange(
                        if (checked) {
                            todo.linkedDate ?: LocalDate.now().toString()
                        } else {
                            null
                        },
                    )
                },
            )
            Text("Kalender")
            todo.linkedDate?.let { date ->
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable(onClick = onPickLinkedDate),
                )
            }
        }
    }
}

private sealed interface DatePickerTarget {
    data object NewTodo : DatePickerTarget
    data class ExistingTodo(val id: String) : DatePickerTarget
}
