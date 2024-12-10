/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.internal.ComposeCompilerArtifactProvider
import org.jetbrains.compose.internal.KOTLIN_ANDROID_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_JS_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_JVM_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.Version
import org.jetbrains.compose.internal.ideaIsInSyncProvider
import org.jetbrains.compose.internal.mppExtOrNull
import org.jetbrains.compose.internal.webExt
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

internal fun Project.configureComposeCompilerPlugin() {
    //only one of them can be applied to the project
    listOf(
        KOTLIN_MPP_PLUGIN_ID,
        KOTLIN_JVM_PLUGIN_ID,
        KOTLIN_ANDROID_PLUGIN_ID,
        KOTLIN_JS_PLUGIN_ID
    ).forEach { pluginId ->
        plugins.withId(pluginId) { plugin ->
            configureComposeCompilerPlugin(plugin as KotlinBasePlugin)
        }
    }
}

internal const val newCompilerIsAvailableVersion = "2.0.0-RC2-238"
internal const val newComposeCompilerKotlinSupportPluginId = "org.jetbrains.kotlin.plugin.compose"
internal const val newComposeCompilerError =
    "Since Kotlin 2.0.0-RC2 to use Compose Multiplatform " +
            "you must apply \"$newComposeCompilerKotlinSupportPluginId\" plugin." +
            "\nSee the migration guide https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compiler.html#migrating-a-compose-multiplatform-project"

private fun Project.configureComposeCompilerPlugin(kgp: KotlinBasePlugin) {
    val kgpVersion = kgp.pluginVersion

    if (Version.fromString(kgpVersion) < Version.fromString(newCompilerIsAvailableVersion)) {
        logger.info("Apply ComposeCompilerKotlinSupportPlugin (KGP version = $kgpVersion)")
        project.plugins.apply(ComposeCompilerKotlinSupportPlugin::class.java)

        //legacy logic applied for Kotlin < 2.0 only
        project.afterEvaluate {
            val composeExtension = project.extensions.getByType(ComposeExtension::class.java)
            project.tasks.withType(org.jetbrains.kotlin.gradle.dsl.KotlinCompile::class.java).configureEach {
                it.kotlinOptions.apply {
                    freeCompilerArgs = freeCompilerArgs +
                            composeExtension.kotlinCompilerPluginArgs.get().flatMap { arg ->
                                listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:$arg")
                            }
                }
            }

            val hasAnyWebTarget = project.mppExtOrNull?.targets?.firstOrNull {
                it.platformType == KotlinPlatformType.js ||
                        it.platformType == KotlinPlatformType.wasm
            } != null
            if (hasAnyWebTarget) {
                // currently k/wasm compile task is covered by KotlinJsCompile type
                project.tasks.withType(KotlinJsCompile::class.java).configureEach {
                    it.kotlinOptions.freeCompilerArgs += listOf(
                        "-Xklib-enable-signature-clash-checks=false",
                    )
                }
            }
        }
    } else {
        //There is no other way to check that the plugin WASN'T applied!
        afterEvaluate {
            logger.info("Check that new '$newComposeCompilerKotlinSupportPluginId' was applied")
            if (!project.plugins.hasPlugin(newComposeCompilerKotlinSupportPluginId)) {
                val ideaIsInSync = project.ideaIsInSyncProvider().get()
                if (ideaIsInSync) logger.error("e: Configuration problem: $newComposeCompilerError")
                else error("e: Configuration problem: $newComposeCompilerError")
            }
        }
    }
}

class ComposeCompilerKotlinSupportPlugin : KotlinCompilerPluginSupportPlugin {
    private lateinit var composeCompilerArtifactProvider: ComposeCompilerArtifactProvider
    private lateinit var applicableForPlatformTypes: Provider<Set<KotlinPlatformType>>


    override fun apply(target: Project) {
        super.apply(target)
        target.plugins.withType(ComposePlugin::class.java) {
            val composeExt = target.extensions.getByType(ComposeExtension::class.java)

            composeCompilerArtifactProvider = ComposeCompilerArtifactProvider {
                composeExt.kotlinCompilerPlugin.orNull
                    ?: ComposeCompilerCompatibility.compilerVersionFor(target.getKotlinPluginVersion())
            }

            applicableForPlatformTypes = composeExt.platformTypes
        }
    }

    override fun getCompilerPluginId(): String =
        "androidx.compose.compiler.plugins.kotlin"

    override fun getPluginArtifact(): SubpluginArtifact =
        composeCompilerArtifactProvider.compilerArtifact

    override fun getPluginArtifactForNative(): SubpluginArtifact =
        composeCompilerArtifactProvider.compilerHostedArtifact

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val applicableTo = applicableForPlatformTypes.get()

        return when (val type = kotlinCompilation.target.platformType) {
            KotlinPlatformType.js -> isApplicableJsTarget(kotlinCompilation.target) && applicableTo.contains(type)
            else -> applicableTo.contains(type)
        }
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
        KotlinPlatformType.js to options("generateDecoys" to "false"),
        KotlinPlatformType.wasm to options("generateDecoys" to "false")
    )

    private fun options(vararg options: Pair<String, String>): List<SubpluginOption> =
        options.map { SubpluginOption(it.first, it.second) }
}
