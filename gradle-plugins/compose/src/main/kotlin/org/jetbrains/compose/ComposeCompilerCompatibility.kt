package org.jetbrains.compose

private const val KOTLIN_COMPATIBILITY_LINK =
    "https://github.com/JetBrains/compose-jb/blob/master/VERSIONING.md#kotlin-compatibility"

internal object ComposeCompilerCompatibility {
    private val kotlinToCompiler = sortedMapOf(
        "1.7.10" to "1.3.0",
        "1.7.20" to "1.3.2.2",
        "1.8.0" to "1.4.0",
        "1.8.10" to "1.4.2",
        "1.8.20" to "1.4.5",
    )

    fun compilerVersionFor(kotlinVersion: String): String {
        return kotlinToCompiler[kotlinVersion] ?: throw RuntimeException(
            "Compose Multiplatform ${ComposeBuildConfig.composeGradlePluginVersion} doesn't support Kotlin " +
                    "$kotlinVersion. " +
                    "Please see $KOTLIN_COMPATIBILITY_LINK " +
                    "to know the latest supported version of Kotlin."
        )
    }
}
