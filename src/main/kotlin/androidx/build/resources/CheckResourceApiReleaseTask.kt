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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task for updating the public Android resource surface, e.g. `public.xml`.
 */
abstract class CheckResourceApiReleaseTask : DefaultTask() {
    /**
     * Text file from which the resource API signatures will be read.
     */
    @get:InputFile
    abstract val referenceApiFile: Property<File>

    /**
     * Text file representing the current resource API surface to check.
     *
     * A file path must be specified but the file may not exist if the library has no resources.
     */
    @get:Internal
    abstract val apiFile: RegularFileProperty

    @InputFile
    @Optional
    fun getApiFileIfExists(): File? {
        val file = apiFile.get().asFile
        return if (file.exists()) {
            file
        } else {
            null
        }
    }

    @TaskAction
    fun checkResourceApiRelease() {
        val referenceApiFile = referenceApiFile.get()
        val apiFile = apiFile.get().asFile

        // Read the current API surface, if any, into memory.
        val newApiSet = if (apiFile.exists()) {
            apiFile.readLines().toSet()
        } else {
            emptySet()
        }

        // Read the reference API surface into memory.
        val referenceApiSet = referenceApiFile.readLines().toSet()
        checkApiReleaseCompatibility(referenceApiSet, newApiSet)
    }

    private fun checkApiReleaseCompatibility(
        referenceApiSet: Set<String>,
        newApiSet: Set<String>
    ) {
        // POLICY: Ensure that no resources are removed from the last released version.
        val removedApiSet = referenceApiSet - newApiSet
        if (removedApiSet.isNotEmpty()) {
            var errorMessage = "Public resources have been removed since the previous revision:\n"
            for (e in removedApiSet) {
                errorMessage += "$e\n"
            }
            throw GradleException(errorMessage)
        }
    }
}
