package de.konstellarum.synesis.core.controller

/**
 * Validates the user's store-interval input before it is written to the
 * controller. Limits follow [StoreIntervalConfig]: whole seconds in
 * 5..3600. The device additionally ignores invalid writes, but the app
 * validates first so the user gets immediate feedback.
 */
object StoreIntervalValidator {

    sealed interface ValidationResult {
        data class Valid(val seconds: Int) : ValidationResult
        data class Invalid(val message: String) : ValidationResult
    }

    fun validate(text: String): ValidationResult {
        if (text.isBlank()) {
            return ValidationResult.Invalid("Wert darf nicht leer sein.")
        }
        if (text.any { !it.isDigit() }) {
            return ValidationResult.Invalid("Nur ganze Sekunden erlaubt.")
        }
        val seconds = text.toIntOrNull()
            ?: return ValidationResult.Invalid("Zahl ist zu groß.")
        if (!StoreIntervalConfig.isValid(seconds)) {
            return ValidationResult.Invalid(
                "Bereich ${StoreIntervalConfig.MIN_SECONDS}–${StoreIntervalConfig.MAX_SECONDS} s.",
            )
        }
        return ValidationResult.Valid(seconds)
    }
}
