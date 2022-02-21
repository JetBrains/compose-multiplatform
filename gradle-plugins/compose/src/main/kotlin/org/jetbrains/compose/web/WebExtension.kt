/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.internal.kotlinJsExtOrNull
import org.jetbrains.compose.internal.mppExtOrNull
import org.jetbrains.compose.web.dsl.WebApplication
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import javax.inject.Inject

abstract class WebExtension : ExtensionAware {
    @get:Inject
    internal abstract val objects: ObjectFactory

    internal var _isApplicationInitialized = false
        private set

    val application: WebApplication by lazy {
        _isApplicationInitialized = true
        objects.newInstance(WebApplication::class.java, "main")
    }

    fun application(fn: Action<WebApplication>) {
        fn.execute(application)
    }

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
        val mppExt = project.mppExtOrNull

        if (mppExt != null) {
            val mppTargets = mppExt.targets.asMap.values
            val jsIRTargets = mppTargets.filterIsInstanceTo(LinkedHashSet<KotlinJsIrTarget>())

            return if (jsIRTargets.size > 1) {
                project.logger.error(
                    "w: Default configuration for Compose for Web is disabled: " +
                            "multiple Kotlin JS IR targets are defined. " +
                            "Specify Compose for Web Kotlin targets by using `compose.web.targets()`"
                )
                emptySet()
            } else jsIRTargets
        }

        val jsExt = project.kotlinJsExtOrNull
        if (jsExt != null) {
            val target = jsExt.target
            return if (target is KotlinJsIrTarget) {
                setOf(target)
            } else {
                project.logger.error(
                    "w: Default configuration for Compose for Web is disabled: " +
                            "Compose for Web does not support legacy (non-IR) JS targets"
                )
                emptySet()
            }
        }

        return emptySet()
    }
}