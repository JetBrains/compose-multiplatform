package org.jetbrains.compose.resources

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.jetbrains.compose.internal.KOTLIN_JVM_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.io.File


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
    plugins.withId(KOTLIN_MPP_PLUGIN_ID) { onKgpApplied(config) }
    plugins.withId(KOTLIN_JVM_PLUGIN_ID) { onKotlinJvmApplied(config) }
}

private fun Project.onKgpApplied(config: Provider<ResourcesExtension>) {
    val kotlinExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

    //common resources must be converted (XML -> CVR)
    val commonMain = KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
    val preparedCommonResources = prepareCommonResources(commonMain)

    val hasKmpResources = extraProperties.has(KMP_RES_EXT)
    val currentGradleVersion = GradleVersion.current()
    val minGradleVersion = GradleVersion.version(MIN_GRADLE_VERSION_FOR_KMP_RESOURCES)
    val kmpResourcesAreAvailable = hasKmpResources && currentGradleVersion >= minGradleVersion

    if (kmpResourcesAreAvailable) {
        configureKmpResources(kotlinExtension, extraProperties.get(KMP_RES_EXT)!!, preparedCommonResources, config)
    } else {
        if (!hasKmpResources) logger.info(
            """
                Compose resources publication requires Kotlin Gradle Plugin >= 2.0
                Current Kotlin Gradle Plugin is ${KotlinVersion.CURRENT}
            """.trimIndent()
        )
        if (currentGradleVersion < minGradleVersion) logger.info(
            """
                Compose resources publication requires Gradle >= $MIN_GRADLE_VERSION_FOR_KMP_RESOURCES
                Current Gradle is ${currentGradleVersion.version}
            """.trimIndent()
        )

        configureComposeResources(kotlinExtension, commonMain, preparedCommonResources, config)

        //when applied AGP then configure android resources
        androidPluginIds.forEach { pluginId ->
            plugins.withId(pluginId) {
                val androidExtension = project.extensions.getByType(BaseExtension::class.java)
                configureAndroidComposeResources(kotlinExtension, androidExtension, preparedCommonResources)
            }
        }
    }
}

private fun Project.onKotlinJvmApplied(config: Provider<ResourcesExtension>) {
    val kotlinExtension = project.extensions.getByType(KotlinProjectExtension::class.java)
    val main = SourceSet.MAIN_SOURCE_SET_NAME
    val preparedCommonResources = prepareCommonResources(main)
    configureComposeResources(kotlinExtension, main, preparedCommonResources, config)
}

//common resources must be converted (XML -> CVR)
private fun Project.prepareCommonResources(commonSourceSetName: String): Provider<File> {
    val preparedResourcesTask = registerPrepareComposeResourcesTask(
        project.projectDir.resolve("src/$commonSourceSetName/$COMPOSE_RESOURCES_DIR"),
        layout.buildDirectory.dir("$RES_GEN_DIR/preparedResources/$commonSourceSetName/$COMPOSE_RESOURCES_DIR")
    )
    return preparedResourcesTask.flatMap { it.outputDir }
}

// sourceSet.resources.srcDirs doesn't work for Android targets.
// Android resources should be configured separately
private fun Project.configureComposeResources(
    kotlinExtension: KotlinProjectExtension,
    commonSourceSetName: String,
    preparedCommonResources: Provider<File>,
    config: Provider<ResourcesExtension>
) {
    logger.info("Configure compose resources")
    kotlinExtension.sourceSets.all { sourceSet ->
        val sourceSetName = sourceSet.name
        val resourcesDir = project.projectDir.resolve("src/$sourceSetName/$COMPOSE_RESOURCES_DIR")
        if (sourceSetName == commonSourceSetName) {
            sourceSet.resources.srcDirs(preparedCommonResources)
            configureGenerationComposeResClass(preparedCommonResources, sourceSet, config, false)
        } else {
            sourceSet.resources.srcDirs(resourcesDir)
        }
    }
}
