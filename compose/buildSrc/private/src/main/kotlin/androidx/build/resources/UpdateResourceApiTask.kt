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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task for updating the public Android resource surface, e.g. `public.xml`.
 */
@CacheableTask
abstract class UpdateResourceApiTask : DefaultTask() {
    /** Generated resource API file (in build output). */
    @get:Internal
    abstract val apiLocation: Property<ApiLocation>

    @get:Input
    abstract val forceUpdate: Property<Boolean>

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getTaskInput(): File {
        return apiLocation.get().resourceFile
    }

    /** Resource API files to which APIs should be written (in source control). */
    @get:Internal // outputs are declared in getTaskOutputs()
    abstract val outputApiLocations: ListProperty<ApiLocation>

    @OutputFiles
    fun getTaskOutputs(): List<File> {
        return outputApiLocations.get().flatMap { outputApiLocation ->
            listOf(
                outputApiLocation.resourceFile
            )
        }
    }

    @TaskAction
    fun updateResourceApi() {
        var permitOverwriting = true
        for (outputApi in outputApiLocations.get()) {
            val version = outputApi.version()
            if (version != null && version.isFinalApi() &&
                outputApi.publicApiFile.exists() &&
                !forceUpdate.get()
            ) {
                permitOverwriting = false
            }
        }

        val inputApi = apiLocation.get().resourceFile

        for (outputApi in outputApiLocations.get()) {
            androidx.build.metalava.copy(
                inputApi,
                outputApi.resourceFile,
                permitOverwriting,
                logger
            )
        }
    }
}
