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
package androidx.build.license

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * This task creates a configuration for the project that has all of its external dependencies
 * and then ensures that those dependencies:
 * a) come from prebuilts
 * b) has a license file.
 */
open class CheckExternalDependencyLicensesTask : DefaultTask() {
    @Suppress("unused")
    @TaskAction
    fun checkDependencies() {
        val supportRoot = (project.rootProject.property("ext") as ExtraPropertiesExtension)
                .get("supportRootFolder") as File
        val prebuiltsRoot = File(supportRoot, "../../prebuilts").canonicalFile

        val checkerConfig = project.configurations.getByName(CONFIG)

        project
                .configurations
                .flatMap {
                    it.allDependencies
                            .filterIsInstance(ExternalDependency::class.java)
                            .filterNot {
                                it.group?.startsWith("com.android") == true
                            }
                            .filterNot {
                                it.group?.startsWith("android.arch") == true
                            }
                            .filterNot {
                                it.group?.startsWith("androidx") == true
                            }
                }
                .forEach {
                    checkerConfig.dependencies.add(it)
                }
        val missingLicenses = checkerConfig.resolve().filter {
            findLicenseFile(it.canonicalFile, prebuiltsRoot) == null
        }
        if (missingLicenses.isNotEmpty()) {
            val suggestions = missingLicenses.joinToString("\n") {
                "$it does not have a license file. It should probably live in " +
                        "${it.parentFile.parentFile}"
            }
            throw GradleException("""
                Any external library referenced in the support library
                build must have a LICENSE or NOTICE file next to it in the prebuilts.
                The following libraries are missing it:
                $suggestions
                """.trimIndent())
        }
    }

    private fun findLicenseFile(dependency: File, prebuiltsRoot: File): File? {
        if (!dependency.absolutePath.startsWith(prebuiltsRoot.absolutePath)) {
            throw GradleException("prebuilts should come from prebuilts folder. $dependency is" +
                    " not there")
        }
        fun recurse(folder: File): File? {
            if (folder == prebuiltsRoot) {
                return null
            }
            if (!folder.isDirectory) {
                return recurse(folder.parentFile)
            }

            val found = folder.listFiles().firstOrNull {
                it.name.toUpperCase().startsWith("NOTICE")
                        || it.name.toUpperCase().startsWith("LICENSE")
            }
            return found ?: recurse(folder.parentFile)
        }
        return recurse(dependency)
    }

    companion object {
        private const val CONFIG = "allExternalDependencies"
        const val ROOT_TASK_NAME = "checkExternalLicenses"
        private const val PER_PROJECT_TASK_NAME = ROOT_TASK_NAME
        fun configure(project: Project) {
            val task = project.tasks.create(PER_PROJECT_TASK_NAME,
                    CheckExternalDependencyLicensesTask::class.java)
            project.configurations.create(CONFIG)
            val rootTask = project.rootProject.tasks.findByName(ROOT_TASK_NAME)
                    ?: project.rootProject.tasks.create(ROOT_TASK_NAME)
            rootTask.dependsOn(task)
        }
    }
}