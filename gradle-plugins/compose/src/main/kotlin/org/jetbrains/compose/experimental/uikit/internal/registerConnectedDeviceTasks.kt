/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.*
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.experimental.dsl.DeployTarget
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask
import java.io.File


fun Project.registerConnectedDeviceTasks(
    id: String,
    deploy: DeployTarget.ConnectedDevice,
    buildIosDir: File,
    projectName: String,
    bundleIdPrefix: String
) {
    val xcodeProjectDir = buildIosDir.resolve("$projectName.xcodeproj")
    val iosCompiledAppDir = xcodeProjectDir.resolve("build/Build/Products/Debug-iphoneos/$projectName.app")
    val taskBuild = tasks.composeIosTask<AbstractComposeIosTask>("iosBuildIphoneOs$id") {
        dependsOn(TASK_USE_XCODE_GEN_NAME)
        doLast {
            val sdk = SDK_PREFIX_IPHONEOS + getSimctlListData().runtimes.first().version // xcrun xcodebuild -showsdks
            val scheme = projectName // xcrun xcodebuild -list -project .
            repeat(2) {
                // todo repeat(2) is workaround of error (domain=NSPOSIXErrorDomain, code=22)
                //  The bundle identifier of the application could not be determined
                //  Ensure that the application's Info.plist contains a value for CFBundleIdentifier.
                runExternalTool(
                    MacUtils.xcrun,
                    listOf(
                        "xcodebuild",
                        "-scheme", scheme,
                        "-project", ".",
                        "-configuration", deploy.buildConfiguration,
                        "-derivedDataPath", "build",
                        "-arch", "arm64",
                        "-sdk", sdk
                    ),
                    workingDir = xcodeProjectDir
                )
            }
        }
    }

    val taskDeploy = tasks.composeIosTask<AbstractComposeIosTask>("iosDeploy$id") {
        dependsOn(taskBuild)
        doLast {
            runExternalTool(
                MacUtils.iosDeploy,
                listOf(
                    "--debug",
                    "--bundle", iosCompiledAppDir.absolutePath,
                    "--no-wifi"
                )
            )
        }
    }


}
