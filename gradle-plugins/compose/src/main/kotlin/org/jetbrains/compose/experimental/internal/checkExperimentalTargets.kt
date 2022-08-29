/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.internal

import org.gradle.api.Project
import org.jetbrains.compose.experimental.dsl.ExperimentalExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

private const val SKIKO_ARTIFACT_PREFIX = "org.jetbrains.skiko:skiko"

private class TargetType(
    val id: String,
    val presets: List<String>,
    /**
     * @return true if target is experimental
     */
    val experimentalCondition: (ExperimentalExtension) -> Boolean
)

private val TargetType.gradlePropertyName get() = "org.jetbrains.compose.experimental.$id.enabled"

private val EXPERIMENTAL_TARGETS: Set<TargetType> = setOf(
    TargetType("uikit", presets = listOf("iosSimulatorArm64", "iosArm64", "iosX64")) { true },
    TargetType("macos", presets = listOf("macosX64", "macosArm64")) { true },
    TargetType("jscanvas", presets = listOf("jsIr", "js")) { experimentalExtension ->
        experimentalExtension.web._isApplicationInitialized
    },
)

private sealed interface CheckResult {
    object Success : CheckResult
    class Fail(val target: TargetType) : CheckResult
}

internal fun Project.checkExperimentalTargetsWithSkikoIsEnabled(
    experimentalExtension: ExperimentalExtension
) = afterEvaluate {
    val mppExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return@afterEvaluate
    val failedResults = mppExt.targets.map { checkTarget(it, experimentalExtension) }
        .filterIsInstance<CheckResult.Fail>()
        .distinctBy { it.target }

    if (failedResults.isNotEmpty()) {
        val ids = failedResults.map { it.target.id }
        val msg = buildString {
            appendLine("ERROR: Compose targets '$ids' are experimental and may have bugs!")
            appendLine("But, if you still want to use them, add to gradle.properties:")
            failedResults.forEach {
                appendLine("${it.target.gradlePropertyName}=true")
            }
        }

        project.logger.error(msg)
        error(msg)
    }
}

private fun Project.checkTarget(target: KotlinTarget, experimentalExtension: ExperimentalExtension): CheckResult {
    val presetName = target.preset?.name ?: return CheckResult.Success

    val targetType = EXPERIMENTAL_TARGETS.firstOrNull {
        it.presets.contains(presetName)
    } ?: return CheckResult.Success

    val targetConfigurationNames = target.compilations.map { compilation ->
        compilation.compileDependencyConfigurationName
    }

    configurations.forEach { configuration ->
        if (configuration.isCanBeResolved && configuration.name in targetConfigurationNames) {
            val isExperimental = targetType.experimentalCondition(experimentalExtension)
            if (isExperimental) {
                if (project.findProperty(targetType.gradlePropertyName) != "true") {
                    return CheckResult.Fail(targetType)
                }
            }
        }
    }
    return CheckResult.Success
}
