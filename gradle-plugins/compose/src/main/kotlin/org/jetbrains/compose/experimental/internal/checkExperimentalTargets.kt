/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.internal

import org.gradle.api.Project
import org.jetbrains.compose.internal.utils.findLocalOrGlobalProperty
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

internal fun Project.configureExperimentalTargetsFlagsCheck(mppExt: KotlinMultiplatformExtension) {
    gradle.taskGraph.whenReady {
        checkExperimentalTargetsWithSkikoIsEnabled(project, mppExt)
    }
}

private const val SKIKO_ARTIFACT_PREFIX = "org.jetbrains.skiko:skiko"

private class TargetType(
    val id: String,
    val presets: List<String>
)

private val TargetType.gradlePropertyName get() = "org.jetbrains.compose.experimental.$id.enabled"

private val EXPERIMENTAL_TARGETS: Set<TargetType> = setOf(
    TargetType("macos", presets = listOf("macosX64", "macosArm64")),
    TargetType("jscanvas", presets = listOf("jsIr", "js")),
)

private sealed interface CheckResult {
    object Success : CheckResult
    class Fail(val target: TargetType) : CheckResult
}

private fun checkExperimentalTargetsWithSkikoIsEnabled(
    project: Project,
    mppExt: KotlinMultiplatformExtension,
) {
    val failedResults = mppExt.targets.map { checkTarget(project, it) }
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

private fun checkTarget(project: Project, target: KotlinTarget): CheckResult {
    val presetName = target.preset?.name ?: return CheckResult.Success

    val targetType = EXPERIMENTAL_TARGETS.firstOrNull {
        it.presets.contains(presetName)
    } ?: return CheckResult.Success

    val targetConfigurationNames = target.compilations.map { compilation ->
        compilation.compileDependencyConfigurationName
    }

    project.configurations.forEach { configuration ->
        if (configuration.isCanBeResolved && configuration.name in targetConfigurationNames) {
            val containsSkikoArtifact = configuration.resolvedConfiguration.resolvedArtifacts.any {
                it.id.displayName.contains(SKIKO_ARTIFACT_PREFIX)
            }
            if (containsSkikoArtifact) {
                val targetIsDisabled = project.findLocalOrGlobalProperty(targetType.gradlePropertyName).map { it != "true" }
                if (targetIsDisabled.get()) {
                    return CheckResult.Fail(targetType)
                }
            }
        }
    }
    return CheckResult.Success
}
