/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by Alex Hosh (n34to0@gmail.com) 2021.
 */

// usages in build scripts are not tracked properly
@file:Suppress("unused")

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import java.io.File

private fun Project.buildLocalDependenciesDir(): File =
    rootProject.gradle.gradleUserHomeDir.resolve(rootProject.findProperty("deps.rootFolder") as String)

private fun Project.buildOrganization(): String = rootProject.findProperty("deps.organization") as String

private fun Project.buildLocalRepoDir(): File = buildLocalDependenciesDir().resolve("repo")

private fun Project.ideModuleName(): String = rootProject.findProperty("intellijSdk.platform") as String

private fun Project.ideModuleVersion(): String = rootProject.findProperty("intellijSdk.version") as String

fun RepositoryHandler.buildLocalRepo(project: Project): IvyArtifactRepository = ivy {
    val baseDir = project.buildLocalRepoDir()
    url = baseDir.toURI()

    patternLayout {
        ivy("[organisation]/[module]/[revision]/[module].ivy.xml")
        ivy("[organisation]/[module]/[revision]/ivy/[module].ivy.xml")
        ivy("[organisation]/${project.ideModuleName()}/[revision]/ivy/[module].ivy.xml") // bundled plugins

        artifact("[organisation]/[module]/[revision]/artifacts/lib/[artifact](-[classifier]).[ext]")
        artifact("[organisation]/[module]/[revision]/artifacts/[artifact](-[classifier]).[ext]")
        artifact("[organisation]/intellij-core/[revision]/artifacts/[artifact](-[classifier]).[ext]")
        artifact("[organisation]/${project.ideModuleName()}/[revision]/artifacts/plugins/[module]/lib/[artifact](-[classifier]).[ext]") // bundled plugins
        artifact("[organisation]/sources/[artifact]-[revision](-[classifier]).[ext]")
        artifact("[organisation]/[module]/[revision]/[artifact](-[classifier]).[ext]")
    }

    metadataSources {
        ivyDescriptor()
    }
}

@kotlin.jvm.JvmOverloads
fun Project.intellijDep(module: String? = null) =
    "${buildOrganization()}:${module ?: ideModuleName()}:${ideModuleVersion()}"

fun Project.intellijPluginDep(plugin: String) = intellijDep(plugin)

fun Project.intellijPlatformDir(): File = buildLocalRepoDir().resolve(buildOrganization())
    .resolve(ideModuleName())
    .resolve(ideModuleVersion())
    .resolve("artifacts")

fun ModuleDependency.includeJars(vararg names: String, rootProject: Project? = null) {
    names.forEach {
        var baseName = it.removeSuffix(".jar")
        if (rootProject != null && rootProject.extensions.extraProperties.has("versions.jar.$baseName")) {
            baseName += "-${rootProject.extensions.extraProperties["versions.jar.$baseName"]}"
        }
        artifact {
            name = baseName
            type = "jar"
            extension = "jar"
        }
    }
}
