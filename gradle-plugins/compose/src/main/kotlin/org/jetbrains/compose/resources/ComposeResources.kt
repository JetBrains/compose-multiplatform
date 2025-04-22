package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.KOTLIN_JVM_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.extraProperties

internal const val COMPOSE_RESOURCES_DIR = "composeResources"
internal const val RES_GEN_DIR = "generated/compose/resourceGenerator"
internal const val KMP_RES_EXT = "multiplatformResourcesPublication"
private const val MIN_GRADLE_VERSION_FOR_KMP_RESOURCES = "7.6"
private const val AGP_APP_ID = "com.android.application"
private const val AGP_LIB_ID = "com.android.library"
internal const val AGP_KMP_LIB_ID = "com.android.kotlin.multiplatform.library"

internal fun Project.configureComposeResources(extension: ResourcesExtension) {
    val config = provider { extension }
    plugins.withId(KOTLIN_MPP_PLUGIN_ID) { onKgpApplied(config, it as KotlinBasePlugin) }
    plugins.withId(KOTLIN_JVM_PLUGIN_ID) { onKotlinJvmApplied(config) }
}

private fun Project.onKgpApplied(config: Provider<ResourcesExtension>, kgp: KotlinBasePlugin) {
    val kotlinExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

    val hasKmpResources = extraProperties.has(KMP_RES_EXT)
    val currentGradleVersion = GradleVersion.current()
    val minGradleVersion = GradleVersion.version(MIN_GRADLE_VERSION_FOR_KMP_RESOURCES)
    val disableMultimoduleResources = ComposeProperties.disableMultimoduleResources(providers).get()
    val kmpResourcesAreAvailable = !disableMultimoduleResources && hasKmpResources && currentGradleVersion >= minGradleVersion

    if (kmpResourcesAreAvailable) {
        configureMultimoduleResources(kotlinExtension, config)
        configureXCFrameworkComposeResources(kotlinExtension, kgp)
    } else {
        if (!disableMultimoduleResources) {
            if (!hasKmpResources) logger.info(
                """
                    Compose resources publication requires Kotlin Gradle Plugin >= 2.0
                    Current Kotlin Gradle Plugin is ${kgp.pluginVersion}
                """.trimIndent()
            )
            if (currentGradleVersion < minGradleVersion) logger.info(
                """
                    Compose resources publication requires Gradle >= $MIN_GRADLE_VERSION_FOR_KMP_RESOURCES
                    Current Gradle is ${currentGradleVersion.version}
                """.trimIndent()
            )
        }

        configureSinglemoduleResources(kotlinExtension, config)
    }

    configureSyncIosComposeResources(kotlinExtension)
}

internal fun Project.onKotlinJvmApplied(config: Provider<ResourcesExtension>) {
    val kotlinExtension = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
    configureJvmOnlyResources(kotlinExtension, config)
}

internal fun Project.onAgpApplied(block: (pluginId: String) -> Unit) {
    listOf(AGP_APP_ID, AGP_LIB_ID, AGP_KMP_LIB_ID).forEach { pluginId ->
        plugins.withId(pluginId) { block(pluginId) }
    }
}
