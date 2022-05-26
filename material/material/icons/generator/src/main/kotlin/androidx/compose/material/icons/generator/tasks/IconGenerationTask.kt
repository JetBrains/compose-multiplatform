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

import androidx.compose.material.icons.generator.Icon
import androidx.compose.material.icons.generator.IconProcessor
import com.android.build.gradle.LibraryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File
import java.util.Locale

/**
 * Base [org.gradle.api.Task] for tasks relating to icon generation.
 */
@CacheableTask
abstract class IconGenerationTask : DefaultTask() {

    /**
     * Directory containing raw drawables. These icons will be processed to generate programmatic
     * representations.
     */
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputDirectory
    val allIconsDirectory =
        project.rootProject.project(GeneratorProject).projectDir.resolve("raw-icons")

    /**
     * Specific theme to generate icons for, or null to generate all
     */
    @Optional
    @Input
    var themeName: String? = null

    /**
     * Specific icon directories to use in this task
     */
    @Internal
    fun getIconDirectories(): List<File> {
        val themeName = themeName
        if (themeName != null) {
            return listOf(allIconsDirectory.resolve(themeName))
        } else {
            return allIconsDirectory.listFiles()!!.filter { it.isDirectory }
        }
    }

    /**
     * Checked-in API file for the generator module, where we will track all the generated icons
     */
    @PathSensitive(PathSensitivity.NONE)
    @InputFile
    val expectedApiFile =
        project.rootProject.project(GeneratorProject).projectDir.resolve("api/icons.txt")

    /**
     * Root build directory for this task, where outputs will be placed into.
     */
    @OutputDirectory
    lateinit var buildDirectory: File

    /**
     * Generated API file that will be placed in the build directory. This can be copied manually
     * to [expectedApiFile] to confirm that API changes were intended.
     */
    @get:OutputFile
    val generatedApiFile: File
        get() = buildDirectory.resolve("api/icons.txt")

    /**
     * @return a list of all processed [Icon]s from [getIconDirectories].
     */
    fun loadIcons(): List<Icon> {
        // material-icons-core loads and verifies all of the icons from all of the themes:
        // both that all icons are present in all themes, and also that no icons have been removed.
        // So, when we're loading just one theme, we don't need to verify it
        val verifyApi = themeName == null
        return IconProcessor(
            getIconDirectories(),
            expectedApiFile,
            generatedApiFile,
            verifyApi
        ).process()
    }

    @get:OutputDirectory
    val generatedSrcMainDirectory: File
        get() = buildDirectory.resolve(GeneratedSrcMain)

    @get:OutputDirectory
    val generatedSrcAndroidTestDirectory: File
        get() = buildDirectory.resolve(GeneratedSrcAndroidTest)

    @get:OutputDirectory
    val generatedResourceDirectory: File
        get() = buildDirectory.resolve(GeneratedResource)

    /**
     * The action for this task
     */
    @TaskAction
    abstract fun run()

    companion object {
        /**
         * Registers the core [project]. The core project contains only the icons defined in
         * [androidx.compose.material.icons.generator.CoreIcons], and no tests.
         */
        @JvmStatic
        fun registerCoreIconProject(
            project: Project,
            libraryExtension: LibraryExtension,
            isMpp: Boolean
        ) {
            if (isMpp) {
                CoreIconGenerationTask.register(project, null)
            } else {
                libraryExtension.libraryVariants.all { variant ->
                    CoreIconGenerationTask.register(project, variant)
                }
            }
        }

        /**
         * Registers the extended [project]. The core project contains all icons except for the
         * icons defined in [androidx.compose.material.icons.generator.CoreIcons], as well as a
         * bitmap comparison test for every icon in both the core and extended project.
         */
        @JvmStatic
        fun registerExtendedIconThemeProject(
            project: Project,
            libraryExtension: LibraryExtension,
            isMpp: Boolean
        ) {
            if (isMpp) {
                ExtendedIconGenerationTask.register(project, null)
            } else {
                libraryExtension.libraryVariants.all { variant ->
                    ExtendedIconGenerationTask.register(project, variant)
                }
            }

            // b/175401659 - disable lint as it takes a long time, and most errors should
            // be caught by lint on material-icons-core anyway
            project.afterEvaluate {
                project.tasks.named("lintAnalyzeDebug") { t ->
                    t.enabled = false
                }
                project.tasks.named("lintDebug") { t ->
                    t.enabled = false
                }
            }
        }

        @JvmStatic
        fun registerExtendedIconMainProject(
            project: Project,
            libraryExtension: LibraryExtension
        ) {
            libraryExtension.testVariants.all { variant ->
                IconTestingGenerationTask.register(project, variant)
            }
        }

        const val GeneratedSrcMain = "src/commonMain/kotlin"

        const val GeneratedSrcAndroidTest = "src/androidAndroidTest/kotlin"

        const val GeneratedResource = "generatedIcons/res"
    }
}

// Path to the generator project
private const val GeneratorProject = ":compose:material:material:icons:generator"

/**
 * Registers a new [T] in [this], and sets [IconGenerationTask.buildDirectory] depending on
 * [variant].
 *
 * @param variant the [com.android.build.gradle.api.BaseVariant] to associate this task with, or
 * `null` if this task does not change between variants.
 * @return a [Pair] of the created [TaskProvider] of [T] of [IconGenerationTask], and the [File]
 * for the directory that files will be generated to
 */
@Suppress("DEPRECATION") // BaseVariant
fun <T : IconGenerationTask> Project.registerGenerationTask(
    taskName: String,
    taskClass: Class<T>,
    variant: com.android.build.gradle.api.BaseVariant? = null
): Pair<TaskProvider<T>, File> {
    val variantName = variant?.name ?: "allVariants"

    val themeName = if (project.name.contains("material-icons-extended-")) {
        project.name.replace("material-icons-extended-", "")
    } else {
        null
    }

    val buildDirectory = project.buildDir.resolve("generatedIcons/$variantName")

    return tasks.register("$taskName${variantName.capitalize(Locale.getDefault())}", taskClass) {
        it.themeName = themeName
        it.buildDirectory = buildDirectory
    } to buildDirectory
}

fun Project.getMultiplatformSourceSet(name: String): KotlinSourceSet {
    val sourceSet = project.multiplatformExtension!!.sourceSets.find { it.name == name }
    return requireNotNull(sourceSet) {
        "No source sets found matching $name"
    }
}

private val Project.multiplatformExtension
    get() = extensions.findByType(KotlinMultiplatformExtension::class.java)
