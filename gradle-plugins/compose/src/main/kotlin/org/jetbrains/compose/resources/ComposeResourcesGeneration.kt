package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.IDEA_IMPORT_TASK_NAME
import org.jetbrains.compose.internal.IdeaImportTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.tooling.core.withClosure
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
    val resClassName = config.map { it.nameOfResClass }
    val makeAccessorsPublic = config.map { it.publicResClass }
    val packagingDir = config.getModuleResourcesDir(project)

    kotlinExtension.sourceSets.all { sourceSet ->
        if (sourceSet.name == resClassSourceSetName) {
            configureResClassGeneration(
                sourceSet,
                shouldGenerateCode,
                packageName,
                resClassName,
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
            resClassName,
            makeAccessorsPublic,
            packagingDir,
            generateModulePath
        )
    }

    configureResourceCollectorsGeneration(
        kotlinExtension,
        shouldGenerateCode,
        packageName,
        resClassName,
        makeAccessorsPublic
    )

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
    resClassName: Provider<String>,
    makeAccessorsPublic: Provider<Boolean>,
    packagingDir: Provider<File>,
    generateModulePath: Boolean
) {
    logger.info("Configure '$resClassName' class generation for ${resClassSourceSet.name}")

    val genTask = tasks.register(
        "generateComposeResClass",
        GenerateResClassTask::class.java
    ) { task ->
        task.packageName.set(packageName)
        task.resClassName.set(resClassName)
        task.makeAccessorsPublic.set(makeAccessorsPublic)
        task.codeDir.set(layout.buildDirectory.dir("$RES_GEN_DIR/kotlin/commonResClass"))

        if (generateModulePath) {
            task.packagingDir.set(packagingDir)
        }
        task.onlyIf { shouldGenerateCode.get() }
    }

    //register generated source set
    resClassSourceSet.kotlin.srcDir(
        genTask.zip(shouldGenerateCode) { task, flag ->
            if (flag) listOf(task.codeDir) else emptyList()
        }
    )
}

private fun Project.configureResourceAccessorsGeneration(
    sourceSet: KotlinSourceSet,
    resourcesDir: Provider<File>,
    shouldGenerateCode: Provider<Boolean>,
    packageName: Provider<String>,
    resClassName: Provider<String>,
    makeAccessorsPublic: Provider<Boolean>,
    packagingDir: Provider<File>,
    generateModulePath: Boolean
) {
    logger.info("Configure resource accessors generation for ${sourceSet.name}")

    val genTask = tasks.register(
        sourceSet.getResourceAccessorsGenerationTaskName(),
        GenerateResourceAccessorsTask::class.java
    ) { task ->
        task.packageName.set(packageName)
        task.resClassName.set(resClassName)
        task.sourceSetName.set(sourceSet.name)
        task.makeAccessorsPublic.set(makeAccessorsPublic)
        task.resDir.set(resourcesDir)
        task.codeDir.set(layout.buildDirectory.dir("$RES_GEN_DIR/kotlin/${sourceSet.name}ResourceAccessors"))
        task.disableResourceContentHashGeneration.set(ComposeProperties.disableResourceContentHashGeneration(providers))

        if (generateModulePath) {
            task.packagingDir.set(packagingDir)
        }
        task.onlyIf { shouldGenerateCode.get() }
    }

    //register generated source set
    sourceSet.kotlin.srcDir(
        genTask.zip(shouldGenerateCode) { task, flag ->
            if (flag) listOf(task.codeDir) else emptyList()
        }
    )
}

private fun KotlinSourceSet.getResourceAccessorsGenerationTaskName(): String {
    return "generateResourceAccessorsFor${this.name.uppercaseFirstChar()}"
}

