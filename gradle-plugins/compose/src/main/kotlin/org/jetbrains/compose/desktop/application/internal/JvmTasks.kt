/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.desktop.application.dsl.JvmApplicationBuildType
import org.jetbrains.compose.internal.utils.uppercaseFirstChar

internal class JvmTasks(
    private val project: Project,
    private val buildType: JvmApplicationBuildType,
    private val taskGroup: String? = composeDesktopTaskGroup
) {
    /**
     * Registers new Compose/Desktop tasks.
     * Naming pattern for tasks is: [taskNameAction][taskNameClassifier][taskNameObject]
     * Where:
     *   [taskNameAction] -- name for a task's action (e.g. 'run' or 'package')
     *   taskNameDisambiguationClassifier -- optional name for an disambiguation classifier (e.g. 'release')
     *   [taskNameObject] -- name for an object of action (e.g. 'distributable' or 'dmg')
     * Examples: 'runDistributable', 'runReleaseDistributable', 'packageDmg', 'packageReleaseDmg'
     */
    inline fun <reified T : Task> register(
        taskNameAction: String,
        taskNameObject: String = "",
        args: List<Any> = emptyList(),
        noinline configureFn: T.() -> Unit = {}
    ): TaskProvider<T> {
        val buildTypeClassifier = buildType.classifier.uppercaseFirstChar()
        val objectClassifier = taskNameObject.uppercaseFirstChar()
        val taskName = "$taskNameAction$buildTypeClassifier$objectClassifier"
        return register(taskName, klass = T::class.java, args = args, configureFn = configureFn)
    }

    fun <T : Task> register(
        name: String,
        klass: Class<T>,
        args: List<Any>,
        configureFn: T.() -> Unit
    ): TaskProvider<T> =
        project.tasks.register(name, klass, *args.toTypedArray()).apply {
            configure { task ->
                task.group = taskGroup
                task.configureFn()
            }
        }
}