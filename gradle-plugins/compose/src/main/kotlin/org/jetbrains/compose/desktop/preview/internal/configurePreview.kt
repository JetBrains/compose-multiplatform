package org.jetbrains.compose.desktop.preview.internal

import org.gradle.api.Project
import org.jetbrains.compose.composeVersion
import org.jetbrains.compose.desktop.application.dsl.Application
import org.jetbrains.compose.desktop.application.internal.javaHomeOrDefault
import org.jetbrains.compose.desktop.application.internal.provider
import org.jetbrains.compose.desktop.preview.tasks.AbstractRunComposePreviewTask

internal const val PREVIEW_RUNTIME_CLASSPATH_CONFIGURATION = "composeDesktopPreviewRuntimeClasspath"
private val COMPOSE_PREVIEW_RUNTIME_DEPENDENCY = "org.jetbrains.compose:compose-preview-runtime-desktop:$composeVersion"

fun Project.initializePreview() {
    configurations.create(PREVIEW_RUNTIME_CLASSPATH_CONFIGURATION).defaultDependencies { deps ->
        deps.add(dependencies.create(COMPOSE_PREVIEW_RUNTIME_DEPENDENCY))
    }
}

internal fun AbstractRunComposePreviewTask.configureRunPreviewTask(app: Application) {
    app._configurationSource?.let { configSource ->
        dependsOn(configSource.jarTaskName)
        classpath = configSource.runtimeClasspath(project)
        javaHome.set(provider { app.javaHomeOrDefault() })
        jvmArgs.set(provider { app.jvmArgs })
    }
}