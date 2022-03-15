/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.*
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.experimental.dsl.DeployTarget
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask
import org.jetbrains.compose.internal.getLocalProperty
import org.jetbrains.compose.internal.localPropertiesFile

fun Project.registerConnectedDeviceTasks(
    id: String,
    deploy: DeployTarget.ConnectedDevice,
    projectName: String,
    bundleIdPrefix: String,
    taskInstallXcodeGen: TaskProvider<*>,
    taskInstallIosDeploy: TaskProvider<*>,
) {
    val xcodeProjectDir = getBuildIosDir(id).resolve("$projectName.xcodeproj")
    val iosCompiledAppDir =
        xcodeProjectDir.resolve("build/Build/Products/${deploy.buildConfiguration}-iphoneos/$projectName.app")
    val taskGenerateXcodeProject = configureTaskToGenerateXcodeProject(
        id = id,
        projectName = projectName,
        bundleIdPrefix = bundleIdPrefix,
        teamId = deploy.teamId ?: getLocalProperty(TEAM_ID_PROPERTY_KEY)
        ?: error(
            buildString {
                appendLine("In local.properties (${localPropertiesFile.absolutePath})")
                appendLine("Add property")
                appendLine("$TEAM_ID_PROPERTY_KEY=***")
                appendLine("Or set teamId in deploy with id: $id")
            }
        ),
        taskInstallXcodeGen = taskInstallXcodeGen,
    )
    val taskBuild = tasks.composeIosTask<AbstractComposeIosTask>("iosBuildIphoneOs$id") {
        dependsOn(taskGenerateXcodeProject)
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
                        "-sdk", sdk,
                        "-allowProvisioningUpdates",
                        "-allowProvisioningDeviceRegistration",
                    ),
                    workingDir = xcodeProjectDir
                )
            }
        }
    }

    val taskDeploy = tasks.composeIosTask<AbstractComposeIosTask>("iosDeploy$id") {
        dependsOn(taskInstallIosDeploy)
        dependsOn(taskBuild)
        doLast {
            runExternalTool(
                iosDeployExecutable,
                listOf(
                    "--debug",
                    "--bundle", iosCompiledAppDir.absolutePath,
                )
            )
        }
    }


}
