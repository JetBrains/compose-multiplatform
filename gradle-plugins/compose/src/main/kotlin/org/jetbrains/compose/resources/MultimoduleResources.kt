package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import java.io.File

//configure multimodule resources (with publishing and module isolation)
@OptIn(ComposeKotlinGradlePluginApi::class)
internal fun Project.configureMultimoduleResources(
    kotlinExtension: KotlinProjectExtension,
    kmpResources: Any,
    config: Provider<ResourcesExtension>
) {
    kotlinExtension as KotlinMultiplatformExtension
    kmpResources as KotlinTargetResourcesPublication

    logger.info("Configure KMP resources")

    val commonMain = KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
    configureComposeResourcesGeneration(kotlinExtension, commonMain, config, true)

    val moduleIsolationDirectory = config.getModuleResourcesDir(project)

    val platformsForSetupKmpResources = listOf(
        KotlinPlatformType.native, KotlinPlatformType.js, KotlinPlatformType.wasm
    )
    kotlinExtension.targets
        .matching { target -> target.platformType != KotlinPlatformType.androidJvm }
        .all { target ->
            target.compilations.all { compilation ->
                if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME) {
                    logger.info("Configure resources publication for '${target.targetName}' target")
                    val mainResources = files({
                        compilation.allKotlinSourceSets.map { sourceSet -> getPreparedComposeResourcesDir(sourceSet) }
                    })

                    val assembleResTask = registerTask<AssembleTargetResourcesTask>(
                        name = "assemble${target.targetName.uppercaseFirstChar()}MainResources"
                    ) {
                        resourceDirectories.setFrom(mainResources)
                        relativeResourcePlacement.set(moduleIsolationDirectory)
                        outputDirectory.set(layout.buildDirectory.dir("$RES_GEN_DIR/assembledResources/${target.targetName}Main"))
                    }

                    if (target.platformType in platformsForSetupKmpResources) {
                        //configure NATIVE/JS/WASM resources
                        //TODO temporary API misuse. will be changed on the KMP side
                        //https://youtrack.jetbrains.com/issue/KT-70909
                        val kmpResourceRoot = KotlinTargetResourcesPublication.ResourceRoot(
                            assembleResTask.flatMap { it.outputDirectory.asFile },
                            emptyList(),
                            emptyList()
                        )
                        val kmpEmptyPath = provider { File("") }
                        kmpResources.publishResourcesAsKotlinComponent(
                            target,
                            { kmpResourceRoot },
                            kmpEmptyPath
                        )

                        val allResources = kmpResources.resolveResources(target)
                        configureResourcesForCompilation(compilation, allResources)
                    } else if (target.platformType == KotlinPlatformType.jvm) {
                        //configure JVM resources
                        compilation.defaultSourceSet.resources.srcDirs(assembleResTask)
                    }
                }
            }
        }

    //configure ANDROID resources
    onAgpApplied {
        configureAndroidComposeResources(moduleIsolationDirectory)
        fixAndroidLintTaskDependencies()
    }
}

/**
 * Add resolved resources to a kotlin compilation to include it into a resulting platform artefact
 * It is required for JS and Native targets.
 * For JVM and Android it works automatically via jar files
 */
private fun Project.configureResourcesForCompilation(
    compilation: KotlinCompilation<*>,
    directoryWithAllResourcesForCompilation: Provider<File>
) {
    logger.info("Add all resolved resources to '${compilation.target.targetName}' target '${compilation.name}' compilation")
    compilation.defaultSourceSet.resources.srcDir(directoryWithAllResourcesForCompilation)

    //JS packaging requires explicit dependency
    if (compilation is KotlinJsCompilation) {
        tasks.named(compilation.processResourcesTaskName).configure { processResourcesTask ->
            processResourcesTask.dependsOn(directoryWithAllResourcesForCompilation)
        }
    }
}