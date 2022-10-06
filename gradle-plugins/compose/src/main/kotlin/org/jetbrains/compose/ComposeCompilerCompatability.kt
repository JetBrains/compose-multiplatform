package org.jetbrains.compose

internal object ComposeCompilerCompatability {
    fun compilerVersionFor(kotlinVersion: String): ComposeCompilerVersion? = when (kotlinVersion) {
        "1.7.10" -> ComposeCompilerVersion("1.3.0-alpha01")
        "1.7.20" -> ComposeCompilerVersion("1.3.2-alpha01", isJsSupported = false)
        else -> null
    }
}

internal data class ComposeCompilerVersion(
    val version: String,
    val isJsSupported: Boolean = true
)
