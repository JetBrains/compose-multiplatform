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
import androidx.build.AndroidXExtension
import androidx.build.checkapi.ApiLocation
import androidx.build.checkapi.ApiViolationExclusions
import androidx.build.checkapi.getCurrentApiLocation
import androidx.build.checkapi.getRequiredCompatibilityApiLocation
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
            val dependency = dependencies.create("com.android:metalava:1.2.5-SNAPSHOT:shadow@jar")
            it.dependencies.add(dependency)
        }
    }

    fun Project.configureAndroidProjectForMetalava(
        library: LibraryExtension,
        extension: AndroidXExtension
    ) {
        afterEvaluate {
            if (!hasApiTasks(this, extension)) {
                return@afterEvaluate
            }

            library.libraryVariants.all { variant ->
                if (variant.name == Release.DEFAULT_PUBLISH_CONFIG) {
                    if (!hasApiFolder()) {
                        logger.info(
                            "Project $name doesn't have an api folder, ignoring API tasks."
                        )
                        return@all
                    }

                    val javaInputs = JavaCompileInputs.fromLibraryVariant(library, variant)
                    setupProject(this, javaInputs, extension)
                }
            }
        }
    }

    fun Project.configureJavaProjectForMetalava(
        extension: AndroidXExtension
    ) {
        afterEvaluate {
            if (!hasApiTasks(this, extension)) {
                return@afterEvaluate
            }
            if (!hasApiFolder()) {
                logger.info(
                    "Project $name doesn't have an api folder, ignoring API tasks."
                )
                return@afterEvaluate
            }

            val javaPluginConvention = convention.getPlugin<JavaPluginConvention>()
            val mainSourceSet = javaPluginConvention.sourceSets.getByName("main")
            val javaInputs = JavaCompileInputs.fromSourceSet(mainSourceSet, this)
            setupProject(this, javaInputs, extension)
        }
    }

    fun applyInputs(inputs: JavaCompileInputs, task: MetalavaTask) {
        task.sourcePaths = inputs.sourcePaths
        task.dependencyClasspath = inputs.dependencyClasspath
        task.bootClasspath = inputs.bootClasspath
    }

    fun setupProject(
        project: Project,
        javaCompileInputs: JavaCompileInputs,
        extension: AndroidXExtension
    ) {
        val metalavaConfiguration = project.createMetalavaConfiguration()

        // the api files whose file names contain the version of the library
        val libraryVersionApi = project.getCurrentApiLocation()
        // the api files whose file names contain "current.txt"
        val currentTxtApi = ApiLocation.fromPublicApiFile(File(
            libraryVersionApi.publicApiFile.parentFile, "current.txt"))

        // make sure to update current.txt if it wasn't previously planned to be updated
        val outputApiLocations: List<ApiLocation> =
            if (libraryVersionApi.publicApiFile.path.equals(currentTxtApi.publicApiFile.path)) {
                listOf(libraryVersionApi)
            } else {
                listOf(libraryVersionApi, currentTxtApi)
            }

        val builtApiLocation = ApiLocation.fromPublicApiFile(
            File(project.docsDir(), "release/${project.name}/current.txt"))

        var generateApi = project.tasks.create("generateApi", GenerateApiTask::class.java) { task ->
            task.group = "API"
            task.description = "Generates API files from source"
            task.apiLocation = builtApiLocation
            task.configuration = metalavaConfiguration
            task.generateRestrictedAPIs = extension.trackRestrictedAPIs
            task.dependsOn(metalavaConfiguration)
        }
        applyInputs(javaCompileInputs, generateApi)

        val checkApi =
            project.tasks.create("checkApi", CheckApiEquivalenceTask::class.java) { task ->
                task.group = "API"
                task.description = "Checks that the API generated from source code matches the " +
                        "checked in API file"
                task.builtApi = generateApi.apiLocation
                task.checkedInApis = outputApiLocations
                task.checkRestrictedAPIs = extension.trackRestrictedAPIs
                task.dependsOn(generateApi)
            }

        val lastReleasedApiFile = project.getRequiredCompatibilityApiLocation()
        if (lastReleasedApiFile != null) {
            val checkApiRelease = project.tasks.create(
                "checkApiRelease",
                CheckApiCompatibilityTask::class.java
            ) { task ->
                task.configuration = metalavaConfiguration
                task.referenceApi = lastReleasedApiFile
                task.exclusions = ApiViolationExclusions.fromApiLocation(libraryVersionApi)
                task.dependsOn(metalavaConfiguration)
                task.checkRestrictedAPIs = extension.trackRestrictedAPIs
            }
            applyInputs(javaCompileInputs, checkApiRelease)
            checkApi.dependsOn(checkApiRelease)

            val updateApiTrackingExceptions =
                project.tasks.create("ignoreApiChanges", IgnoreApiChangesTask::class.java) { task ->
                    task.configuration = metalavaConfiguration
                    task.referenceApi = checkApiRelease.referenceApi
                    task.exclusions = checkApiRelease.exclusions
                    task.processRestrictedAPIs = extension.trackRestrictedAPIs
                    task.intermediateExclusionsFile =
                            File(project.docsDir(), "release/${project.name}/api-changes.ignore")
                }
            applyInputs(javaCompileInputs, updateApiTrackingExceptions)
        }

        project.tasks.create("updateApi", UpdateApiTask::class.java) { task ->
            task.group = "API"
            task.description = "Updates the checked in API files to match source code API"
            task.inputApiLocation = generateApi.apiLocation
            task.outputApiLocations = checkApi.checkedInApis
            task.updateRestrictedAPIs = extension.trackRestrictedAPIs
            task.dependsOn(generateApi)
        }

        project.tasks.getByName("check").dependsOn(checkApi)
        project.rootProject.tasks.getByName(BUILD_ON_SERVER_TASK).dependsOn(checkApi)
    }
}
