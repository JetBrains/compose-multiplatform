/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.gradle

import org.gradle.api.provider.SetProperty
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * Configures HTML lowering for Kotlin compilations. Enabled by default for JVM targets.
 *
 * ```kotlin
 * htmlLowering {
 *     enabledTargets.set(setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm))
 * }
 * ```
 */
abstract class HtmlLoweringExtension {
    /**
     * Kotlin platform types for which lowering is enabled. Defaults to [KotlinPlatformType.jvm].
     * Set to an empty set to disable lowering for all targets.
     * Enabling an unsupported compiler platform reports a warning and skips lowering.
     */
    abstract val enabledTargets: SetProperty<KotlinPlatformType>
}
