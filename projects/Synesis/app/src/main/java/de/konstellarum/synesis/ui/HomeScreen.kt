package de.konstellarum.synesis.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.konstellarum.synesis.BuildConfig
import de.konstellarum.synesis.core.platform.FeatureModule
import de.konstellarum.synesis.update.UpdateInfo
import de.konstellarum.synesis.update.UpdateState

@Composable
fun HomeScreen(
    appVersion: String,
    state: UpdateState,
    modules: List<FeatureModule>,
    onOpenModule: (String) -> Unit,
    onOpenChat: () -> Unit,
    onStartUpdate: (UpdateInfo) -> Unit,
    onRetry: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Synesis v$appVersion",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Deine persönliche Zentrale — erweiterbar um Module.",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedButton(
                onClick = onOpenChat,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Chat mit Thomas öffnen")
            }

            when (state) {
                UpdateState.Checking -> Text(text = "Suche nach Updates …")

                UpdateState.UpToDate -> UpdateCard(
                    title = "Aktuell",
                    body = "Die installierte Version ist bereits auf dem neuesten Stand.",
                )

                is UpdateState.Available -> UpdateCard(
                    title = "Update verfügbar: ${state.info.version}",
                    body = state.info.changelog,
                    action = {
                        Button(onClick = { onStartUpdate(state.info) }) {
                            Text(text = "Update installieren")
                        }
                    },
                )

                is UpdateState.Error -> UpdateCard(
                    title = "Update-Check fehlgeschlagen",
                    body = state.message,
                    action = {
                        OutlinedButton(onClick = onRetry) {
                            Text(text = "Erneut versuchen")
                        }
                    },
                )
            }

            Text(text = "Module", style = MaterialTheme.typography.titleMedium)

            modules.forEach { module ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenModule(module.id) },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = module.title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = module.description,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Text(
                text = "Update-Quelle: GitHub Releases · Repo: ${BuildConfig.UPDATE_REPO_OWNER}/${BuildConfig.UPDATE_REPO_NAME}",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun UpdateCard(
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = body)
            action?.invoke()
        }
    }
}
