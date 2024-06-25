package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.internal.IDEA_IMPORT_TASK_NAME
import org.jetbrains.compose.internal.IdeaImportTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

internal fun Project.configureComposeResourcesGeneration(
    kotlinExtension: KotlinProjectExtension,
    resClassSourceSetName: String,
    config: Provider<ResourcesExtension>,
    generateModulePath: Boolean
) {
    logger.info("Configure compose resources generation")

    //lazy check a dependency on the Resources library
    val shouldGenerateCode = config.map {
        when (it.generateResClass) {
            ResourcesExtension.ResourceClassGeneration.Auto -> {
                configurations.run {
                    val commonSourceSet = kotlinExtension.sourceSets.getByName(resClassSourceSetName)
                    //because the implementation configuration doesn't extend the api in the KGP ¯\_(ツ)_/¯
                    getByName(commonSourceSet.implementationConfigurationName).allDependencies +
                            getByName(commonSourceSet.apiConfigurationName).allDependencies
                }.any { dep ->
                    val depStringNotation = dep.let { "${it.group}:${it.name}:${it.version}" }
                    depStringNotation == ComposePlugin.CommonComponentsDependencies.resources
                }
            }

            ResourcesExtension.ResourceClassGeneration.Always -> true
            ResourcesExtension.ResourceClassGeneration.Never -> false
        }
    }
    val packageName = config.getResourcePackage(project)
    val makeAccessorsPublic = config.map { it.publicResClass }
    val packagingDir = config.getModuleResourcesDir(project)

    kotlinExtension.sourceSets.all { sourceSet ->
        if (sourceSet.name == resClassSourceSetName) {
            configureResClassGeneration(
                sourceSet,
                shouldGenerateCode,
                packageName,
                makeAccessorsPublic,
                packagingDir,
                generateModulePath
            )
        }

        //common resources must be converted (XML -> CVR)
        val preparedResourcesTask = registerPrepareComposeResourcesTask(sourceSet, config)
        val preparedResources = preparedResourcesTask.flatMap { it.outputDir.asFile }
        configureResourceAccessorsGeneration(
            sourceSet,
            preparedResources,
            shouldGenerateCode,
            packageName,
            makeAccessorsPublic,
            packagingDir,
            generateModulePath
        )
    }

    //setup task execution during IDE import
    tasks.configureEach { importTask ->
        if (importTask.name == IDEA_IMPORT_TASK_NAME) {
            importTask.dependsOn(tasks.withType(IdeaImportTask::class.java))
        }
    }
}

private fun Project.configureResClassGeneration(
    resClassSourceSet: KotlinSourceSet,
    shouldGenerateCode: Provider<Boolean>,
    packageName: Provider<String>,
    makeAccessorsPublic: Provider<Boolean>,
    packagingDir: Provider<File>,
    generateModulePath: Boolean
) {
    logger.info("Configure Res class generation for ${resClassSourceSet.name}")

    val genTask = tasks.register(
        "generateComposeResClass",
        GenerateResClassTask::class.java
    ) { task ->
        task.packageName.set(packageName)
        task.shouldGenerateCode.set(shouldGenerateCode)
        task.makeAccessorsPublic.set(makeAccessorsPublic)
        task.codeDir.set(layout.buildDirectory.dir("$RES_GEN_DIR/kotlin/commonResClass"))

        if (generateModulePath) {
            task.packagingDir.set(packagingDir)
        }
    }

    //register generated source set
    resClassSourceSet.kotlin.srcDir(genTask.map { it.codeDir })
}

private fun Project.configureResourceAccessorsGeneration(
    sourceSet: KotlinSourceSet,
    resourcesDir: Provider<File>,
    shouldGenerateCode: Provider<Boolean>,
    packageName: Provider<String>,
    makeAccessorsPublic: Provider<Boolean>,
    packagingDir: Provider<File>,
    generateModulePath: Boolean
) {
    logger.info("Configure resource accessors generation for ${sourceSet.name}")

    val genTask = tasks.register(
        "generateResourceAccessorsFor${sourceSet.name.uppercaseFirstChar()}",
        GenerateResourceAccessorsTask::class.java
    ) { task ->
        task.packageName.set(packageName)
        task.sourceSetName.set(sourceSet.name)
        task.shouldGenerateCode.set(shouldGenerateCode)
        task.makeAccessorsPublic.set(makeAccessorsPublic)
        task.resDir.set(resourcesDir)
        task.codeDir.set(layout.buildDirectory.dir("$RES_GEN_DIR/kotlin/${sourceSet.name}ResourceAccessors"))

        if (generateModulePath) {
            task.packagingDir.set(packagingDir)
        }
    }

    //register generated source set
    sourceSet.kotlin.srcDir(genTask.map { it.codeDir })
}