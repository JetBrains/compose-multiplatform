/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.Project
import org.jetbrains.compose.experimental.uikit.tasks.ExperimentalPackComposeApplicationForXCodeTask
import org.jetbrains.compose.internal.fileToDir
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.io.File

internal fun Project.configurePackComposeUiKitApplicationForXCodeTask(
    mppExt: KotlinMultiplatformExtension,
    id: String,
    configName: String,
    projectName: String,
    targetBuildPath: File,
    targetType: ExperimentalPackComposeApplicationForXCodeTask.UikitTarget,
) = tasks.register(
    "packComposeUikitApplicationForXCode$id$configName",
    ExperimentalPackComposeApplicationForXCodeTask::class.java
) { packTask ->
    val buildType = project.provider { configName }.map {
        if (it.equals("release", ignoreCase = true)) NativeBuildType.RELEASE
        else NativeBuildType.DEBUG
    }.orElse(NativeBuildType.DEBUG)
    val target = mppExt.targets.getByName(targetType.targetName) as KotlinNativeTarget
    val kotlinBinary = target.binaries.getExecutable(buildType.get())
    val targetBuildDir = project.provider { targetBuildPath }.fileToDir(project)
    val executablePath = "${projectName}.app/${projectName}"

    packTask.targetType.set(targetType)
    packTask.buildType.set(buildType)
    packTask.dependsOn(kotlinBinary.linkTask)
    packTask.kotlinBinary.set(kotlinBinary.outputFile)
    packTask.destinationDir.set(targetBuildDir)
    packTask.executablePath.set(executablePath)
}
