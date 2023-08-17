/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.internal

import org.gradle.api.Project
import org.jetbrains.compose.ComposeMultiplatformBuildService
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.compose.internal.utils.KGPPropertyProvider
import org.jetbrains.compose.internal.utils.configureEachWithType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName

private const val PROJECT_CACHE_KIND_PROPERTY_NAME = "kotlin.native.cacheKind"
private const val COMPOSE_NATIVE_MANAGE_CACHE_KIND = "compose.kotlin.native.manageCacheKind"
private const val NONE_VALUE = "none"

private val SUPPORTED_NATIVE_TARGETS = setOf(
    KonanTarget.IOS_ARM32,
    KonanTarget.IOS_X64,
    KonanTarget.IOS_ARM64,
    KonanTarget.IOS_SIMULATOR_ARM64,
    KonanTarget.MACOS_X64,
    KonanTarget.MACOS_ARM64,
)

internal val SUPPORTED_NATIVE_CACHE_KIND_PROPERTIES =
    SUPPORTED_NATIVE_TARGETS.map { it.targetCacheKindPropertyName } +
        PROJECT_CACHE_KIND_PROPERTY_NAME

internal fun Project.configureNativeCompilerCaching() {
    if (findProperty(COMPOSE_NATIVE_MANAGE_CACHE_KIND) == "false") return

    plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
        mppExt.targets.configureEachWithType<KotlinNativeTarget> {
            if (konanTarget in SUPPORTED_NATIVE_TARGETS) {
                checkExplicitCacheKind()
                disableKotlinNativeCache()
            }
        }
    }
}

private fun KotlinNativeTarget.checkExplicitCacheKind() {
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
                ComposeMultiplatformBuildService
                    .getInstance(project)
                    .warnOnceAfterBuild(
                        explicitCacheKindWarningMessage(cacheKindProperty, value, provider)
                    )
                return
            }
        }
    }
}

private fun explicitCacheKindWarningMessage(
    cacheKindProperty: String,
    value: String,
    provider: KGPPropertyProvider
) = """
    |Warning: '$cacheKindProperty' is explicitly set to '$value'.
    |Compose Multiplatform Gradle plugin manages this property automatically based on a Kotlin compiler version being used.
    |In future versions of Compose Multiplatform this warning will become an error.
    |  * Recommended action: remove explicit '$cacheKindProperty=$value' from ${provider.location}. 
    |  * Alternative action: disable cache kind management by adding '$COMPOSE_NATIVE_MANAGE_CACHE_KIND=false' to your 'gradle.properties'.
""".trimMargin()


private val KotlinNativeTarget.targetCacheKindPropertyName: String
    get() = konanTarget.targetCacheKindPropertyName

private val KonanTarget.targetCacheKindPropertyName: String
    get() = "$PROJECT_CACHE_KIND_PROPERTY_NAME.${presetName}"

private fun KotlinNativeTarget.disableKotlinNativeCache() {
    val existingValue = project.findProperty(targetCacheKindPropertyName)?.toString()
    if (NONE_VALUE.equals(existingValue, ignoreCase = true)) return

    if (targetCacheKindPropertyName in project.properties) {
        project.setProperty(targetCacheKindPropertyName, NONE_VALUE)
    } else {
        project.extensions.extraProperties.set(targetCacheKindPropertyName, NONE_VALUE)
    }
}