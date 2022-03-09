/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.*
import org.gradle.api.tasks.TaskContainer
import org.jetbrains.compose.experimental.dsl.DeployTarget
import org.jetbrains.compose.experimental.dsl.ExperimentalUiKitApplication

const val SDK_PREFIFX_SIMULATOR = "iphonesimulator"
const val SDK_PREFIX_IPHONEOS = "iphoneos"

fun Project.getBuildIosDir(id: String) = buildDir.resolve("ios").resolve(id)

internal fun Project.configureIosDeployTasks(application: ExperimentalUiKitApplication) {
    val projectName = application.projectName
    val bundleIdPrefix = application.bundleIdPrefix

    configureInstallXcodeGenTask()
    configureInstallIosDeployTask()

    application.deployConfigurations.deployTargets.forEach { target ->
        val id = target.id // .replaceFirstChar { it.uppercase() } // todo upperCase first char? ./gradlew iosDeployId
        when (target.deploy) {
            is DeployTarget.Simulator -> {
                registerSimulatorTasks(
                    id = id,
                    deploy = target.deploy,
                    projectName = projectName,
                    bundleIdPrefix = bundleIdPrefix,
                )
            }
            is DeployTarget.LocalFile -> {
                registerLocalFileTasks(
                    id = id,
                    deploy = target.deploy,
                    projectName = projectName,
                    bundleIdPrefix = bundleIdPrefix,
                )
            }
            is DeployTarget.ConnectedDevice -> {
                registerConnectedDeviceTasks(
                    id = id,
                    deploy = target.deploy,
                    projectName = projectName,
                    bundleIdPrefix = bundleIdPrefix,
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
