package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

internal fun Project.configureGenerationComposeResClass(
    commonComposeResourcesDir: Provider<File>,
    commonSourceSet: KotlinSourceSet,
    config: Provider<ResourcesExtension>,
    generateModulePath: Boolean
) {
    logger.info("Configure accessors for '${commonSourceSet.name}'")

    //lazy check a dependency on the Resources library
    val shouldGenerateResClass = config.map {
        when (it.generateResClass) {
            ResourcesExtension.ResourceClassGeneration.Auto -> {
                configurations.run {
                    //because the implementation configuration doesn't extend the api in the KGP ¯\_(ツ)_/¯
                    getByName(commonSourceSet.implementationConfigurationName).allDependencies +
                            getByName(commonSourceSet.apiConfigurationName).allDependencies
                }.any { dep ->
                    val depStringNotation = dep.let { "${it.group}:${it.name}:${it.version}" }
                    depStringNotation == ComposePlugin.CommonComponentsDependencies.resources
                }
            }

            ResourcesExtension.ResourceClassGeneration.Always -> {
                true
            }

            ResourcesExtension.ResourceClassGeneration.Never -> {
                false
            }
        }
    }

    val genTask = tasks.register(
        "generateComposeResClass",
        GenerateResClassTask::class.java
    ) { task ->
        task.packageName.set(config.getResourcePackage(project))
        task.shouldGenerateResClass.set(shouldGenerateResClass)
        task.makeResClassPublic.set(config.map { it.publicResClass })
        task.resDir.set(commonComposeResourcesDir)
        task.codeDir.set(layout.buildDirectory.dir("$RES_GEN_DIR/kotlin").map { it.asFile })

        if (generateModulePath) {
            task.moduleDir.set(config.getModuleResourcesDir(project))
        }
    }

    //register generated source set
    commonSourceSet.kotlin.srcDir(genTask.map { it.codeDir })

    //setup task execution during IDE import
    tasks.configureEach {
        if (it.name == "prepareKotlinIdeaImport") {
            it.dependsOn(genTask)
        }
    }
}