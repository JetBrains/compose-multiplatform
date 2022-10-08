/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal

import org.jetbrains.compose.ComposeCompilerCompatability
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact

private const val KOTLIN_COMPATABILITY_LINK =
    "https://github.com/JetBrains/compose-jb/blob/master/VERSIONING.md#kotlin-compatibility"

internal class ComposeCompilerArtifactProvider(
    private val kotlinVersion: String,
    customPluginString: () -> String?
) {
    fun checkTargetSupported(target: KotlinTarget) {
        require(!unsupportedPlatforms.contains(target.platformType)) {
            "This version of Compose Multiplatform doesn't support Kotlin " +
                "$kotlinVersion for ${target.platformType} target. " +
                "Please see $KOTLIN_COMPATABILITY_LINK " +
                "to know the latest supported version of Kotlin."
        }
    }

    private var unsupportedPlatforms: Set<KotlinPlatformType> = emptySet()

    val compilerArtifact: SubpluginArtifact

    init {
        val customPlugin = customPluginString()
        val customCoordinates = customPlugin?.split(":")
        when (customCoordinates?.size) {
            null -> {
                val version = requireNotNull(
                    ComposeCompilerCompatability.compilerVersionFor(kotlinVersion)
                ) {
                    "This version of Compose Multiplatform doesn't support Kotlin " +
                        "$kotlinVersion. " +
                        "Please see $KOTLIN_COMPATABILITY_LINK " +
                        "to know the latest supported version of Kotlin."
                }

                compilerArtifact = DefaultCompiler.pluginArtifact(
                    version = version.version
                )
                unsupportedPlatforms = version.unsupportedPlatforms
            }
            1 -> {
                val customVersion = customCoordinates[0]
                check(customVersion.isNotBlank()) { "'compose.kotlinCompilerPlugin' cannot be blank!" }
                compilerArtifact = DefaultCompiler.pluginArtifact(version = customVersion)
            }
            3 -> compilerArtifact = DefaultCompiler.pluginArtifact(
                version = customCoordinates[2],
                groupId = customCoordinates[0],
                artifactId = customCoordinates[1],
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

        fun pluginArtifact(
            version: String,
            groupId: String = GROUP_ID,
            artifactId: String = ARTIFACT_ID,
        ): SubpluginArtifact =
            SubpluginArtifact(groupId = groupId, artifactId = artifactId, version = version)
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