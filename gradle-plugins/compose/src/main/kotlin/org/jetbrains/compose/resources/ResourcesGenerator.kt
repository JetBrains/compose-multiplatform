package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

internal const val COMPOSE_RESOURCES_DIR = "composeResources"
private const val RES_GEN_DIR = "generated/compose/resourceGenerator"

internal fun Project.configureComposeResources() {
    pluginManager.withPlugin(KOTLIN_MPP_PLUGIN_ID) {
        val kotlinExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlinExtension.sourceSets.all { sourceSet ->
            val sourceSetName = sourceSet.name
            val composeResourcesPath = project.projectDir.resolve("src/$sourceSetName/$COMPOSE_RESOURCES_DIR")
            sourceSet.resources.srcDirs(composeResourcesPath)
            if (sourceSetName == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME) {
                configureResourceGenerator(composeResourcesPath, sourceSet)
            }
        }
    }
}

private fun Project.configureResourceGenerator(commonComposeResourcesDir: File, commonSourceSet: KotlinSourceSet) {
    val commonComposeResources = provider { commonComposeResourcesDir }
    val packageName = provider {
        buildString {
            val group = project.group.toString().asUnderscoredIdentifier()
            append(group)
            if (group.isNotEmpty()) append(".")
            append(project.name.lowercase())
            append(".generated.resources")
        }
    }

    fun buildDir(path: String) = layout.dir(layout.buildDirectory.map { File(it.asFile, path) })

    val resDir = layout.dir(commonComposeResources)

    //lazy check a dependency on the Resources library
    val shouldGenerateResourceAccessors: Provider<Boolean> = provider {
        if (ComposeProperties.alwaysGenerateResourceAccessors(providers).get()) {
            true
        } else {
            configurations
                .getByName(commonSourceSet.implementationConfigurationName)
                .allDependencies.any { dep ->
                    val depStringNotation = dep.let { "${it.group}:${it.name}:${it.version}" }
                    depStringNotation == ComposePlugin.CommonComponentsDependencies.resources
                }
        }
    }

    val genTask = tasks.register(
        "generateComposeResClass",
        GenerateResClassTask::class.java
    ) {
        it.packageName.set(packageName)
        it.resDir.set(resDir)
        it.codeDir.set(buildDir("$RES_GEN_DIR/kotlin"))
        it.onlyIf { shouldGenerateResourceAccessors.get() }
    }

    //register generated source set
    commonSourceSet.kotlin.srcDir(genTask.map { it.codeDir })

    //setup task execution during IDE import
    tasks.configureEach {
        if (it.name == "prepareKotlinIdeaImport") {
            it.dependsOn(genTask)
        }
    }

    val androidExtension = project.extensions.findByName("android")
    if (androidExtension != null) {
        configureAndroidResources(
            commonComposeResources,
            buildDir("$RES_GEN_DIR/androidFonts").map { it.asFile },
            shouldGenerateResourceAccessors
        )
    }
}
