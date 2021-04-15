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

package androidx.build.dackka

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

abstract class DackkaTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    // Classpath containing Dackka
    @get:Classpath
    abstract val dackkaClasspath: ConfigurableFileCollection

    // Directory containing the code samples
    @InputFiles
    lateinit var samplesDir: File

    // Directory containing the source code for Dackka to process
    @InputFiles
    lateinit var sourcesDir: File

    // Location of generated reference docs
    @OutputDirectory
    lateinit var destinationDir: File

    // Documentation for Dackka command line usage and arguments can be found at
    // https://kotlin.github.io/dokka/1.4.0/user_guide/cli/usage/
    private fun computeArguments(): List<String> {
        return listOf(

            // moduleName arg needs to be present but is not used the generated docs
            // b/184166302 tracks an update to the CLI to mark this as optional
            "-moduleName",
            "",

            // location of the generated docs
            "-outputDir",
            "$destinationDir",

            // Configuration of sources. The generated string looks like this:
            // "-sourceSet -src /path/to/src -samples /path/to/samples ..."
            "-sourceSet",
            "-src $sourcesDir -samples $samplesDir"
        )
    }

    @TaskAction
    fun generate() {
        runDackkaWithArgs(dackkaClasspath, computeArguments(), workerExecutor)
    }
}

@Suppress("UnstableApiUsage")
interface DackkaParams : WorkParameters {
    val args: ListProperty<String>
    val classpath: SetProperty<File>
}

@Suppress("UnstableApiUsage")
fun runDackkaWithArgs(
    classpath: FileCollection,
    args: List<String>,
    workerExecutor: WorkerExecutor
) {
    val workQueue = workerExecutor.noIsolation()
    workQueue.submit(DackkaWorkAction::class.java) { parameters ->
        parameters.args.set(args)
        parameters.classpath.set(classpath)
    }
}

@Suppress("UnstableApiUsage")
abstract class DackkaWorkAction @Inject constructor (
    private val execOperations: ExecOperations
) : WorkAction<DackkaParams> {
    override fun execute() {
        execOperations.javaexec {
            it.args = parameters.args.get()
            it.classpath(parameters.classpath.get())

            // b/183989795 tracks moving this away from an environment variable
            it.environment("DEVSITE_TENANT", "androidx")
        }
    }
}
