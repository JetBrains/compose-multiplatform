/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

private class Target(
    val gradlePropertySuffix: String,
    val presets: List<String>
)

private val EXPERIMENTAL_TARGETS: Set<Target> = setOf(
    Target("uikit", presets = listOf("iosSimulatorArm64", "iosArm64", "iosX64")),
    Target("macos", presets = listOf("macosX64", "macosArm64")),
    Target("skikojs", presets = listOf("jsIr", "js")),
)

private const val SKIKO_ARTIFACT_PREFIX = "org.jetbrains.skiko:skiko"

internal fun Project.checkExperimentalTargetsWithSkikoIsEnabled() = project.afterEvaluate {
    val mppExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return@afterEvaluate
    mppExt.targets.forEach {
        checkTarget(it)
    }
}

private fun Project.checkTarget(target: KotlinTarget) {
    val presetName = target.preset?.name ?: return

    val gradlePropertySuffix = EXPERIMENTAL_TARGETS.firstOrNull {
        it.presets.contains(presetName)
    }?.gradlePropertySuffix ?: return

    val targetConfigurationNames = target.compilations.map { compilation ->
        compilation.compileDependencyConfigurationName
    }

    configurations.forEach { configuration ->
        if (configuration.isCanBeResolved && configuration.name in targetConfigurationNames) {
            val containsSkikoArtifact = configuration.resolvedConfiguration.resolvedArtifacts.any {
                it.id.displayName.contains(SKIKO_ARTIFACT_PREFIX)
            }
            if (containsSkikoArtifact) {
                val gradlePropertyName = "org.jetbrains.compose.experimental.$gradlePropertySuffix.enabled"
                if (project.findProperty(gradlePropertyName) != "true") {
                    val msg = """
                            ERROR: Compose target '${target.name}' is experimental and may have bugs!
                            But, if you still want to use it, add to gradle.properties: $gradlePropertyName=true
                        """.trimIndent()
                    project.logger.error(msg)
                    error(msg)
                }
            }
        }
    }
}
