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

import androidx.build.OperatingSystem
import androidx.build.getOperatingSystem
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Task which depends on `[GenerateNativeApiTask] and takes the generated native API files from the
 * build directory and copies them to the current /native-api directory.
 */
@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class UpdateNativeApi : DefaultTask() {

    @get:Internal
    abstract val artifactNames: ListProperty<String>

    @get:Internal
    abstract val inputApiLocation: Property<File>

    @get:Internal
    abstract val outputApiLocations: ListProperty<File>

    @[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    fun getTaskInputs(): List<File> {
        return getLocationsForArtifacts(
            inputApiLocation.get(),
            artifactNames.get()
        )
    }

    @OutputFiles
    fun getTaskOutputs(): List<File> {
        return outputApiLocations.get().flatMap { outputApiLocation ->
            getLocationsForArtifacts(
                outputApiLocation,
                artifactNames.get()
            )
        }
    }

    @TaskAction
    fun exec() {
        if (getOperatingSystem() != OperatingSystem.LINUX) {
            logger.warn(
                "Native API checking is currently not supported on non-linux devices"
            )
            return
        }
        outputApiLocations.get().forEach { dir ->
            dir.listFiles()?.forEach {
                it.delete()
            }
        }
        outputApiLocations.get().forEach { outputLocation ->
            inputApiLocation.get().copyRecursively(target = outputLocation, overwrite = true)
        }
    }
}
