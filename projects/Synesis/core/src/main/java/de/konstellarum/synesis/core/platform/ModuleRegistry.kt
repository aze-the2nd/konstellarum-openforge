package de.konstellarum.synesis.core.platform

/**
 * A group of modules shown as one top-level entry (e.g. "IoT") with tabbed sub-screens.
 */
data class ModuleCategory(
    val id: String,
    val title: String,
    val modules: List<FeatureModule>,
)

/**
 * Registration order is significant (module list order); lookup happens by id.
 * Categorized modules are excluded from [uncategorized] and grouped in [categories]
 * (first-seen category order, module order preserved within a category).
 */
class ModuleRegistry(
    modules: List<FeatureModule>,
    categoryTitles: Map<String, String> = emptyMap(),
) {

    private val modulesById: Map<String, FeatureModule> = modules.associateBy { it.id }

    val ordered: List<FeatureModule> = modules.toList()

    val ids: Set<String> = modulesById.keys

    val uncategorized: List<FeatureModule> = modules.filter { it.category == null }

    val categories: List<ModuleCategory> = buildList {
        val grouped = LinkedHashMap<String, MutableList<FeatureModule>>()
        modules.forEach { module ->
            val category = module.category ?: return@forEach
            grouped.getOrPut(category) { mutableListOf() }.add(module)
        }
        grouped.forEach { (id, categoryModules) ->
            add(ModuleCategory(id = id, title = categoryTitles[id] ?: id, modules = categoryModules))
        }
    }

    fun module(id: String): FeatureModule? = modulesById[id]
}
