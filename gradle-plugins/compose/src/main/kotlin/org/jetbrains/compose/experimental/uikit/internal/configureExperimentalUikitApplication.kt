/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.internal.OS
import org.jetbrains.compose.desktop.application.internal.currentOS
import org.jetbrains.compose.experimental.dsl.ExperimentalUiKitApplication
import org.jetbrains.compose.experimental.uikit.tasks.ExperimentalPackComposeApplicationForXCodeTask
import org.jetbrains.compose.experimental.uikit.tasks.ExperimentalPackComposeApplicationForXCodeTask.UikitTarget
import org.jetbrains.compose.internal.toDir
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

internal fun Project.configureExperimentalUikitApplication(
    mppExt: KotlinMultiplatformExtension,
    application: ExperimentalUiKitApplication
) {
    if (currentOS != OS.MacOS) return

    tasks.register(
        "packComposeUikitApplicationForXCode",
        ExperimentalPackComposeApplicationForXCodeTask::class.java
    ) { packTask ->
        val targetType = project.providers.environmentVariable("SDK_NAME").map {
            if (it.startsWith("iphoneos"))
                UikitTarget.Arm64
            else UikitTarget.X64
        }.orElse(UikitTarget.X64)
        val buildType = project.providers.environmentVariable("CONFIGURATION").map {
            if (it.equals("release", ignoreCase = true)) NativeBuildType.RELEASE
            else NativeBuildType.DEBUG
        }.orElse(NativeBuildType.DEBUG)
        val target = mppExt.targets.getByName(targetType.get().targetName) as KotlinNativeTarget
        val kotlinBinary = target.binaries.getExecutable(buildType.get())
        val targetBuildDir = project.providers.environmentVariable("TARGET_BUILD_DIR").toDir(project)
        val executablePath = project.providers.environmentVariable("EXECUTABLE_PATH")

        packTask.targetType.set(targetType)
        packTask.buildType.set(buildType)
        packTask.dependsOn(kotlinBinary.linkTask)
        packTask.kotlinBinary.set(kotlinBinary.outputFile)
        packTask.destinationDir.set(targetBuildDir)
        packTask.executablePath.set(executablePath)
    }

    configureIosDeployTasks(application)
}
