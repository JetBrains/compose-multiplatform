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

package androidx.build

import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.maven.MavenDeployer
import org.gradle.api.tasks.Upload
import org.gradle.kotlin.dsl.withGroovyBuilder
import java.io.File

fun apply(project: Project, extension: SupportLibraryExtension) {
    project.afterEvaluate {
        if (extension.publish) {
            if (extension.mavenGroup == null) {
                throw Exception("You must specify mavenGroup for ${project.name}  project")
            }
            if (extension.mavenVersion == null) {
                throw Exception("You must specify mavenVersion for ${project.name}  project")
            }
            project.group = extension.mavenGroup!!
            project.version = extension.mavenVersion.toString()
        }
    }

    project.apply(mapOf("plugin" to "maven"))

    // Set uploadArchives options.
    val uploadTask = project.tasks.getByName("uploadArchives") as Upload

    val repo = project.uri(project.rootProject.property("supportRepoOut") as File)
            ?: throw Exception("supportRepoOut not set")

    uploadTask.repositories {
        it.withGroovyBuilder {
            "mavenDeployer" {
                "repository"(mapOf("url" to repo))
            }
        }
    }

    project.afterEvaluate {
        if (extension.publish) {
            uploadTask.repositories.withType(MavenDeployer::class.java) { mavenDeployer ->
                mavenDeployer.isUniqueVersion = true

                mavenDeployer.getPom().project {
                    it.withGroovyBuilder {
                        "name"(extension.name)
                        "description"(extension.description)
                        "url"(extension.url)
                        "inceptionYear"(extension.inceptionYear)

                        "licenses" {
                            "license" {
                                "name"("The Apache Software License, Version 2.0")
                                "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                "distribution"("repo")
                            }
                            for (license in extension.getLicenses()) {
                                "license" {
                                    "name"(license.name)
                                    "url"(license.url)
                                    "distribution"("repo")
                                }
                            }
                        }

                        "scm" {
                            "url"("http://source.android.com")
                            "connection"(ANDROID_GIT_URL)
                        }

                        "developers" {
                            "developer" {
                                "name"("The Android Open Source Project")
                            }
                        }
                    }
                }

                // TODO(aurimas): remove this when Gradle bug is fixed.
                // https://github.com/gradle/gradle/issues/3170
                uploadTask.doFirst {
                    val allDeps = HashSet<ProjectDependency>()
                    collectDependenciesForConfiguration(allDeps, project, "api")
                    collectDependenciesForConfiguration(allDeps, project, "implementation")
                    collectDependenciesForConfiguration(allDeps, project, "compile")

                    mavenDeployer.getPom().whenConfigured {
                        it.dependencies.forEach { dep ->
                            if (dep == null) {
                                return@forEach
                            }

                            val getGroupIdMethod =
                                    dep::class.java.getDeclaredMethod("getGroupId")
                            val groupId: String = getGroupIdMethod.invoke(dep) as String
                            val getArtifactIdMethod =
                                    dep::class.java.getDeclaredMethod("getArtifactId")
                            val artifactId: String = getArtifactIdMethod.invoke(dep) as String

                            if (isAndroidProject(groupId, artifactId, allDeps)) {
                                val setTypeMethod = dep::class.java.getDeclaredMethod("setType",
                                        java.lang.String::class.java)
                                setTypeMethod.invoke(dep, "aar")
                            }
                        }
                    }
                }
            }

            // Before the upload, make sure the repo is ready.
            uploadTask.dependsOn(project.rootProject.tasks.getByName("prepareRepo"))

            // Make the mainUpload depend on this uploadTask one.
            project.rootProject.tasks.getByName("mainUpload").dependsOn(uploadTask)
        } else {
            uploadTask.enabled = false
        }
    }
}

private fun collectDependenciesForConfiguration(
        projectDependencies: MutableSet<ProjectDependency>,
        project: Project,
        name: String
) {
    val config = project.configurations.findByName(name)
    if (config != null) {
        config.dependencies.withType(ProjectDependency::class.java).forEach {
            dep -> projectDependencies.add(dep)
        }
    }
}

private fun isAndroidProject(
        groupId: String,
        artifactId: String,
        deps: Set<ProjectDependency>
): Boolean {
    for (dep in deps) {
        if (dep.group == groupId && dep.name == artifactId) {
            return dep.getDependencyProject().plugins.hasPlugin(LibraryPlugin::class.java)
        }
    }
    return false
}

private const val ANDROID_GIT_URL =
        "scm:git:https://android.googlesource.com/platform/frameworks/support"