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

import androidx.build.checkapi.ApiLocation
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task for verifying changes in the public Android resource surface, e.g. `public.xml`.
 */
@CacheableTask
abstract class CheckResourceApiReleaseTask : DefaultTask() {
    /** Reference resource API file (in source control). */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val referenceApiFile: Property<File>

    /** Generated resource API file (in build output). */
    @get:Internal
    abstract val apiLocation: Property<ApiLocation>

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getTaskInput(): File {
        return apiLocation.get().resourceFile
    }

    @TaskAction
    fun checkResourceApiRelease() {
        val referenceApiFile = referenceApiFile.get()
        val apiFile = apiLocation.get().resourceFile

        // Read the current API surface, if any, into memory.
        val newApiSet = if (apiFile.exists()) {
            apiFile.readLines().toSet()
        } else {
            emptySet()
        }

        // Read the reference API surface into memory.
        val referenceApiSet = referenceApiFile.readLines().toSet()

        // POLICY: Ensure that no resources are removed from the last released version.
        val removedApiSet = referenceApiSet - newApiSet
        if (removedApiSet.isNotEmpty()) {
            var removed = ""
            for (e in removedApiSet) {
                removed += "$e\n"
            }

            val errorMessage = """Public resources have been removed since the previous revision

Previous definition is ${referenceApiFile.canonicalPath}
Current  definition is ${apiFile.canonicalPath}

Public resources are considered part of the library's API surface
and may not be removed within a major version.

Removed resources:
$removed"""

            throw GradleException(errorMessage)
        }
    }
}
