/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal

import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact

internal class ComposeCompilerArtifactProvider(private val customPluginString: () -> String?) {
    val compilerArtifact: SubpluginArtifact
        get() {
            val customPlugin = customPluginString()
            val customCoordinates = customPlugin?.split(":")
            return when (customCoordinates?.size) {
                null -> DefaultCompiler.pluginArtifact
                1 -> {
                    val customVersion = customCoordinates[0]
                    check(customVersion.isNotBlank()) { "'compose.kotlinCompilerPlugin' cannot be blank!" }
                    DefaultCompiler.pluginArtifact.copy(version = customVersion)
                }
                3 -> DefaultCompiler.pluginArtifact.copy(
                    groupId = customCoordinates[0],
                    artifactId = customCoordinates[1],
                    version = customCoordinates[2]
                )
                else -> error("""
                        Illegal format of 'compose.kotlinCompilerPlugin' property.
                        Expected format: either '<VERSION>' or '<GROUP_ID>:<ARTIFACT_ID>:<VERSION>'
                        Actual value: '$customPlugin'
                """.trimIndent())
            }
        }

    val compilerHostedArtifact: SubpluginArtifact
        get() = compilerArtifact.run {
            val newArtifactId =
                if (groupId == DefaultCompiler.GROUP_ID && artifactId == DefaultCompiler.ARTIFACT_ID) {
                    DefaultCompiler.HOSTED_ARTIFACT_ID
                } else artifactId

            copy(artifactId = newArtifactId)
        }

    internal object DefaultCompiler {
        const val GROUP_ID = "org.jetbrains.compose.compiler"
        const val ARTIFACT_ID = "compiler"
        const val HOSTED_ARTIFACT_ID = "compiler-hosted"
        const val VERSION = ComposeBuildConfig.composeCompilerVersion

        val pluginArtifact: SubpluginArtifact
            get() = SubpluginArtifact(groupId = GROUP_ID, artifactId = ARTIFACT_ID, version = VERSION)
    }
}

internal fun SubpluginArtifact.copy(
    groupId: String? = null,
    artifactId: String? = null,
    version: String? = null
): SubpluginArtifact =
    SubpluginArtifact(
        groupId = groupId ?: this.groupId,
        artifactId = artifactId ?: this.artifactId,
        version = version ?: this.version
    )