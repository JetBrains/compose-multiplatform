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

package androidx.build.playground

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.Properties

/**
 * Compares the properties file for playground projects with the main androidx properties file
 * to ensure playgrounds do not define any property in their own build that conflicts with the
 * main build.
 */
@Suppress("UnstableApiUsage") // for fileProperty
abstract class VerifyPlaygroundGradlePropertiesTask : DefaultTask() {
    @get:InputFile
    abstract val androidxProperties: RegularFileProperty

    @get:InputFile
    abstract val playgroundProperties: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun compareProperties() {
        val rootProperties = loadPropertiesFile(androidxProperties.get().asFile)
        val playgroundProperties = loadPropertiesFile(playgroundProperties.get().asFile)
        validateProperties(rootProperties, playgroundProperties)
    }

    private fun validateProperties(
        rootProperties: Properties,
        playgroundProperties: Properties
    ) {
        // ensure we don't define properties that do not match the root file
        // this includes properties that are not defined in the root androidx build as they might
        // be properties which can alter the build output. We might consider whitelisting certain
        // properties in the future if necessary.
        playgroundProperties.forEach {
            val rootValue = rootProperties[it.key]
            if (rootValue != it.value) {
                throw GradleException(
                    """
                    ${it.key} is defined as ${it.value} in playground properties but
                    it does not match the value defined in root properties file ($rootValue).
                    Having inconsistent properties in playground projects might trigger wrong
                    compilation output in the main AndroidX build, thus not allowed.
                    """.trimIndent()
                )
            }
        }
        // put the success into an output so that task can be up to date.
        outputFile.get().asFile.writeText("valid", Charsets.UTF_8)
    }

    private fun loadPropertiesFile(file: File) = file.inputStream().use { inputStream ->
        Properties().apply {
            load(inputStream)
        }
    }

    companion object {
        private const val TASK_NAME = "verifyPlaygroundGradleProperties"

        /**
         * Creates the task to verify playground properties if an only if we have the
         * playground-common folder to check against.
         */
        fun createIfNecessary(
            project: Project
        ): TaskProvider<VerifyPlaygroundGradlePropertiesTask>? {
            return if (project.projectDir.resolve("playground-common").exists()) {
                project.tasks.register(
                    TASK_NAME,
                    VerifyPlaygroundGradlePropertiesTask::class.java
                ) {
                    it.androidxProperties.set(
                        project.layout.projectDirectory.file("gradle.properties")
                    )
                    it.playgroundProperties.set(
                        project.layout.projectDirectory.file(
                            "playground-common/androidx-shared.properties"
                        )
                    )
                    it.outputFile.set(
                        project.layout.buildDirectory.file("playgroundPropertiesValidation.out")
                    )
                }
            } else {
                null
            }
        }
    }
}
