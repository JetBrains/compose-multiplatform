package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import java.io.File


@OptIn(ComposeKotlinGradlePluginApi::class)
internal fun Project.configureKmpResources(
    kotlinExtension: KotlinProjectExtension,
    kmpResources: Any,
    config: Provider<ResourcesExtension>
) {
    kotlinExtension as KotlinMultiplatformExtension
    kmpResources as KotlinTargetResourcesPublication

    logger.info("Configure KMP resources")

    val commonMain = KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
    configureComposeResourcesGeneration(kotlinExtension, commonMain, config, true)

    //configure KMP resources publishing for each supported target
    kotlinExtension.targets
        .matching { target -> kmpResources.canPublishResources(target) }
        .all { target ->
            logger.info("Configure resources publication for '${target.targetName}' target")
            val packedResourceDir = config.getModuleResourcesDir(project)

            kmpResources.publishResourcesAsKotlinComponent(
                target,
                { sourceSet ->
                    KotlinTargetResourcesPublication.ResourceRoot(
                        getPreparedComposeResourcesDir(sourceSet),
                        emptyList(),
                        //for android target exclude fonts
                        if (target is KotlinAndroidTarget) listOf("**/font*/*") else emptyList()
                    )
                },
                packedResourceDir
            )

            if (target is KotlinAndroidTarget) {
                //for android target publish fonts in assets
                logger.info("Configure fonts relocation for '${target.targetName}' target")
                kmpResources.publishInAndroidAssets(
                    target,
                    { sourceSet ->
                        KotlinTargetResourcesPublication.ResourceRoot(
                            getPreparedComposeResourcesDir(sourceSet),
                            listOf("**/font*/*"),
                            emptyList()
                        )
                    },
                    packedResourceDir
                )
            }
        }

    //add all resolved resources for browser and native compilations
    val platformsForSetupCompilation = listOf(KotlinPlatformType.native, KotlinPlatformType.js, KotlinPlatformType.wasm)
    kotlinExtension.targets
        .matching { target -> target.platformType in platformsForSetupCompilation }
        .all { target: KotlinTarget ->
            val allResources = kmpResources.resolveResources(target)
            target.compilations.all { compilation ->
                if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME) {
                    configureResourcesForCompilation(compilation, allResources)
                }
            }
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