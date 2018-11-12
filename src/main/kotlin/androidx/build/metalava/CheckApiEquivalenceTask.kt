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

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import org.apache.commons.io.FileUtils
import org.gradle.api.attributes.Attribute
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Compares two API txt files against each other. */
open class CheckApiEquivalenceTask : DefaultTask() {
    /**
     * Api file (in the build dir) to check
     *
     * Note: Marked as an output so that this task will be properly incremental.
     */
    @get:InputFile
    @get:OutputFile
    var file1: File? = null

    /**
     * Api file (in source control) to compare against
     */
    @get:InputFile
    var file2: File? = null

    /**
     * Message to show on failure
     */
    var failureMessage: String = "Public API definition has changed. Please run ./gradlew updateApi to confirm\n" +
                "these changes are intentional by updating the public API definition."

    @TaskAction
    fun exec() {
        val file1 = checkNotNull(file1) { "file1 not set" }
        val file2 = checkNotNull(file2) { "file2 not set" }
        if (!FileUtils.contentEquals(file1, file2)) {
            throw GradleException(failureMessage);
        }
    }
}
