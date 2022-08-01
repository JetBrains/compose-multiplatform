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
import androidx.build.addToCheckTask
import androidx.build.checkapi.ApiBaselinesLocation
import androidx.build.checkapi.ApiLocation
import androidx.build.checkapi.getRequiredCompatibilityApiLocation
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.java.JavaCompileInputs
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.android.build.gradle.tasks.ProcessLibraryManifest
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

object MetalavaTasks {

    fun setupProject(
        project: Project,
        javaCompileInputs: JavaCompileInputs,
        extension: AndroidXExtension,
        processManifest: ProcessLibraryManifest? = null,
        baselinesApiLocation: ApiBaselinesLocation,
        builtApiLocation: ApiLocation,
        outputApiLocations: List<ApiLocation>
    ) {
        val metalavaClasspath = project.getMetalavaClasspath()

        // Policy: If the artifact belongs to an atomic (e.g. same-version) group, we don't enforce
        // binary compatibility for APIs annotated with @RestrictTo(LIBRARY_GROUP). This is
        // implemented by excluding APIs with this annotation from the restricted API file.
        val generateRestrictToLibraryGroupAPIs = !extension.mavenGroup!!.requireSameVersion
        val generateApi = project.tasks.register("generateApi", GenerateApiTask::class.java) {
            task ->

            task.group = "API"
            task.description = "Generates API files from source"
            task.apiLocation.set(builtApiLocation)
            task.metalavaClasspath.from(metalavaClasspath)
            task.generateRestrictToLibraryGroupAPIs = generateRestrictToLibraryGroupAPIs
            task.baselines.set(baselinesApiLocation)
            task.targetsJavaConsumers = extension.targetsJavaConsumers
            processManifest?.let {
                task.manifestPath.set(processManifest.manifestOutputFile)
            }
            applyInputs(javaCompileInputs, task)
            AffectedModuleDetector.configureTaskGuard(task)
            // If we will be updating the api lint baselines, then we should do that before
            // using it to validate the generated api
            task.mustRunAfter("updateApiLintBaseline")
        }

        // Policy: If the artifact has previously been released, e.g. has a beta or later API file
        // checked in, then we must verify "release compatibility" against the work-in-progress
        // API file.
        var checkApiRelease: TaskProvider<CheckApiCompatibilityTask>? = null
        var ignoreApiChanges: TaskProvider<IgnoreApiChangesTask>? = null
        project.getRequiredCompatibilityApiLocation()?.let { lastReleasedApiFile ->
            checkApiRelease = project.tasks.register(
                "checkApiRelease",
                CheckApiCompatibilityTask::class.java
            ) { task ->
                task.metalavaClasspath.from(metalavaClasspath)
                task.referenceApi.set(lastReleasedApiFile)
                task.baselines.set(baselinesApiLocation)
                task.api.set(builtApiLocation)
                task.dependencyClasspath = javaCompileInputs.dependencyClasspath
                task.bootClasspath = javaCompileInputs.bootClasspath
                task.cacheEvenIfNoOutputs()
                task.dependsOn(generateApi)
                AffectedModuleDetector.configureTaskGuard(task)
            }

            ignoreApiChanges = project.tasks.register(
                "ignoreApiChanges",
                IgnoreApiChangesTask::class.java
            ) { task ->
                task.metalavaClasspath.from(metalavaClasspath)
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
            task.metalavaClasspath.from(metalavaClasspath)
            task.baselines.set(baselinesApiLocation)
            task.targetsJavaConsumers.set(extension.targetsJavaConsumers)
            processManifest?.let {
                task.manifestPath.set(processManifest.manifestOutputFile)
            }
            applyInputs(javaCompileInputs, task)
        }

        // Policy: All changes to API surfaces for which compatibility is enforced must be
        // explicitly confirmed by running the updateApi task. To enforce this, the implementation
        // checks the "work-in-progress" built API file against the checked in current API file.
        val checkApi =
            project.tasks.register("checkApi", CheckApiEquivalenceTask::class.java) { task ->
                task.group = "API"
                task.description = "Checks that the API generated from source code matches the " +
                    "checked in API file"
                task.builtApi.set(generateApi.flatMap { it.apiLocation })
                task.cacheEvenIfNoOutputs()
                task.checkedInApis.set(outputApiLocations)
                task.dependsOn(generateApi)
                checkApiRelease?.let {
                    task.dependsOn(checkApiRelease)
                }
                AffectedModuleDetector.configureTaskGuard(task)
            }

        val regenerateOldApis = project.tasks.register(
            "regenerateOldApis",
            RegenerateOldApisTask::class.java
        ) { task ->
            task.group = "API"
            task.description = "Regenerates historic API .txt files using the " +
                "corresponding prebuilt and the latest Metalava"
            task.generateRestrictToLibraryGroupAPIs = generateRestrictToLibraryGroupAPIs
        }

        // ignoreApiChanges depends on the output of this task for the "last released" API
        // surface. Make sure it always runs *after* the regenerateOldApis task.
        ignoreApiChanges?.configure { it.mustRunAfter(regenerateOldApis) }

        // checkApiRelease validates the output of this task, so make sure it always runs
        // *after* the regenerateOldApis task.
        checkApiRelease?.configure { it.mustRunAfter(regenerateOldApis) }

        val updateApi = project.tasks.register("updateApi", UpdateApiTask::class.java) { task ->
            task.group = "API"
            task.description = "Updates the checked in API files to match source code API"
            task.inputApiLocation.set(generateApi.flatMap { it.apiLocation })
            task.outputApiLocations.set(checkApi.flatMap { it.checkedInApis })
            task.forceUpdate = project.hasProperty("force")
            task.dependsOn(generateApi)

            // If a developer (accidentally) makes a non-backwards compatible change to an API,
            // the developer will want to be informed of it as soon as possible. So, whenever a
            // developer updates an API, if backwards compatibility checks are enabled in the
            // library, then we want to check that the changes are backwards compatible.
            checkApiRelease?.let { task.dependsOn(it) }
            AffectedModuleDetector.configureTaskGuard(task)
        }

        // ignoreApiChanges depends on the output of this task for the "current" API surface.
        // Make sure it always runs *after* the updateApi task.
        ignoreApiChanges?.configure { it.mustRunAfter(updateApi) }

        project.tasks.register("regenerateApis") { task ->
            task.group = "API"
            task.description = "Regenerates current and historic API .txt files using the " +
                "corresponding prebuilt and the latest Metalava, then updates API ignore files"
            task.dependsOn(regenerateOldApis)
            task.dependsOn(updateApi)
            ignoreApiChanges?.let { task.dependsOn(it) }
        }

        project.addToCheckTask(checkApi)
        project.addToBuildOnServer(checkApi)
    }

    private fun applyInputs(inputs: JavaCompileInputs, task: MetalavaTask) {
        task.sourcePaths = inputs.sourcePaths
        task.dependsOn(inputs.sourcePaths)
        task.dependencyClasspath = inputs.dependencyClasspath
        task.bootClasspath = inputs.bootClasspath
    }
}
