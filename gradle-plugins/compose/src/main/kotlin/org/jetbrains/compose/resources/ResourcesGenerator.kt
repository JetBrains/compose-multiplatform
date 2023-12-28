package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

internal const val COMPOSE_RESOURCES_DIR = "composeResources"
private const val RES_GEN_DIR = "generated/compose/resourceGenerator"

internal fun Project.configureResourceGenerator() {
    val kotlinExtension = project.extensions.getByType(KotlinProjectExtension::class.java)
    val commonSourceSet = kotlinExtension.sourceSets.findByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME) ?: return
    val commonResourcesDir = provider { commonSourceSet.resources.sourceDirectories.first() }

    val packageName = provider {
        buildString {
            val group = project.group.toString().asUnderscoredIdentifier()
            append(group)
            if (group.isNotEmpty()) append(".")
            append("generated.resources")
        }
    }

    fun buildDir(path: String) = layout.dir(layout.buildDirectory.map { File(it.asFile, path) })

    val resDir = layout.dir(commonResourcesDir.map { it.resolve(COMPOSE_RESOURCES_DIR) })

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
            commonResourcesDir,
            buildDir("$RES_GEN_DIR/androidFonts").map { it.asFile },
            shouldGenerateResourceAccessors
        )
    }
}

