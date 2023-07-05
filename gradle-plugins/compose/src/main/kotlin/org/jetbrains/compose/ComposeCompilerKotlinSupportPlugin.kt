/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.internal.ComposeCompilerArtifactProvider
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.compose.internal.webExt
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

class ComposeCompilerKotlinSupportPlugin : KotlinCompilerPluginSupportPlugin {
    private lateinit var composeCompilerArtifactProvider: ComposeCompilerArtifactProvider

    override fun apply(target: Project) {
        super.apply(target)
        target.plugins.withType(ComposePlugin::class.java) {
            val composeExt = target.extensions.getByType(ComposeExtension::class.java)

            composeCompilerArtifactProvider = ComposeCompilerArtifactProvider {
                composeExt.kotlinCompilerPlugin.orNull ?:
                    ComposeCompilerCompatibility.compilerVersionFor(target.getKotlinPluginVersion())
            }
        }
        warnAboutJetpackComposeCompilerUsageForNonJvm(target)
    }

    @Suppress("NON_EXHAUSTIVE_WHEN")
    private fun warnAboutJetpackComposeCompilerUsageForNonJvm(target: Project) {
        val isUsingJetpackComposeCompilerPlugin =
            composeCompilerArtifactProvider.compilerArtifact.groupId.startsWith("androidx.compose.compiler")

        if (isUsingJetpackComposeCompilerPlugin) {
            target.mppExt.targets.forEach {
                when (it.platformType) {
                    KotlinPlatformType.native,
                    KotlinPlatformType.js,
                    KotlinPlatformType.wasm -> target.logger.warn(WARN_ABOUT_JC_COMPILER)
                }
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
            KotlinPlatformType.wasm -> false
        }

    private fun isApplicableJsTarget(kotlinTarget: KotlinTarget): Boolean {
        if (kotlinTarget !is KotlinJsIrTarget) return false

        val project = kotlinTarget.project
        val webExt = project.webExt ?: return false

        return kotlinTarget in webExt.targetsToConfigure(project)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val target = kotlinCompilation.target
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

private const val COMPOSE_COMPILER_COMPATIBILITY_LINK =
    "https://github.com/JetBrains/compose-jb/blob/master/VERSIONING.md"

private val WARN_ABOUT_JC_COMPILER = """
    | WARNING: You are using the 'androidx.compose.compiler' plugin in your Kotlin multiplatform project.
    | This plugin is only guaranteed to work with JVM targets (desktop or Android).
    | The usage with Kotlin/JS or Kotlin/Native targets is not supported and might cause issues.
    | Make sure you are using compatible versions of the Jetpack Compose Compiler and Kotlin.
    | You can find the compatibility table here: $COMPOSE_COMPILER_COMPATIBILITY_LINK
""".trimMargin()