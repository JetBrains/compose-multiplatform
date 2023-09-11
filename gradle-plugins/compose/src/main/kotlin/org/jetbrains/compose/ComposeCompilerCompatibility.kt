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
        "1.8.21" to "1.4.7",
        "1.8.22" to "1.4.8",
        "1.9.0-Beta" to "1.4.7.1-beta",
        "1.9.0-RC" to "1.4.8-beta",
        "1.9.0" to "1.5.1",
        "1.9.10" to "1.5.2",
        "1.9.20-Beta" to "1.5.2.1-Beta"
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
