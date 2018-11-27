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

import androidx.build.checkapi.ApiLocation
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import org.apache.commons.io.FileUtils
import org.gradle.api.attributes.Attribute
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Compares two API txt files against each other. */
open class CheckApiEquivalenceTask : DefaultTask() {
    /**
     * Api file (in the build dir) to check
     *
     * Note: Marked as an output so that this task will be properly incremental.
     */
    @get:InputFiles
    @get:OutputFiles
    var builtApi: ApiLocation? = null

    /**
     * Api file (in source control) to compare against
     */
    @get:InputFiles
    var checkedInApis: List<ApiLocation> = listOf()

    /**
     * Message to show on comparison failure of public API
     */
    var publicApiFailureMessage: String = "Public API definition has changed. Please run ./gradlew updateApi to confirm\n" +
                "these changes are intentional by updating the public API definition."
    /**
     * Message to show on comparison failure of restricted API
     */
    var restrictedApiFailureMessage: String = "Restricted API definition (marked by the RestrictTo annotation) has changed. Please run ./gradlew updateApi to confirm\n" +
                "these changes are intentional by updating the restricted API definition."
    @TaskAction
    fun exec() {
        val publicApi1 = checkNotNull(builtApi?.publicApiFile) { "publicApi1 not set" }
        val restrictedApi1 = checkNotNull(builtApi?.restrictedApiFile) { "restrictedApi1 not set" }
        for (checkedInApi in checkedInApis) {
            val publicApi2 = checkNotNull(checkedInApi?.publicApiFile) { "publicApi2 not set" }
            val restrictedApi2 = checkNotNull(checkedInApi?.restrictedApiFile) { "restrictedApi2 not set" }
            if (!FileUtils.contentEquals(publicApi1, publicApi2)) {
                throw GradleException(publicApiFailureMessage);
            }
            if (!FileUtils.contentEquals(restrictedApi1, restrictedApi2)) {
                throw GradleException(restrictedApiFailureMessage);
            }
        }
    }
}
