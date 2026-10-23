/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.gradle

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * Configures HTML lowering for Kotlin compilations. Enabled by default for JVM targets.
 *
 * ```kotlin
 * htmlLowering {
 *     enabled.set(false)
 *     targets.put("jvm", true)
 * }
 * ```
 */
abstract class HtmlLoweringExtension {
    /**
     * Enables or disables lowering for all targets unless [targets] overrides it.
     * If unset, only JVM targets are enabled. Android JVM targets require an explicit opt-in.
     * Enabling an unsupported compiler platform reports a warning and skips lowering.
     */
    abstract val enabled: Property<Boolean>

    /**
     * Overrides [enabled] by Kotlin target name, such as `jvm`.
     * Enabling a target still requires a supported compiler platform.
     */
    abstract val targets: MapProperty<String, Boolean>
}
