/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.utils.property
import javax.inject.Inject

/**
 * Configuration of the compatibility check for Compose Multiplatform runtime dependencies.
 *
 * The check warns when resolved versions of Compose Multiplatform libraries don't match the Gradle plugin version,
 * or when Skiko is resolved to a version that is incompatible with the requested one.
 */
abstract class DependencyCompatibilityExtension @Inject constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
) {
    /**
     * Whether the compatibility check runs.
     *
     * Default is `true`, or `false` when `org.jetbrains.compose.library.compatibility.check.disable=true`
     * is set in `gradle.properties`.
     */
    val enabled: Property<Boolean> = objects.property<Boolean>().convention(
        ComposeProperties.disableLibraryCompatibilityCheck(providers).map { disabled -> !disabled }
    )

    internal val excludedModules: SetProperty<String> = objects.setProperty(String::class.java)

    /**
     * Excludes a library from the compatibility check.
     *
     * The notation is a whole group (`"com.example"`) or a single module (`"com.example:library"`),
     * without a version.
     *
     * A version mismatch is not reported when the exclusion matches the library with the mismatched version
     * or the library that depends on it:
     * ```
     * compose {
     *     dependencyCompatibility {
     *         // keep the check for other dependencies, but ignore mismatches introduced by com.example libraries
     *         exclude("com.example")
     *         // ignore every Skiko version mismatch
     *         exclude("org.jetbrains.skiko:skiko")
     *     }
     * }
     * ```
     */
    fun exclude(notation: String) {
        val parts = notation.split(":")
        require(parts.size <= 2 && parts.none { it.isBlank() }) {
            "Invalid library notation '$notation'. Expected 'group' or 'group:name' without a version, " +
                    "for example 'com.example' or 'com.example:library'."
        }
        excludedModules.add(notation)
    }
}