//we have to generate actual resource collector functions for each leaf source set
private fun Project.configureResourceCollectorsGeneration(
    kotlinExtension: KotlinProjectExtension,
    shouldGenerateCode: Provider<Boolean>,
    packageName: Provider<String>,
    resClassName: Provider<String>,
    makeAccessorsPublic: Provider<Boolean>
) {
    if (kotlinExtension is KotlinMultiplatformExtension) {
        kotlinExtension.sourceSets
            .matching { it.name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME }
            .all { commonMainSourceSet ->
                configureExpectResourceCollectorsGeneration(
                    commonMainSourceSet,
                    shouldGenerateCode,
                    packageName,
                    resClassName,
                    makeAccessorsPublic
                )
            }

        kotlinExtension.targets.all { target ->
            if (target is KotlinAndroidTarget) {
                kotlinExtension.sourceSets.matching { it.name == "androidMain" }.all { androidMain ->
                    configureActualResourceCollectorsGeneration(
                        androidMain,
                        shouldGenerateCode,
                        packageName,
                        resClassName,
                        makeAccessorsPublic,
                        true
                    )
                }
            } else if (target !is KotlinMetadataTarget) {
                target.compilations.matching { it.name == KotlinCompilation.MAIN_COMPILATION_NAME }.all { compilation ->
                    configureActualResourceCollectorsGeneration(
                        compilation.defaultSourceSet,
                        shouldGenerateCode,
                        packageName,
                        resClassName,
                        makeAccessorsPublic,
                        true
                    )
                }
            }
        }
    } else if (kotlinExtension is KotlinSingleTargetExtension<*>) {
        //JVM only projects
        kotlinExtension.target.compilations
            .findByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            ?.let { compilation ->
                configureActualResourceCollectorsGeneration(
                    compilation.defaultSourceSet,
                    shouldGenerateCode,
                    packageName,
                    resClassName,
                    makeAccessorsPublic,
                    false
                )
            }
    }

}

private fun Project.configureExpectResourceCollectorsGeneration(
    sourceSet: KotlinSourceSet,
    shouldGenerateCode: Provider<Boolean>,
    packageName: Provider<String>,
    resClassName: Provider<String>,
    makeAccessorsPublic: Provider<Boolean>
) {
    logger.info("Configure expect resource collectors generation for ${sourceSet.name}")


    val genTask = tasks.register(
        "generateExpectResourceCollectorsFor${sourceSet.name.uppercaseFirstChar()}",
        GenerateExpectResourceCollectorsTask::class.java
    ) { task ->
        task.packageName.set(packageName)
        task.resClassName.set(resClassName)
        task.makeAccessorsPublic.set(makeAccessorsPublic)
        task.codeDir.set(layout.buildDirectory.dir("$RES_GEN_DIR/kotlin/${sourceSet.name}ResourceCollectors"))
        task.onlyIf { shouldGenerateCode.get() }
    }

    //register generated source set
    sourceSet.kotlin.srcDir(
        genTask.zip(shouldGenerateCode) { task, flag ->
            if (flag) listOf(task.codeDir) else emptyList()
        }
    )
}

private fun Project.configureActualResourceCollectorsGeneration(
    sourceSet: KotlinSourceSet,
    shouldGenerateCode: Provider<Boolean>,
    packageName: Provider<String>,
    resClassName: Provider<String>,
    makeAccessorsPublic: Provider<Boolean>,
    useActualModifier: Boolean
) {
    val taskName = "generateActualResourceCollectorsFor${sourceSet.name.uppercaseFirstChar()}"
    if (tasks.names.contains(taskName)) {
        logger.info("Actual resource collectors generation for ${sourceSet.name} is already configured")
        return
    }
    logger.info("Configure actual resource collectors generation for ${sourceSet.name}")

    val accessorDirs = project.files({
        val allSourceSets = sourceSet.withClosure { it.dependsOn }
        allSourceSets.mapNotNull { item ->
            val accessorsTaskName = item.getResourceAccessorsGenerationTaskName()
            if (tasks.names.contains(accessorsTaskName)) {
                tasks.named(accessorsTaskName, GenerateResourceAccessorsTask::class.java).map { it.codeDir }
            } else null
        }
    })

    val genTask = tasks.register(
        taskName,
        GenerateActualResourceCollectorsTask::class.java
    ) { task ->
        task.packageName.set(packageName)
        task.resClassName.set(resClassName)
        task.makeAccessorsPublic.set(makeAccessorsPublic)
        task.useActualModifier.set(useActualModifier)
        task.resourceAccessorDirs.from(accessorDirs)
        task.codeDir.set(layout.buildDirectory.dir("$RES_GEN_DIR/kotlin/${sourceSet.name}ResourceCollectors"))
        task.onlyIf { shouldGenerateCode.get() }
    }

    //register generated source set
    sourceSet.kotlin.srcDir(
        genTask.zip(shouldGenerateCode) { task, flag ->
            if (flag) listOf(task.codeDir) else emptyList()
        }
    )
}