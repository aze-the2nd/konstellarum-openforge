package de.konstellarum.synesis.todos

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import de.konstellarum.synesis.core.domain.TodoRepository

@Composable
fun TodosModule(repository: TodoRepository) {
    val todos by repository.todos.collectAsState()
    var newTitle by remember { mutableStateOf("") }

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
                    repository.addTodo(newTitle)
                    newTitle = ""
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aufgabe hinzufügen")
            }
        }

        if (todos.isEmpty()) {
            Text("Noch keine Aufgaben.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(todos, key = { it.id }) { todo ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = todo.done,
                            onCheckedChange = { repository.setTodoDone(todo.id, it) },
                        )
                        Text(
                            text = todo.title,
                            modifier = Modifier.weight(1f),
                            textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                        )
                        IconButton(onClick = { repository.removeTodo(todo.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Aufgabe löschen")
                        }
                    }
                }
            }
        }
    }
}
