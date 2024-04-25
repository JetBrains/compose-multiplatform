package org.jetbrains.compose.internal

internal data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val meta: String
): Comparable<Version> {
    override fun compareTo(other: Version): Int = when {
        major != other.major -> major - other.major
        minor != other.minor -> minor - other.minor
        patch != other.patch -> patch - other.patch
        else -> {
            if (meta.isEmpty()) 1
            else if (other.meta.isEmpty()) -1
            else meta.compareTo(other.meta)
        }
    }

    companion object {
        private val SEMVER_REGEXP = """^(\d+)(?:\.(\d*))?(?:\.(\d*))?(?:-(.*))?${'$'}""".toRegex()
        fun fromString(versionString: String): Version {
            val matchResult: MatchResult = SEMVER_REGEXP.matchEntire(versionString) ?: return Version(0,0,0, "")
            val major: Int = matchResult.groups[1]?.value?.toInt() ?: 0
            val minor: Int = matchResult.groups[2]?.value?.toInt() ?: 0
            val patch: Int = matchResult.groups[3]?.value?.toInt() ?: 0
            val meta: String = matchResult.groups[4]?.value ?: ""
            return Version(major, minor, patch, meta)
        }
    }
}