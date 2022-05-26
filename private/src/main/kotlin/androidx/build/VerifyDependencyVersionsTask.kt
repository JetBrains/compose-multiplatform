/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.setProperty
import org.gradle.api.tasks.CacheableTask

/**
 * Task for verifying the androidx dependency-stability-suffix rule
 * (A library is only as stable as its least stable dependency)
 */
@CacheableTask
abstract class VerifyDependencyVersionsTask : DefaultTask() {

    init {
        group = "Verification"
        description = "Task for verifying the androidx dependency-stability-suffix rule"
    }

    @get:Input
    abstract val version: Property<String>

    @get:Input
    val androidXDependencySet: SetProperty<AndroidXDependency> = project.objects.setProperty()

    /**
     * Iterate through the dependencies of the project and ensure none of them are of an inferior
     * release. This means that a beta project should not have any alpha dependencies, an rc project
     * should not have any alpha or beta dependencies and a stable version should only depend on
     * other stable versions. Dependencies defined with testCompile and friends along with
     * androidTestImplementation and similar are excluded from this verification.
     */
    @TaskAction
    fun verifyDependencyVersions() {
        androidXDependencySet.get().forEach { dependency ->
            verifyDependencyVersion(dependency)
        }
    }

    private fun verifyDependencyVersion(dependency: AndroidXDependency) {
        // If the version is unspecified then treat as an alpha version. If the depending project's
        // version is unspecified then it won't matter, and if the dependency's version is
        // unspecified then any non alpha project won't be able to depend on it to ensure safety.
        val projectVersionExtra = if (version.get() ==
            AndroidXExtension.DEFAULT_UNSPECIFIED_VERSION
        ) {
            "-alpha01"
        } else {
            Version(version.get()).extra ?: ""
        }
        val dependencyVersionExtra = if (dependency.version ==
            AndroidXExtension.DEFAULT_UNSPECIFIED_VERSION
        ) {
            "-alpha01"
        } else {
            Version(dependency.version).extra ?: ""
        }
        val projectReleasePhase = releasePhase(projectVersionExtra)
        if (projectReleasePhase < 0) {
            throw GradleException(
                "Project has unexpected release phase $projectVersionExtra"
            )
        }
        val dependencyReleasePhase = releasePhase(dependencyVersionExtra)
        if (dependencyReleasePhase < 0) {
            throw GradleException(
                "Dependency ${dependency.group}:${dependency.name}" +
                    ":${dependency.version} has unexpected release phase $dependencyVersionExtra"
            )
        }
        if (dependencyReleasePhase < projectReleasePhase) {
            throw GradleException(
                "Project with version ${version.get()} may " +
                    "not take a dependency on less-stable artifact ${dependency.group}:" +
                    "${dependency.name}:${dependency.version} for configuration " +
                    "${dependency.configurationName}. Dependency versions must be at least as " +
                    "stable as the project version."
            )
        }
    }

    private fun releasePhase(versionExtra: String): Int {
        return if (versionExtra == "") {
            4
        } else if (versionExtra.startsWith("-rc")) {
            3
        } else if (versionExtra.startsWith("-beta")) {
            2
        } else if (versionExtra.startsWith("-alpha") || versionExtra.startsWith("-qpreview") ||
            versionExtra.startsWith("-dev")
        ) {
            1
        } else {
            -1
        }
    }
}

data class AndroidXDependency(
    val group: String,
    val name: String,
    val version: String,
    val configurationName: String
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID = 344435634564L
    }
}

internal fun Project.createVerifyDependencyVersionsTask():
    TaskProvider<VerifyDependencyVersionsTask>? {
    /**
     * Ignore -Pandroidx.useMaxDepVersions when verifying dependency versions because it is a
     * hypothetical build which is only intended to check for forward compatibility.
     */
    if (project.usingMaxDepVersions()) {
        return null
    }

    val taskProvider = tasks.register(
        "verifyDependencyVersions",
        VerifyDependencyVersionsTask::class.java
    ) { task ->
        task.version.set(project.version.toString())
        task.androidXDependencySet.set(project.provider {
            val dependencies = mutableSetOf<AndroidXDependency>()
            project.configurations.filter(::shouldVerifyConfiguration).forEach { configuration ->
                configuration.allDependencies.filter(
                    ::shouldVerifyDependency
                ).forEach { dependency ->
                    dependencies.add(
                        AndroidXDependency(
                            dependency.group!!,
                            dependency.name,
                            dependency.version!!,
                            configuration.name
                        )
                    )
                }
            }
            dependencies
        })
        task.cacheEvenIfNoOutputs()
    }
    addToBuildOnServer(taskProvider)
    return taskProvider
}

private fun shouldVerifyConfiguration(configuration: Configuration): Boolean {
    // Only verify configurations that are exported to POM. In an ideal world, this would be an
    // inclusion derived from the mappings used by the Maven Publish Plugin; however, since we
    // don't have direct access to those, this should remain an exclusion list.
    val name = configuration.name

    // Don't check any Android-specific variants of Java plugin configurations -- releaseApi for
    // api, debugImplementation for implementation, etc. -- or test configurations.
    if (name.startsWith("androidTest")) return false
    if (name.startsWith("androidAndroidTest")) return false
    if (name.startsWith("androidCommonTest")) return false
    if (name.startsWith("debug")) return false
    if (name.startsWith("androidDebug")) return false
    if (name.startsWith("release")) return false
    if (name.startsWith("test")) return false

    // Don't check any tooling configurations.
    if (name == "annotationProcessor") return false
    if (name == "errorprone") return false
    if (name.startsWith("lint")) return false
    if (name == "metalava") return false

    // Don't check any configurations that directly bundle the dependencies with the output
    if (name == "bundleInside") return false
    if (name == "embedThemesDebug") return false
    if (name == "embedThemesRelease") return false

    // Don't check any compile-only configurations
    if (name.startsWith("compile")) return false

    // allow tip of tree compose compiler
    if (name.startsWith("kotlinPlugin")) return false

    // Don't check Hilt compile-only configurations
    if (name.startsWith("hiltCompileOnly")) return false
    return true
}

private fun shouldVerifyDependency(dependency: Dependency): Boolean {
    // Only verify dependencies within the scope of our versioning policies.
    if (dependency.group == null) return false
    if (!dependency.group.toString().startsWith("androidx.")) return false
    if (dependency.name == "annotation-sampled") return false
    if (dependency.version == AndroidXPlaygroundRootImplPlugin.SNAPSHOT_MARKER) {
        // This only happens in playground builds where this magic version gets replaced with
        // the version from the snapshotBuildId defined in playground-common/playground.properties.
        // It is best to leave their validation to the aosp build to ensure it is the right
        // version.
        return false
    }
    return true
}
