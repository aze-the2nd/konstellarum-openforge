package de.konstellarum.synesis.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModuleRegistryTest {

    private class FakeModule(
        override val id: String,
        override val title: String,
        override val description: String,
    ) : FeatureModule

    private val alpha = FakeModule("alpha", "Alpha", "first")
    private val beta = FakeModule("beta", "Beta", "second")
    private val registry = ModuleRegistry(listOf(alpha, beta))

    @Test
    fun `ordered modules preserve registration order`() {
        assertEquals(listOf(alpha, beta), registry.ordered)
    }

    @Test
    fun `module lookup finds a registered id`() {
        assertEquals(beta, registry.module("beta"))
    }

    @Test
    fun `module lookup returns null for an unknown id`() {
        assertNull(registry.module("missing"))
    }

    @Test
    fun `ids expose all registered module ids`() {
        assertEquals(setOf("alpha", "beta"), registry.ids)
    }

    @Test
    fun `registry without modules is empty but usable`() {
        val emptyRegistry = ModuleRegistry(emptyList())

        assertEquals(emptyList(), emptyRegistry.ordered)
        assertNull(emptyRegistry.module("anything"))
    }
}
