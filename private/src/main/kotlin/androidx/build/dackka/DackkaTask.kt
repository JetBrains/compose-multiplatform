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
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class DackkaTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    // Classpath containing Dackka
    @get:Classpath
    abstract val dackkaClasspath: ConfigurableFileCollection

    // Classpath containing dependencys of libraries needed to resolve types in docs
    @get:[InputFiles Classpath]
    lateinit var dependenciesClasspath: FileCollection

    // Directory containing the code samples from framework
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    lateinit var frameworkSamplesDir: File

    // Directory containing the code samples
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    lateinit var samplesDir: File

    // Directory containing the source code for Dackka to process
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    lateinit var sourcesDir: File

    // Directory containing the docs project and package-lists
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    lateinit var docsProjectDir: File

    // Location of generated reference docs
    @get:OutputDirectory
    lateinit var destinationDir: File

    // Set of packages to exclude for refdoc generation for all languages
    @Input
    lateinit var excludedPackages: Set<String>

    // Set of packages to exclude for Java refdoc generation
    @Input
    lateinit var excludedPackagesForJava: Set<String>

    // Set of packages to exclude for Kotlin refdoc generation
    @Input
    lateinit var excludedPackagesForKotlin: Set<String>

    // Documentation for Dackka command line usage and arguments can be found at
    // https://kotlin.github.io/dokka/1.4.0/user_guide/cli/usage/
    private fun computeArguments(): List<String> {

        // path comes with colons but dokka expects a semicolon delimited string
        val classPath = dependenciesClasspath.asPath.replace(':', ';')

        var linksConfiguration = ""
        mapOf(
            "coroutinesCore"
                to "https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core",
            "android" to "https://developer.android.com/reference",
            "guava" to "https://guava.dev/releases/18.0/api/docs/",
            "kotlin" to "https://kotlinlang.org/api/latest/jvm/stdlib/"
        ).map {
            // Dokka sets this format: url^packageListUrl^^url2...
            linksConfiguration +=
                "${it.value}^${docsProjectDir.toPath()}/package-lists/${it.key}/package-list^^"
        }
        val includes = sourcesDir.walkTopDown()
            .filter { it.name.endsWith("documentation.md") }
            .joinToString(";")
        val includesString = if (includes.isNotEmpty()) { "-includes $includes" } else { "" }

        return listOf(

            // moduleName arg needs to be present but is not used the generated docs
            // b/184166302 tracks an update to the CLI to mark this as optional
            "-moduleName",
            "",

            // location of the generated docs
            "-outputDir",
            "$destinationDir",

            // links to external types
            "-globalLinks",
            linksConfiguration,

            // Set logging level to only show warnings and errors
            "-loggingLevel",
            "WARN",

            // Configuration of sources. The generated string looks like this:
            // "-sourceSet -src /path/to/src -samples /path/to/samples ..."
            "-sourceSet",
            "-src $sourcesDir" +
                " -samples $samplesDir" +
                " -samples $frameworkSamplesDir" +
                " -classpath $classPath" +
                " $includesString",

            "-offlineMode"
        )
    }

    @TaskAction
    fun generate() {
        runDackkaWithArgs(
            dackkaClasspath,
            computeArguments(),
            workerExecutor,
            excludedPackages,
            excludedPackagesForJava,
            excludedPackagesForKotlin,
        )
    }
}

@Suppress("UnstableApiUsage")
interface DackkaParams : WorkParameters {
    val args: ListProperty<String>
    val classpath: SetProperty<File>
    val excludedPackages: ListProperty<String>
    val excludedPackagesForJava: ListProperty<String>
    val excludedPackagesForKotlin: ListProperty<String>
}

@Suppress("UnstableApiUsage")
fun runDackkaWithArgs(
    classpath: FileCollection,
    args: List<String>,
    workerExecutor: WorkerExecutor,
    excludedPackages: Set<String>,
    excludedPackagesForJava: Set<String>,
    excludedPackagesForKotlin: Set<String>,
) {
    val workQueue = workerExecutor.noIsolation()
    workQueue.submit(DackkaWorkAction::class.java) { parameters ->
        parameters.args.set(args)
        parameters.classpath.set(classpath)
        parameters.excludedPackages.set(excludedPackages)
        parameters.excludedPackagesForJava.set(excludedPackagesForJava)
        parameters.excludedPackagesForKotlin.set(excludedPackagesForKotlin)
    }
}

@Suppress("UnstableApiUsage")
abstract class DackkaWorkAction @Inject constructor (
    private val execOperations: ExecOperations
) : WorkAction<DackkaParams> {
    override fun execute() {
        execOperations.javaexec {
            it.mainClass.set("org.jetbrains.dokka.MainKt")
            it.args = parameters.args.get()
            it.classpath(parameters.classpath.get())
            // b/183989795 tracks moving these away from an environment variables
            it.environment("DEVSITE_TENANT", "androidx")

            if (parameters.excludedPackages.get().isNotEmpty())
                it.environment(
                    "DACKKA_EXCLUDED_PACKAGES",
                    parameters.excludedPackages.get().joinToString(",")
                )

            if (parameters.excludedPackagesForJava.get().isNotEmpty())
                it.environment(
                    "DACKKA_EXCLUDED_PACKAGES_JAVA",
                    parameters.excludedPackagesForJava.get().joinToString(",")
                )

            if (parameters.excludedPackagesForKotlin.get().isNotEmpty())
                it.environment(
                    "DACKKA_EXCLUDED_PACKAGES_KOTLIN",
                    parameters.excludedPackagesForKotlin.get().joinToString(",")
                )
        }
    }
}
