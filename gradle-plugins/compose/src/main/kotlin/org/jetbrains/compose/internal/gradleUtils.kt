/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.*

internal inline fun Logger.info(fn: () -> String) {
    if (isInfoEnabled) {
        info(fn())
    }
}

internal inline fun Logger.debug(fn: () -> String) {
    if (isDebugEnabled) {
        debug(fn())
    }
}

internal inline fun <reified T : Task> Project.registerTask(
    name: String,
    crossinline fn: T.() -> Unit
): TaskProvider<T> =
    tasks.register(name, T::class.java) { task ->
        task.fn()
    }

internal fun Provider<String>.toDir(project: Project): Provider<Directory> =
    project.layout.dir(map { File(it) })

internal fun Provider<File>.fileToDir(project: Project): Provider<Directory> =
    project.layout.dir(this)

val Project.localPropertiesFile get() = project.rootProject.file("local.properties")

fun Project.getLocalProperty(key: String): String? {
    if (localPropertiesFile.exists()) {
        val properties = Properties()
        localPropertiesFile.inputStream().buffered().use { input ->
            properties.load(input)
        }
        return properties.getProperty(key)
    } else {
        localPropertiesFile.createNewFile()
        return null
    }
}
