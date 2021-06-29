package org.jetbrains.compose.desktop.preview.internal

import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.dsl.Application
import org.jetbrains.compose.desktop.application.internal.javaHomeOrDefault
import org.jetbrains.compose.desktop.application.internal.provider
import org.jetbrains.compose.desktop.preview.tasks.AbstractConfigureDesktopPreviewTask

fun Project.initializePreview() {
}

internal fun AbstractConfigureDesktopPreviewTask.configureConfigureDesktopPreviewTask(app: Application) {
    app._configurationSource?.let { configSource ->
        dependsOn(configSource.jarTaskName)
        previewClasspath = configSource.runtimeClasspath(project)
        javaHome.set(provider { app.javaHomeOrDefault() })
        jvmArgs.set(provider { app.jvmArgs })
    }
}