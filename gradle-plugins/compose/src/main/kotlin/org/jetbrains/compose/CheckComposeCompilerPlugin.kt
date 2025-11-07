/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose

import org.gradle.api.Project
import org.jetbrains.compose.internal.KOTLIN_ANDROID_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_JS_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_JVM_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.Version
import org.jetbrains.compose.internal.ideaIsInSyncProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

internal fun Project.checkComposeCompilerPlugin() {
    //only one of them can be applied to the project
    listOf(
        KOTLIN_MPP_PLUGIN_ID,
        KOTLIN_JVM_PLUGIN_ID,
        KOTLIN_ANDROID_PLUGIN_ID,
        KOTLIN_JS_PLUGIN_ID
    ).forEach { pluginId ->
        plugins.withId(pluginId) { plugin ->
            checkComposeCompilerPlugin(plugin as KotlinBasePlugin)
        }
    }
}

internal const val minimalSupportedKgpVersion = "2.1.0"
internal const val minimalSupportedKgpVersionError = "e: Configuration problem: " +
        "Minimal supported Kotlin Gradle Plugin version is $minimalSupportedKgpVersion"
internal const val newComposeCompilerKotlinSupportPluginId = "org.jetbrains.kotlin.plugin.compose"
internal const val newComposeCompilerError =
    "Starting with Compose Multiplatform 1.6.10, " +
            "you should apply \"$newComposeCompilerKotlinSupportPluginId\" plugin." +
            "\nSee the migration guide https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compiler.html#migrating-a-compose-multiplatform-project"

private fun Project.checkComposeCompilerPlugin(kgp: KotlinBasePlugin) {
    val kgpVersion = kgp.pluginVersion
    val ideaIsInSync = project.ideaIsInSyncProvider().get()

    if (Version.fromString(kgpVersion) < Version.fromString(minimalSupportedKgpVersion)) {
        if (ideaIsInSync) logger.error(minimalSupportedKgpVersionError)
        else error(minimalSupportedKgpVersionError)
    } else {
        //There is no other way to check that the plugin WASN'T applied!
        afterEvaluate {
            logger.info("Check that new '$newComposeCompilerKotlinSupportPluginId' was applied")
            if (!project.plugins.hasPlugin(newComposeCompilerKotlinSupportPluginId)) {
                if (ideaIsInSync) logger.error("e: Configuration problem: $newComposeCompilerError")
                else error("e: Configuration problem: $newComposeCompilerError")
            }
        }
    }
}
