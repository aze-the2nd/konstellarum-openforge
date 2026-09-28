package de.konstellarum.synesis.core.platform

/**
 * Registration order is significant (module list order); lookup happens by id.
 */
class ModuleRegistry(modules: List<FeatureModule>) {

    private val modulesById: Map<String, FeatureModule> = modules.associateBy { it.id }

    val ordered: List<FeatureModule> = modules.toList()

    val ids: Set<String> = modulesById.keys

    fun module(id: String): FeatureModule? = modulesById[id]
}
