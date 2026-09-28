package de.konstellarum.synesis

import androidx.compose.runtime.Composable
import de.konstellarum.synesis.calendar.CalendarModule
import de.konstellarum.synesis.core.domain.EventRepository
import de.konstellarum.synesis.core.domain.NoteRepository
import de.konstellarum.synesis.core.domain.TodoRepository
import de.konstellarum.synesis.core.platform.FeatureModule
import de.konstellarum.synesis.core.platform.ModuleRegistry
import de.konstellarum.synesis.notes.NotesModule
import de.konstellarum.synesis.todos.TodosModule

/**
 * Host object that owns one repository instance per data type. The shell creates it once and
 * passes it to every module, so cross-module views (e.g. calendar showing linked notes) stay live.
 */
class ModuleHost(
    val noteRepository: NoteRepository,
    val todoRepository: TodoRepository,
    val eventRepository: EventRepository,
)

/**
 * A registered module: a core descriptor plus the Compose content it contributes to the shell.
 */
class AppModule(
    val descriptor: FeatureModule,
    val content: @Composable (ModuleHost) -> Unit,
)

object AppModules {

    private val modules: List<AppModule> = listOf(
        AppModule(
            descriptor = moduleDescriptor(
                moduleId = "notes",
                moduleTitle = "Notizen",
                moduleDescription = "Gedanken festhalten, optional mit dem Kalender verknüpft.",
            ),
            content = { host -> NotesModule(host.noteRepository) },
        ),
        AppModule(
            descriptor = moduleDescriptor(
                moduleId = "todos",
                moduleTitle = "Aufgaben",
                moduleDescription = "Einfache Aufgabenliste mit Erledigt-Status.",
            ),
            content = { host -> TodosModule(host.todoRepository) },
        ),
        AppModule(
            descriptor = moduleDescriptor(
                moduleId = "calendar",
                moduleTitle = "Kalender",
                moduleDescription = "Monatsansicht mit Terminen und verknüpften Notizen.",
            ),
            content = { host -> CalendarModule(host.eventRepository, host.noteRepository) },
        ),
    )

    val registry: ModuleRegistry = ModuleRegistry(modules.map { it.descriptor })

    fun byId(id: String): AppModule? = modules.firstOrNull { it.descriptor.id == id }

    private fun moduleDescriptor(
        moduleId: String,
        moduleTitle: String,
        moduleDescription: String,
    ): FeatureModule =
        object : FeatureModule {
            override val id: String = moduleId
            override val title: String = moduleTitle
            override val description: String = moduleDescription
        }
}
