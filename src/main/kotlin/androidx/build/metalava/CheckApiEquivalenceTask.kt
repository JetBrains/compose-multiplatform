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
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Compares two API txt files against each other. */
abstract class CheckApiEquivalenceTask : DefaultTask() {
    /**
     * Api file (in the build dir) to check
     */
    @get:Input
    abstract val builtApi: Property<ApiLocation>

    /**
     * Api file (in source control) to compare against
     */
    @get:Input
    abstract val checkedInApis: ListProperty<ApiLocation>

    /**
     * Whether to check restricted APIs too
     */
    @get:Input
    var checkRestrictedAPIs = false

    @InputFiles
    fun getTaskInputs(): List<File> {
        if (checkRestrictedAPIs) {
            return checkedInApis.get().flatMap { it.files() }
        }
        return checkedInApis.get().map { it.publicApiFile }
    }

    @TaskAction
    fun exec() {
        val truePublicDefinition = checkNotNull(builtApi.get().publicApiFile) {
            "builtApi.publicApiFile not set"
        }
        val trueRestrictedApi = checkNotNull(builtApi.get().restrictedApiFile) {
            "builtApi.restrictedApiFile not set"
        }
        for (checkedInApi in checkedInApis.get()) {
            val declaredPublicApi = checkNotNull(checkedInApi.publicApiFile) {
                "checkedInApi.publicApiFile not set"
            }
            val declaredRestrictedApi = checkNotNull(checkedInApi.restrictedApiFile) {
                "checkedInApi.restrictedApiFile not set"
            }
            if (!FileUtils.contentEquals(declaredPublicApi, truePublicDefinition)) {
                val message = "Public API definition has changed.\n\n" +
                        "Declared definition is $declaredPublicApi\n" +
                        "True     definition is $truePublicDefinition\n\n" +
                        "Please run `./gradlew updateApi` to confirm these changes are " +
                        "intentional by updating the public API definition"
                throw GradleException(message)
            }
            if (checkRestrictedAPIs) {
                if (!FileUtils.contentEquals(declaredRestrictedApi, trueRestrictedApi)) {
                    val message = "Restricted API definition (marked by the RestrictedTo " +
                            "annotation) has changed.\n\n" +
                            "Declared definition is $declaredRestrictedApi\n" +
                            "True     definition is $trueRestrictedApi\n" +
                            "Please run `./gradlew updateApi` to confirm these changes are " +
                            "intentional by updating the restricted API definition"
                    throw GradleException(message)
                }
            }
        }
    }
}
