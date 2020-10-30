package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal sealed class ConfigurationSource {
    abstract val jarTaskName: String
    abstract val runtimeClasspath: FileCollection
    fun jarTask(project: Project): TaskProvider<Jar> =
        project.tasks.named(jarTaskName, Jar::class.java)

    class GradleSourceSet(val sourceSet: SourceSet) : ConfigurationSource() {
        override val jarTaskName: String
            get() = sourceSet.jarTaskName

        override val runtimeClasspath: FileCollection
            get() = sourceSet.runtimeClasspath

    }

    class KotlinMppTarget(val target: KotlinJvmTarget) : ConfigurationSource() {
        override val jarTaskName: String
            get() = target.artifactsTaskName

        override val runtimeClasspath: FileCollection
            get() = target.project.objects.fileCollection().apply {
                from(jarTask(target.project).flatMap { it.archiveFile })
                from(target.compilations.getByName("main").runtimeDependencyFiles)
            }
    }
}