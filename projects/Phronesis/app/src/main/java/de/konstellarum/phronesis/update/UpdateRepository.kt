package de.konstellarum.phronesis.update

interface UpdateRepository {
    suspend fun fetchLatest(): UpdateInfo?
}
