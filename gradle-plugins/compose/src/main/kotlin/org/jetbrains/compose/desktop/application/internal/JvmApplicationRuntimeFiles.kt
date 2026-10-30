/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal class JvmApplicationRuntimeFiles(
    val allRuntimeJars: FileCollection,
    val mainJar: Provider<RegularFile>,
    private val taskDependencies: Array<Any>,
    val skikoJars: FileCollection = allRuntimeJars.filter { false }
) {
    operator fun component1() = allRuntimeJars
    operator fun component2() = mainJar

    fun <T : Task> configureUsageBy(task: T, fn: T.(JvmApplicationRuntimeFiles) -> Unit) {
        task.dependsOn(taskDependencies)
        task.fn(this)
    }
}

internal sealed class JvmApplicationRuntimeFilesProvider {
    abstract fun jvmApplicationRuntimeFiles(project: Project): JvmApplicationRuntimeFiles

    abstract class GradleJvmApplicationRuntimeFilesProvider : JvmApplicationRuntimeFilesProvider() {
        protected abstract val jarTaskName: String
        protected abstract val runtimeFiles: FileCollection
        protected abstract val runtimeConfigurationName: String

        override fun jvmApplicationRuntimeFiles(project: Project): JvmApplicationRuntimeFiles {
            val jarTask = project.tasks.named(jarTaskName, Jar::class.java)
            val mainJar = jarTask.flatMap { it.archiveFile }
            val runtimeJarFiles = project.objects.fileCollection().apply {
                from(mainJar)
                from(runtimeFiles.filter { it.path.endsWith(".jar") })
            }
            val skikoJars = project.configurations.getByName(runtimeConfigurationName).incoming.artifactView {
                it.componentFilter { id ->
                    id is org.gradle.api.artifacts.component.ModuleComponentIdentifier && id.group == "org.jetbrains.skiko"
                }
            }.files
            return JvmApplicationRuntimeFiles(runtimeJarFiles, mainJar, arrayOf(jarTask), skikoJars)

        }
    }

    class FromGradleSourceSet(private val sourceSet: SourceSet) : GradleJvmApplicationRuntimeFilesProvider() {
        override val jarTaskName: String
            get() = sourceSet.jarTaskName

        override val runtimeConfigurationName: String get() = sourceSet.runtimeClasspathConfigurationName
        override val runtimeFiles: FileCollection
            get() = sourceSet.runtimeClasspath
    }

    class FromKotlinMppTarget(private val target: KotlinJvmTarget) : GradleJvmApplicationRuntimeFilesProvider() {
        override val jarTaskName: String
            get() = target.artifactsTaskName

        override val runtimeConfigurationName: String get() = target.compilations.getByName("main").runtimeDependencyConfigurationName
        override val runtimeFiles: FileCollection
            get() = target.compilations.getByName("main").runtimeDependencyFiles
    }

    class Custom(
        private val runtimeJarFiles: FileCollection,
        private val mainJar: Provider<RegularFile>,
        private val taskDependencies: Array<Any>
    ) : JvmApplicationRuntimeFilesProvider() {
        override fun jvmApplicationRuntimeFiles(project: Project): JvmApplicationRuntimeFiles =
            JvmApplicationRuntimeFiles(runtimeJarFiles, mainJar, taskDependencies)
    }
}