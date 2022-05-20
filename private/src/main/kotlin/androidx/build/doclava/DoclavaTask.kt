/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.build.doclava

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

// external/doclava/src/com/google/doclava/Errors.java
val DEFAULT_DOCLAVA_CONFIG = ChecksConfig(
    errors = listOf(
        101, // unresolved link
        103, // unknown tag
        104 // unknown param name
    ),
    warnings = listOf(121 /* hidden type param */),
    hidden = listOf(
        111, // hidden super class
        113 // @deprecation mismatch
    )
)

@CacheableTask()
abstract class DoclavaTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    // All lowercase name to match MinimalJavadocOptions#docletpath
    @Classpath
    private lateinit var docletpath: FileCollection

    @Input
    var checksConfig: ChecksConfig = DEFAULT_DOCLAVA_CONFIG

    /**
     * If non-null, the list of packages that will be treated as if they were
     * marked with {@literal @hide}.<br>
     * Packages names will be matched exactly; sub-packages are not automatically recognized.
     */
    @Optional
    @Input
    var hiddenPackages: Collection<String>? = null

    /**
     * If non-null and not-empty, the inclusion list of packages that will be present in the
     * generated stubs; if null or empty, then all packages have stubs generated.<br>
     * Wildcards are accepted.
     */
    @Optional
    @Input
    var stubPackages: Set<String>? = null

    @Input
    var generateDocs = true

    /**
     * If non-null, the location of where to place the generated api file.
     * If this is non-null, then {@link #removedApiFile} must be non-null as well.
     */
    @Optional
    @OutputFile
    var apiFile: File? = null

    /**
     * If non-null, the location of where to place the generated removed api file.
     */
    @Optional
    @OutputFile
    var removedApiFile: File? = null

    /**
     * If non-null, the location to put the generated stub sources.
     */
    @Optional
    @OutputDirectory
    var stubsDir: File? = null

    init {
        // If none of generateDocs, apiFile, or stubJarsDir are true, then there is
        // no work to do.
        onlyIf({ generateDocs || apiFile != null || stubsDir != null })
    }

    /**
     * The doclet path which has the {@code com.google.doclava.Doclava} class.
     * This option will override any doclet path set in this instance's
     * {@link #options JavadocOptions}.
     * @see MinimalJavadocOptions#getDocletpath()
     */
    @InputFiles
    fun getDocletpath(): List<File> {
        return docletpath.files.toList()
    }

    /**
     * Sets the doclet path which has the {@code com.gogole.doclava.Doclava} class.
     * This option will override any doclet path set in this instance's
     * {@link #options JavadocOptions}.
     * @see MinimalJavadocOptions#setDocletpath(java.util.List)
     */
    fun setDocletpath(docletpath: FileCollection) {
        this.docletpath = docletpath
    }

    @OutputDirectory
    var destinationDir: File? = null

    @InputFiles @Classpath
    var classpath: FileCollection? = null

    @[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    val sources = mutableListOf<FileCollection>()

    fun source(files: FileCollection) {
        sources.add(files)
    }

    /**
     * Builder containing extra arguments
     */
    @Internal
    val extraArgumentsBuilder = DoclavaArgumentBuilder()

    @Input
    val extraArguments = extraArgumentsBuilder.build()

    private fun computeArguments(): List<String> {
        val args = DoclavaArgumentBuilder()

        // classpath
        val classpathFile = File.createTempFile("doclavaClasspath", ".txt")
        classpathFile.deleteOnExit()
        classpathFile.bufferedWriter().use { writer ->
            val classpathString = classpath!!.files.map({ f -> f.toString() }).joinToString(":")
            writer.write(classpathString)
        }
        args.addStringOption("cp", "@$classpathFile")
        args.addStringOption("doclet", "com.google.doclava.Doclava")
        args.addStringOption("docletpath", "@$classpathFile")

        args.addOption("quiet")
        args.addStringOption("encoding", "UTF-8")

        // configure doclava error/warning/hide levels
        args.addRepeatableOption("hide", checksConfig.hidden)
        args.addRepeatableOption("warning", checksConfig.warnings)
        args.addRepeatableOption("error", checksConfig.errors)

        if (hiddenPackages != null) {
            args.addRepeatableOption("hidePackage", hiddenPackages!!)
        }

        if (!generateDocs) {
            args.addOption("nodocs")
        }

        // If requested, generate the API files.
        if (apiFile != null) {
            args.addFileOption("api", apiFile!!)
            if (removedApiFile != null) {
                args.addFileOption("removedApi", removedApiFile!!)
            }
        }

        // If requested, generate stubs.
        if (stubsDir != null) {
            args.addFileOption("stubs", stubsDir!!)
            val stubs = stubPackages
            if (stubs != null) {
                args.addStringOption("stubpackages", stubs.joinToString(":"))
            }
        }
        // Always treat this as an Android docs task.
        args.addOption("android")

        // destination directory
        args.addFileOption("d", destinationDir!!)

        // source files
        val tmpArgs = File.createTempFile("doclavaSourceArgs", ".txt")
        tmpArgs.deleteOnExit()
        tmpArgs.bufferedWriter().use { writer ->
            for (source in sources) {
                for (file in source) {
                    val arg = file.toString()
                    // Doclava does not know how to parse Kotlin files
                    if (!arg.endsWith(".kt")) {
                        writer.write(arg)
                        writer.newLine()
                    }
                }
            }
        }
        args.add("@$tmpArgs")

        return args.build() + extraArgumentsBuilder.build()
    }

    @TaskAction
    fun generate() {
        val args = computeArguments()
        runDoclavaWithArgs(getDocletpath(), args, workerExecutor)
    }
}

class DoclavaArgumentBuilder {
    fun add(value: String) {
        args.add(value)
    }

    fun addOption(name: String) {
        args.add("-" + name)
    }

    fun addStringOption(name: String, value: String) {
        addOption(name)
        args.add(value)
    }

    fun addBooleanOption(name: String, value: Boolean) {
        addStringOption(name, value.toString())
    }

    fun addFileOption(name: String, value: File) {
        addStringOption(name, value.toString())
    }

    fun addRepeatableOption(name: String, values: Collection<*>) {
        for (value in values) {
            addStringOption(name, value.toString())
        }
    }

    fun addStringOption(name: String, values: Collection<String>) {
        args.add("-" + name)
        for (value in values) {
            args.add(value)
        }
    }

    fun build(): List<String> {
        return args
    }

    private val args = mutableListOf<String>()
}

interface DoclavaParams : WorkParameters {
    fun getClasspath(): ListProperty<File>
    fun getArgs(): ListProperty<String>
}

fun runDoclavaWithArgs(classpath: List<File>, args: List<String>, workerExecutor: WorkerExecutor) {
    val workQueue = workerExecutor.noIsolation()
    workQueue.submit(DoclavaWorkAction::class.java) { parameters ->
        parameters.getArgs().set(args)
        parameters.getClasspath().set(classpath)
    }
}

abstract class DoclavaWorkAction @Inject constructor(
    private val execOperations: ExecOperations
) : WorkAction<DoclavaParams> {
    override fun execute() {
        val args = getParameters().getArgs().get()
        val classpath = getParameters().getClasspath().get()

        execOperations.javaexec {
            it.classpath(classpath)
            it.mainClass.set("com.google.doclava.Doclava")
            it.args = args
        }
    }
}
