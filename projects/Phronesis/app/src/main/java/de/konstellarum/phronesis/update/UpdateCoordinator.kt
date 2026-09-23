package de.konstellarum.phronesis.update

import de.konstellarum.phronesis.AppVersion

class UpdateCoordinator(
    private val repository: UpdateRepository,
    private val currentVersionProvider: () -> AppVersion,
) {
    suspend fun checkForUpdate(): UpdateState {
        return try {
            val latest = repository.fetchLatest() ?: return UpdateState.UpToDate
            val current = currentVersionProvider()

            if (latest.version > current) {
                UpdateState.Available(latest)
            } else {
                UpdateState.UpToDate
            }
        } catch (throwable: Throwable) {
            UpdateState.Error(throwable.message ?: "Unbekannter Update-Fehler")
        }
    }
}
