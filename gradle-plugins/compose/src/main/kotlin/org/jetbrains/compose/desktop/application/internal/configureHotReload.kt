package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.jetbrains.compose.internal.KOTLIN_JVM_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

private fun Project.isJVMTargetAvailable(): Boolean {
    return plugins.findPlugin(KOTLIN_JVM_PLUGIN_ID) != null ||
           plugins.findPlugin(KOTLIN_MPP_PLUGIN_ID)
               ?.let { mppExt.targets.withType(KotlinJvmTarget::class.java).isNotEmpty() } == true
}

fun Project.configureHotReload() {
    if (!isJVMTargetAvailable() || ComposeProperties.disableHotReload(providers).get()) {
        return
    }
    project.pluginManager.apply("org.jetbrains.compose.hot-reload")
}