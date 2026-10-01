/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.jetbrains.compose.IosExtension
import org.jetbrains.compose.IosRenderingBackend
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

private const val skikoGroup = "org.jetbrains.skiko"
private const val skikoGaneshModule = "skiko-ganesh-gpu-provider"
private const val skikoGraphiteModule = "skiko-graphite-gpu-provider"

internal fun Project.configureIos(iosExtension: IosExtension) {
    plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
        mppExt.targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
            if (target.konanTarget.family != Family.IOS) return@configureEach

            target.compilations.configureEach { compilation ->
                project.configurations.named(compilation.compileDependencyConfigurationName).configure { configuration ->
                    configuration.resolutionStrategy.dependencySubstitution.all { details ->
                        val requestedModule = details.requested as? ModuleComponentSelector ?: return@all
                        if (
                            iosExtension.renderingBackend.get() == IosRenderingBackend.Graphite &&
                            requestedModule.group == skikoGroup &&
                            requestedModule.module == skikoGaneshModule
                        ) {
                            details.useTarget(
                                "$skikoGroup:$skikoGraphiteModule:${requestedModule.version}",
                                "Graphite was selected as the iOS rendering backend",
                            )
                        }
                    }
                }
            }
        }
    }
}