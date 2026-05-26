package de.blackforest.androidupdatepilot.update

interface UpdateRepository {
    suspend fun fetchLatest(): UpdateInfo?
}
