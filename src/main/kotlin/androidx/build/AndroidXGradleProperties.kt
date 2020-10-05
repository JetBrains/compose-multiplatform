/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.build.dependencyTracker.AffectedModuleDetector
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Setting this property makes Test tasks succeed even if there
 * are some failing tests. Useful when running tests in CI where build
 * passes test results as XML to test reporter.
 */
const val TEST_FAILURES_DO_NOT_FAIL_TEST_TASK = "androidx.ignoreTestFailures"

/**
 * Setting this property to false makes test tasks not display detailed output to stdout.
 */
const val DISPLAY_TEST_OUTPUT = "androidx.displayTestOutput"

/**
 * Setting this property turns javac and kotlinc warnings into errors that fail the build.
 */
const val ALL_WARNINGS_AS_ERRORS = "androidx.allWarningsAsErrors"

/**
 * Setting this property enables calculating the fraction of code covered by tests
 */
const val COVERAGE_ENABLED = "androidx.coverageEnabled"

/**
 * Returns whether the project should generate documentation.
 */
const val ENABLE_DOCUMENTATION = "androidx.enableDocumentation"

/**
 * Setting this property puts a summary of the relevant failure messages into standard error
 */
const val SUMMARIZE_STANDARD_ERROR = "androidx.summarizeStderr"

/**
 * Setting this property enables writing versioned API files
 */
const val WRITE_VERSIONED_API_FILES = "androidx.writeVersionedApiFiles"

/**
 * Specifies the type of Android Studio to use for the project's Studio task
 */
const val STUDIO_TYPE = "androidx.studio.type"

/**
 * Build id used to pull SNAPSHOT versions to substitute project dependencies in Playground projects
 */
const val PLAYGROUND_SNAPSHOT_BUILD_ID = "androidx.playground.snapshotBuildId"

/**
 * Build Id used to pull SNAPSHOT version of Metalava for Playground projects
 */
const val PLAYGROUND_METALAVA_BUILD_ID = "androidx.playground.metalavaBuildId"

/**
 * Build Id used to pull SNAPSHOT version of Dokka for Playground projects
 */
const val PLAYGROUND_DOKKA_BUILD_ID = "androidx.playground.dokkaBuildId"

/**
 * Specifies to validate that the build doesn't generate any unrecognized messages
 * This prevents developers from inadvertently adding new warnings to the build output
 */
const val VALIDATE_NO_UNRECOGNIZED_MESSAGES = "androidx.validateNoUnrecognizedMessages"

const val EXPERIMENTAL_KOTLIN_BACKEND_ENABLED = "androidx.experimentalKotlinBackendEnabled"

val ALL_ANDROIDX_PROPERTIES = setOf(
    ALL_WARNINGS_AS_ERRORS,
    COVERAGE_ENABLED,
    DISPLAY_TEST_OUTPUT,
    ENABLE_DOCUMENTATION,
    STUDIO_TYPE,
    SUMMARIZE_STANDARD_ERROR,
    TEST_FAILURES_DO_NOT_FAIL_TEST_TASK,
    VALIDATE_NO_UNRECOGNIZED_MESSAGES,
    WRITE_VERSIONED_API_FILES,
    AffectedModuleDetector.CHANGED_PROJECTS_ARG,
    AffectedModuleDetector.ENABLE_ARG,
    AffectedModuleDetector.DEPENDENT_PROJECTS_ARG,
    AffectedModuleDetector.CHANGED_PROJECTS_ARG,
    PLAYGROUND_SNAPSHOT_BUILD_ID,
    PLAYGROUND_METALAVA_BUILD_ID,
    PLAYGROUND_DOKKA_BUILD_ID,
    EXPERIMENTAL_KOTLIN_BACKEND_ENABLED
)

/**
 * Validates that all properties passed by the user of the form "-Pandroidx.*" are not misspelled
 */
fun Project.validateAllAndroidxArgumentsAreRecognized() {
    for (propertyName in project.properties.keys) {
        if (propertyName.startsWith("androidx")) {
            if (!ALL_ANDROIDX_PROPERTIES.contains(propertyName)) {
                val message = "Unrecognized Androidx property '$propertyName'.\n" +
                    "\n" +
                    "Is this a misspelling? All recognized Androidx properties:\n" +
                    ALL_ANDROIDX_PROPERTIES.joinToString("\n") + "\n" +
                    "\n" +
                    "See AndroidXGradleProperties.kt if you need to add this property to " +
                    "the list of known properties."
                throw GradleException(message)
            }
        }
    }
}

/**
 * Returns whether tests in the project should display output
 */
fun Project.isDisplayTestOutput(): Boolean =
    (project.findProperty(DISPLAY_TEST_OUTPUT) as? String)?.toBoolean() ?: true

/**
 * Returns whether the project should write versioned API files, e.g. `1.1.0-alpha01.txt`.
 * <p>
 * When set to `true`, the `updateApi` task will write the current API surface to both `current.txt`
 * and `<version>.txt`. When set to `false`, only `current.txt` will be written. The default value
 * is `true`.
 */
fun Project.isWriteVersionedApiFilesEnabled(): Boolean =
    (project.findProperty(WRITE_VERSIONED_API_FILES) as? String)?.toBoolean() ?: true

/**
 * Returns whether the project should generate documentation.
 */
fun Project.isDocumentationEnabled(): Boolean =
    (project.findProperty(ENABLE_DOCUMENTATION) as? String)?.toBoolean() ?: true

/**
 * Returns whether the project has coverage enabled.
 */
fun Project.isCoverageEnabled(): Boolean =
    (project.findProperty(COVERAGE_ENABLED) as? String)?.toBoolean() ?: false

/**
 * Returns the Studio type for the project's studio task
 */
fun Project.studioType() = StudioType.findType(
    findProperty(STUDIO_TYPE)?.toString()
)

enum class StudioType {
    ANDROIDX,
    PLAYGROUND,
    COMPOSE;

    companion object {
        fun findType(value: String?) = when (value) {
            "playground" -> PLAYGROUND
            "compose" -> COMPOSE
            null, "androidx" -> ANDROIDX
            else -> error("Invalid project type $value")
        }
    }
}
