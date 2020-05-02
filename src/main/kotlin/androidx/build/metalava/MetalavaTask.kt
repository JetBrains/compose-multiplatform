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

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/** Base class for invoking Metalava. */
abstract class MetalavaTask @Inject constructor(
    @Internal
    protected val workerExecutor: WorkerExecutor
) : DefaultTask() {

    /** Configuration containing Metalava and its dependencies. */
    @get:Classpath
    @get:InputFiles
    lateinit var configuration: Configuration

    /** Android's boot classpath. Obtained from [BaseExtension.getBootClasspath]. */
    @get:InputFiles
    lateinit var bootClasspath: Collection<File>

    /** Dependencies of [sourcePaths]. */
    @get:InputFiles
    lateinit var dependencyClasspath: FileCollection

    /** Source files against which API signatures will be validated. */
    @get:InputFiles
    var sourcePaths: Collection<File> = emptyList()

    @get:InputFile
    @get:Optional
    abstract val manifestPath: RegularFileProperty

    fun runWithArgs(args: List<String>) {
        runMetalavaWithArgs(configuration, args, workerExecutor)
    }
}
