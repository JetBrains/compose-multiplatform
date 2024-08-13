package org.jetbrains.compose

private const val KOTLIN_COMPATIBILITY_LINK =
    "https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compatibility-and-versioning.html"

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
        "1.9.20-Beta" to "1.5.2.1-Beta2",
        "1.9.20-Beta2" to "1.5.2.1-Beta3",
        "1.9.20-RC" to "1.5.2.1-rc01",
        "1.9.20-RC2" to "1.5.3-rc01",
        "1.9.20" to "1.5.3",
        "1.9.21" to "1.5.4",
        "1.9.22" to "1.5.8.1",
        "1.9.23" to "1.5.13.5",
        "1.9.24" to "1.5.14.1-beta02",
        "2.0.0-Beta1" to "1.5.4-dev1-kt2.0.0-Beta1",
        "2.0.0-Beta4" to "1.5.9-kt-2.0.0-Beta4",
        "2.0.0-Beta5" to "1.5.11-kt-2.0.0-Beta5",
        "2.0.0-RC1" to "1.5.11-kt-2.0.0-RC1",
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
