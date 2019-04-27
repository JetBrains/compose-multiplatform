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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskAction

/**
 * Task for verifying the androidx dependency-stability-suffix rule
 * (A library is only as stable as its lease stable dependency)
 */
open class VerifyDependencyVersionsTask : DefaultTask() {

    init {
        group = "Verification"
        description = "Task for verifying the androidx dependency-stability-suffix rule"
    }

    /**
     * Iterate through the dependencies of the project and ensure none of them are of an inferior
     * release. This means that a beta project should not have any alpha dependencies, an rc project
     * should not have any alpha or beta dependencies and a stable version should only depend on
     * other stable versions. Dependencies defined with testCompile and friends along with
     * androidTestImplementation and similars are excluded from this verification.
     */
    @TaskAction
    fun verifyDependencyVersions() {
        project.configurations.all { configuration ->
            if (!configuration.name.toLowerCase().contains("test")) {
                configuration.allDependencies.forEach { dep ->
                    if (dep.group != null && dep.group.toString().startsWith("androidx.") &&
                        !dep.group.toString().startsWith("androidx.test")) {
                        verifyDependencyVersion(dep)
                    }
                }
            }
        }
    }

    fun verifyDependencyVersion(dependency: Dependency) {
        // If the version is unspecified then treat as an alpha version. If the depending project's
        // version is unspecified then it won't matter, and if the dependency's version is
        // unspecified then any non alpha project won't be able to depend on it to ensure safety.
        val projectVersionExtra = if (project.version ==
            AndroidXExtension.DEFAULT_UNSPECIFIED_VERSION) "-alpha01"
            else Version(project.version.toString()).extra ?: ""
        val dependencyVersionExtra = if (dependency.version!! ==
            AndroidXExtension.DEFAULT_UNSPECIFIED_VERSION) "-alpha01" else
            Version(dependency.version!!).extra ?: ""
        val projectReleasePhase = releasePhase(projectVersionExtra)
        if (projectReleasePhase < 0) {
            throw GradleException("Project ${project.name} has unexpected release phase " +
                    "$projectVersionExtra")
        }
        val dependencyReleasePhase = releasePhase(dependencyVersionExtra)
        if (dependencyReleasePhase < 0) {
            throw GradleException("Dependency ${dependency.group}:${dependency.name}" +
                    ":${dependency.version} has unexpected release phase $dependencyVersionExtra")
        }
        if (dependencyReleasePhase < projectReleasePhase) {
            throw GradleException("Project ${project.name} is of version ${project.version} " +
                    "and is incompatible with dependency ${dependency.group}:${dependency.name}:" +
                    "${dependency.version}. Stable projects can only depend on stable version," +
                    " rc versions can only depend on other rc or stable dependencies beta" +
                    " versions can only depend on other beta, rc or stable versions.")
        }
    }

    fun releasePhase(versionExtra: String): Int {
        if (versionExtra == "") {
            return 4
        } else if (versionExtra.startsWith("-rc")) {
            return 3
        } else if (versionExtra.startsWith("-beta")) {
            return 2
        } else if (versionExtra.startsWith("-alpha") || versionExtra.startsWith("-qpreview")) {
            return 1
        } else {
            return -1
        }
    }
}
