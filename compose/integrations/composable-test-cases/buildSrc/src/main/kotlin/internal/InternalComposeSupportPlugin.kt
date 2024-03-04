/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package internal

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

// Partially copy-pasted from https://github.com/JetBrains/compose-multiplatform/tree/master/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose
class InternalComposeSupportPlugin : KotlinCompilerPluginSupportPlugin {
    private lateinit var composeCompilerArtifactProvider: ComposeCompilerArtifactProvider

    override fun apply(target: Project) {
        super.apply(target)

        val composeCompilerVersion = target.properties["compose.kotlinCompilerPluginVersion"] as? String
            ?: error("'compose.kotlinCompilerPluginVersion' is not defined")
        composeCompilerArtifactProvider = ComposeCompilerArtifactProvider { composeCompilerVersion }
    }

    override fun getCompilerPluginId(): String = "androidx.compose.compiler.plugins.kotlin"

    override fun getPluginArtifact(): SubpluginArtifact =
        composeCompilerArtifactProvider.compilerArtifact

    override fun getPluginArtifactForNative(): SubpluginArtifact =
        composeCompilerArtifactProvider.compilerHostedArtifact

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val target = kotlinCompilation.target
        return target.project.provider { emptyList() }
    }

    private fun options(vararg options: Pair<String, String>): List<SubpluginOption> =
        options.map { SubpluginOption(it.first, it.second) }
}

val Project.composeVersion: String
    get() = properties["compose.version"] as? String
        ?: error("'compose.version' is not defined")

val Project.composeRuntimeDependency: String
    get() = properties["compose.runtime.artifactId"] as? String
        ?: properties["compose.runtime.groupId"]?.let { it.toString() + ":runtime:$composeVersion" }
        ?: "org.jetbrains.compose.runtime:runtime:${composeVersion}"