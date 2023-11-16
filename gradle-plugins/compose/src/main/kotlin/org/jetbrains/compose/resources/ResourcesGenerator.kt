package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.jetbrains.compose.experimental.uikit.tasks.SyncComposeResourcesForIosTask
import org.jetbrains.compose.internal.utils.dependsOn
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

private const val COMPOSE_RESOURCES_DIR = "composeRes"
private const val RES_GEN_DIR = "generated/compose/resourceGenerator"

internal fun Project.configureResourceGenerator() {
    val kotlinExtension = project.extensions.getByType(KotlinProjectExtension::class.java)
    val commonSourceSet = kotlinExtension.sourceSets.findByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME) ?: return
    val commonResourcesDir = provider { commonSourceSet.resources.sourceDirectories.first() }

    val packageName = provider {
        buildString {
            val group = project.group.toString()
            append(group)
            if (group.isNotEmpty()) append(".")
            append("generated.resources")
        }
    }

    fun buildDir(path: String) = layout.dir(layout.buildDirectory.map { File(it.asFile, path) })

    val resDir = layout.dir(commonResourcesDir.map { it.resolve(COMPOSE_RESOURCES_DIR) })

    val genTask = tasks.register("generateComposeResClass", GenerateResClassTask::class.java) {
        it.packageName.set(packageName)
        it.resDir.set(resDir)
        it.codeDir.set(buildDir("$RES_GEN_DIR/kotlin"))
        it.indexDir.set(buildDir("$RES_GEN_DIR/resources"))
    }

    //register generated source set
    commonSourceSet.kotlin.srcDir(genTask.map { it.codeDir })
    commonSourceSet.resources.srcDir(genTask.map { it.indexDir })

    //setup task execution during IDE import
    tasks.named("prepareKotlinIdeaImport").dependsOn(genTask)

    tasks.withType(SyncComposeResourcesForIosTask::class.java).all {
        it.dependsOn(genTask)
    }

    val androidExtension = project.extensions.findByName("android")
    if (androidExtension != null) {
        configureAndroidResources(
            commonResourcesDir,
            genTask.flatMap { it.indexDir.asFile },
            buildDir("$RES_GEN_DIR/androidFonts").map { it.asFile }
        )
    }
}

