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

package androidx.build.resources

import androidx.build.AndroidXImplPlugin.Companion.TASK_GROUP_API
import androidx.build.addToBuildOnServer
import androidx.build.addToCheckTask
import androidx.build.checkapi.ApiLocation
import androidx.build.checkapi.getRequiredCompatibilityApiLocation
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.metalava.UpdateApiTask
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import org.gradle.api.Project
import java.util.Locale

object ResourceTasks {
    private const val GENERATE_RESOURCE_API_TASK = "generateResourceApi"
    private const val CHECK_RESOURCE_API_RELEASE_TASK = "checkResourceApiRelease"
    private const val CHECK_RESOURCE_API_TASK = "checkResourceApi"
    private const val UPDATE_RESOURCE_API_TASK = "updateResourceApi"

    fun setupProject(
        project: Project,
        variantName: String,
        builtApiLocation: ApiLocation,
        outputApiLocations: List<ApiLocation>
    ) {
        @OptIn(ExperimentalStdlibApi::class)
        val packageResTask = project.tasks
            .named(
                "package${variantName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                }}Resources"
            )
        val builtApiFile = packageResTask.flatMap { task ->
            (task as com.android.build.gradle.tasks.MergeResources).publicFile
        }

        val outputApiFiles = outputApiLocations.map { location ->
            location.resourceFile
        }

        val generateResourceApi = project.tasks.register(
            GENERATE_RESOURCE_API_TASK,
            GenerateResourceApiTask::class.java
        ) { task ->
            task.group = "API"
            task.description = "Generates resource API files from source"
            task.builtApi.set(builtApiFile)
            task.apiLocation.set(builtApiLocation)
            AffectedModuleDetector.configureTaskGuard(task)
        }

        // Policy: If the artifact has previously been released, e.g. has a beta or later API file
        // checked in, then we must verify "release compatibility" against the work-in-progress
        // API file.
        val checkResourceApiRelease = project.getRequiredCompatibilityApiLocation()?.let {
            lastReleasedApiFile ->
            project.tasks.register(
                CHECK_RESOURCE_API_RELEASE_TASK,
                CheckResourceApiReleaseTask::class.java
            ) { task ->
                task.referenceApiFile.set(lastReleasedApiFile.resourceFile)
                task.apiLocation.set(generateResourceApi.flatMap { it.apiLocation })
                // Since apiLocation isn't a File, we have to manually set up the dependency.
                task.dependsOn(generateResourceApi)
                task.cacheEvenIfNoOutputs()
                AffectedModuleDetector.configureTaskGuard(task)
            }
        }

        // Policy: All changes to API surfaces for which compatibility is enforced must be
        // explicitly confirmed by running the updateApi task. To enforce this, the implementation
        // checks the "work-in-progress" built API file against the checked in current API file.
        val checkResourceApi = project.tasks.register(
            CHECK_RESOURCE_API_TASK,
            CheckResourceApiTask::class.java
        ) { task ->
            task.group = TASK_GROUP_API
            task.description = "Checks that the resource API generated from source matches the " +
                "checked in resource API file"
            task.apiLocation.set(generateResourceApi.flatMap { it.apiLocation })
            // Since apiLocation isn't a File, we have to manually set up the dependency.
            task.dependsOn(generateResourceApi)
            task.cacheEvenIfNoOutputs()
            task.checkedInApiFiles.set(outputApiFiles)
            checkResourceApiRelease?.let {
                task.dependsOn(it)
            }
            AffectedModuleDetector.configureTaskGuard(task)
        }

        val updateResourceApi = project.tasks.register(
            UPDATE_RESOURCE_API_TASK,
            UpdateResourceApiTask::class.java
        ) { task ->
            task.group = TASK_GROUP_API
            task.description = "Updates the checked in resource API files to match source code API"
            task.apiLocation.set(generateResourceApi.flatMap { it.apiLocation })
            // Since apiLocation isn't a File, we have to manually set up the dependency.
            task.dependsOn(generateResourceApi)
            task.outputApiLocations.set(outputApiLocations)
            task.forceUpdate.set(project.providers.gradleProperty("force").isPresent)
            checkResourceApiRelease?.let {
                // If a developer (accidentally) makes a non-backwards compatible change to an
                // API, the developer will want to be informed of it as soon as possible.
                // So, whenever a developer updates an API, if backwards compatibility checks are
                // enabled in the library, then we want to check that the changes are backwards
                // compatible
                task.dependsOn(it)
            }
            AffectedModuleDetector.configureTaskGuard(task)
        }

        // Ensure that this task runs as part of "updateApi" task from MetalavaTasks.
        project.tasks.withType(UpdateApiTask::class.java).configureEach { task ->
            task.dependsOn(updateResourceApi)
        }

        project.addToCheckTask(checkResourceApi)
        project.addToBuildOnServer(checkResourceApi)
    }
}
