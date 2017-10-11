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
import java.io.File
import java.util.concurrent.Future

private const val REPO_PROP = "repo.prop"

@Suppress("unused")
class ModifiedProjectsReporter {

    private fun fetchRepoProps(rootProject: Project, buildId: Int): Future<File> {
        val workingDir = File(rootProject.buildDir, "$buildId")
        if (workingDir.exists()) {
            workingDir.deleteRecursively()
        }
        workingDir.mkdir()
        return fetch(rootProject.buildDir, buildId, "support_library_app_toolkit", REPO_PROP)
    }

    fun printModifiedProjectsbyBuildIds(gitRoot: File, buildIdFirst: Int, buildIdSecond: Int,
                              rootProject: Project) {
        val sha1 = frameworksSupportSHA(fetchRepoProps(rootProject, buildIdFirst).get())
        val sha2 = frameworksSupportSHA(fetchRepoProps(rootProject, buildIdSecond).get())
        printModifiedProjectsSHAs(gitRoot, sha1, sha2, rootProject)
    }

    fun printModifiedProjectsSHAs(gitRoot: File,
                              shaFirst: String, shaLast: String, rootProject: Project) {

        val modifiedRootProjects = rootProject.subprojects
                .filter { project ->
                    project.hasMavenArtifact()
                            && project.projectDir.canonicalPath.startsWith(gitRoot.canonicalPath)
                }
                .map { project ->
                    project to hasModification(shaFirst, shaLast, project.projectDir)
                }
                .associate { (project, future) -> project to future.get() }
                .filterValues { it }
                .keys
        val graph = PublicProjectsGraph(rootProject)
        val modifiedProjects  = markDependentProjects(graph, modifiedRootProjects)

        if (modifiedProjects.isEmpty()) {
            println("No modifications found in public modules")
            return
        }

        println("Modified Projects:")
        modifiedProjects.forEach {
            println("   $it")
        }
    }

    private fun markDependentProjects(graph: PublicProjectsGraph,
                                      modified: Set<Project>): Set<Project> {
        val result = mutableSetOf<Project>()

        fun visit(project: Project) {
            if (project in result) {
                return
            }
            result.add(project)
            graph.dependents(project).forEach(::visit)
        }
        modified.forEach(::visit)
        return result
    }

    private fun hasModification(shaFirst: String, shaLast: String, projectDir: File) = ioThread {
        // sample: git log 367f3f6c46af1591..3636a95826a9 -- lifecycle/
        val process = ProcessBuilder("git", "log", "$shaFirst..$shaLast", "--",
                projectDir.absolutePath).start()

        process.awaitSuccess("Git log")
        process.inputStream.bufferedReader().use { !it.readLine().isNullOrEmpty() }
    }
}