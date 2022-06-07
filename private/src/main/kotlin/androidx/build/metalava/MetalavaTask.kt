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

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/** Base class for invoking Metalava. */
@CacheableTask
abstract class MetalavaTask @Inject constructor(
    @Internal
    protected val workerExecutor: WorkerExecutor
) : DefaultTask() {
    /** Classpath containing Metalava and its dependencies. */
    @get:Classpath
    abstract val metalavaClasspath: ConfigurableFileCollection

    /** Android's boot classpath */
    @get:Classpath
    lateinit var bootClasspath: FileCollection

    /** Dependencies of [sourcePaths]. */
    @get:Classpath
    lateinit var dependencyClasspath: FileCollection

    /** Source files against which API signatures will be validated. */
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    var sourcePaths: FileCollection = project.files()

    @get:[Optional InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val manifestPath: RegularFileProperty

    fun runWithArgs(args: List<String>) {
        runMetalavaWithArgs(metalavaClasspath, args, workerExecutor)
    }
}
