/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

private val pluginProperties = java.util.Properties().apply {
    requireNotNull(HtmlLoweringGradlePlugin::class.java.getResourceAsStream("/compose-html-lowering.properties")) {
        "Missing Compose HTML lowering plugin metadata"
    }.use(::load)
}

/** Adds the HTML compiler plugin and the `htmlLowering` configuration. */
class HtmlLoweringGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("htmlLowering", HtmlLoweringExtension::class.java).apply {
            enabledTargets.convention(setOf(KotlinPlatformType.jvm))
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        kotlinCompilation.platformType != KotlinPlatformType.common

    override fun getCompilerPluginId() = pluginProperties.getProperty("id")

    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = "org.jetbrains.compose.html",
        artifactId = "compose-html-compiler-plugin",
        version = pluginProperties.getProperty("version")
    )

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val extension = kotlinCompilation.target.project.extensions.getByType(HtmlLoweringExtension::class.java)
        val enabled = extension.enabledTargets.map { kotlinCompilation.platformType in it }
        val supportsLowering = kotlinCompilation.platformType in setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)
        kotlinCompilation.compileTaskProvider.configure {
            it.compilerOptions.freeCompilerArgs.addAll(enabled.map { lower ->
                if (lower && supportsLowering) listOf(
                    // The lowering must see HTML calls before the Compose compiler rewrites them.
                    "-Xcompiler-plugin-order=${getCompilerPluginId()}>androidx.compose.compiler.plugins.kotlin"
                ) else emptyList()
            })
        }
        return enabled.map { listOf(SubpluginOption("enabled", it.toString())) }
    }
}
