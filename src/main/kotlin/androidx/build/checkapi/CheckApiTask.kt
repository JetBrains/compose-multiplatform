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

package androidx.build.checkapi

import androidx.build.doclava.ChecksConfig
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/** Character that resets console output color. */
private const val ANSI_RESET = "\u001B[0m"

/** Character that sets console output color to red. */
private const val ANSI_RED = "\u001B[31m"

/** Character that sets console output color to yellow. */
private const val ANSI_YELLOW = "\u001B[33m"

private val ERROR_REGEX = Regex("^(.+):(.+): (\\w+) (\\d+): (.+)$")

private fun ByteArray.encodeHex() = fold(StringBuilder(), { builder, byte ->
    val hexString = Integer.toHexString(byte.toInt() and 0xFF)
    if (hexString.length < 2) {
        builder.append("0")
    }
    builder.append(hexString)
}).toString()

private fun getShortHash(src: String): String {
    val str = MessageDigest.getInstance("SHA-1")
            .digest(src.toByteArray()).encodeHex()
    val len = str.length
    return str.substring(len - 7, len)
}

/**
 * Task used to verify changes between two API files.
 * <p>
 * This task may be configured to ignore, warn, or fail with a message for a specific set of
 * Doclava-defined error codes. See {@link com.google.doclava.Errors} for a complete list of
 * supported error codes.
 * <p>
 * Specific failures may be ignored by specifying a list of SHAs in {@link #whitelistErrors}. Each
 * SHA is unique to a specific API change and is logged to the error output on failure.
 */
open class CheckApiTask : DefaultTask() {

    /** API file that represents the existing API surface. */
    @Optional
    @InputFile
    var oldApiFile: File? = null

    /** API file that represents the existing API surface's removals. */
    @Optional
    @InputFile
    var oldRemovedApiFile: File? = null

    /** API file that represents the candidate API surface. */
    @InputFile
    lateinit var newApiFile: File

    /** API file that represents the candidate API surface's removals. */
    @Optional
    @InputFile
    var newRemovedApiFile: File? = null

    /** Optional file containing a newline-delimited list of error SHAs to ignore. */
    var whitelistErrorsFile: File? = null

    @Optional
    @InputFile
    fun getWhiteListErrorsFileInput(): File? {
        // Gradle requires non-null InputFiles to exist -- even with Optional -- so work around that
        // by returning null for this field if the file doesn't exist.
        if (whitelistErrorsFile?.exists() == true) {
            return whitelistErrorsFile
        }
        return null
    }

    /**
     * Optional set of error SHAs to ignore.
     * <p>
     * Each error SHA is unique to a specific API change.
     */
    @Optional
    @Input
    var whitelistErrors = emptySet<String>()

    var detectedWhitelistErrors = mutableSetOf<String>()

    @InputFiles
    var doclavaClasspath: Collection<File> = emptyList()

    // A dummy output file meant only to tag when this check was last ran.
    // Without any outputs, Gradle will run this task every time.
    @Optional
    private var mOutputFile: File? = null

    @OutputFile
    fun getOutputFile(): File {
        return if (mOutputFile != null) {
            mOutputFile!!
        } else {
            File(project.buildDir, "checkApi/$name-completed")
        }
    }

    @Optional
    fun setOutputFile(outputFile: File) {
        mOutputFile = outputFile
    }

    @Input
    lateinit var checksConfig: ChecksConfig

    init {
        group = "Verification"
        description = "Invoke Doclava\'s ApiCheck tool to make sure current.txt is up to date."
    }

    private fun collectAndVerifyInputs(): Set<File> {
        if (oldRemovedApiFile != null && newRemovedApiFile != null) {
            return setOf(oldApiFile!!, newApiFile, oldRemovedApiFile!!, newRemovedApiFile!!)
        } else {
            return setOf(oldApiFile!!, newApiFile)
        }
    }

    @TaskAction
    fun exec() {
        if (oldApiFile == null) {
            // Nothing to do.
            return
        }

        val apiFiles = collectAndVerifyInputs()

        val errStream = ByteArrayOutputStream()

        // If either of those gets tweaked, then this should be refactored to extend JavaExec.
        project.javaexec { spec ->
            spec.apply {
                // Put Doclava on the classpath so we can get the ApiCheck class.
                classpath(doclavaClasspath)
                main = "com.google.doclava.apicheck.ApiCheck"

                minHeapSize = "128m"
                maxHeapSize = "1024m"

                // add -error LEVEL for every error level we want to fail the build on.
                checksConfig.errors.forEach { args("-error", it) }
                checksConfig.warnings.forEach { args("-warning", it) }
                checksConfig.hidden.forEach { args("-hide", it) }

                spec.args(apiFiles.map { it.absolutePath })

                // Redirect error output so that we can whitelist specific errors.
                errorOutput = errStream
                // We will be handling failures ourselves with a custom message.
                setIgnoreExitValue(true)
            }
        }

        // Load the whitelist file, if present.
        val whitelistFile = whitelistErrorsFile
        if (whitelistFile?.exists() == true) {
            whitelistErrors += whitelistFile.readLines()
        }

        // Parse the error output.
        val unparsedErrors = mutableSetOf<String>()
        val detectedErrors = mutableSetOf<List<String>>()
        val parsedErrors = mutableSetOf<List<String>>()
        ByteArrayInputStream(errStream.toByteArray()).bufferedReader().lines().forEach {
            val match = ERROR_REGEX.matchEntire(it)

            if (match == null) {
                unparsedErrors.add(it)
            } else if (match.groups[3]?.value == "error") {
                val hash = getShortHash(match.groups[5]?.value!!)
                val error = match.groupValues.subList(1, match.groupValues.size) + listOf(hash)
                if (hash in whitelistErrors) {
                    detectedErrors.add(error)
                    detectedWhitelistErrors.add(error[5])
                } else {
                    parsedErrors.add(error)
                }
            }
        }

        unparsedErrors.forEach { error -> logger.error("$ANSI_RED$error$ANSI_RESET") }
        parsedErrors.forEach { logger.error("$ANSI_RED${it[5]}$ANSI_RESET ${it[4]}") }
        detectedErrors.forEach { logger.warn("$ANSI_YELLOW${it[5]}$ANSI_RESET ${it[4]}") }

        if (unparsedErrors.isNotEmpty() || parsedErrors.isNotEmpty()) {
            throw GradleException(checksConfig.onFailMessage ?: "")
        }

        // Just create a dummy file upon completion. Without any outputs, Gradle will run this task
        // every time.
        val outputFile = getOutputFile()
        outputFile.parentFile.mkdirs()
        outputFile.createNewFile()
    }
}