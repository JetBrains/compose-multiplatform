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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

// first level line filter - we ignore lines that don't contain the following for speed
const val ROOT_MATCH = "noinspection"
// Map of invalid pattern to correct replacement
// These could be regex for fancy whitespace matching, but don't seem to need in practice
val MATCHERS = mapOf(
    "//noinspection deprecation" to "@SuppressWarnings(\"deprecation\")",
    "//noinspection unchecked" to "@SuppressWarnings(\"unchecked\")"
)

open class CheckInvalidSuppressTask : DefaultTask() {

    @Option(option = "files", description = "List of files to check")
    @Internal
    lateinit var filenamesOption: String

    @Option(
        option = "format",
        description = "Use --format to auto-correct invalid suppressions"
    )
    @Internal
    var autocorrectOption = false

    // Check a line for any invalid suppressions, and return it if found
    fun getInvalidSuppression(line: String): String {
        MATCHERS.keys.forEach { bad ->
            if (line.trimStart().startsWith(bad)) {
                return bad
            }
        }
        return ""
    }

    // Get report for line with invalid suppression
    fun getReportForLine(filename: String, i: Int, lines: List<String>, good: String): String {
        var context = ""
        for (index in 0..2) {
            if (index + i >= lines.size) break
            context += lines[index + i] + "\n"
        }
        return "\n%s:%d:\nError: unsupported comment suppression\n%sInstead, use: %s\n"
            .format(
                filename, i, context, good
            )
    }

    @TaskAction
    fun checkForInvalidSuppression() {
        val filenames = filenamesOption.split(" ")
        var report = ""

        for (filename in filenames) {
            // suppress comments ignored in kotlin, but may as well block there too
            if (!filename.endsWith(".java") && !filename.endsWith(".kt"))
                continue

            // check file exists
            val file = File(filename)
            if (!file.exists())
                continue

            val lines = file.readLines(Charsets.UTF_8).toMutableList()
            for ((i, line) in lines.withIndex()) {
                if (line.contains(ROOT_MATCH)) {
                    val bad = getInvalidSuppression(line)
                    if (bad != "") {
                        if (autocorrectOption) {
                            if (line.trimStart() == bad) {
                                lines[i] = line.replaceFirst(bad, MATCHERS[bad]!!)
                            } else {
                                lines[i] = line.replaceFirst(bad, MATCHERS[bad]!! + " // ")
                            }
                            file.writeText(lines.joinToString(System.lineSeparator()))
                        } else {
                            report += getReportForLine(filename, i, lines, MATCHERS[bad]!!)
                        }
                    }
                }
            }
        }
        if (report != "") {
            throw GradleException(
                "Invalid, IDEA-specific warning suppression found. These cause " +
                    "warnings during compilation." + "\n" + report
            )
        }
    }
}

fun Project.configureCheckInvalidSuppress() {
    tasks.register("checkInvalidSuppress", CheckInvalidSuppressTask::class.java) { task ->
        task.description = "Task used to find IDEA-specific warning suppressions that do not " +
            "work for command line builds."
        task.group = "Verification"
    }
}
