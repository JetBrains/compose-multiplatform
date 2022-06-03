/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build

import org.gradle.api.Project
import java.io.File
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Utility class which represents a version
 */
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val extra: String? = null
) : Comparable<Version>, java.io.Serializable {

    constructor(versionString: String) : this(
        Integer.parseInt(checkedMatcher(versionString).group(1)),
        Integer.parseInt(checkedMatcher(versionString).group(2)),
        Integer.parseInt(checkedMatcher(versionString).group(3)),
        if (checkedMatcher(versionString).groupCount() == 4) checkedMatcher(
            versionString
        ).group(4) else null
    )

    fun isPatch(): Boolean = patch != 0

    fun isSnapshot(): Boolean = "-SNAPSHOT" == extra

    fun isAlpha(): Boolean = extra?.lowercase(Locale.getDefault())?.startsWith("-alpha") ?: false

    fun isBeta(): Boolean = extra?.lowercase(Locale.getDefault())?.startsWith("-beta") ?: false

    fun isDev(): Boolean = extra?.lowercase(Locale.getDefault())?.startsWith("-dev") ?: false

    fun isRC(): Boolean = extra?.lowercase(Locale.getDefault())?.startsWith("-rc") ?: false

    fun isStable(): Boolean = (extra == null)

    // Returns whether the API surface is allowed to change within the current revision (see go/androidx/versioning for policy definition)
    fun isFinalApi(): Boolean = !(isSnapshot() || isAlpha() || isDev())

    override fun compareTo(other: Version) = compareValuesBy(
        this, other,
        { it.major },
        { it.minor },
        { it.patch },
        { it.extra == null }, // False (no extra) sorts above true (has extra)
        { it.extra } // gradle uses lexicographic ordering
    )

    override fun toString(): String {
        return if (extra != null) {
            "$major.$minor.$patch$extra"
        } else "$major.$minor.$patch"
    }

    companion object {
        private const val serialVersionUID = 345435634563L

        private val VERSION_FILE_REGEX = Pattern.compile("^(res-)?(.*).txt$")
        private val VERSION_REGEX = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(-.+)?$")

        private fun checkedMatcher(versionString: String): Matcher {
            val matcher = VERSION_REGEX.matcher(versionString)
            if (!matcher.matches()) {
                throw IllegalArgumentException("Can not parse version: $versionString")
            }
            return matcher
        }

        /**
         * @return Version or null, if a name of the given file doesn't match
         */
        fun parseOrNull(file: File): Version? {
            if (!file.isFile) return null
            val matcher = VERSION_FILE_REGEX.matcher(file.name)
            return if (matcher.matches()) parseOrNull(matcher.group(2)) else null
        }

        /**
         * @return Version or null, if the given string doesn't match
         */
        fun parseOrNull(versionString: String): Version? {
            val matcher = VERSION_REGEX.matcher(versionString)
            return if (matcher.matches()) Version(versionString) else null
        }

        /**
         * Tells whether a version string would refer to a dependency range
         */
        fun isDependencyRange(version: String): Boolean {
            if ((version.startsWith("[") || version.startsWith("(")) &&
                version.contains(",") &&
                (version.endsWith("]") || version.endsWith(")"))
            ) {
                return true
            }
            if (version.endsWith("+")) {
                return true
            }
            return false
        }
    }
}

fun Project.isVersionSet() = project.version is Version

fun Project.version(): Version {
    return if (project.version is Version) {
        project.version as Version
    } else {
        throw IllegalStateException("Tried to use project version for $name that was never set.")
    }
}
