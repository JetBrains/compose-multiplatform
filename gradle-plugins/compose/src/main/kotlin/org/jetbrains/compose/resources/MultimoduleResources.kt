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
    val platformsForSkip = listOf(
        KotlinPlatformType.common, KotlinPlatformType.androidJvm
    )
    kotlinExtension.targets
        .matching { target -> target.platformType !in platformsForSkip }
        .all { target ->
            target.compilations.all { compilation ->
                logger.info("Configure ${compilation.name} resources for '${target.targetName}' target")
                val compilationResources = files({
                    compilation.allKotlinSourceSets.map { sourceSet -> getPreparedComposeResourcesDir(sourceSet) }
                })
                val assembleResTask = registerTask<AssembleTargetResourcesTask>(
                    name = "assemble${target.targetName.uppercaseFirstChar()}${compilation.name.uppercaseFirstChar()}Resources"
                ) {
                    resourceDirectories.setFrom(compilationResources)
                    relativeResourcePlacement.set(moduleIsolationDirectory)
                    outputDirectory.set(
                        layout.buildDirectory.dir(
                            "$RES_GEN_DIR/assembledResources/${target.targetName}${compilation.name.uppercaseFirstChar()}"
                        )
                    )
                }
                val allCompilationResources = assembleResTask.flatMap { it.outputDirectory.asFile }

                //For Native/Js/Wasm main resources:
                // 1) we have to configure new Kotlin component publication
                // 2) we have to collect all transitive main resources
                if (
                    target.platformType in platformsForSetupKmpResources
                    && compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME
                ) {
                    //TODO temporary API misuse. will be changed on the KMP side
                    //https://youtrack.jetbrains.com/issue/KT-70909
                    val kmpResourceRoot = KotlinTargetResourcesPublication.ResourceRoot(
                        allCompilationResources,
                        emptyList(),
                        emptyList()
                    )
                    val kmpEmptyPath = provider { File("") }
                    logger.info("Configure KMP component publication for '${compilation.target.targetName}'")
                    kmpResources.publishResourcesAsKotlinComponent(
                        target,
                        { kmpResourceRoot },
                        kmpEmptyPath
                    )

                    val allResources = kmpResources.resolveResources(target)
                    logger.info("Collect resolved ${compilation.name} resources for '${compilation.target.targetName}'")
                    configureResourcesForCompilation(compilation, allResources)
                } else {
                    configureResourcesForCompilation(compilation, allCompilationResources)
                }
            }
        }

    //configure ANDROID resources
    onAgpApplied {
        configureAndroidComposeResources(moduleIsolationDirectory)
        fixAndroidLintTaskDependencies()
    }
}

private fun Project.configureResourcesForCompilation(
    compilation: KotlinCompilation<*>,
    directoryWithAllResourcesForCompilation: Provider<File>
) {
    compilation.defaultSourceSet.resources.srcDir(directoryWithAllResourcesForCompilation)

    //JS packaging requires explicit dependency
    if (compilation is KotlinJsCompilation) {
        tasks.named(compilation.processResourcesTaskName).configure { processResourcesTask ->
            processResourcesTask.dependsOn(directoryWithAllResourcesForCompilation)
        }
    }
}