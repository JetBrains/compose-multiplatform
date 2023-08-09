/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.internal

import org.gradle.api.Project
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.compose.internal.utils.KGPPropertyProvider
import org.jetbrains.compose.internal.utils.configureEachWithType
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.presetName

private const val PROJECT_CACHE_KIND_PROPERTY_NAME = "kotlin.native.cacheKind"
private const val COMPOSE_NATIVE_MANAGE_CACHE_KIND = "compose.kotlin.native.manageCacheKind"
private const val NONE_VALUE = "none"

internal fun Project.configureNativeCompilerCaching() {
    if (findProperty(COMPOSE_NATIVE_MANAGE_CACHE_KIND) == "false") return

    plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
        val kotlinVersion = kotlinVersionNumbers(this)
        mppExt.targets.configureEachWithType<KotlinNativeTarget> {
            assertCacheKindUserValueIsNotNone()
            configureTargetCompilerCache(kotlinVersion)
        }
    }
}

private fun KotlinNativeTarget.assertCacheKindUserValueIsNotNone() {
    // To determine cache kind KGP checks kotlin.native.cacheKind.<PRESET_NAME> first, then kotlin.native.cacheKind
    // For each property it tries to read Project.property, then checks local.properties
    // See https://github.com/JetBrains/kotlin/blob/d4d30dcfcf1afb083f09279c6f1ba05031efeabb/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/PropertiesProvider.kt#L416
    val cacheKindProperties = listOf(targetCacheKindPropertyName, PROJECT_CACHE_KIND_PROPERTY_NAME)
    val propertyProviders = listOf(
        KGPPropertyProvider.GradleProperties(project),
        KGPPropertyProvider.LocalProperties(project)
    )

    for (cacheKindProperty in cacheKindProperties) {
        for (provider in propertyProviders) {
            val value = provider.valueOrNull(cacheKindProperty)
            if (value != null) {
                if (value.equals(NONE_VALUE, ignoreCase = true)) {
                    project.logger.warn(cacheKindPropertyWarningMessage(cacheKindProperty, provider))
                }
                return
            }
        }
    }
}

private fun cacheKindPropertyWarningMessage(
    cacheKindProperty: String,
    provider: KGPPropertyProvider
) = """
    |Warning: '$cacheKindProperty' is explicitly set to `none`.
    |This option significantly slows the Kotlin/Native compiler.
    |Compose Multiplatform Gradle plugin can set this property automatically,
    |when it is necessary.
    |  * Recommended action: remove explicit '$cacheKindProperty=$NONE_VALUE' from ${provider.location}. 
    |  * Alternative action: if you are sure you need '$cacheKindProperty=$NONE_VALUE', disable
    |this warning by adding '$COMPOSE_NATIVE_MANAGE_CACHE_KIND=false' to your 'gradle.properties'.
""".trimMargin()

private fun KotlinNativeTarget.configureTargetCompilerCache(kotlinVersion: KotlinVersionNumbers) {
    val (majorVer, minorVer, patchVer) = kotlinVersion
    // See comments in https://youtrack.jetbrains.com/issue/KT-57329
    // When:
    //    KotlinVersion < 1.9.0,          => set cacheKind=none
    //    1.9.0 <= KotlinVersion < 1.9.20 => add -Xlazy-ir-for-caches=disable
    //    KotlinVersion >= 1.9.20         => do nothing, everything should work as is
    when {
        majorVer < 1 || majorVer == 1 && minorVer < 9 -> {
            disableKotlinNativeCache()
        }
        majorVer == 1 && minorVer == 9 && patchVer < 20 -> {
            disableLazyIrForCaches()
        }
        else -> {}
    }
}

private val KotlinNativeTarget.targetCacheKindPropertyName: String
    get() = "$PROJECT_CACHE_KIND_PROPERTY_NAME.${konanTarget.presetName}"
private fun KotlinNativeTarget.disableKotlinNativeCache() {
    if (project.hasProperty(targetCacheKindPropertyName)) {
        project.setProperty(targetCacheKindPropertyName, NONE_VALUE)
    } else {
        project.extensions.extraProperties.set(targetCacheKindPropertyName, NONE_VALUE)
    }
}

private fun KotlinNativeTarget.disableLazyIrForCaches() {
    compilations.configureEach { compilation ->
        compilation.kotlinOptions.freeCompilerArgs += listOf("-Xlazy-ir-for-caches=disable")
    }
}

private data class KotlinVersionNumbers(val major: Int, val minor: Int, val patch: Int)

private fun kotlinVersionNumbers(project: Project): KotlinVersionNumbers {
    val version = project.getKotlinPluginVersion()
    val m = Regex("(\\d+)\\.(\\d+)\\.(\\d+)").find(version) ?: error("Kotlin version has unexpected format: '$version'")
    val (_, majorPart, minorPart, patchPart) = m.groupValues
    return KotlinVersionNumbers(
        major = majorPart.toIntOrNull() ?: error("Could not parse major part '$majorPart' of Kotlin plugin version: '$version'"),
        minor = minorPart.toIntOrNull() ?: error("Could not parse minor part '$minorPart' of Kotlin plugin version: '$version'"),
        patch = patchPart.toIntOrNull() ?: error("Could not parse patch part '$patchPart' of Kotlin plugin version: '$version'"),
    )
}