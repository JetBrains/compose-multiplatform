/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.experimental.dsl.DeployTarget
import org.jetbrains.compose.experimental.dsl.UiKitConfiguration
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask
import org.jetbrains.compose.experimental.uikit.tasks.ExperimentalPackComposeApplicationForXCodeTask
import org.jetbrains.compose.internal.getLocalProperty
import org.jetbrains.compose.internal.localPropertiesFile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.registerConnectedDeviceTasks(
    mppExt: KotlinMultiplatformExtension,
    id: String,
    deploy: DeployTarget.ConnectedDevice,
    projectName: String,
    bundleIdPrefix: String,
    taskInstallXcodeGen: TaskProvider<*>,
    taskInstallIosDeploy: TaskProvider<*>,
    configurations: List<UiKitConfiguration>,
) {
    val xcodeProjectDir = getBuildIosDir(id).resolve("$projectName.xcodeproj")
    val taskGenerateXcodeProject = configureTaskToGenerateXcodeProject(
        id = id,
        projectName = projectName,
        bundleIdPrefix = bundleIdPrefix,
        getTeamId = {
            deploy.teamId ?: getLocalProperty(TEAM_ID_PROPERTY_KEY)
            ?: error(
                buildString {
                    appendLine("In local.properties (${localPropertiesFile.absolutePath})")
                    appendLine("Add property")
                    appendLine("$TEAM_ID_PROPERTY_KEY=***")
                    appendLine("Or set teamId in deploy with id: $id")
                }
            )
        },
        taskInstallXcodeGen = taskInstallXcodeGen,
    )

    for (configuration in configurations) {
        val configName = configuration.name
        val targetBuildPath = xcodeProjectDir.resolve(RELATIVE_PRODUCTS_PATH)
            .resolve("$configName-iphoneos")
        val iosCompiledAppDir = targetBuildPath.resolve("${projectName}.app")

        val taskPackageUiKitAppForXcode = configurePackComposeUiKitApplicationForXCodeTask(
            mppExt = mppExt,
            id = id,
            configName = configName,
            projectName = projectName,
            targetBuildPath = targetBuildPath,
            targetType = ExperimentalPackComposeApplicationForXCodeTask.UikitTarget.Arm64,
        )

        val taskBuild = tasks.composeIosTask<AbstractComposeIosTask>("iosBuildIphoneOs$id$configName") {
            dependsOn(taskGenerateXcodeProject)
            dependsOn(taskPackageUiKitAppForXcode)
            doLast {
                // xcrun xcodebuild -showsdks (list all sdk)
                val sdk = SDK_PREFIX_IPHONEOS + getSimctlListData().runtimes.first().version
                val scheme = projectName // xcrun xcodebuild -list -project . (list all schemes)

                runExternalTool(
                    MacUtils.xcrun,
                    listOf(
                        "xcodebuild",
                        "-scheme", scheme,
                        "-project", ".",
                        "-configuration", configName,
                        "-derivedDataPath", BUILD_DIR_NAME,
                        "-arch", "arm64",
                        "-sdk", sdk,
                        "-allowProvisioningUpdates",
                        "-allowProvisioningDeviceRegistration",
                    ),
                    workingDir = xcodeProjectDir
                )
            }
        }

        val taskDeploy = tasks.composeIosTask<AbstractComposeIosTask>("iosDeploy$id$configName") {
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

}
