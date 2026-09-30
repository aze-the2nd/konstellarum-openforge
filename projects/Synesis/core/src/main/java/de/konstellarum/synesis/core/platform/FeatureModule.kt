package de.konstellarum.synesis.core.platform

/**
 * Contract every Synesis module satisfies: a stable identity plus the metadata the shell
 * shows in the module list. Modules may opt into a [category]; categorized modules are
 * no longer listed as flat home-screen entries but grouped under their category entry
 * (e.g. the IoT category opens a tabbed sensor screen).
 */
interface FeatureModule {
    val id: String
    val title: String
    val description: String

    /**
     * Optional group the module belongs to. Null (default) keeps the module as a direct
     * home-screen entry. Non-null groups modules with the same category id together.
     */
    val category: String?
        get() = null
}
