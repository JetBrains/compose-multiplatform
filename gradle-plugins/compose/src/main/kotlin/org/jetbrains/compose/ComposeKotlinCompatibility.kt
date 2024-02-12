package org.jetbrains.compose

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal object ComposeKotlinCompatibility {
    fun checkKotlinIsSupported(kotlinVersion: String, kotlinPlatformType: KotlinPlatformType) {
        val kotlinVersion = kotlinVersion.parseVersionOrNull()

        when(kotlinPlatformType) {
            KotlinPlatformType.wasm -> check(
                kotlinVersion == null ||
                kotlinVersion.major == 0 ||
                kotlinVersion > Version(1, 9, 21)
            ) {
                "Compose Multiplatform ${ComposeBuildConfig.composeGradlePluginVersion} doesn't support Kotlin " +
                        "$kotlinVersion. Minimal supported version is 1.9.22"
            }
            else -> Unit
        }
    }
}

// parse only simple numbers, as we need only them, not whole semantic versioning checks
private fun String.parseVersionOrNull(): Version? {
    val parts = split(".")
    return Version(
        parts.getOrNull(0)?.toIntOrNull() ?: return null,
        parts.getOrNull(1)?.toIntOrNull() ?: return null,
        parts.getOrNull(2)?.toIntOrNull() ?: return null,
    )
}

private class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
    override fun compareTo(other: Version) = compareValuesBy(
        this,
        other,
        { it.major },
        { it.minor },
        { it.patch },
    )
}