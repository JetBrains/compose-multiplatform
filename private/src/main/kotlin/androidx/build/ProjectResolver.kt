/*
 * Copyright 2022 The Android Open Source Project
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

import org.gradle.api.Project
import org.gradle.api.UnknownProjectException

// Resolves the given project, and if it is not found,
// throws an exception that mentions the active project subset, if any (MAIN, COMPOSE, ...)
public fun Project.resolveProject(projectSpecification: String): Project {
    try {
        return project.project(projectSpecification)
    } catch (e: UnknownProjectException) {
        val subset = project.getProjectSubset()
        val subsetDescription = if (subset == null) {
            ""
        } else {
            " in subset $subset"
        }
        throw UnknownProjectException(
            "Project $projectSpecification not found$subsetDescription",
            e
        )
    }
}

private fun Project.getProjectSubset(): String? {
    val prop = project.providers.gradleProperty("androidx.projects")
    if (prop.isPresent()) {
        return prop.get().uppercase()
    }

    val envProp = project.providers.environmentVariable("ANDROIDX_PROJECTS")
    if (envProp.isPresent()) {
        return envProp.get().uppercase()
    }
    return null
}
