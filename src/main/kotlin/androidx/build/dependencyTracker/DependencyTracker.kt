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

package androidx.build.dependencyTracker

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.Logger

/**
 * Utility class that traverses all project dependencies and discover which modules depend
 * on each other. This is mainly used by [AffectedModuleDetector] to find out which projects
 * should be run.
 */
internal class DependencyTracker constructor(
    private val rootProject: Project,
    private val logger: Logger?
) {
    private val dependentList: Map<Project, Set<Project>> by lazy {
        val result = mutableMapOf<Project, MutableSet<Project>>()
        rootProject.subprojects.forEach { project ->
            logger?.info("checking ${project.path} for dependencies")
            project.configurations.forEach { config ->
                logger?.info("checking config ${project.path}/$config for dependencies")
                config
                        .dependencies
                        .filterIsInstance(ProjectDependency::class.java)
                        .forEach {
                            logger?.info("there is a dependency from ${project.path} to " +
                                    it.dependencyProject.path)
                            result.getOrPut(it.dependencyProject) { mutableSetOf() }
                                    .add(project)
                        }
            }
        }
        result
    }

    fun findAllDependents(project: Project): Set<Project> {
        logger?.info("finding dependents of ${project.path}")
        val result = mutableSetOf<Project>()
        fun addAllDependents(project: Project) {
            if (result.add(project)) {
                dependentList[project]?.forEach(::addAllDependents)
            }
        }
        addAllDependents(project)
        logger?.info("dependents of ${project.path} is ${result.map {
            it.path
        }}")
        return result
    }
}