/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.internal.ComposeCompilerArtifactProvider
import org.jetbrains.compose.internal.webExt
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

class ComposeCompilerKotlinSupportPlugin : KotlinCompilerPluginSupportPlugin {
    private lateinit var composeCompilerArtifactProvider: ComposeCompilerArtifactProvider

    override fun apply(target: Project) {
        super.apply(target)
        target.plugins.withType(ComposePlugin::class.java) {
            val composeExt = target.extensions.getByType(ComposeExtension::class.java)

            composeCompilerArtifactProvider = ComposeCompilerArtifactProvider(
                kotlinVersion = target.getKotlinPluginVersion()
            ) {
                composeExt.kotlinCompilerPlugin.orNull
            }
        }
    }

    override fun getCompilerPluginId(): String =
        "androidx.compose.compiler.plugins.kotlin"

    override fun getPluginArtifact(): SubpluginArtifact =
        composeCompilerArtifactProvider.compilerArtifact

    override fun getPluginArtifactForNative(): SubpluginArtifact =
        composeCompilerArtifactProvider.compilerHostedArtifact

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        when (kotlinCompilation.target.platformType) {
            KotlinPlatformType.common -> true
            KotlinPlatformType.jvm -> true
            KotlinPlatformType.js -> isApplicableJsTarget(kotlinCompilation.target)
            KotlinPlatformType.androidJvm -> true
            KotlinPlatformType.native -> true
        }

    private fun isApplicableJsTarget(kotlinTarget: KotlinTarget): Boolean {
        if (kotlinTarget !is KotlinJsIrTarget) return false

        val project = kotlinTarget.project
        val webExt = project.webExt ?: return false

        return kotlinTarget in webExt.targetsToConfigure(project)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val target = kotlinCompilation.target
        composeCompilerArtifactProvider.checkTargetSupported(target)
        return target.project.provider {
            platformPluginOptions[target.platformType] ?: emptyList()
        }
    }

    private val platformPluginOptions = mapOf(
        KotlinPlatformType.js to options("generateDecoys" to "true")
    )

    private fun options(vararg options: Pair<String, String>): List<SubpluginOption> =
        options.map { SubpluginOption(it.first, it.second) }
}