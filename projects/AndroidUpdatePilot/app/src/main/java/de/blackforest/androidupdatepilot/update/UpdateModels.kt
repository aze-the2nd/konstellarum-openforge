package de.blackforest.androidupdatepilot.update

import de.blackforest.androidupdatepilot.AppVersion

data class UpdateInfo(
    val version: AppVersion,
    val tagName: String,
    val downloadUrl: String,
    val changelog: String,
)

sealed interface UpdateState {
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Error(val message: String) : UpdateState
}
