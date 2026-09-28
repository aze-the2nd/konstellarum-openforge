package de.konstellarum.synesis

data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int {
        return when {
            major != other.major -> major.compareTo(other.major)
            minor != other.minor -> minor.compareTo(other.minor)
            else -> patch.compareTo(other.patch)
        }
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun parse(value: String): AppVersion? {
            val cleaned = value.trim().removePrefix("v")
            val parts = cleaned.split('.')
            if (parts.size != 3) return null
            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            val patch = parts[2].toIntOrNull() ?: return null
            return AppVersion(major, minor, patch)
        }
    }
}
