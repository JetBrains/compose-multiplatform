/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.Project
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask
import java.io.File

internal fun Project.configureUseXcodeGenTask(
    projectName: String,
    bundleIdPrefix: String,
    xcodeGenExecutable: File,
    teamId: String
) {
    tasks.composeIosTask<AbstractComposeIosTask>(TASK_USE_XCODE_GEN_NAME) {
        dependsOn(TASK_INSTALL_XCODE_GEN)
        doLast {
            buildIosDir.mkdirs()
            buildIosDir.resolve("project.yml").writeText(
                """
                name: $projectName
                options:
                  bundleIdPrefix: $bundleIdPrefix
                settings:
                  DEVELOPMENT_TEAM: "$teamId"
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
                    prebuildScripts:
                      - script: cd "${rootDir.absolutePath}" && ./gradlew -i -p . packComposeUikitApplicationForXCode
                        name: GradleCompile
                    info:
                      path: plists/Ios/Info.plist
                      properties:
                        UILaunchStoryboardName: ""
                        method: "development"
                    sources:
                      - "../../src/"
                    settings:
                      LIBRARY_SEARCH_PATHS: "$(inherited)"
                      ENABLE_BITCODE: "YES"
                      ONLY_ACTIVE_ARCH: "NO"
                      VALID_ARCHS: "arm64"
            """.trimIndent()
            )
            runExternalTool(xcodeGenExecutable, emptyList(), workingDir = buildIosDir)
        }
    }
}
