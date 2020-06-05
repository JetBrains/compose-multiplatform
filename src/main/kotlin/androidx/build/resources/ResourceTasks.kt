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

import androidx.build.AndroidXExtension
import androidx.build.AndroidXPlugin.Companion.CHECK_RESOURCE_API_RELEASE_TASK
import androidx.build.AndroidXPlugin.Companion.CHECK_RESOURCE_API_TASK
import androidx.build.AndroidXPlugin.Companion.TASK_GROUP_API
import androidx.build.AndroidXPlugin.Companion.UPDATE_RESOURCE_API_TASK
import androidx.build.addToBuildOnServer
import androidx.build.checkapi.getCurrentApiLocation
import androidx.build.checkapi.getRequiredCompatibilityApiLocation
import androidx.build.checkapi.getVersionedApiLocation
import androidx.build.checkapi.hasApiFileDirectory
import androidx.build.checkapi.hasApiTasks
import androidx.build.defaultPublishVariant
import androidx.build.isVersionedApiFileWritingEnabled
import androidx.build.metalava.UpdateApiTask
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import java.util.Locale

object ResourceTasks {

    @Suppress("UnstableApiUsage")
    fun Project.configureAndroidProjectForResourceTasks(
        library: LibraryExtension,
        extension: AndroidXExtension
    ) {
        afterEvaluate { project ->
            if (!hasApiTasks(this, extension)) {
                return@afterEvaluate
            }

            library.defaultPublishVariant { variant ->
                if (!project.hasApiFileDirectory()) {
                    logger.info(
                        "Project $name doesn't have an api folder, ignoring API tasks."
                    )
                    return@defaultPublishVariant
                }

                setupProject(project, variant.name)
            }
        }
    }

    private fun setupProject(
        project: Project,
        variantName: String
    ) {
        @OptIn(ExperimentalStdlibApi::class)
        val packageResTask = project.tasks
            .named("package${variantName.capitalize(Locale.US)}Resources")
        val builtApiFile = packageResTask.flatMap { task ->
            (task as com.android.build.gradle.tasks.MergeResources).publicFile
        }

        val versionedApiLocation = project.getVersionedApiLocation()
        val currentApiLocation = project.getCurrentApiLocation()

        val outputApiLocations = if (project.isVersionedApiFileWritingEnabled()) {
            listOf(
                versionedApiLocation,
                currentApiLocation
            )
        } else {
            listOf(
                currentApiLocation
            )
        }

        val outputApiFiles = outputApiLocations.map { location ->
            location.resourceFile
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
                task.apiFile.set(builtApiFile)
                task.dependsOn(packageResTask)
                task.cacheEvenIfNoOutputs()
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
            task.builtApi.set(builtApiFile)
            task.dependsOn(packageResTask)
            task.cacheEvenIfNoOutputs()
            task.checkedInApis.set(outputApiFiles)
            checkResourceApiRelease?.let {
                task.dependsOn(it)
            }
        }

        val updateResourceApi = project.tasks.register(
            UPDATE_RESOURCE_API_TASK,
            UpdateResourceApiTask::class.java
        ) { task ->
            task.group = TASK_GROUP_API
            task.description = "Updates the checked in resource API files to match source code API"
            task.inputApiFile.set(builtApiFile)
            task.dependsOn(packageResTask)
            task.outputApiLocations.set(outputApiLocations)
            checkResourceApiRelease?.let {
                // If a developer (accidentally) makes a non-backwards compatible change to an
                // API, the developer will want to be informed of it as soon as possible.
                // So, whenever a developer updates an API, if backwards compatibility checks are
                // enabled in the library, then we want to check that the changes are backwards
                // compatible
                task.dependsOn(it)
            }
        }

        // Ensure that this task runs as part of updateApi and buildOnServer
        project.tasks.withType(UpdateApiTask::class.java).configureEach { task ->
            task.dependsOn(updateResourceApi)
        }
        project.addToBuildOnServer(checkResourceApi)
    }
}
