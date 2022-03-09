package org.jetbrains.compose.desktop.preview.internal

import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.internal.ConfigurationSource
import org.jetbrains.compose.desktop.preview.tasks.AbstractConfigureDesktopPreviewTask
import org.jetbrains.compose.internal.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

fun Project.initializePreview() {
    plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
        mppExt.targets.all { target ->
            if (target.platformType == KotlinPlatformType.jvm) {
                val config = ConfigurationSource.KotlinMppTarget(target as KotlinJvmTarget)
                registerConfigurePreviewTask(project, config, targetName = target.name)
            }
        }
    }
    plugins.withId(KOTLIN_JVM_PLUGIN_ID) {
        val config = ConfigurationSource.GradleSourceSet(project.javaSourceSets.getByName("main"))
        registerConfigurePreviewTask(project, config)
    }
}

private fun registerConfigurePreviewTask(project: Project, config: ConfigurationSource, targetName: String = "") {
    project.tasks.register(
        previewTaskName(targetName),
        AbstractConfigureDesktopPreviewTask::class.java
    ) { previewTask ->
        previewTask.dependsOn(config.jarTask(project))
        previewTask.previewClasspath = config.runtimeClasspath(project)
    }
}

private fun previewTaskName(targetName: String) =
    "configureDesktopPreview${targetName.uppercaseFirstChar()}"