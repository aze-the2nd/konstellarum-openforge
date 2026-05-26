package de.blackforest.androidupdatepilot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.blackforest.androidupdatepilot.BuildConfig
import de.blackforest.androidupdatepilot.update.UpdateInfo
import de.blackforest.androidupdatepilot.update.UpdateState

@Composable
fun HomeScreen(
    appVersion: String,
    state: UpdateState,
    onStartUpdate: (UpdateInfo) -> Unit,
    onRetry: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "AndroidUpdatePilot v$appVersion",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Die App prüft beim Start GitHub Releases und bietet ein direktes Update an.",
                style = MaterialTheme.typography.bodyMedium,
            )

            when (state) {
                UpdateState.Checking -> {
                    Text(text = "Suche nach Updates …")
                }

                UpdateState.UpToDate -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Aktuell",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(text = "Die installierte Version ist bereits auf dem neuesten Stand.")
                        }
                    }
                }

                is UpdateState.Available -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Update verfügbar: ${state.info.version}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(text = state.info.changelog)
                            Button(onClick = { onStartUpdate(state.info) }) {
                                Text(text = "Update installieren")
                            }
                        }
                    }
                }

                is UpdateState.Error -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Update-Check fehlgeschlagen",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(text = state.message)
                            OutlinedButton(onClick = onRetry) {
                                Text(text = "Erneut versuchen")
                            }
                        }
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
