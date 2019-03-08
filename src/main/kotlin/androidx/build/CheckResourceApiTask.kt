/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task for detecting changes in the public resource surface
 */
open class CheckResourceApiTask : DefaultTask() {

    @InputFiles
    @Optional
    var oldApiFile: File? = null

    @InputFiles
    @Optional
    var newApiFile: File? = null

    @TaskAction
    fun checkResourceApi() {

        if (oldApiFile == null || !oldApiFile!!.exists()) {
            throw GradleException("No resource api file for the current version exists, please" +
                    " run updateApi to create one.")
        }
        var oldResourceApi: HashSet<String> = HashSet<String>(oldApiFile?.readLines())
        var newResourceApi: HashSet<String> = HashSet<String>()
        if (newApiFile != null && newApiFile!!.exists()) {
            newResourceApi = HashSet<String>(newApiFile?.readLines())
        }
        if (!oldResourceApi.equals(newResourceApi)) {
            throw GradleException("Public resource surface changes detected, please run" +
                    " updateApi to confirm this change is intentional.")
        }
    }
}
