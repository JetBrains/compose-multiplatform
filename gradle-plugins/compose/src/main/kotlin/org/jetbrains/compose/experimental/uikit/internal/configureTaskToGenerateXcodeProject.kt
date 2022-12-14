/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask

internal fun Project.configureTaskToGenerateXcodeProject(
    id: String,
    projectName: String,
    bundleIdPrefix: String,
    getTeamId: () -> String? = { null },
    taskInstallXcodeGen: TaskProvider<*>,
): TaskProvider<AbstractComposeIosTask> = tasks.composeIosTask<AbstractComposeIosTask>("iosGenerateXcodeProject$id") {
    dependsOn(taskInstallXcodeGen)
    doLast {
        val commonMainResources = file("src/commonMain/resources").absolutePath
        val uikitMainResources = file("src/uikitMain/resources").absolutePath
        val iosMainResources = file("src/iosMain/resources").absolutePath
        val buildIosDir = getBuildIosDir(id)
        buildIosDir.mkdirs()
        buildIosDir.resolve("project.yml").writeText(
            """
            name: $projectName
            options:
              bundleIdPrefix: $bundleIdPrefix
            settings:
              ${if (getTeamId() != null) "DEVELOPMENT_TEAM: \"${getTeamId()}\"" else ""}
              CODE_SIGN_IDENTITY: "iPhone Developer"
              CODE_SIGN_STYLE: Automatic
              MARKETING_VERSION: "1.0"
              CURRENT_PROJECT_VERSION: "4"
              SDKROOT: iphoneos
            targets:
              $projectName:
                type: application
                platform: iOS
                deploymentTarget: "12.0"
                info:
                  path: plists/Ios/Info.plist
                  properties:
                    UILaunchStoryboardName: $projectName
                settings:
                  LIBRARY_SEARCH_PATHS: "$(inherited)"
                  ENABLE_BITCODE: "YES"
                  ONLY_ACTIVE_ARCH: "NO"
                  VALID_ARCHS: "arm64"
                sources:
                  - path: $commonMainResources
                    optional: true
                    buildPhase: resources
                  - path: $uikitMainResources
                    optional: true
                    buildPhase: resources
                  - path: $iosMainResources
                    optional: true
                    buildPhase: resources
            """.trimIndent()
        )
        runExternalTool(xcodeGenExecutable, emptyList(), workingDir = buildIosDir)
    }
}
