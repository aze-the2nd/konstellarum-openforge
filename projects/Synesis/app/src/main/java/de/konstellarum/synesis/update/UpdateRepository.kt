package de.konstellarum.synesis.update

interface UpdateRepository {
    suspend fun fetchLatest(): UpdateInfo?
}
