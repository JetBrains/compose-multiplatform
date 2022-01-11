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

import com.google.common.annotations.VisibleForTesting
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
 * Compares the playground Gradle configuration with the main androidx Gradle configuration
 * to ensure playgrounds do not define any property in their own build that conflicts with the
 * main build.
 */
abstract class VerifyPlaygroundGradleConfigurationTask : DefaultTask() {
    @get:InputFile
    abstract val androidxProperties: RegularFileProperty

    @get:InputFile
    abstract val playgroundProperties: RegularFileProperty

    @get:InputFile
    abstract val androidxGradleWrapper: RegularFileProperty

    @get:InputFile
    abstract val playgroundGradleWrapper: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun checkPlaygroundGradleConfiguration() {
        compareProperties()
        compareGradleWrapperVersion()
        // put the success into an output so that task can be up to date.
        outputFile.get().asFile.writeText("valid", Charsets.UTF_8)
    }

    private fun compareProperties() {
        val rootProperties = loadPropertiesFile(androidxProperties.get().asFile)
        val playgroundProperties = loadPropertiesFile(playgroundProperties.get().asFile)
        validateProperties(rootProperties, playgroundProperties)
    }

    private fun compareGradleWrapperVersion() {
        val androidxGradleVersion = readGradleVersionFromWrapperProperties(
            androidxGradleWrapper.get().asFile
        )
        val playgroundGradleVersion = readGradleVersionFromWrapperProperties(
            playgroundGradleWrapper.get().asFile
        )
        if (androidxGradleVersion != playgroundGradleVersion) {
            throw GradleException("""
                Playground gradle version ($playgroundGradleVersion) must match the AndroidX main
                build gradle version ($androidxGradleVersion).
            """.trimIndent())
        }
    }

    private fun readGradleVersionFromWrapperProperties(
        file: File
    ): String {
        val distributionUrl = loadPropertiesFile(file).getProperty("distributionUrl")
        checkNotNull(distributionUrl) {
            "cannot read distribution url from gradle wrapper file: ${file.canonicalPath}"
        }
        val gradleVersion = extractGradleVersion(distributionUrl)
        return checkNotNull(gradleVersion) {
            "Failed to extract gradle version from gradle wrapper file. Input: $distributionUrl"
        }
    }

    private fun validateProperties(
        rootProperties: Properties,
        playgroundProperties: Properties
    ) {
        // ensure we don't define properties that do not match the root file
        // this includes properties that are not defined in the root androidx build as they might
        // be properties which can alter the build output. We might consider allow listing certain
        // properties in the future if necessary.
        playgroundProperties.forEach {
            val rootValue = rootProperties[it.key]
            if (rootValue != it.value && it.key != "org.gradle.jvmargs") {
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
    }

    private fun loadPropertiesFile(file: File) = file.inputStream().use { inputStream ->
        Properties().apply {
            load(inputStream)
        }
    }

    companion object {
        private const val TASK_NAME = "verifyPlaygroundGradleConfiguration"

        /**
         * Regular expression to extract the gradle version from a distributionUrl property.
         * Sample input looks like: <some-path>/gradle-7.3-rc-2-all.zip
         */
        private val GRADLE_VERSION_REGEX = """/gradle-(.+)-(all|bin)\.zip$""".toRegex()

        @VisibleForTesting // make it accessible for buildSrc-tests
        fun extractGradleVersion(
            distributionUrl: String
        ): String? {
            return GRADLE_VERSION_REGEX.find(distributionUrl)?.groupValues?.getOrNull(1)
        }

        /**
         * Creates the task to verify playground properties if an only if we have the
         * playground-common folder to check against.
         */
        fun createIfNecessary(
            project: Project
        ): TaskProvider<VerifyPlaygroundGradleConfigurationTask>? {
            return if (project.projectDir.resolve("playground-common").exists()) {
                project.tasks.register(
                    TASK_NAME,
                    VerifyPlaygroundGradleConfigurationTask::class.java
                ) {
                    it.androidxProperties.set(
                        project.layout.projectDirectory.file("gradle.properties")
                    )
                    it.playgroundProperties.set(
                        project.layout.projectDirectory.file(
                            "playground-common/androidx-shared.properties"
                        )
                    )
                    it.androidxGradleWrapper.set(
                        project.layout.projectDirectory.file(
                            "gradle/wrapper/gradle-wrapper.properties"
                        )
                    )
                    it.playgroundGradleWrapper.set(
                        project.layout.projectDirectory.file(
                            "playground-common/gradle/wrapper/gradle-wrapper.properties"
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
