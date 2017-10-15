/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.Upload

class PublicProjectsGraph(rootProject: Project) {
    private val graph: MutableMap<Project, MutableSet<Project>> = mutableMapOf()

    init {
        val logger = rootProject.logger
        rootProject.subprojects.filter { project ->
            if (project.subprojects.size != 0) {
                logger.info("PublicModuleGraph: $project ignored because it's top level")
                false
            } else {
                val hasMavenArtifact = project.hasMavenArtifact()
                if (!hasMavenArtifact) {
                    logger.info("PublicModuleGraph: $project ignored because " +
                            "it doesn't have upload task")
                }
                hasMavenArtifact
            }
        }.forEach { project ->
            addDependenciesFrom(project, "compile")
            addDependenciesFrom(project, "api")
            addDependenciesFrom(project, "implementation")
        }
    }

    fun addDependenciesFrom(project: Project, configName: String) {
        val config: Configuration = project.configurations.findByName(configName) ?: return
        config.dependencies.withType(ProjectDependency::class.java).forEach { dep ->
            graph.getOrPut(dep.dependencyProject, { mutableSetOf<Project>() }).add(project)
        }
    }

    fun dependents(project: Project): Set<Project> = graph.getOrDefault(project, mutableSetOf())
}

fun Project.hasMavenArtifact() = tasks.withType(Upload::class.java).any { task -> task.enabled }
