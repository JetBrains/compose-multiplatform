package org.jetbrains.compose.desktop.preview.internal

import org.gradle.api.Project
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.internal.JvmApplicationRuntimeFilesProvider
import org.jetbrains.compose.desktop.preview.tasks.AbstractConfigureDesktopPreviewTask
import org.jetbrains.compose.internal.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

fun Project.initializePreview(desktopExtension: DesktopExtension) {
    plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
        mppExt.targets.all { target ->
            if (target.platformType == KotlinPlatformType.jvm) {
                val runtimeFilesProvider = JvmApplicationRuntimeFilesProvider.FromKotlinMppTarget(target as KotlinJvmTarget)
                registerConfigurePreviewTask(project, runtimeFilesProvider, targetName = target.name)
            }
        }
    }
    plugins.withId(KOTLIN_JVM_PLUGIN_ID) {
        val sourceSet = project.javaSourceSets.getByName("main")
        val runtimeFilesProvider = JvmApplicationRuntimeFilesProvider.FromGradleSourceSet(sourceSet)
        registerConfigurePreviewTask(project, runtimeFilesProvider)
    }
}

private fun registerConfigurePreviewTask(
    project: Project,
    runtimeFilesProvider: JvmApplicationRuntimeFilesProvider,
    targetName: String = ""
) {
    val runtimeFiles = runtimeFilesProvider.jvmApplicationRuntimeFiles(project)
    project.tasks.register(
        previewTaskName(targetName),
        AbstractConfigureDesktopPreviewTask::class.java
    ) { previewTask ->
        runtimeFiles.configureUsageBy(previewTask) { (runtimeJars, _) ->
            previewClasspath = runtimeJars
        }
    }
}

private fun previewTaskName(targetName: String) =
    "configureDesktopPreview${targetName.uppercaseFirstChar()}"