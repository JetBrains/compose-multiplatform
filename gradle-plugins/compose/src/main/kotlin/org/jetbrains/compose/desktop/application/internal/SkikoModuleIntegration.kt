/* Copyright 2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license. */
package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.compose.desktop.application.tasks.AbstractPrepareSkikoModuleTask
import org.jetbrains.compose.internal.utils.dir

internal fun JvmApplicationContext.registerSkikoModuleTask(): TaskProvider<AbstractPrepareSkikoModuleTask> =
    tasks.register<AbstractPrepareSkikoModuleTask>(
        taskNameAction = "prepare", taskNameObject = "SkikoModule"
    ) {
        javaHome.set(app.javaHomeProvider)
        destinationDir.set(appTmpDir.dir("skiko-module"))
        nativeLibrariesDir.set(appTmpDir.dir("skiko-natives"))
        useAppRuntimeFiles { files ->
            runtimeJars.from(files.allRuntimeJars)
            skikoJars.from(files.skikoJars)
        }
    }

internal fun TaskProvider<AbstractPrepareSkikoModuleTask>.runtimeJvmArgs() = map { task ->
    if (task.skikoJars.isEmpty) emptyList() else listOf(
        "--add-modules=org.jetbrains.skiko", "--enable-native-access=org.jetbrains.skiko"
    )
}

internal fun JvmApplicationContext.skikoModuleTask(): TaskProvider<AbstractPrepareSkikoModuleTask> =
    project.tasks.named("prepareSkikoModule", AbstractPrepareSkikoModuleTask::class.java)

internal fun TaskProvider<AbstractPrepareSkikoModuleTask>.withoutModuleInputs(files: FileCollection): FileCollection {
    val moduleDirectory = flatMap { it.destinationDir }
    val skikoFiles = map { it.skikoJars.files }
    // Keep the directory lookup lazy: configuration-cache storage happens before
    // the preparation task has produced the module dependency JARs on a clean build.
    return files.filter { file ->
        file !in skikoFiles.get() && !moduleDirectory.get().asFile.resolve(file.name).isFile
    }
}

internal class SkikoModuleArguments(@get:Classpath val modulePath: FileCollection) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = if (modulePath.isEmpty) emptyList() else listOf(
        "--module-path", modulePath.asPath,
        "--add-modules=org.jetbrains.skiko",
        "--enable-native-access=org.jetbrains.skiko"
    )
}
