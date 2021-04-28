/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose

import org.gradle.api.provider.Provider
import org.jetbrains.compose.internal.webExt
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

class ComposeCompilerKotlinSupportPlugin : KotlinCompilerPluginSupportPlugin {
    override fun getCompilerPluginId(): String =
        "androidx.compose.compiler.plugins.kotlin"

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        when (kotlinCompilation.target.platformType) {
            KotlinPlatformType.common -> true
            KotlinPlatformType.jvm -> true
            KotlinPlatformType.js -> isApplicableJsTarget(kotlinCompilation.target)
            KotlinPlatformType.androidJvm -> true
            KotlinPlatformType.native -> false
        }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val target = kotlinCompilation.target
        return target.project.provider {
            platformPluginOptions[target.platformType] ?: emptyList()
        }
    }

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = "org.jetbrains.compose.compiler", artifactId = "compiler", version = composeVersion
        )

    private fun isApplicableJsTarget(kotlinTarget: KotlinTarget): Boolean {
        if (kotlinTarget !is KotlinJsIrTarget) return false

        val project = kotlinTarget.project
        val webExt = project.webExt ?: return false

        return kotlinTarget in webExt.targetsToConfigure(project)
    }

    private val platformPluginOptions = mapOf(
        KotlinPlatformType.js to options("generateDecoys" to "true")
    )

    private fun options(vararg options: Pair<String, String>): List<SubpluginOption> =
        options.map { SubpluginOption(it.first, it.second) }
}