package de.konstellarum.synesis

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.konstellarum.synesis.calendar.FileEventRepository
import de.konstellarum.synesis.cellar.HttpTempRepository
import de.konstellarum.synesis.chat.TelegramChatLauncher
import de.konstellarum.synesis.controller.BleControllerRepository
import de.konstellarum.synesis.core.chat.TelegramChatLink
import de.konstellarum.synesis.notes.FileNoteRepository
import de.konstellarum.synesis.transcribe.FileTranscriptRepository
import de.konstellarum.synesis.transcribe.HttpWhisperTranscriptionRepository
import de.konstellarum.synesis.todos.FileTodoRepository
import de.konstellarum.synesis.ui.CategoryScreen
import de.konstellarum.synesis.ui.HomeScreen
import de.konstellarum.synesis.ui.ModuleScreen
import de.konstellarum.synesis.update.GitHubReleaseUpdateRepository
import de.konstellarum.synesis.update.UpdateCoordinator
import de.konstellarum.synesis.update.UpdateLauncher
import de.konstellarum.synesis.update.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SynesisApp() {
    val context = LocalContext.current
    val chatLauncher = remember(context) { TelegramChatLauncher(context) }
    val chatLink = remember { TelegramChatLink.forBot(BuildConfig.HERMES_TELEGRAM_BOT_USERNAME) }
    val host = remember(context) {
        ModuleHost(
            noteRepository = FileNoteRepository(context),
            todoRepository = FileTodoRepository(context),
            eventRepository = FileEventRepository(context),
            transcriptRepository = FileTranscriptRepository(context),
            tempRepository = HttpTempRepository(context),
            controllerRepository = BleControllerRepository(context),
            whisperTranscriptionRepository = HttpWhisperTranscriptionRepository(),
            openChat = { chatLauncher.open(chatLink) },
        )
    }

    var selectedModuleId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }

    val launcher = remember(context) { UpdateLauncher(context) }
    val coordinator = remember {
        UpdateCoordinator(
            repository = GitHubReleaseUpdateRepository(),
            currentVersionProvider = {
                AppVersion.parse(BuildConfig.VERSION_NAME)
                    ?: AppVersion(0, 1, 0)
            },
        )
    }

    var state by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }
    var refreshToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshToken) {
        state = UpdateState.Checking
        state = withContext(Dispatchers.IO) {
            coordinator.checkForUpdate()
        }
    }

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    ) {
        val activeModule = selectedModuleId?.let { AppModules.byId(it) }
        val activeCategory = selectedCategoryId
            ?.let { id -> AppModules.registry.categories.firstOrNull { it.id == id } }
        when {
            activeModule != null -> ModuleScreen(
                module = activeModule.descriptor,
                host = host,
                content = activeModule.content,
                onBack = { selectedModuleId = null },
            )

            activeCategory != null -> CategoryScreen(
                category = activeCategory,
                host = host,
                onBack = { selectedCategoryId = null },
            )

            else -> HomeScreen(
                appVersion = BuildConfig.VERSION_NAME,
                state = state,
                modules = AppModules.registry.uncategorized,
                categories = AppModules.registry.categories,
                onOpenModule = { selectedModuleId = it },
                onOpenCategory = { selectedCategoryId = it },
                onOpenChat = { chatLauncher.open(chatLink) },
                onStartUpdate = { launcher.install(it.downloadUrl) },
                onRetry = { refreshToken += 1 },
            )
        }
    }
}
