/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.desktop.application.dsl.NativeApplication
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractNativeMacApplicationPackageAppDirTask
import org.jetbrains.compose.desktop.application.tasks.AbstractNativeMacApplicationPackageDmgTask
import org.jetbrains.compose.desktop.application.tasks.AbstractNativeMacApplicationPackageTask
import org.jetbrains.compose.desktop.tasks.AbstractUnpackDefaultComposeApplicationResourcesTask
import org.jetbrains.compose.internal.joinLowerCamelCase
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import java.util.*

internal fun configureNativeApplication(
    project: Project,
    app: NativeApplication,
    unpackDefaultResources: TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>
) {
    if (currentOS != OS.MacOS) return

    for (target in app._targets) {
        configureNativeApplication(project, app, target, unpackDefaultResources)
    }
}

private fun configureNativeApplication(
    project: Project,
    app: NativeApplication,
    target: KotlinNativeTarget,
    unpackDefaultResources: TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>
) {
    for (binary in target.binaries) {
        if (binary.outputKind == NativeOutputKind.EXECUTABLE) {
            configureNativeApplication(project, app, binary, unpackDefaultResources)
        }
    }
}

private fun configureNativeApplication(
    project: Project,
    app: NativeApplication,
    binary: NativeBinary,
    unpackDefaultResources: TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>
) {
    val createDistributable = project.tasks.composeDesktopNativeTask<AbstractNativeMacApplicationPackageAppDirTask>(
        desktopNativeTaskName("createDistributableNative", binary)
    ) {
        configureNativePackageTask(app, binary, TargetFormat.AppImage)

        dependsOn(unpackDefaultResources)
        val macIcon = app.distributions.macOS.iconFile
        val defaultIcon = unpackDefaultResources.flatMap { it.resources.macIcon }
        iconFile.set(macIcon.orElse(defaultIcon))

        dependsOn(binary.linkTaskProvider)
        executable.set(project.layout.file(binary.linkTaskProvider.map { it.binary.outputFile }))
        appCategory.set(project.provider { app.distributions.macOS.appCategory ?: "Unknown" })
        copyright.set(project.provider {
            app.distributions.copyright ?: "Copyright (C) ${Calendar.getInstance().get(Calendar.YEAR)}"
        })
    }

    if (TargetFormat.Dmg in app.distributions.targetFormats) {
        val packageDmg = project.tasks.composeDesktopNativeTask<AbstractNativeMacApplicationPackageDmgTask>(
            desktopNativeTaskName("packageDmgNative", binary)
        ) {
            configureNativePackageTask(app, binary, TargetFormat.Dmg)

            dependsOn(createDistributable)
            appDir.set(createDistributable.flatMap { it.destinationDir })

            installDir.set(project.provider {
                app.distributions.macOS.installationPath ?: "/Applications"
            })
        }
    }
}

private fun AbstractNativeMacApplicationPackageTask.configureNativePackageTask(
    app: NativeApplication,
    binary: NativeBinary,
    format: TargetFormat
) {
    packageName.set(project.provider {
        app.distributions.macOS.packageName
            ?: app.distributions.packageName
            ?: project.name
    })

    // todo: dmg package version
    packageVersion.set(
        project.provider {
            app.distributions.macOS.packageVersion
                ?: app.distributions.packageVersion
                ?: project.version.toString().takeIf { it != "unspecified" }
                ?: "1.0.0"
        }
    )

    destinationDir.set(app.distributions.outputBaseDir.dir(
        "${app.name}/native-${binary.target.name}-${binary.buildType.name.lowercase()}-${format.id}"
    ))
}

private fun desktopNativeTaskName(action: String, binary: NativeBinary): String =
    joinLowerCamelCase(action, binary.buildType.name.lowercase(), binary.target.name)

private inline fun <reified T : Task> TaskContainer.composeDesktopNativeTask(
    name: String,
    args: List<Any> = emptyList(),
    noinline configureFn: T.() -> Unit = {}
) = register(name, T::class.java, *args.toTypedArray()).apply {
    configure {
        it.group = "compose desktop (native)"
        it.configureFn()
    }
}