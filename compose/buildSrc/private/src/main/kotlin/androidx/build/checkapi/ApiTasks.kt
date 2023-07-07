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

package androidx.build.checkapi

import androidx.build.AndroidXExtension
import androidx.build.LibraryType
import androidx.build.Release
import androidx.build.RunApiTasks
import androidx.build.Version
import androidx.build.isWriteVersionedApiFilesEnabled
import androidx.build.java.JavaCompileInputs
import androidx.build.libabigail.NativeApiTasks
import androidx.build.metalava.MetalavaTasks
import androidx.build.resources.ResourceTasks
import androidx.build.version
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.tasks.ProcessLibraryManifest
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType

sealed class ApiTaskConfig
data class LibraryApiTaskConfig(val library: LibraryExtension) : ApiTaskConfig()
object JavaApiTaskConfig : ApiTaskConfig()
object KmpApiTaskConfig : ApiTaskConfig()

fun AndroidXExtension.shouldConfigureApiTasks(): Boolean {
    if (!project.state.executed) {
        throw GradleException(
            "Project ${project.name} has not been evaluated. Extension" +
                "properties may only be accessed after the project has been evaluated."
        )
    }

    // API behavior is default for type
    if (type.checkApi is RunApiTasks.No && runApiTasks is RunApiTasks.No) {
        project.logger.info("Projects of type ${type.name} do not track API.")
        return false
    }

    when (runApiTasks) {
        // API behavior for type must have been overridden, because previous check did not trigger
        is RunApiTasks.No -> {
            project.logger.info(
                "Project ${project.name} has explicitly disabled API tasks with " +
                    "reason: ${(runApiTasks as RunApiTasks.No).reason}"
            )
            return false
        }
        is RunApiTasks.Yes -> {
            // API behavior is default for type; not overridden
            if (type.checkApi is RunApiTasks.Yes) { return true }
            // API behavior for type is overridden
            (runApiTasks as RunApiTasks.Yes).reason?.let { reason ->
                project.logger.info(
                    "Project ${project.name} has explicitly enabled API tasks " +
                        "with reason: $reason"
                )
            }
            return true
        }
        else -> {}
    }

    if (project.version !is Version) {
        project.logger.info("Project ${project.name} has no version set, ignoring API tasks.")
        return false
    }

    // If the project has an "api" directory, either because they used to track APIs or they
    // added one manually to force tracking (as recommended below), continue tracking APIs.
    if (project.hasApiFileDirectory() && !shouldRelease()) {
        project.logger.error(
            "Project ${project.name} is not published, but has an existing API " +
                "directory. Forcing API tasks enabled. Please migrate to runApiTasks=Yes."
        )
        return true
    }

    if (!shouldRelease()) {
        project.logger.info(
            "Project ${project.name} is not published, ignoring API tasks. " +
                "If you still want to track APIs, create an \"api\" directory in your project" +
                " root and run the updateApi task."
        )
        return false
    }

    return true
}

/**
 * Returns whether the project should write versioned API files, e.g. `1.1.0-alpha01.txt`.
 * <p>
 * When set to `true`, the `updateApi` task will write the current API surface to both `current.txt`
 * and `<version>.txt`. When set to `false`, only `current.txt` will be written. The default value
 * is `true`.
 */
private fun Project.shouldWriteVersionedApiFile(): Boolean {
    // Is versioned file writing disabled globally, ex. we're on a downstream branch?
    if (!project.isWriteVersionedApiFilesEnabled()) {
        return false
    }

    // Policy: Don't write versioned files for non-final API surfaces, ex. dev or alpha, or for
    // versions that should only exist in dead-end release branches, ex. rc or stable.
    if (!project.version().isFinalApi() ||
        project.version().isRC() ||
        project.version().isStable()
    ) {
        return false
    }

    return true
}

fun Project.configureProjectForApiTasks(
    config: ApiTaskConfig,
    extension: AndroidXExtension
) {
    // afterEvaluate required to read extension properties
    afterEvaluate {
        if (!extension.shouldConfigureApiTasks()) {
            return@afterEvaluate
        }

        val builtApiLocation = project.getBuiltApiLocation()
        val versionedApiLocation = project.getVersionedApiLocation()
        val currentApiLocation = project.getCurrentApiLocation()
        val outputApiLocations = if (project.shouldWriteVersionedApiFile()) {
            listOf(
                versionedApiLocation,
                currentApiLocation
            )
        } else {
            listOf(
                currentApiLocation
            )
        }

        val javaInputs: JavaCompileInputs
        val processManifest: ProcessLibraryManifest?
        when (config) {
            is LibraryApiTaskConfig -> {
                val variant = config.library.libraryVariants.find {
                    it.name == Release.DEFAULT_PUBLISH_CONFIG
                } ?: return@afterEvaluate

                javaInputs = JavaCompileInputs.fromLibraryVariant(
                    variant,
                    project,
                    // Note, in addition to androidx, bootClasspath will also include stub jars
                    // from android { useLibrary "android.foo" } block.
                    files(config.library.bootClasspath)
                )
                processManifest = config.library.buildOutputs.getByName(variant.name)
                    .processManifestProvider.get() as ProcessLibraryManifest
            }
            is KmpApiTaskConfig -> {
                javaInputs = JavaCompileInputs.fromKmpJvmTarget(project)
                processManifest = null
            }
            is JavaApiTaskConfig -> {
                val javaExtension = extensions.getByType<JavaPluginExtension>()
                val mainSourceSet = javaExtension.sourceSets.getByName("main")
                javaInputs = JavaCompileInputs.fromSourceSet(mainSourceSet, this)
                processManifest = null
            }
        }

        val baselinesApiLocation = ApiBaselinesLocation.fromApiLocation(currentApiLocation)

        MetalavaTasks.setupProject(
            project, javaInputs, extension, processManifest, baselinesApiLocation,
            builtApiLocation, outputApiLocations
        )

        if (extension.type == LibraryType.PUBLISHED_NATIVE_LIBRARY) {
            NativeApiTasks.setupProject(
                project = project,
                builtApiLocation = builtApiLocation.nativeApiDirectory,
                outputApiLocations = outputApiLocations.map { it.nativeApiDirectory }
            )
        }

        if (config is LibraryApiTaskConfig) {
            ResourceTasks.setupProject(
                project, Release.DEFAULT_PUBLISH_CONFIG,
                builtApiLocation, outputApiLocations
            )
        }
    }
}
