/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask
import org.jetbrains.compose.internal.getLocalProperty

internal fun Project.createOpenXcodeTask(
    projectName: String,
    bundleIdPrefix: String,
    taskInstallXcodeGen: TaskProvider<*>
) {
    val id = "Xcode"
    val xcodeProjectDir = getBuildIosDir(id).resolve("$projectName.xcodeproj")
    val taskGenerateCommonXcodeProject = configureTaskToGenerateXcodeProject(
        id = id,
        projectName = projectName,
        bundleIdPrefix = bundleIdPrefix,
        taskInstallXcodeGen = taskInstallXcodeGen,
        getTeamId = { getLocalProperty(TEAM_ID_PROPERTY_KEY) }
    )
    tasks.composeIosTask<AbstractComposeIosTask>("iosOpenXcode") {
        dependsOn(taskGenerateCommonXcodeProject)
        doLast {
            runExternalTool(
                MacUtils.open,
                listOf(xcodeProjectDir.absolutePath)
            )
        }
    }
}
