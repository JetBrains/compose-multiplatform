package org.jetbrains.compose.resources

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.LintModelWriterTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.jetbrains.compose.ComposePlugin
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

    val hasKmpResources = extraProperties.has(KMP_RES_EXT)
    val currentGradleVersion = GradleVersion.current()
    val minGradleVersion = GradleVersion.version(MIN_GRADLE_VERSION_FOR_KMP_RESOURCES)
    val kmpResourcesAreAvailable = hasKmpResources && currentGradleVersion >= minGradleVersion

    if (kmpResourcesAreAvailable) {
        configureKmpResources(kotlinExtension, extraProperties.get(KMP_RES_EXT)!!, config)
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

        val commonMain = KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
        configureComposeResources(kotlinExtension, commonMain, config)

        //when applied AGP then configure android resources
        androidPluginIds.forEach { pluginId ->
            plugins.withId(pluginId) {
                val androidExtension = project.extensions.getByType(BaseExtension::class.java)
                configureAndroidComposeResources(kotlinExtension, androidExtension)


                /*
                  There is a dirty fix for the problem:

                  Reason: Task ':generateDemoDebugUnitTestLintModel' uses this output of task ':generateResourceAccessorsForAndroidUnitTest' without declaring an explicit or implicit dependency. This can lead to incorrect results being produced, depending on what order the tasks are executed.

                  Possible solutions:
                    1. Declare task ':generateResourceAccessorsForAndroidUnitTest' as an input of ':generateDemoDebugUnitTestLintModel'.
                    2. Declare an explicit dependency on ':generateResourceAccessorsForAndroidUnitTest' from ':generateDemoDebugUnitTestLintModel' using Task#dependsOn.
                    3. Declare an explicit dependency on ':generateResourceAccessorsForAndroidUnitTest' from ':generateDemoDebugUnitTestLintModel' using Task#mustRunAfter.
                 */
                tasks.matching {
                    it is AndroidLintAnalysisTask || it is LintModelWriterTask
                }.configureEach {
                    it.mustRunAfter(tasks.withType(GenerateResourceAccessorsTask::class.java))
                }
            }
        }
    }

    configureSyncIosComposeResources(kotlinExtension)
}

private fun Project.onKotlinJvmApplied(config: Provider<ResourcesExtension>) {
    val kotlinExtension = project.extensions.getByType(KotlinProjectExtension::class.java)
    val main = SourceSet.MAIN_SOURCE_SET_NAME
    configureComposeResources(kotlinExtension, main, config)
}

// sourceSet.resources.srcDirs doesn't work for Android targets.
// Android resources should be configured separately
private fun Project.configureComposeResources(
    kotlinExtension: KotlinProjectExtension,
    resClassSourceSetName: String,
    config: Provider<ResourcesExtension>
) {
    logger.info("Configure compose resources")
    configureComposeResourcesGeneration(kotlinExtension, resClassSourceSetName, config)

    kotlinExtension.sourceSets.all { sourceSet ->
        sourceSet.resources.srcDirs(sourceSet.getPreparedComposeResourcesDir())
    }
}
