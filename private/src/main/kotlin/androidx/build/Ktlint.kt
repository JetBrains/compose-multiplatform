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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.options.Option
import java.io.File

private fun Project.getKtlintConfiguration(): Configuration {
    return configurations.findByName("ktlint") ?: configurations.create("ktlint") {
        val version = project.extensions.getByType(
            VersionCatalogsExtension::class.java
        ).find("libs").get().findVersion("ktlint").get().requiredVersion

        val dependency = dependencies.create("com.pinterest:ktlint:$version")
        it.dependencies.add(dependency)
    }
}

private val DisabledRules = listOf(
    // does not match IJ default ordering
    "import-ordering",
    // not useful for our projects
    "final-newline",
    // TODO: reenable when https://github.com/pinterest/ktlint/issues/1221 is resolved
    "indent",
).joinToString(",")

private const val excludeTestDataFiles = "**/test-data/**/*.kt"
private const val excludeExternalFiles = "**/external/**/*.kt"

fun Project.configureKtlint() {
    val outputDir = "${buildDir.relativeTo(projectDir)}/reports/ktlint/"
    val inputDir = "src"
    val includeFiles = "**/*.kt"
    val inputFiles = project.fileTree(
        mutableMapOf(
            "dir" to inputDir, "include" to includeFiles,
            "exclude" to listOf(excludeTestDataFiles, excludeExternalFiles)
        )
    )
    val outputFile = "${outputDir}ktlint-checkstyle-report.xml"

    val lintProvider = tasks.register("ktlint", KtlintCheckTask::class.java) { task ->
        task.inputFiles = inputFiles
        task.report = File(outputFile)
        task.classpath = getKtlintConfiguration()
        task.args = listOf(
            "--android",
            "--disabled_rules",
            DisabledRules,
            "--reporter=plain",
            "--reporter=checkstyle,output=$outputFile",
            "$inputDir/$includeFiles",
            "!$inputDir/$excludeTestDataFiles",
            "!$inputDir/$excludeExternalFiles"
        )
    }

    // afterEvaluate because Gradle's default "check" task doesn't exist yet
    afterEvaluate {
        addToCheckTask(lintProvider)
    }
    addToBuildOnServer(lintProvider)

    val outputFileFormat = "${outputDir}ktlint-format-checkstyle-report.xml"
    tasks.register("ktlintFormat", JavaExec::class.java) { task ->
        task.inputs.files(
            inputFiles.apply {
                setExcludes(listOf(excludeTestDataFiles, excludeExternalFiles))
            }
        )
        task.outputs.file(outputFileFormat)
        task.description = "Fix Kotlin code style deviations."
        task.group = "formatting"
        task.classpath = getKtlintConfiguration()
        task.mainClass.set("com.pinterest.ktlint.Main")
        task.args = listOf(
            "--android",
            "-F",
            "--disabled_rules",
            DisabledRules,
            "--reporter=plain",
            "--reporter=checkstyle,output=$outputFileFormat",
            "$inputDir/$includeFiles",
            "!$inputDir/$excludeTestDataFiles",
            "!$inputDir/$excludeExternalFiles"
        )
    }
}

@CacheableTask
open class KtlintCheckTask : JavaExec() {
    init {
        description = "Check Kotlin code style."
        group = "Verification"
        mainClass.set("com.pinterest.ktlint.Main")
    }

    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    lateinit var inputFiles: FileTree

    @get:OutputFile
    lateinit var report: File
}

open class KtlintCheckFileTask : JavaExec() {
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
}

fun Project.configureKtlintCheckFile() {
    tasks.register("ktlintCheckFile", KtlintCheckFileTask::class.java) { task ->
        task.description = "Check Kotlin code style."
        task.group = "Verification"
        task.classpath = getKtlintConfiguration()
        task.mainClass.set("com.pinterest.ktlint.Main")

        task.doFirst {
            if (task.files.isEmpty()) {
                throw StopExecutionException()
            }
            val kotlinFiles = task.files.filter { file ->
                file.endsWith(".kt") || file.endsWith(".ktx")
            }
            if (kotlinFiles.isNullOrEmpty()) {
                throw StopExecutionException()
            }
            val args = mutableListOf(
                "--android",
                "--disabled_rules",
                DisabledRules
            )
            args.addAll(kotlinFiles)
            if (task.format) {
                args.add("-F")
            }

            // Note: These exclusions must come after the inputs.
            args.add("!$excludeTestDataFiles")
            args.add("!$excludeExternalFiles")

            task.args = args
        }
    }
}
