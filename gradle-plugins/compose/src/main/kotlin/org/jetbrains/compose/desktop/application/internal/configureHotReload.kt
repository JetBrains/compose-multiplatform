package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.jetbrains.compose.internal.KOTLIN_JVM_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.Version
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

private const val minimalKotlinVersionForCHR = "2.1.20"

private const val minimalKotlinVersionForCHRWarning = "w: " +
        "Compose Hot Reload is disabled. To make use of it, you need to be on Kotlin $minimalKotlinVersionForCHR or higher."

private fun Project.applyHotReload(kgp: KotlinBasePlugin) {
    // check minimal kotlin version requirement
    val kgpVersion = kgp.pluginVersion
    if (Version.fromString(kgpVersion) < Version.fromString(minimalKotlinVersionForCHR)) {
        logger.warn(minimalKotlinVersionForCHRWarning)
        return
    }

    // We add an explicit runtime dependency to the Compose Hot Reload Gradle plugin of a
    // specific preferred version (https://docs.gradle.org/current/userguide/dependency_versions.html),
    // so we are able to apply the plugin by id here at least of that preferred version
    // (because the plugin is present on the class-path),
    // until a user explicitly specifies their own version.
    project.pluginManager.apply("org.jetbrains.compose.hot-reload")
}

fun Project.configureHotReload() {
    if (!ComposeProperties.disableHotReload(providers).get()) {
        plugins.withId(KOTLIN_MPP_PLUGIN_ID) { plugin ->
            mppExt.targets.withType(KotlinJvmTarget::class.java) {
                applyHotReload(plugin as KotlinBasePlugin)
            }
        }
        plugins.withId(KOTLIN_JVM_PLUGIN_ID) { plugin ->
            applyHotReload(plugin as KotlinBasePlugin)
        }
    }
}