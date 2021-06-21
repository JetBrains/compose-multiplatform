/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.icons.generator.tasks

import androidx.compose.material.icons.generator.CoreIcons
import androidx.compose.material.icons.generator.IconWriter
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

/**
 * Task responsible for converting core icons from xml to a programmatic representation.
 */
@CacheableTask
open class CoreIconGenerationTask : IconGenerationTask() {
    override fun run() =
        IconWriter(loadIcons()).generateTo(generatedSrcMainDirectory) { it in CoreIcons }

    companion object {
        /**
         * Registers [CoreIconGenerationTask] in [project].
         */
        fun register(project: Project, variant: BaseVariant? = null) {
            val (task, buildDirectory) = project.registerGenerationTask(
                "generateCoreIcons",
                CoreIconGenerationTask::class.java,
                variant
            )
            // Multiplatform
            if (variant == null) {
                registerIconGenerationTask(project, task, buildDirectory)
            }
            // AGP
            else variant.registerIconGenerationTask(project, task, buildDirectory)
        }
    }
}

/**
 * Task responsible for converting extended icons from xml to a programmatic representation.
 */
@CacheableTask
open class ExtendedIconGenerationTask : IconGenerationTask() {
    override fun run() =
        IconWriter(loadIcons()).generateTo(generatedSrcMainDirectory) { it !in CoreIcons }

    companion object {
        /**
         * Registers [ExtendedIconGenerationTask] in [project]. (for use with mpp)
         */
        fun register(project: Project, variant: BaseVariant? = null) {
            val (task, buildDirectory) = project.registerGenerationTask(
                "generateExtendedIcons",
                ExtendedIconGenerationTask::class.java,
                variant
            )
            // Multiplatform
            if (variant == null) {
                registerIconGenerationTask(project, task, buildDirectory)
            }
            // AGP
            else variant.registerIconGenerationTask(project, task, buildDirectory)
        }

        /**
         * Registers the icon generation task just for source jar generation, and not for
         * compilation. This is temporarily needed since we manually parallelize compilation in
         * material-icons-extended for the AGP build. When we remove that parallelization code,
         * we can remove this too.
         */
        @JvmStatic
        fun registerSourceJarOnly(project: Project, variant: BaseVariant) {
            // Setup the source jar task if this is the release variant
            if (variant.name == "release") {
                val (task, buildDirectory) = project.registerGenerationTask(
                    "generateExtendedIcons",
                    ExtendedIconGenerationTask::class.java,
                    variant
                )
                val generatedSrcMainDirectory = buildDirectory.resolve(GeneratedSrcMain)
                project.addToSourceJar(generatedSrcMainDirectory, task)
            }
        }
    }
}

/**
 * Helper to register [task] that outputs to [buildDirectory] as the Kotlin source generating
 * task for [project].
 */
private fun registerIconGenerationTask(
    project: Project,
    task: TaskProvider<*>,
    buildDirectory: File
) {
    val sourceSet = project.getMultiplatformSourceSet(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
    val generatedSrcMainDirectory = buildDirectory.resolve(IconGenerationTask.GeneratedSrcMain)
    sourceSet.kotlin.srcDir(project.files(generatedSrcMainDirectory).builtBy(task))
    project.addToSourceJar(generatedSrcMainDirectory, task)
}

/**
 * Helper to register [task] as the java source generating task that outputs to [buildDirectory].
 */
private fun BaseVariant.registerIconGenerationTask(
    project: Project,
    task: TaskProvider<*>,
    buildDirectory: File
) {
    val generatedSrcMainDirectory = buildDirectory.resolve(IconGenerationTask.GeneratedSrcMain)
    registerJavaGeneratingTask(task, generatedSrcMainDirectory)
    // Setup the source jar task if this is the release variant
    if (name == "release") {
        project.addToSourceJar(generatedSrcMainDirectory, task)
    }
}

/**
 * Adds the contents of [buildDirectory] to the source jar generated for this [Project] by [task]
 */
// TODO: b/191485164 remove when AGP lets us get generated sources from a TestedExtension or
// similar, then we can just add generated sources in SourceJarTaskHelper for all projects,
// instead of needing one-off support here.
private fun Project.addToSourceJar(buildDirectory: File, task: TaskProvider<*>) {
    afterEvaluate {
        val sourceJar = tasks.named("sourceJarRelease", Jar::class.java)
        sourceJar.configure {
            // Generating source jars requires the generation task to run first. This shouldn't
            // be needed for the MPP build because we use builtBy to set up the dependency
            // (https://github.com/gradle/gradle/issues/17250) but the path is different for AGP,
            // so we will still need this for the AGP build.
            it.dependsOn(task)
            it.from(buildDirectory)
        }
    }
}
