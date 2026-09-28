package de.konstellarum.synesis

import androidx.compose.runtime.Composable
import de.konstellarum.synesis.calendar.CalendarModule
import de.konstellarum.synesis.cellar.CellarModule
import de.konstellarum.synesis.controller.ControllerModule
import de.konstellarum.synesis.core.controller.ControllerRepository
import de.konstellarum.synesis.core.domain.EventRepository
import de.konstellarum.synesis.core.domain.NoteRepository
import de.konstellarum.synesis.core.domain.TranscriptRepository
import de.konstellarum.synesis.core.domain.TodoRepository
import de.konstellarum.synesis.core.platform.FeatureModule
import de.konstellarum.synesis.core.platform.ModuleRegistry
import de.konstellarum.synesis.core.sensor.TempRepository
import de.konstellarum.synesis.notes.NotesModule
import de.konstellarum.synesis.transcribe.TranscribeModule
import de.konstellarum.synesis.todos.TodosModule

/**
 * Host object that owns one repository instance per data type. The shell creates it once and
 * passes it to every module, so cross-module views (e.g. calendar showing linked notes) stay live.
 */
class ModuleHost(
    val noteRepository: NoteRepository,
    val todoRepository: TodoRepository,
    val eventRepository: EventRepository,
    val transcriptRepository: TranscriptRepository,
    val tempRepository: TempRepository,
    val controllerRepository: ControllerRepository,
    val openChat: () -> Boolean,
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
                moduleDescription = "Gedanken festhalten — unabhängig vom Kalender.",
            ),
            content = { host -> NotesModule(host.noteRepository) },
        ),
        AppModule(
            descriptor = moduleDescriptor(
                moduleId = "todos",
                moduleTitle = "Aufgaben",
                moduleDescription = "Aufgabenliste mit Erledigt-Status und optionaler Kalender-Verknüpfung.",
            ),
            content = { host -> TodosModule(host.todoRepository) },
        ),
        AppModule(
            descriptor = moduleDescriptor(
                moduleId = "calendar",
                moduleTitle = "Kalender",
                moduleDescription = "Monatsansicht mit Terminen und verknüpften Aufgaben.",
            ),
            content = { host -> CalendarModule(host.eventRepository, host.todoRepository) },
        ),
        AppModule(
            descriptor = moduleDescriptor(
                moduleId = "transcribe",
                moduleTitle = "Transkription",
                moduleDescription = "Sprache erfassen, lokal speichern und per KI präzisieren lassen.",
            ),
            content = { host -> TranscribeModule(host.transcriptRepository, host.openChat) },
        ),
        AppModule(
            descriptor = moduleDescriptor(
                moduleId = "cellar",
                moduleTitle = "Keller",
                moduleDescription = "Temperaturüberwachung des Kellertemperatur-Sensors.",
            ),
            content = { host -> CellarModule(host.tempRepository) },
        ),
        AppModule(
            descriptor = moduleDescriptor(
                moduleId = "controller",
                moduleTitle = "Controller",
                moduleDescription = "WLAN-Einstellungen des Sensors per Bluetooth ändern.",
            ),
            content = { host -> ControllerModule(host.controllerRepository) },
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
