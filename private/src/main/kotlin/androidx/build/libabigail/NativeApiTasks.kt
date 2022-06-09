/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.build.libabigail

import androidx.build.addToBuildOnServer
import androidx.build.addToCheckTask
import androidx.build.checkapi.getRequiredCompatibilityApiLocation
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import java.io.File

/**
 * Adds native API generation / updating / checking tasks to a project.
 */
object NativeApiTasks {
    private const val apiGroup = "API"

    fun setupProject(
        project: Project,
        builtApiLocation: File,
        outputApiLocations: List<File>,
    ) {
        val artifactNames = project.extensions.getByType(
            LibraryExtension::class.java
        ).prefab.map { it.name }

        // Generates API files from source in the build directory
        val generateNativeApi = project.tasks.register(
            "generateNativeApi",
            GenerateNativeApiTask::class.java
        ) { task ->
            task.group = apiGroup
            task.description = "Generates API files from native source"
            task.projectRootDir.set(project.rootDir)
            task.prefabDirectory.set(
                project.buildDir.resolve("intermediates/prefab_package/release/prefab")
            )
            task.artifactNames.set(artifactNames)
            task.apiLocation.set(builtApiLocation)
            task.dependsOn("prefabReleasePackage")
        }

        // Checks that there are no breaking changes since the last (non alpha) release
        val requiredCompatibilityApiLocation = project.getRequiredCompatibilityApiLocation()
        val checkNativeApiRelease = requiredCompatibilityApiLocation?.let { lastReleasedApiFile ->
            project.tasks.register(
                "checkNativeApiRelease",
                CheckNativeApiCompatibilityTask::class.java
            ) { task ->
                task.group = apiGroup
                task.description = "Checks that the API generated from native sources is  " +
                    "compatible with the last released API file"
                task.artifactNames.set(artifactNames)
                task.builtApiLocation.set(builtApiLocation)
                task.currentApiLocation.set(lastReleasedApiFile.nativeApiDirectory)
                // only check for breaking changes here
                task.strict.set(false)
                task.dependsOn(generateNativeApi)
            }
        }

        // Checks that API present in source matches that of the current generated API files
        val checkNativeApi =
            project.tasks.register(
                "checkNativeApi",
                CheckNativeApiEquivalenceTask::class.java
            ) { task ->
                task.group = apiGroup
                task.description = "Checks that the API generated from native sources matches " +
                    "the checked in API file"
                task.artifactNames.set(artifactNames)
                task.builtApi.set(builtApiLocation)
                task.checkedInApis.set(outputApiLocations)
                task.cacheEvenIfNoOutputs()
                // Even if our API files are up to date, we still want to make sure we haven't
                // made any incompatible changes since last release
                checkNativeApiRelease?.let { task.dependsOn(it) }
                task.dependsOn(generateNativeApi)
            }

        // Update the native API files if there are no breaking changes since the last (non-alpha)
        // release.
        project.tasks.register("updateNativeApi", UpdateNativeApi::class.java) {
                task ->
            task.group = apiGroup
            task.description = "Updates the checked in API files to match source code API"
            task.artifactNames.set(artifactNames)
            task.inputApiLocation.set(builtApiLocation)
            task.outputApiLocations.set(outputApiLocations)
            task.dependsOn(generateNativeApi)
            // only allow updating the API files if there are no breaking changes from the last
            // released version. If for whatever reason we want to ignore this,
            // `ignoreBreakingChangesAndUpdateNativeApi` can be used.
            checkNativeApiRelease?.let { task.dependsOn(it) }
        }

        // Identical to `updateNativeApi` but does not depend on `checkNativeApiRelease`
        project.tasks.register(
            "ignoreBreakingChangesAndUpdateNativeApi",
            UpdateNativeApi::class.java
        ) { task ->
            task.group = apiGroup
            task.description = "Updates the checked in API files to match source code API" +
                "including breaking changes"
            task.artifactNames.set(artifactNames)
            task.inputApiLocation.set(builtApiLocation)
            task.outputApiLocations.set(outputApiLocations)
            task.dependsOn(generateNativeApi)
        }

        project.addToCheckTask(checkNativeApi)
        project.addToBuildOnServer(checkNativeApi)
    }
}