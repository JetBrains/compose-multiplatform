/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal sealed class ConfigurationSource {
    abstract val jarTaskName: String

    abstract fun runtimeClasspath(project: Project): FileCollection

    fun jarTask(project: Project): TaskProvider<Jar> =
        project.tasks.named(jarTaskName, Jar::class.java)

    class GradleSourceSet(val sourceSet: SourceSet) : ConfigurationSource() {
        override val jarTaskName: String
            get() = sourceSet.jarTaskName

        override fun runtimeClasspath(project: Project): FileCollection =
            project.objects.fileCollection().apply {
                from(jarTask(project).flatMap { it.archiveFile })
                from(sourceSet.runtimeClasspath.filter { it.path.endsWith(".jar") })
            }
    }

    class KotlinMppTarget(val target: KotlinJvmTarget) : ConfigurationSource() {
        override val jarTaskName: String
            get() = target.artifactsTaskName

        override fun runtimeClasspath(project: Project): FileCollection =
            project.objects.fileCollection().apply {
                from(jarTask(project).flatMap { it.archiveFile })
                from(target.compilations.getByName("main").runtimeDependencyFiles.filter { it.path.endsWith(".jar") })
            }
    }
}