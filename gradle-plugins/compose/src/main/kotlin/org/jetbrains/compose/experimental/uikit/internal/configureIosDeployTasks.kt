/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.*
import org.gradle.api.tasks.TaskContainer
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.desktop.application.internal.UnixUtils
import org.jetbrains.compose.experimental.dsl.DeployTarget
import org.jetbrains.compose.experimental.dsl.ExperimentalUiKitApplication
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask

const val XCODE_GEN_GIT = "https://github.com/yonaskolb/XcodeGen.git"
const val XCODE_GEN_TAG = "2.26.0"
const val TASK_INSTALL_XCODE_GEN_NAME = "iosInstallXcodeGen"
const val TASK_USE_XCODE_GEN_NAME = "iosUseXCodeGen"
const val IOS_DEPLOY_GIT = "https://github.com/ios-control/ios-deploy.git"
const val IOS_DEPLOY_TAG = "1.11.4"
const val TASK_INSTALL_IOS_DEPLOY_NAME = "iosInstallIosDeploy"
const val SDK_PREFIFX_SIMULATOR = "iphonesimulator"
const val SDK_PREFIX_IPHONEOS = "iphoneos"

internal fun Project.configureIosDeployTasks(application: ExperimentalUiKitApplication) {
    val projectName = application.projectName
    val bundleIdPrefix = application.bundleIdPrefix
    val xcodeGenSrc = rootProject.buildDir.resolve("xcodegen-$XCODE_GEN_TAG-src")
    val iosDeploySrc = rootProject.buildDir.resolve("ios-deploy-$IOS_DEPLOY_TAG-src")
    val xcodeGenExecutable = xcodeGenSrc.resolve(".build/apple/Products/Release/xcodegen")
    val iosDeployExecutable = iosDeploySrc.resolve("build/Release/ios-deploy")
    val buildIosDir = buildDir.resolve("ios")

    tasks.composeIosTask<AbstractComposeIosTask>(TASK_INSTALL_XCODE_GEN_NAME) {
        onlyIf { !xcodeGenExecutable.exists() }
        doLast {
            xcodeGenSrc.deleteRecursively()
            runExternalTool(
                UnixUtils.git,
                listOf(
                    "clone",
                    "--depth", "1",
                    "--branch", XCODE_GEN_TAG,
                    XCODE_GEN_GIT,
                    xcodeGenSrc.absolutePath
                )
            )
            runExternalTool(
                MacUtils.make,
                listOf("build"),
                workingDir = xcodeGenSrc
            )
        }
    }

    tasks.composeIosTask<AbstractComposeIosTask>(TASK_INSTALL_IOS_DEPLOY_NAME) {
        onlyIf { !iosDeployExecutable.exists() }
        doLast {
            iosDeploySrc.deleteRecursively()
            runExternalTool(
                UnixUtils.git,
                listOf(
                    "clone",
                    "--depth", "1",
                    "--branch", IOS_DEPLOY_TAG,
                    IOS_DEPLOY_GIT,
                    iosDeploySrc.absolutePath
                )
            )
            runExternalTool(
                MacUtils.xcodeBuild,
                listOf("-target", "ios-deploy"),
                workingDir = iosDeploySrc
            )
        }
    }

    configureUseXcodeGenTask(
        buildIosDir = buildIosDir,
        projectName = projectName,
        bundleIdPrefix = bundleIdPrefix,
        xcodeGenExecutable = xcodeGenExecutable,
        teamId = application.teamId
    )

    application.deployConfigurations.deployTargets.forEach { target ->
        val id = target.id // .replaceFirstChar { it.uppercase() } // todo upperCase first char? ./gradlew iosDeployId
        when (target.deploy) {
            is DeployTarget.Simulator -> {
                registerSimulatorTasks(
                    id = id,
                    deploy = target.deploy,
                    buildIosDir = buildIosDir,
                    projectName = projectName,
                    bundleIdPrefix = bundleIdPrefix
                )
            }
            is DeployTarget.LocalFile -> {
                registerLocalFileTasks(
                    id = id,
                    deploy = target.deploy,
                    buildIosDir = buildIosDir,
                    projectName = projectName,
                    bundleIdPrefix = bundleIdPrefix
                )
            }
            is DeployTarget.ConnectedDevice -> {
                registerConnectedDeviceTasks(
                    id = id,
                    deploy = target.deploy,
                    buildIosDir = buildIosDir,
                    projectName = projectName,
                    iosDeployExecutable = iosDeployExecutable
                )
            }
        }
    }
}

inline fun <reified T : Task> TaskContainer.composeIosTask(
    name: String,
    args: List<Any> = emptyList(),
    noinline configureFn: T.() -> Unit = {}
) = register(name, T::class.java, *args.toTypedArray()).apply {
    configure {
        it.group = "Compose iOS"
        it.configureFn()
    }
}
