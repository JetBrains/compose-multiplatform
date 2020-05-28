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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task for detecting changes in the public Android resource surface, e.g. `public.xml`.
 */
abstract class CheckResourceApiTask : DefaultTask() {
    /**
     * API file (in the build dir) to check.
     *
     * A file path must be specified but the file may not exist if the library has no resources.
     */
    @get:Internal
    abstract val builtApi: RegularFileProperty

    @InputFile
    @Optional
    fun getBuiltApiFileIfExists(): File? {
        val file = builtApi.get().asFile
        return if (file.exists()) {
            file
        } else {
            null
        }
    }

    /**
     * API files (in source control) to compare against.
     */
    @get:Input
    abstract val checkedInApis: ListProperty<File>

    @TaskAction
    fun checkResourceApi() {
        val apiFile = builtApi.get().asFile

        // The built API file, if any, needs to be sorted first because that's how we store them.
        val sortedApiLines = if (apiFile.exists()) {
            apiFile.readLines().toSortedSet()
        } else {
            emptySet<String>()
        }

        val builtApiSorted = File.createTempFile("res", "txt")
        builtApiSorted.bufferedWriter().use { out ->
            sortedApiLines.forEach {
                out.write(it)
                out.newLine()
            }
        }

        for (checkedInApi in checkedInApis.get()) {
            androidx.build.metalava.checkEqual(checkedInApi, builtApiSorted)
        }
    }
}
