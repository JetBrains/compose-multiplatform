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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Validates that all tasks (except a temporary whitelist) are considered up-to-date.
 * The expected usage of this is that the user will invoke a build with the
 * TaskUpToDateValidator disabled, and then reinvoke the same build with the TaskUpToDateValidator
 * enabled. If the second build actually runs any tasks, then some tasks don't have the correct
 * inputs/outputs declared and are running more often than necessary.
 */

const val DISALLOW_TASK_EXECUTION_FLAG_NAME = "disallowExecution"

// Temporary whitelist of tasks that are known to still be out-of-date after running once
val EXEMPT_TASK_NAMES = setOf(
    "buildOnServer",
    "checkSameVersionLibraryGroups",
    "desugarPublicDebugFileDependencies",
    "desugarTipOfTreeDebugFileDependencies",
    "distPublicDokkaDocs",
    "dokkaJavaPublicDocs",
    "dokkaKotlinPublicDocs",
    "jacocoPublicDebug",
    "jacocoTipOfTreeDebug",
    "lintVitalRelease",
    "mergeExtDexPublicDebug",
    "mergeExtDexTipOfTreeDebug",
    "mergeLibDexPublicDebug",
    "mergeProjectDexPublicDebug",
    "mergeProjectDexTipOfTreeDebug",
    "mergePublicDebugAssets",
    "mergePublicDebugJavaResource",
    "mergePublicDebugJniLibFolders",
    "mergePublicDebugNativeLibs",
    "mergePublicDebugShaders",
    "mergeTipOfTreeDebugAssets",
    "mergeTipOfTreeDebugJavaResource",
    "packagePublicDebug",
    "packageTipOfTreeDebug",
    "transformClassesWithDexBuilderForPublicDebug",
    "transformClassesWithDexBuilderForTipOfTreeDebug",
    "lint",
    "reportLibraryMetrics",
    "verifyDependencyVersions",
    "lintDebug",
    "checkExternalLicenses",
    "dist"
)
class TaskUpToDateValidator {
    companion object {
        private fun shouldEnable(project: Project): Boolean {
            return project.hasProperty(DISALLOW_TASK_EXECUTION_FLAG_NAME)
        }

        private fun isExemptTask(task: Task): Boolean {
            return EXEMPT_TASK_NAMES.contains(task.name)
        }

        fun setup(rootProject: Project) {
            if (!shouldEnable(rootProject)) {
                return
            }
            rootProject.gradle.taskGraph.afterTask { task ->
                if (task.didWork) {
                    if (!isExemptTask(task)) {
                        val message = "Error: executed ${task} but " +
                            DISALLOW_TASK_EXECUTION_FLAG_NAME +
                            " was specified. This indicates that ${task} does not declare" +
                            " inputs and/or outputs correctly."
                        throw GradleException(message)
                    }
                }
            }
        }
    }
}
