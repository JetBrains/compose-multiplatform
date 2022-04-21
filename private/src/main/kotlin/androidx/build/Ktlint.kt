/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build

import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

val bundlingAttribute: Attribute<String> =
    Attribute.of(
        "org.gradle.dependency.bundling",
        String::class.java
    )

private fun Project.getKtlintConfiguration(): ConfigurableFileCollection {
    return files(
        configurations.findByName("ktlint") ?: configurations.create("ktlint") {
            val version = project.extensions.getByType(
                VersionCatalogsExtension::class.java
            ).find("libs").get().findVersion("ktlint").get().requiredVersion
            val dependency = dependencies.create("com.pinterest:ktlint:$version")
            it.dependencies.add(dependency)
            it.attributes.attribute(bundlingAttribute, "external")
        }
    )
}

private val DisabledRules = listOf(
    // does not match IJ default ordering
    "import-ordering",
    // not useful for our projects
    "final-newline",
    // TODO: reenable when https://github.com/pinterest/ktlint/issues/1221 is resolved
    "indent",
).joinToString(",")

private const val ExcludeTestDataFiles = "**/test-data/**/*.kt"
private const val ExcludeExternalFiles = "**/external/**/*.kt"
private const val MainClass = "com.pinterest.ktlint.Main"
private const val InputDir = "src"
private const val IncludedFiles = "**/*.kt"

fun Project.configureKtlint() {
    val outputDir = "${buildDir.relativeTo(projectDir)}/reports/ktlint/"
    val lintProvider = tasks.register("ktlint", KtlintCheckTask::class.java) { task ->
        task.report = File("${outputDir}ktlint-checkstyle-report.xml")
        task.ktlintClasspath.from(getKtlintConfiguration())
    }

    // afterEvaluate because Gradle's default "check" task doesn't exist yet
    afterEvaluate {
        addToCheckTask(lintProvider)
    }
    addToBuildOnServer(lintProvider)

    tasks.register("ktlintFormat", KtlintFormatTask::class.java) { task ->
        task.report = File("${outputDir}ktlint-format-checkstyle-report.xml")
        task.ktlintClasspath.from(getKtlintConfiguration())
    }
}

@CacheableTask
abstract class BaseKtlintTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Classpath
    abstract val ktlintClasspath: ConfigurableFileCollection

    @[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    fun getInputFiles(): FileTree? {
        val projectDirectory = overrideDirectory
        val subdirectories = overrideSubdirectories
        if (projectDirectory == null || subdirectories == null || subdirectories.isEmpty()) {
            // If we have a valid override, use that as the default fileTree
            return project.fileTree(
                mutableMapOf(
                    "dir" to InputDir, "include" to IncludedFiles,
                    "exclude" to listOf(ExcludeTestDataFiles, ExcludeExternalFiles)
                )
            )
        }
        return project.fileTree(projectDirectory) { tree ->
            subdirectories.forEach {
                tree.include("$it/src/**/*.kt")
            }
        }
    }

    /**
     * Allows overriding to use a custom directory instead of default [Project.getProjectDir].
     */
    @get:Internal
    var overrideDirectory: File? = null

    /**
     * Used together with [overrideDirectory] to specify which specific subdirectories should
     * be analyzed.
     */
    @get:Internal
    var overrideSubdirectories: List<String>? = null

    @get:OutputFile
    lateinit var report: File

    protected fun getArgsList(shouldFormat: Boolean): List<String> {
        val arguments = mutableListOf("--android")
        if (shouldFormat) arguments.add("-F")
        arguments.add("--disabled_rules")
        arguments.add(DisabledRules)
        arguments.add("--reporter=plain")
        arguments.add("--reporter=checkstyle,output=$report")

        overrideDirectory?.let {
            val subdirectories = overrideSubdirectories
            if (subdirectories == null || subdirectories.isEmpty()) return@let
            subdirectories.map { arguments.add("$it/$InputDir/$IncludedFiles") }
        } ?: arguments.add("$InputDir/$IncludedFiles")

        arguments.add("!$InputDir/$ExcludeTestDataFiles")
        arguments.add("!$InputDir/$ExcludeExternalFiles")
        return arguments
    }
}

@CacheableTask
abstract class KtlintCheckTask : BaseKtlintTask() {
    init {
        description = "Check Kotlin code style."
        group = "Verification"
    }

    @get:Internal
    val projectPath: String = project.path

    @TaskAction
    fun runCheck() {
        val result = execOperations.javaexec { javaExecSpec ->
            javaExecSpec.mainClass.set(MainClass)
            javaExecSpec.classpath = ktlintClasspath
            javaExecSpec.args = getArgsList(shouldFormat = false)
            overrideDirectory?.let { javaExecSpec.workingDir = it }
            javaExecSpec.isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            println("""

                ********************************************************************************
                ${TERMINAL_RED}You can attempt to automatically fix these issues with:
                ./gradlew $projectPath:ktlintFormat$TERMINAL_RESET
                ********************************************************************************
                """.trimIndent()
            )
            result.assertNormalExitValue()
        }
    }
}

@CacheableTask
abstract class KtlintFormatTask : BaseKtlintTask() {
    init {
        description = "Fix Kotlin code style deviations."
        group = "formatting"
    }

    @TaskAction
    fun runFormat() {
        execOperations.javaexec { javaExecSpec ->
            javaExecSpec.mainClass.set(MainClass)
            javaExecSpec.classpath = ktlintClasspath
            javaExecSpec.args = getArgsList(shouldFormat = true)
            overrideDirectory?.let { javaExecSpec.workingDir = it }
        }
    }
}

@CacheableTask
abstract class KtlintCheckFileTask : DefaultTask() {
    init {
        description = "Check Kotlin code style."
        group = "Verification"
    }

    @get:Input
    @set:Option(
        option = "file",
        description = "File to check. This option can be used multiple times: --file file1.kt " +
            "--file file2.kt"
    )
    var files: List<String> = emptyList()

    @get:Input
    @set:Option(
        option = "format",
        description = "Use --format to auto-correct style violations (if some errors cannot be " +
            "fixed automatically they will be printed to stderr)"
    )
    var format = false

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Classpath
    abstract val ktlintClasspath: ConfigurableFileCollection

    @TaskAction
    fun runKtlint() {
        if (files.isEmpty()) throw StopExecutionException()
        val kotlinFiles = files.filter { file ->
            file.endsWith(".kt") || file.endsWith(".ktx")
        }
        if (kotlinFiles.isEmpty()) throw StopExecutionException()
        val result = execOperations.javaexec { javaExecSpec ->
            javaExecSpec.mainClass.set(MainClass)
            javaExecSpec.classpath = ktlintClasspath
            val args = mutableListOf(
                "--android",
                "--disabled_rules",
                DisabledRules
            )
            args.addAll(kotlinFiles)
            if (format) args.add("-F")

            // Note: These exclusions must come after the inputs.
            args.add("!$ExcludeTestDataFiles")
            args.add("!$ExcludeExternalFiles")

            javaExecSpec.args = args
            javaExecSpec.isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            println("""

                ********************************************************************************
                ${TERMINAL_RED}You can attempt to automatically fix these issues with:
                ./gradlew :ktlintCheckFile --format ${kotlinFiles.joinToString { "--file $it" }}$TERMINAL_RESET
                ********************************************************************************
                """.trimIndent()
            )
            result.assertNormalExitValue()
        }
    }
}

fun Project.configureKtlintCheckFile() {
    tasks.register("ktlintCheckFile", KtlintCheckFileTask::class.java) { task ->
        task.ktlintClasspath.from(getKtlintConfiguration())
    }
}
