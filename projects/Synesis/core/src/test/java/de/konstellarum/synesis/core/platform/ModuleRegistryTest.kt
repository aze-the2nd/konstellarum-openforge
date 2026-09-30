package de.konstellarum.synesis.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModuleRegistryTest {

    private class FakeModule(
        override val id: String,
        override val title: String,
        override val description: String,
        override val category: String? = null,
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

    @Test
    fun `categorized modules are grouped and excluded from uncategorized`() {
        val cellar = FakeModule("cellar", "Keller", "sensor", category = "IoT")
        val controller = FakeModule("controller", "Controller", "ble", category = "IoT")
        val registry = ModuleRegistry(listOf(alpha, cellar, controller, beta))

        assertEquals(listOf(alpha, beta), registry.uncategorized)
        assertEquals(1, registry.categories.size)
        assertEquals("IoT", registry.categories[0].id)
        assertEquals(listOf(cellar, controller), registry.categories[0].modules)
    }

    @Test
    fun `category order follows first appearance and titles come from the map`() {
        val cellar = FakeModule("cellar", "Keller", "sensor", category = "IoT")
        val registry = ModuleRegistry(
            listOf(cellar),
            categoryTitles = mapOf("IoT" to "IoT & Automation"),
        )

        assertEquals("IoT & Automation", registry.categories[0].title)
    }

    @Test
    fun `category title falls back to the category id`() {
        val cellar = FakeModule("cellar", "Keller", "sensor", category = "IoT")
        val registry = ModuleRegistry(listOf(cellar))

        assertEquals("IoT", registry.categories[0].title)
    }
}
