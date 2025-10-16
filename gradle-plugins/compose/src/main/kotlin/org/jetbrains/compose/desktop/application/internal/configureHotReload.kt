package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.jetbrains.compose.internal.KOTLIN_JVM_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.javaSourceSets
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

private fun Project.applyHotReload() {
    project.pluginManager.apply("org.jetbrains.compose.hot-reload")
}

fun Project.configureHotReload() {
    if (!ComposeProperties.disableHotReload(providers).get()) {
        plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
            mppExt.targets.withType(KotlinJvmTarget::class.java) { target ->
                val runtimeFilesProvider = JvmApplicationRuntimeFilesProvider.FromKotlinMppTarget(target)
                applyHotReload()
            }
        }
        plugins.withId(KOTLIN_JVM_PLUGIN_ID) {
            val sourceSet = project.javaSourceSets.getByName("main")
            val runtimeFilesProvider = JvmApplicationRuntimeFilesProvider.FromGradleSourceSet(sourceSet)
            applyHotReload()
        }
    }
}