/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.experimental.dsl.DeployTarget
import org.jetbrains.compose.experimental.dsl.ExperimentalUiKitApplication
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

const val SDK_PREFIFX_SIMULATOR = "iphonesimulator"
const val SDK_PREFIX_IPHONEOS = "iphoneos"
const val TEAM_ID_PROPERTY_KEY = "compose.ios.teamId"
const val BUILD_DIR_NAME = "build"
const val RELATIVE_PRODUCTS_PATH = "$BUILD_DIR_NAME/Build/Products"

fun Project.getBuildIosDir(id: String) = buildDir.resolve("ios").resolve(id)

internal fun Project.configureIosDeployTasks(
    mppExt: KotlinMultiplatformExtension,
    application: ExperimentalUiKitApplication,
) {
    val projectName = application.projectName
    val bundleIdPrefix = application.bundleIdPrefix

    val taskInstallXcodeGen: TaskProvider<*> = configureInstallXcodeGenTask()
    val taskInstallIosDeploy: TaskProvider<*> = configureInstallIosDeployTask()

    application.deployConfigurations.deployTargets.forEach { target ->
        val id = target.id // .replaceFirstChar { it.uppercase() }
        when (target.deploy) {
            is DeployTarget.Simulator -> {
                registerSimulatorTasks(
                    mppExt = mppExt,
                    id = id,
                    deploy = target.deploy,
                    projectName = projectName,
                    bundleIdPrefix = bundleIdPrefix,
                    taskInstallXcodeGen = taskInstallXcodeGen,
                    configurations = application.configurations,
                )
            }
            is DeployTarget.LocalFile -> {
                TODO("DeployTarget.LocalFile not implemented")
            }
            is DeployTarget.ConnectedDevice -> {
                registerConnectedDeviceTasks(
                    mppExt = mppExt,
                    id = id,
                    deploy = target.deploy,
                    projectName = projectName,
                    bundleIdPrefix = bundleIdPrefix,
                    taskInstallXcodeGen = taskInstallXcodeGen,
                    taskInstallIosDeploy = taskInstallIosDeploy,
                    configurations = application.configurations,
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
