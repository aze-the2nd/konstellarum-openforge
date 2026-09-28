package de.konstellarum.synesis.core.platform

/**
 * Contract every Synesis module satisfies: a stable identity plus the metadata the shell
 * shows in the module list.
 */
interface FeatureModule {
    val id: String
    val title: String
    val description: String
}
