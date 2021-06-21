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

package androidx.build.doclava

import androidx.build.SupportConfig
import androidx.build.getSdkPath
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.util.PatternSet
import java.io.File

data class DacOptions(val libraryroot: String, val dataname: String)

/**
 * Creates a task to generate an API file from the platform SDK's source and stub JARs.
 * <p>
 * This is useful for federating docs against the platform SDK when no API XML file is available.
 */
internal fun createGenerateSdkApiTask(
    project: Project,
    doclavaConfig: Configuration,
    annotationConfig: Configuration,
    destination: File
): TaskProvider<DoclavaTask> =
    project.tasks.register("generateSdkApi", DoclavaTask::class.java) { task ->
        task.apply {
            dependsOn(doclavaConfig)
            dependsOn(annotationConfig)
            description = "Generates API files for the current SDK."
            setDocletpath(doclavaConfig.resolve())
            destinationDir = destination
            // Strip the androidx.annotation classes injected by Metalava. They are not accessible.
            classpath = androidJarFile(project)
                .filter { it.path.contains("androidx/annotation") }
                .plus(project.files(annotationConfig.resolve()))
            source(
                project.zipTree(androidSrcJarFile(project))
                    .matching(PatternSet().include("**/*.java"))
            )
            apiFile = File(destination, "release/sdk_current.txt")
            generateDocs = false
            extraArgumentsBuilder.apply({
                addStringOption("stubpackages", "android.*")
                addStringOption("-release", "8")
            })
        }
    }

/**
 * List of Doclava checks that should be ignored when generating documentation.
 */
private val GENERATEDOCS_HIDDEN = listOf(105, 106, 107, 111, 112, 113, 115, 116, 121)

/**
 * Doclava checks configuration for use in generating documentation.
 */
internal val GENERATE_DOCS_CONFIG = ChecksConfig(
    warnings = emptyList(),
    hidden = GENERATEDOCS_HIDDEN + DEFAULT_DOCLAVA_CONFIG.hidden,
    errors = ((101..122) - GENERATEDOCS_HIDDEN)
)

/**
 * @return the project's Android SDK stub JAR as a File.
 */
fun androidJarFile(project: Project): FileCollection =
    project.files(
        arrayOf(
            File(
                project.getSdkPath(),
                "platforms/${SupportConfig.COMPILE_SDK_VERSION}/android.jar"
            ),
            // Allow using optional android.car APIs
            File(
                project.getSdkPath(),
                "platforms/${SupportConfig.COMPILE_SDK_VERSION}/optional/android.car.jar"
            )

        )
    )

/**
 * @return the project's Android SDK stub source JAR as a File.
 */
private fun androidSrcJarFile(project: Project): File = File(
    project.getSdkPath(),
    "platforms/${SupportConfig.COMPILE_SDK_VERSION}/android-stubs-src.jar"
)
