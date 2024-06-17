package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.util.GradleVersion
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.KOTLIN_JVM_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties

internal const val COMPOSE_RESOURCES_DIR = "composeResources"
internal const val RES_GEN_DIR = "generated/compose/resourceGenerator"
private const val KMP_RES_EXT = "multiplatformResourcesPublication"
private const val MIN_GRADLE_VERSION_FOR_KMP_RESOURCES = "7.6"
private val androidPluginIds = listOf(
    "com.android.application",
    "com.android.library"
)

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
        configureKmpResources(kotlinExtension, extraProperties.get(KMP_RES_EXT)!!, config)
        onAgpApplied { fixAndroidLintTaskDependencies() }
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

        val commonMain = KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
        configureComposeResources(kotlinExtension, commonMain, config)

        onAgpApplied {
            configureAndroidComposeResources(kotlinExtension)
            fixAndroidLintTaskDependencies()
        }
    }

    configureSyncIosComposeResources(kotlinExtension)
}

private fun Project.onAgpApplied(block: () -> Unit) {
    androidPluginIds.forEach { pluginId ->
        plugins.withId(pluginId) {
            block()
        }
    }
}

private fun Project.onKotlinJvmApplied(config: Provider<ResourcesExtension>) {
    val kotlinExtension = project.extensions.getByType(KotlinProjectExtension::class.java)
    val main = SourceSet.MAIN_SOURCE_SET_NAME
    configureComposeResources(kotlinExtension, main, config)
}

private fun Project.configureComposeResources(
    kotlinExtension: KotlinProjectExtension,
    resClassSourceSetName: String,
    config: Provider<ResourcesExtension>
) {
    logger.info("Configure compose resources")
    configureComposeResourcesGeneration(kotlinExtension, resClassSourceSetName, config, false)

    // mark prepared resources as sourceSet.resources
    // 1) it automatically packs the resources to JVM jars
    // 2) it configures the webpack to use the resources
    // 3) for native targets we will use source set resources to pack them into the final app. see IosResources.kt
    // 4) for the android it DOESN'T pack resources! we copy resources to assets in AndroidResources.kt
    kotlinExtension.sourceSets.all { sourceSet ->
        // the HACK is here because KGP copy androidMain java resources to Android target
        // if the resources were registered in the androidMain source set before the target declaration
        afterEvaluate {
            sourceSet.resources.srcDirs(getPreparedComposeResourcesDir(sourceSet))
        }
    }
}
