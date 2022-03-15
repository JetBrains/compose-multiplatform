/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.*
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.desktop.application.internal.Arch
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.desktop.application.internal.currentArch
import org.jetbrains.compose.experimental.dsl.DeployTarget
import org.jetbrains.compose.experimental.dsl.UiKitConfiguration
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask
import java.io.File


fun Project.registerSimulatorTasks(
    id: String,
    deploy: DeployTarget.Simulator,
    projectName: String,
    bundleIdPrefix: String,
    taskInstallXcodeGen: TaskProvider<*>,
    configurations: List<UiKitConfiguration>,
) {
    val xcodeProjectDir = getBuildIosDir(id).resolve("$projectName.xcodeproj")
    val deviceName = "device-$id"

    val taskGenerateXcodeProject = configureTaskToGenerateXcodeProject(
        id = id,
        projectName = projectName,
        bundleIdPrefix = bundleIdPrefix,
        taskInstallXcodeGen = taskInstallXcodeGen,
    )

    val taskCreateSimulator = tasks.composeIosTask<AbstractComposeIosTask>("iosSimulatorCreate$id") {
        onlyIf { getSimctlListData().devices.map { it.value }.flatten().none { it.name == deviceName } }
        doFirst {
            val availableRuntimes = getSimctlListData().runtimes.filter { runtime ->
                runtime.supportedDeviceTypes.any { it.identifier == deploy.device.id }
            }
            val runtime = availableRuntimes.firstOrNull() ?: error("device not found is runtimes")
            runExternalTool(
                MacUtils.xcrun,
                listOf("simctl", "create", deviceName, deploy.device.id, runtime.identifier)
            )
        }
    }

    val taskBootSimulator = tasks.composeIosTask<AbstractComposeIosTask>("iosSimulatorBoot$id") {
        onlyIf {
            getSimctlListData().devices.map { it.value }.flatten().any { it.name == deviceName && it.booted.not() }
        }
        dependsOn(taskCreateSimulator)
        doLast {
            val device = getSimctlListData().devices.map { it.value }.flatten().firstOrNull { it.name == deviceName }
                ?: error("device '$deviceName' not found")

            runExternalTool(
                MacUtils.xcrun,
                listOf("simctl", "boot", device.udid)
            )
            runExternalTool(
                MacUtils.open,
                listOf(
                    "-a", "Simulator",
                    "--args", "-CurrentDeviceUDID", device.udid
                )
            )
        }
    }

    val simulatorArch = when (currentArch) {
        Arch.X64 -> "x86_64"
        Arch.Arm64 -> "arm64"
    }

    for (configuration in configurations) {
        val configName = configuration.name
        val iosCompiledAppDir = xcodeProjectDir.resolve(RELATIVE_PRODUCTS_PATH)
                .resolve("$configName-iphonesimulator/${projectName}.app")

        val taskBuild = tasks.composeIosTask<AbstractComposeIosTask>("iosSimulatorBuild$id$configName") {
            dependsOn(taskGenerateXcodeProject)
            doLast {
                // xcrun xcodebuild -showsdks (list all sdk)
                val sdk = SDK_PREFIFX_SIMULATOR + getSimctlListData().runtimes.first().version
                val scheme = projectName // xcrun xcodebuild -list -project . (list all schemes)
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
                            "-configuration", configName,
                            "-derivedDataPath", "build",
                            "-arch", simulatorArch,
                            "-sdk", sdk
                        ),
                        workingDir = xcodeProjectDir
                    )
                }
            }
        }

        val installIosSimulator = tasks.composeIosTask<AbstractComposeIosTask>("iosSimulatorInstall$id$configName") {
            dependsOn(taskBuild, taskBootSimulator)
            doLast {
                val device = getSimctlListData().devices.map { it.value }.flatten()
                    .firstOrNull { it.name == deviceName && it.booted } ?: error("device $deviceName not booted")
                runExternalTool(
                    MacUtils.xcrun,
                    listOf("simctl", "install", device.udid, iosCompiledAppDir.absolutePath)
                )
            }
        }

        tasks.composeIosTask<AbstractComposeIosTask>("iosDeploy$id$configName") {
            dependsOn(installIosSimulator)
            doFirst {
                val device = getSimctlListData().devices.map { it.value }.flatten()
                    .firstOrNull { it.name == deviceName && it.booted } ?: error("device $deviceName not booted")
                val bundleIdentifier = "$bundleIdPrefix.$projectName"
                runExternalTool(
                    MacUtils.xcrun,
                    listOf("simctl", "launch", "--console", device.udid, bundleIdentifier)
                )
            }
        }
    }

}
