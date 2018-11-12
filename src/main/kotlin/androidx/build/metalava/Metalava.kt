/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.metalava

import androidx.build.AndroidXPlugin.Companion.BUILD_ON_SERVER_TASK
import androidx.build.SupportLibraryExtension
import androidx.build.androidJarFile
import androidx.build.checkapi.getCurrentApiFile
import androidx.build.checkapi.getRequiredCompatibilityApiFile
import androidx.build.checkapi.hasApiFolder
import androidx.build.checkapi.hasApiTasks
import androidx.build.docsDir
import androidx.build.java.JavaCompileInputs
import androidx.build.Release
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.getPlugin
import java.io.File

object Metalava {
    private fun Project.createMetalavaConfiguration(): Configuration {
        return configurations.create("metalava") {
            val dependency = dependencies.create("com.android:metalava:1.1.2-SNAPSHOT:shadow@jar")
            it.dependencies.add(dependency)
        }
    }

    fun registerAndroidProject(
        project: Project,
        library: LibraryExtension,
        extension: SupportLibraryExtension
    ) {
        if (!hasApiTasks(project, extension)) {
            return
        }

        library.libraryVariants.all { variant ->
            if (variant.name == Release.DEFAULT_PUBLISH_CONFIG) {
                if (!project.hasApiFolder()) {
                    project.logger.info(
                        "Project ${project.name} doesn't have an api folder, ignoring API tasks.")
                    return@all
                }

                val javaInputs = JavaCompileInputs.fromLibraryVariant(library, variant)
                setupProject(project, javaInputs)
            }
        }
    }

    fun registerJavaProject(
        project: Project,
        extension: SupportLibraryExtension
    ) {
        if (!hasApiTasks(project, extension)) {
            return
        }
        if (!project.hasApiFolder()) {
            project.logger.info(
                    "Project ${project.name} doesn't have an api folder, ignoring API tasks.")
            return
        }

        val javaPluginConvention = project.convention.getPlugin<JavaPluginConvention>()
        val mainSourceSet = javaPluginConvention.sourceSets.getByName("main")
        val javaInputs = JavaCompileInputs.fromSourceSet(mainSourceSet, project)
        setupProject(project, javaInputs)
    }

    fun applyInputs(inputs: JavaCompileInputs, task: MetalavaTask) {
        task.sourcePaths = inputs.sourcePaths
        task.dependencyClasspath = inputs.dependencyClasspath
        task.bootClasspath = inputs.bootClasspath
    }

    fun setupProject(project: Project, javaCompileInputs: JavaCompileInputs) {
        val metalavaConfiguration = project.createMetalavaConfiguration()
        val currentApiFile = project.getCurrentApiFile()

        var generateApi = project.tasks.create("generateApi", GenerateApiTask::class.java) { task ->
            task.apiFile = File(project.docsDir(), "release/${project.name}/current.txt")
            task.configuration = metalavaConfiguration
            task.dependsOn(metalavaConfiguration)
        }
        applyInputs(javaCompileInputs, generateApi)

        val checkApi = project.tasks.create("checkApi", CheckApiEquivalenceTask::class.java) { task ->
            task.file1 = currentApiFile
            task.file2 = generateApi.apiFile
            task.dependsOn(generateApi)
        }

        val lastReleasedApiFile = project.getRequiredCompatibilityApiFile()
        if (lastReleasedApiFile != null) {
            val checkApiRelease = project.tasks.create("checkApiRelease", CheckApiCompatibilityTask::class.java) { task ->
                 task.configuration = metalavaConfiguration
                 task.apiTxtFile = lastReleasedApiFile
                 task.dependsOn(metalavaConfiguration)
             }
             applyInputs(javaCompileInputs, checkApiRelease)
             checkApi.dependsOn(checkApiRelease)
        }

        val updateApi = project.tasks.create("updateApi", UpdateApiTask::class.java) { task ->
            task.inputApiFile = generateApi.apiFile
            task.outputApiFile = currentApiFile
            task.dependsOn(generateApi)
        }

        project.tasks.getByName("check").dependsOn(checkApi)
        project.rootProject.tasks.getByName(BUILD_ON_SERVER_TASK).dependsOn(checkApi)

    }
}
