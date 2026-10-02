package de.konstellarum.synesis.core.controller

/**
 * What the app currently knows about the controller's store interval.
 *
 * [Unknown] means the value has not been read (yet) or could not be decoded;
 * [Unsupported] means the characteristic is absent or lacks READ/WRITE
 * properties, i.e. the firmware predates v3. The app still allows writing in
 * the [Unknown] case and verifies via read-back.
 */
sealed interface StoreIntervalState {
    data object Unknown : StoreIntervalState
    data class Current(val seconds: Int) : StoreIntervalState
    data object Unsupported : StoreIntervalState
}
