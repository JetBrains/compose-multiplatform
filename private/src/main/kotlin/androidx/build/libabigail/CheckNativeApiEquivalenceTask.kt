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

import androidx.build.metalava.checkEqual
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Checks that the native API files in the build folder are exactly the same as the checked in
 * native API files.
 */
@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class CheckNativeApiEquivalenceTask : DefaultTask() {
    /**
     * Api file (in the build dir) to check
     */
    @get:Input
    abstract val builtApi: Property<File>

    /**
     * Api file (in source control) to compare against
     */
    @get:Input
    abstract val checkedInApis: ListProperty<File>

    @get:Internal
    abstract val artifactNames: ListProperty<String>

    @[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    fun getTaskInputs(): List<File> {
        return getLocationsForArtifacts(
            builtApi.get(),
            artifactNames.get()
        ) + checkedInApis.get().flatMap { checkedInApi ->
            getLocationsForArtifacts(
                checkedInApi,
                artifactNames.get()
            )
        }
    }

    @TaskAction
    fun exec() {
        val builtApiLocation = builtApi.get()
        for (checkedInApi in checkedInApis.get()) {
            for (artifactName in artifactNames.get()) {
                for (arch in architectures) {
                    checkEqual(
                        builtApiLocation.resolve("$arch/lib$artifactName.xml"),
                        checkedInApi.resolve("$arch/lib$artifactName.xml")
                    )
                }
            }
        }
    }
}
