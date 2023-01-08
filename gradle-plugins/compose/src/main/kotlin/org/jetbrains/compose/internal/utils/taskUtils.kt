/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.utils

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

internal fun <T : Task> TaskProvider<T>.dependsOn(vararg dependencies: Any) {
    configure { it.dependsOn(*dependencies) }
}

internal inline fun <reified T : Task> Project.registerTask(
    name: String,
    crossinline fn: T.() -> Unit
): TaskProvider<T> =
    tasks.register(name, T::class.java) { task ->
        task.fn()
    }
