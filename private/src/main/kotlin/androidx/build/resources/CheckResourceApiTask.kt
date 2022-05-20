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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

/**
 * Task for detecting changes in the public Android resource surface, e.g. `public.xml`.
 */
@CacheableTask
abstract class CheckResourceApiTask : DefaultTask() {
    /** Checked in resource API files (in source control). */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val checkedInApiFiles: ListProperty<File>

    /** Generated resource API file (in build output). */
    @get:Internal
    abstract val apiLocation: Property<ApiLocation>

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getTaskInput(): File {
        return apiLocation.get().resourceFile
    }

    @TaskAction
    fun checkResourceApi() {
        val builtApi = apiLocation.get().resourceFile

        for (checkedInApi in checkedInApiFiles.get()) {
            androidx.build.metalava.checkEqual(checkedInApi, builtApi)
        }
    }
}
