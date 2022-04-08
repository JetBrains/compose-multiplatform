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

package androidx.build

import androidx.build.dependencyTracker.DependencyTracker
import androidx.build.dependencyTracker.ProjectGraph
import androidx.build.gradle.isRoot
import androidx.build.playground.FindAffectedModulesTask
import groovy.xml.DOMBuilder
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import java.net.URI
import java.net.URL

/**
 * This plugin is used in Playground projects and adds functionality like resolving to snapshot
 * artifacts instead of projects or allowing access to public maven repositories.
 */
@Suppress("unused") // used in Playground Projects
class AndroidXPlaygroundRootImplPlugin : Plugin<Project> {
    private lateinit var rootProject: Project

    /**
     * List of snapshot repositories to fetch AndroidX artifacts
     */
    private lateinit var repos: PlaygroundRepositories

    /**
     * The configuration for the plugin read from the gradle properties
     */
    private lateinit var config: PlaygroundProperties

    override fun apply(target: Project) {
        if (!target.isRoot) {
            throw GradleException("This plugin should only be applied to root project")
        }
        if (!target.plugins.hasPlugin(AndroidXRootImplPlugin::class.java)) {
            throw GradleException(
                "Must apply AndroidXRootImplPlugin before applying AndroidXPlaygroundRootImplPlugin"
            )
        }
        rootProject = target
        config = PlaygroundProperties.load(rootProject)
        repos = PlaygroundRepositories(config)
        rootProject.repositories.addPlaygroundRepositories()
        rootProject.subprojects {
            configureSubProject(it)
        }

        rootProject.tasks.register(
            "findAffectedModules",
            FindAffectedModulesTask::class.java
        ) { task ->
            task.projectGraph = ProjectGraph(rootProject)
            task.dependencyTracker = DependencyTracker(rootProject, task.logger)
        }
    }

    private fun configureSubProject(project: Project) {
        project.repositories.addPlaygroundRepositories()
        project.configurations.all { configuration ->
            configuration.resolutionStrategy.eachDependency { details ->
                val requested = details.requested
                if (requested.version == SNAPSHOT_MARKER) {
                    val snapshotVersion = findSnapshotVersion(requested.group, requested.name)
                    details.useVersion(snapshotVersion)
                }
            }
        }
    }

    /**
     * Finds the snapshot version from the AndroidX snapshot repository.
     *
     * This is initially done by reading the maven-metadata from the snapshot repository.
     * The result of that query is cached in the build file so that subsequent build requests will
     * not need to access the network.
     */
    private fun findSnapshotVersion(group: String, module: String): String {
        val snapshotVersionCache = rootProject.buildDir.resolve(
            "snapshot-version-cache/${config.snapshotBuildId}"
        )
        val groupPath = group.replace('.', '/')
        val modulePath = module.replace('.', '/')
        val metadataCacheFile = snapshotVersionCache.resolve("$groupPath/$modulePath/version.txt")
        return if (metadataCacheFile.exists()) {
            metadataCacheFile.readText(Charsets.UTF_8)
        } else {
            val metadataUrl = "${repos.snapshots.url}/$groupPath/$modulePath/maven-metadata.xml"
            URL(metadataUrl).openStream().use {
                val parsedMetadata = DOMBuilder.parse(it.reader())
                val versionNodes = parsedMetadata.getElementsByTagName("latest")
                if (versionNodes.length != 1) {
                    throw GradleException(
                        "AndroidXPlaygroundRootImplPlugin#findSnapshotVersion expected exactly " +
                            " one latest version in $metadataUrl, but got ${versionNodes.length}"
                    )
                }
                val snapshotVersion = versionNodes.item(0).textContent
                metadataCacheFile.parentFile.mkdirs()
                metadataCacheFile.writeText(snapshotVersion, Charsets.UTF_8)
                snapshotVersion
            }
        }
    }

    private fun RepositoryHandler.addPlaygroundRepositories() {
        repos.all.forEach { playgroundRepository ->
            maven { repository ->
                repository.url = URI(playgroundRepository.url)
                repository.metadataSources {
                    it.mavenPom()
                    it.artifact()
                }
                repository.content {
                    it.includeGroupByRegex(playgroundRepository.includeGroupRegex)
                }
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    private class PlaygroundRepositories(
        props: PlaygroundProperties
    ) {
        val snapshots = PlaygroundRepository(
            "https://androidx.dev/snapshots/builds/${props.snapshotBuildId}/artifacts/repository",
            includeGroupRegex = """androidx\..*"""
        )
        val metalava = PlaygroundRepository(
            "https://androidx.dev/metalava/builds/${props.metalavaBuildId}/artifacts" +
                "/repo/m2repository",
            includeGroupRegex = """com\.android\.tools\.metalava"""
        )
        val doclava = PlaygroundRepository(
            "https://androidx.dev/dokka/builds/${props.dokkaBuildId}/artifacts/repository",
            includeGroupRegex = """org\.jetbrains\.dokka"""
        )
        val prebuilts = PlaygroundRepository(
            "https://androidx.dev/storage/prebuilts/androidx/internal/repository",
            includeGroupRegex = """androidx\..*"""
        )
        val all = listOf(snapshots, metalava, doclava, prebuilts)
    }

    private data class PlaygroundRepository(
        val url: String,
        val includeGroupRegex: String
    )

    private data class PlaygroundProperties(
        val snapshotBuildId: String,
        val metalavaBuildId: String,
        val dokkaBuildId: String
    ) {
        companion object {
            fun load(project: Project): PlaygroundProperties {
                return PlaygroundProperties(
                    snapshotBuildId = project.requireProperty(PLAYGROUND_SNAPSHOT_BUILD_ID),
                    metalavaBuildId = project.requireProperty(PLAYGROUND_METALAVA_BUILD_ID),
                    dokkaBuildId = project.requireProperty(PLAYGROUND_DOKKA_BUILD_ID)
                )
            }

            private fun Project.requireProperty(name: String): String {
                return checkNotNull(findProperty(name)) {
                    "missing $name property. It must be defined in the gradle.properties file"
                }.toString()
            }
        }
    }

    companion object {
        /**
         * Returns a `project` if exists or the latest artifact coordinates if it doesn't.
         *
         * This can be used for optional dependencies in the playground settings.gradle files.
         *
         * @param path The project path
         * @return A Project instance if it exists or coordinates of the artifact if the project is not
         *         included in this build.
         */
        fun projectOrArtifact(rootProject: Project, path: String): Any {
            val requested = rootProject.findProject(path)
            if (requested != null) {
                return requested
            } else {
                val sections = path.split(":")

                if (sections[0].isNotEmpty()) {
                    throw GradleException(
                        "Expected projectOrArtifact path to start with empty section but got $path"
                    )
                }

                // Typically androidx projects have 3 sections, compose has 4.
                if (sections.size >= 3) {
                    val group = sections
                        // Filter empty sections as many declarations start with ':'
                        .filter { it.isNotBlank() }
                        // Last element is the artifact.
                        .dropLast(1)
                        .joinToString(".")
                    return "androidx.$group:${sections.last()}:$SNAPSHOT_MARKER"
                }

                throw GradleException("projectOrArtifact cannot find/replace project $path")
            }
        }
        const val SNAPSHOT_MARKER = "REPLACE_WITH_SNAPSHOT"
    }
}
