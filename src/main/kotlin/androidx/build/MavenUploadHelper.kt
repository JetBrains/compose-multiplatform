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
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.maven.MavenDeployer
import org.gradle.api.artifacts.maven.MavenPom
import org.gradle.api.tasks.Upload
import org.gradle.kotlin.dsl.withGroovyBuilder
import java.io.File

fun Project.configureMavenArtifactUpload(extension: AndroidXExtension) {
    afterEvaluate {
        if (extension.publish) {
            val mavenGroup = extension.mavenGroup?.group
            if (mavenGroup == null) {
                throw Exception("You must specify mavenGroup for $name project")
            }
            if (extension.mavenVersion == null) {
                throw Exception("You must specify mavenVersion for $name project")
            }
            val strippedGroupId = mavenGroup.substringAfterLast(".")
            if (mavenGroup.startsWith("androidx") && !name.startsWith(strippedGroupId)) {
                throw Exception("Your artifactId must start with $strippedGroupId")
            }
            group = mavenGroup
        }
    }

    apply(mapOf("plugin" to "maven"))

    // Set uploadArchives options.
    val uploadTask = tasks.getByName("uploadArchives") as Upload

    val repo = uri(rootProject.property("supportRepoOut") as File)
            ?: throw Exception("supportRepoOut not set")

    uploadTask.repositories {
        it.withGroovyBuilder {
            "mavenDeployer" {
                "repository"(mapOf("url" to repo))
            }
        }
    }

    afterEvaluate {
        if (extension.publish) {
            uploadTask.repositories.withType(MavenDeployer::class.java) { mavenDeployer ->
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

                uploadTask.doFirst {
                    val androidxDeps = HashSet<Dependency>()
                    collectDependenciesForConfiguration(androidxDeps, this, "api")
                    collectDependenciesForConfiguration(androidxDeps, this, "implementation")
                    collectDependenciesForConfiguration(androidxDeps, this, "compile")

                    mavenDeployer.getPom().whenConfigured { pom ->
                        removeTestDeps(pom)
                        assignAarTypes(pom, androidxDeps)
                        val group = extension.mavenGroup
                        if (group != null) {
                            if (group.requireSameVersion) {
                                assignSingleVersionDependenciesInGroup(pom, group.group)
                            }
                        }
                    }
                }
            }

            // Register it as part of release so that we create a Zip file for it
            Release.register(this, extension)
        } else {
            uploadTask.enabled = false
        }
    }
}

// removes dependencies having scope of "test"
private fun Project.removeTestDeps(pom: MavenPom) {
    pom.dependencies.removeAll { dep ->
        if (dep == null) {
            return@removeAll false
        }

        val getScopeMethod = dep::class.java.getDeclaredMethod("getScope")
        getScopeMethod.invoke(dep) as String == "test"
    }
}

// TODO(aurimas): remove this when Gradle bug is fixed.
// https://github.com/gradle/gradle/issues/3170
private fun Project.assignAarTypes(pom: MavenPom, androidxDeps: HashSet<Dependency>) {
    pom.dependencies.forEach { dep ->
        if (dep == null) {
            return@forEach
        }

        val getGroupIdMethod =
                dep::class.java.getDeclaredMethod("getGroupId")
        val groupId: String = getGroupIdMethod.invoke(dep) as String
        val getArtifactIdMethod =
                dep::class.java.getDeclaredMethod("getArtifactId")
        val artifactId: String = getArtifactIdMethod.invoke(dep) as String

        if (isAndroidProject(groupId, artifactId, androidxDeps)) {
            val setTypeMethod = dep::class.java.getDeclaredMethod("setType",
                    java.lang.String::class.java)
            setTypeMethod.invoke(dep, "aar")
        }
    }
}

/**
 * Specifies that every dependency in <group> refers to a single version and can't be
 * automatically promoted to a new version.
 * This will replace, for example, a version string of "1.0" with a version string of "[1.0]"
 */
private fun Project.assignSingleVersionDependenciesInGroup(pom: MavenPom, group: String) {
    pom.dependencies.forEach { dep ->
        if (dep == null) {
            return@forEach
        }
        val getGroupIdMethod =
                dep::class.java.getDeclaredMethod("getGroupId")
        val groupId: String = getGroupIdMethod.invoke(dep) as String
        if (groupId == group) {
            val getVersionMethod =
                dep::class.java.getDeclaredMethod("getVersion")
            val declaredVersion = getVersionMethod.invoke(dep) as String

            if (isVersionRange(declaredVersion)) {
                throw GradleException("Unsupported version '$declaredVersion': " +
                    "already is a version range")
            }

            val pinnedVersion = "[$declaredVersion]"

            val setVersionMethod = dep::class.java.getDeclaredMethod("setVersion",
                    java.lang.String::class.java)
            setVersionMethod.invoke(dep, pinnedVersion)
        }
    }
}

private fun isVersionRange(text: String): Boolean {
    return text.contains("[") ||
        text.contains("]") ||
        text.contains("(") ||
        text.contains(")") ||
        text.contains(",")
}

private fun collectDependenciesForConfiguration(
    androidxDependencies: MutableSet<Dependency>,
    project: Project,
    name: String
) {
    val config = project.configurations.findByName(name)
    if (config != null) {
        config.dependencies.forEach { dep ->
            if (dep.group?.startsWith("androidx.") ?: false) {
                androidxDependencies.add(dep)
            }
        }
    }
}

private fun Project.isAndroidProject(
    groupId: String,
    artifactId: String,
    deps: Set<Dependency>
): Boolean {
    for (dep in deps) {
        if (dep is ProjectDependency) {
            if (dep.group == groupId && dep.name == artifactId) {
                return dep.getDependencyProject().plugins.hasPlugin(LibraryPlugin::class.java)
            }
        }
    }
    var projectModules = project.getProjectsMap()
    if (projectModules.containsKey("$groupId:$artifactId")) {
        val localProjectVersion = project.findProject(
                projectModules.get("$groupId:$artifactId"))
        if (localProjectVersion != null) {
            return localProjectVersion.plugins.hasPlugin(LibraryPlugin::class.java)
        }
    }
    return false
}

private const val ANDROID_GIT_URL =
        "scm:git:https://android.googlesource.com/platform/frameworks/support"
