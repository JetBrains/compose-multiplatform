/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.compose.internal.mppExtOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

abstract class WebExtension : ExtensionAware {
    private var requestedTargets: Set<KotlinJsIrTarget>? = null
    private var targetsToConfigure: Set<KotlinJsIrTarget>? = null

    // public api
    @Suppress("unused")
    fun targets(vararg targets: KotlinTarget) {
        check(requestedTargets == null) {
            "compose.web.targets() was already set!"
        }

        val jsIrTargets = linkedSetOf<KotlinJsIrTarget>()
        for (target in targets) {
            check(target is KotlinJsIrTarget) {
                """|'${target.name}' is not a JS(IR) target:
                |* add `kotlin.js.compiler=ir` to gradle properties;
                |* define target as `kotlin { js(IR) { ... } }`
            """.trimMargin()
            }
            jsIrTargets.add(target)
        }
        requestedTargets = jsIrTargets
    }

    internal fun targetsToConfigure(project: Project): Set<KotlinJsIrTarget> {
        targetsToConfigure =
            targetsToConfigure
                ?: requestedTargets
                ?: defaultJsTargetsToConfigure(project)

        return targetsToConfigure!!
    }

    private fun defaultJsTargetsToConfigure(project: Project): Set<KotlinJsIrTarget> {
        val mppTargets = project.mppExtOrNull?.targets?.asMap?.values ?: emptySet()
        val jsIRTargets = mppTargets.filterIsInstanceTo(LinkedHashSet<KotlinJsIrTarget>())

        return if (jsIRTargets.size > 1) {
            project.logger.error(
                "w: Default configuration for Compose Web is disabled: " +
                        "multiple Kotlin JS IR targets are defined. " +
                        "Specify Compose Web Kotlin targets by using `compose.web.targets()`"
            )
            emptySet()
        } else jsIRTargets
    }
}