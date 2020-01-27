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

import androidx.build.AndroidXExtension
import androidx.build.addToBuildOnServer
import androidx.build.checkapi.ApiLocation
import androidx.build.checkapi.ApiViolationBaselines
import androidx.build.checkapi.getApiLocation
import androidx.build.checkapi.getRequiredCompatibilityApiLocation
import androidx.build.checkapi.hasApiFolder
import androidx.build.checkapi.hasApiTasks
import androidx.build.defaultPublishVariant
import androidx.build.java.JavaCompileInputs
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.LibraryVariant
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.getPlugin
import java.io.File

const val CREATE_STUB_API_JAR_TASK = "createStubApiJar"

object MetalavaTasks {

    fun Project.configureAndroidProjectForMetalava(
        library: LibraryExtension,
        extension: AndroidXExtension
    ) {
        afterEvaluate {
            if (!hasApiTasks(this, extension)) {
                return@afterEvaluate
            }

            library.defaultPublishVariant { variant ->
                if (!hasApiFolder()) {
                    logger.info(
                        "Project $name doesn't have an api folder, ignoring API tasks."
                    )
                    return@defaultPublishVariant
                }

                val javaInputs = JavaCompileInputs.fromLibraryVariant(library, variant, project)
                setupProject(this, javaInputs, extension)
                // TODO(aurimas): reenable this when kotlin stubs generation is working.
                // setupStubs(this, javaInputs, variant)
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
        task.sourcePaths = inputs.sourcePaths.files
        task.dependsOn(inputs.sourcePaths)
        task.dependencyClasspath = inputs.dependencyClasspath
        task.bootClasspath = inputs.bootClasspath
    }

    fun setupProject(
        project: Project,
        javaCompileInputs: JavaCompileInputs,
        extension: AndroidXExtension
    ) {
        val metalavaConfiguration = project.getMetalavaConfiguration()

        // the api files whose file names contain the version of the library
        val libraryVersionApi = project.getApiLocation()
        // the api files whose file names contain "current.txt"
        val currentTxtApi = ApiLocation.fromPublicApiFile(File(
            libraryVersionApi.publicApiFile.parentFile, "current.txt"))

        // make sure to update current.txt if it wasn't previously planned to be updated
        val outputApiLocations =
            if (libraryVersionApi.publicApiFile.path.equals(currentTxtApi.publicApiFile.path)) {
                listOf(libraryVersionApi)
            } else {
                listOf(libraryVersionApi, currentTxtApi)
            }

        val builtApiLocation = ApiLocation.fromPublicApiFile(
            File(project.buildDir, "api/current.txt"))

        val baselines = ApiViolationBaselines.fromApiLocation(libraryVersionApi)

        val generateRestrictToLibraryGroupAPIs = !extension.mavenGroup!!.requireSameVersion
        val generateApi = project.tasks.register("generateApi", GenerateApiTask::class.java) {
                task ->
            task.group = "API"
            task.description = "Generates API files from source"
            task.apiLocation.set(builtApiLocation)
            task.configuration = metalavaConfiguration
            task.generateRestrictToLibraryGroupAPIs = generateRestrictToLibraryGroupAPIs
            task.baselines.set(baselines)
            task.dependsOn(metalavaConfiguration)
            applyInputs(javaCompileInputs, task)
        }

        var checkApiRelease: TaskProvider<CheckApiCompatibilityTask>? = null

        project.getRequiredCompatibilityApiLocation()?.let { lastReleasedApiFile ->
            checkApiRelease = project.tasks.register(
                "checkApiRelease",
                CheckApiCompatibilityTask::class.java
            ) { task ->
                task.configuration = metalavaConfiguration
                task.referenceApi.set(lastReleasedApiFile)
                task.baselines.set(baselines)
                task.dependsOn(metalavaConfiguration)
                task.api.set(builtApiLocation)
                task.dependencyClasspath = javaCompileInputs.dependencyClasspath
                task.bootClasspath = javaCompileInputs.bootClasspath
                task.dependsOn(generateApi)
            }

            project.tasks.register("ignoreApiChanges", IgnoreApiChangesTask::class.java) { task ->
                task.configuration = metalavaConfiguration
                task.referenceApi.set(checkApiRelease!!.flatMap { it.referenceApi })
                task.baselines.set(checkApiRelease!!.flatMap { it.baselines })
                task.api.set(builtApiLocation)
                task.dependencyClasspath = javaCompileInputs.dependencyClasspath
                task.bootClasspath = javaCompileInputs.bootClasspath
                task.dependsOn(generateApi)
            }
        }

        project.tasks.register(
            "updateApiLintBaseline",
            UpdateApiLintBaselineTask::class.java
        ) { task ->
            task.configuration = metalavaConfiguration
            task.baselines.set(baselines)
            applyInputs(javaCompileInputs, task)
        }

        val checkApi =
            project.tasks.register("checkApi", CheckApiEquivalenceTask::class.java) { task ->
                task.group = "API"
                task.description = "Checks that the API generated from source code matches the " +
                        "checked in API file"
                task.builtApi.set(generateApi.flatMap { it.apiLocation })
                task.checkedInApis.set(outputApiLocations)
                task.dependsOn(generateApi)
                checkApiRelease?.let {
                    task.dependsOn(checkApiRelease)
                }
            }

        val regenerateOldApis = project.tasks.register("regenerateOldApis",
                RegenerateOldApisTask::class.java) { task ->
            task.group = "API"
            task.description = "Regenerates historic API .txt files using the " +
                "corresponding prebuilt and the latest Metalava"
            task.generateRestrictToLibraryGroupAPIs = generateRestrictToLibraryGroupAPIs
            // if checkApiRelease and regenerateOldApis both run, then checkApiRelease must
            // be the second one run of the two (because checkApiRelease validates
            // files modified by regenerateOldApis)
            val cr = checkApiRelease
            if (cr != null) {
                cr.get().mustRunAfter(task)
            }
        }

        val updateApi = project.tasks.register("updateApi", UpdateApiTask::class.java) { task ->
            task.group = "API"
            task.description = "Updates the checked in API files to match source code API"
            task.inputApiLocation.set(generateApi.flatMap { it.apiLocation })
            task.outputApiLocations.set(checkApi.flatMap { it.checkedInApis })
            task.dependsOn(generateApi)
            if (checkApiRelease != null) {
                // If a developer (accidentally) makes a non-backwards compatible change to an
                // api, the developer will want to be informed of it as soon as possible.
                // So, whenever a developer updates an api, if backwards compatibility checks are
                // enabled in the library, then we want to check that the changes are backwards
                // compatible
                task.dependsOn(checkApiRelease)
            }
        }

        project.tasks.register("regenerateApis") { task ->
            task.group = "API"
            task.description = "Regenerates current and historic API .txt files using the " +
                "corresponding prebuilt and the latest Metalava"
            task.dependsOn(regenerateOldApis)
            task.dependsOn(updateApi)
        }

        project.tasks.named("check").configure {
            it.dependsOn(checkApi)
        }
        project.addToBuildOnServer(checkApi)
    }

    @Suppress("unused")
    private fun setupStubs(
        project: Project,
        javaCompileInputs: JavaCompileInputs,
        variant: LibraryVariant
    ) {
        if (hasKotlinCode(project, variant)) return

        val apiStubsDirectory = File(project.buildDir, "stubs/api")
        val docsStubsDirectory = File(project.buildDir, "stubs/docs")
        val generateApiStubClasses = project.tasks.register(
            "generateApiStubClasses",
            GenerateApiStubClassesTask::class.java
        ) { task ->
            task.apiStubsDirectory.set(apiStubsDirectory)
            task.docStubsDirectory.set(docsStubsDirectory)
            task.configuration = project.getMetalavaConfiguration()
            applyInputs(javaCompileInputs, task)
        }

        val apiStubClassesDirectory = File(project.buildDir, "stubs/api_classes")
        val compileStubClasses = project.tasks.register(
            "compileApiStubClasses",
            JavaCompile::class.java
        ) { task ->
            @Suppress("DEPRECATION") val compileTask = variant.javaCompile
            task.source = project.files(apiStubsDirectory).asFileTree
            task.destinationDir = apiStubClassesDirectory

            task.classpath = compileTask.classpath
            task.options.compilerArgs = compileTask.options.compilerArgs
            task.options.bootstrapClasspath = compileTask.options.bootstrapClasspath
            task.sourceCompatibility = compileTask.sourceCompatibility
            task.targetCompatibility = compileTask.targetCompatibility
            task.dependsOn(generateApiStubClasses)
            task.dependsOn(compileTask)
        }

        val apiStubsJar = project.tasks.register(
            CREATE_STUB_API_JAR_TASK,
            Zip::class.java
        ) { task ->
            task.from(apiStubClassesDirectory)
            task.destinationDirectory.set(project.buildDir)
            task.archiveFileName.set("api.jar")
            task.dependsOn(compileStubClasses)
        }

        project.addToBuildOnServer(apiStubsJar)
        /*
            TODO: Enable packaging api.jar inside aars.
            project.tasks.withType(BundleAar::class.java) { task ->
                task.dependsOn(packageStubs)
                task.from(File(project.buildDir, "api.jar"))
            }
         */
    }

    private fun hasKotlinCode(project: Project, variant: LibraryVariant): Boolean {
        return project.files(variant.sourceSets.flatMap { it.javaDirectories })
            .asFileTree
            .files
            .filter { it.extension == "kt" }
            .isNotEmpty()
    }
}
