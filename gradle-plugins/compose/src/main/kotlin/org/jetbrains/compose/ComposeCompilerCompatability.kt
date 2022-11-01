package org.jetbrains.compose

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal object ComposeCompilerCompatability {
    fun compilerVersionFor(kotlinVersion: String): ComposeCompilerVersion? = when (kotlinVersion) {
        "1.7.10" -> ComposeCompilerVersion("1.3.0")
        "1.7.20" -> ComposeCompilerVersion("1.3.2.1-rc02")
        else -> null
    }
}

internal data class ComposeCompilerVersion(
    val version: String,
    val unsupportedPlatforms: Set<KotlinPlatformType> = emptySet()
)
