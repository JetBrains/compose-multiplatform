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

package androidx.build

import com.android.build.gradle.internal.dsl.LintOptions
import org.gradle.api.Project
import java.io.File

fun Project.configureLint(lintOptions: LintOptions, extension: SupportLibraryExtension) {
    // Lint is configured entirely in afterEvaluate so that individual projects cannot easily
    // disable individual checks in the DSL for any reason. That being said, when rolling out a new
    // check as fatal, it can be beneficial to set it to fatal above this comment. This allows you
    // to override it in a build script rather than messing with the baseline files. This is
    // especially relevant for checks which cause hundreds or more failures.
    afterEvaluate {
        lintOptions.apply {
            isAbortOnError = true
            isIgnoreWarnings = true

            // Skip lintVital tasks on assemble. We explicitly run lintRelease for libraries.
            isCheckReleaseBuilds = false

            // Write output directly to the console (and nowhere else).
            textOutput("stderr")
            textReport = true
            htmlReport = false

            // Format output for convenience.
            isExplainIssues = true
            isNoLines = false
            isQuiet = true

            fatal("NewApi")
            fatal("ObsoleteSdkInt")
            fatal("VisibleForTests")
            fatal("NoHardKeywords")
            fatal("SyntheticAccessor")
            fatal("UnusedResources")

            if (extension.mavenVersion?.isFinalApi() == true) {
                fatal("MissingTranslation")
            } else {
                disable("MissingTranslation")
            }

            // Set baseline file for all legacy lint warnings.
            val baselineFile = lintBaseline
            if (baselineFile.exists()) {
                baseline(baselineFile)
            }
        }
    }
}

val Project.lintBaseline get() = File(project.projectDir, "/lint-baseline.xml")
