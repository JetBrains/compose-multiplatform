/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.build.event.BuildEventsListenerRegistry
import org.jetbrains.compose.internal.ComposeCompilerArtifactProvider
import org.jetbrains.compose.internal.mppExtOrNull
import org.jetbrains.compose.internal.webExt
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import javax.inject.Inject

@Suppress("UnstableApiUsage")
class ComposeCompilerKotlinSupportPlugin @Inject constructor(
    @Suppress("UnstableApiUsage") private val buildEventsListenerRegistry: BuildEventsListenerRegistry
) : KotlinCompilerPluginSupportPlugin {
    private lateinit var composeCompilerArtifactProvider: ComposeCompilerArtifactProvider

    override fun apply(target: Project) {
        super.apply(target)
        target.plugins.withType(ComposePlugin::class.java) {
            val composeExt = target.extensions.getByType(ComposeExtension::class.java)

            composeCompilerArtifactProvider = ComposeCompilerArtifactProvider {
                composeExt.kotlinCompilerPlugin.orNull ?:
                    ComposeCompilerCompatibility.compilerVersionFor(target.getKotlinPluginVersion())
            }

            collectUnsupportedCompilerPluginUsages(target)
        }
    }

    private fun collectUnsupportedCompilerPluginUsages(target: Project) {
        fun Project.hasNonJvmTargets(): Boolean {
            val nonJvmTargets = setOf(KotlinPlatformType.native, KotlinPlatformType.js, KotlinPlatformType.wasm)
            return mppExtOrNull?.targets?.any {
                it.platformType in nonJvmTargets
            } ?: false
        }

        fun SubpluginArtifact.isNonJBComposeCompiler(): Boolean {
            return !groupId.startsWith("org.jetbrains.compose.compiler")
        }

        val service = ComposeMultiplatformBuildService.provider(target)
        buildEventsListenerRegistry.onTaskCompletion(service)

        service.get().parameters.unsupportedCompilerPlugins.add(
            target.provider {
                composeCompilerArtifactProvider.compilerArtifact.takeIf {
                    target.hasNonJvmTargets() && it.isNonJBComposeCompiler()
                }
            }
        )
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
    "https://github.com/JetBrains/compose-jb/blob/master/VERSIONING.md#using-compose-multiplatform-compiler"

internal fun createWarningAboutNonCompatibleCompiler(currentCompilerPluginGroupId: String): String {
    return """
WARNING: Usage of the Custom Compose Compiler plugin ('$currentCompilerPluginGroupId') 
with non-JVM targets targets (Kotlin/Native, Kotlin/JS, Kotlin/WASM) is not supported.
For more information, please visit: $COMPOSE_COMPILER_COMPATIBILITY_LINK
""".trimMargin()
}