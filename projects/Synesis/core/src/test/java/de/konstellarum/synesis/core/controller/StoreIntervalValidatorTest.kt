package de.konstellarum.synesis.core.controller

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StoreIntervalValidatorTest {

    @Test
    fun `accepts values inside the allowed range`() {
        assertEquals(
            StoreIntervalValidator.ValidationResult.Valid(5),
            StoreIntervalValidator.validate("5"),
        )
        assertEquals(
            StoreIntervalValidator.ValidationResult.Valid(10),
            StoreIntervalValidator.validate("10"),
        )
        assertEquals(
            StoreIntervalValidator.ValidationResult.Valid(3600),
            StoreIntervalValidator.validate("3600"),
        )
    }

    @Test
    fun `rejects a blank input`() {
        assertEquals(
            "Wert darf nicht leer sein.",
            messageOf(StoreIntervalValidator.validate("")),
        )
        assertEquals(
            "Wert darf nicht leer sein.",
            messageOf(StoreIntervalValidator.validate("   ")),
        )
    }

    @Test
    fun `rejects non-digit characters`() {
        assertIs<StoreIntervalValidator.ValidationResult.Invalid>(
            StoreIntervalValidator.validate("abc"),
        )
        assertIs<StoreIntervalValidator.ValidationResult.Invalid>(
            StoreIntervalValidator.validate("-5"),
        )
        assertIs<StoreIntervalValidator.ValidationResult.Invalid>(
            StoreIntervalValidator.validate("10,5"),
        )
    }

    @Test
    fun `rejects values below 5 and above 3600 seconds`() {
        assertEquals(
            "Bereich 5–3600 s.",
            messageOf(StoreIntervalValidator.validate("4")),
        )
        assertEquals(
            "Bereich 5–3600 s.",
            messageOf(StoreIntervalValidator.validate("3601")),
        )
    }

    @Test
    fun `rejects numbers that overflow an Int`() {
        assertEquals(
            "Zahl ist zu groß.",
            messageOf(StoreIntervalValidator.validate("9999999999")),
        )
    }

    private fun messageOf(result: StoreIntervalValidator.ValidationResult): String =
        (result as StoreIntervalValidator.ValidationResult.Invalid).message
}
