package org.jetbrains.compose.resources

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import java.io.File

//configure multi-module resources (with publishing and module isolation)
internal fun Project.configureMultimoduleResources(
    kotlinExtension: KotlinMultiplatformExtension,
    config: Provider<ResourcesExtension>
) {
    logger.info("Configure multi-module compose resources")

    val commonMain = KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
    configureComposeResourcesGeneration(kotlinExtension, commonMain, config, true)

    val moduleIsolationDirectory = config.getModuleResourcesDir(project)

    kotlinExtension.targets
        .matching { target -> !target.skipResourcesConfiguration() }
        .all { target -> configureTargetResources(target, moduleIsolationDirectory) }


    //configure ANDROID resources
    onAgpApplied { agpId ->
        configureAndroidComposeResources(agpId, moduleIsolationDirectory)
        fixAndroidLintTaskDependencies()
    }
}

//configure java multi-module resources (with module isolation)
internal fun Project.configureJvmOnlyResources(
    kotlinExtension: KotlinJvmProjectExtension,
    config: Provider<ResourcesExtension>
) {
    logger.info("Configure java-only compose resources")

    val main = SourceSet.MAIN_SOURCE_SET_NAME
    configureComposeResourcesGeneration(kotlinExtension, main, config, true)

    val moduleIsolationDirectory = config.getModuleResourcesDir(project)
    val javaTarget = kotlinExtension.target

    configureTargetResources(javaTarget, moduleIsolationDirectory)
}

private fun Project.configureTargetResources(
    target: KotlinTarget,
    moduleIsolationDirectory: Provider<File>
) {
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

        if (
            target.platformType in platformsForSetupKmpResources
            && compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME
        ) {
            configureKmpResources(compilation, allCompilationResources)
        } else {
            configureResourcesForCompilation(compilation, allCompilationResources)
        }
    }
}

private fun KotlinTarget.skipResourcesConfiguration(): Boolean = when {
    this is KotlinMetadataTarget -> true

    //android resources should be configured via AGP
    this is KotlinAndroidTarget -> true

    //new AGP library target
    this.isMultiplatformAndroidTarget() -> true

    else -> false
}

@Suppress("UnstableApiUsage")
private fun KotlinTarget.isMultiplatformAndroidTarget(): Boolean = try {
    this is KotlinMultiplatformAndroidLibraryTarget
} catch (e: NoClassDefFoundError) {
    false
}

private val platformsForSetupKmpResources = listOf(
    KotlinPlatformType.native, KotlinPlatformType.js, KotlinPlatformType.wasm
)

@OptIn(ComposeKotlinGradlePluginApi::class)
private fun Project.configureKmpResources(
    compilation: KotlinCompilation<*>,
    allCompilationResources: Provider<File>
) {
    require(compilation.platformType in platformsForSetupKmpResources)
    val kmpResources = extraProperties.get(KMP_RES_EXT) as KotlinTargetResourcesPublication

    //For Native/Js/Wasm main resources:
    // 1) we have to configure new Kotlin component publication
    // 2) we have to collect all transitive main resources

    //TODO temporary API misuse. will be changed on the KMP side
    //https://youtrack.jetbrains.com/issue/KT-70909
    val target = compilation.target
    val kmpEmptyPath = provider { File("") }
    val emptyDir = layout.buildDirectory.dir("$RES_GEN_DIR/emptyResourcesDir").map { it.asFile }
    logger.info("Configure KMP component publication for '${compilation.target.targetName}'")
    kmpResources.publishResourcesAsKotlinComponent(
        target,
        { kotlinSourceSet ->
            if (kotlinSourceSet == compilation.defaultSourceSet) {
                KotlinTargetResourcesPublication.ResourceRoot(allCompilationResources, emptyList(), emptyList())
            } else {
                KotlinTargetResourcesPublication.ResourceRoot(emptyDir, emptyList(), emptyList())
            }
        },
        kmpEmptyPath
    )

    val allResources = kmpResources.resolveResources(target)
    logger.info("Collect resolved ${compilation.name} resources for '${compilation.target.targetName}'")
    configureResourcesForCompilation(compilation, allResources)
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