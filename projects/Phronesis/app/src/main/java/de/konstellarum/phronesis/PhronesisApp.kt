package de.konstellarum.phronesis

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.konstellarum.phronesis.ui.HomeScreen
import de.konstellarum.phronesis.update.GitHubReleaseUpdateRepository
import de.konstellarum.phronesis.update.UpdateCoordinator
import de.konstellarum.phronesis.update.UpdateLauncher
import de.konstellarum.phronesis.update.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PhronesisApp() {
    val context = LocalContext.current
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

    MaterialTheme {
        HomeScreen(
            appVersion = BuildConfig.VERSION_NAME,
            state = state,
            onStartUpdate = { launcher.install(it.downloadUrl) },
            onRetry = { refreshToken += 1 },
        )
    }
}
