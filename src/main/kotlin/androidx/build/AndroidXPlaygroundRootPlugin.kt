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

import androidx.build.AndroidXRootPlugin.Companion.PROJECT_OR_ARTIFACT_EXT_NAME
import androidx.build.gradle.getByType
import androidx.build.gradle.isRoot
import androidx.build.playground.FindAffectedModulesTask
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import groovy.xml.DOMBuilder
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySubstitution
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.extra
import java.net.URI
import java.net.URL

/**
 * This plugin is used in Playground projects and adds functionality like resolving to snapshot
 * artifacts instead of projects or allowing access to public maven repositories.
 */
@Suppress("unused") // used in Playground Projects
class AndroidXPlaygroundRootPlugin : Plugin<Project> {
    private lateinit var rootProject: Project

    /**
     * List of snapshot repositories to fetch AndroidX artifacts
     */
    private lateinit var repos: PlaygroundRepositories

    /**
     * The configuration for the plugin read from the gradle properties
     */
    private lateinit var config: PlaygroundProperties

    private val projectOrArtifactClosure = KotlinClosure1<String, Any>(
        function = {
            projectOrArtifact(this)
        }
    )

    override fun apply(target: Project) {
        if (!target.isRoot) {
            throw GradleException("This plugin should only be applied to root project")
        }
        if (!target.plugins.hasPlugin(AndroidXRootPlugin::class.java)) {
            throw GradleException(
                "Must apply AndroidXRootPlugin before applying AndroidXPlaygroundRootPlugin"
            )
        }
        rootProject = target
        config = PlaygroundProperties.load(rootProject)
        repos = PlaygroundRepositories(config)
        rootProject.repositories.addPlaygroundRepositories()
        rootProject.subprojects {
            configureSubProject(it)
        }

        // TODO(b/185539993): Re-enable InvalidFragmentVersionForActivityResult which was
        //  temporarily disabled for navigation-dynamic-features-fragment since it depends on an old
        //  (stable) version of activity, which doesn't include aosp/1670206, allowing use of
        //  Fragment 1.4.x.
        target.findProject(":navigation:navigation-dynamic-features-fragment")
            ?.disableInvalidFragmentVersionForActivityResultLint()

        rootProject.tasks.register("findAffectedModules", FindAffectedModulesTask::class.java)
    }

    private fun Project.disableInvalidFragmentVersionForActivityResultLint() {
        plugins.all { plugin ->
            when (plugin) {
                is LibraryPlugin -> {
                    val libraryExtension = extensions.getByType<LibraryExtension>()
                    afterEvaluate {
                        libraryExtension.lintOptions.apply {
                            disable("InvalidFragmentVersionForActivityResult")
                        }
                    }
                }
            }
        }
    }

    private fun configureSubProject(project: Project) {
        project.repositories.addPlaygroundRepositories()
        project.extra.set(PROJECT_OR_ARTIFACT_EXT_NAME, projectOrArtifactClosure)
        project.configurations.all { configuration ->
            configuration.resolutionStrategy.dependencySubstitution.all { substitution ->
                substitution.replaceIfSnapshot()
            }
        }
    }

    /**
     * Returns a `project` if exists or the latest artifact coordinates if it doesn't.
     *
     * This can be used for optional dependencies in the playground settings.gradle files.
     *
     * @param path The project path
     * @return A Project instance if it exists or coordinates of the artifact if the project is not
     *         included in this build.
     */
    private fun projectOrArtifact(path: String): Any {
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
                    .filter { !it.isBlank() }
                    // Last element is the artifact.
                    .dropLast(1)
                    .joinToString(".")
                return "androidx.$group:${sections.last()}:$SNAPSHOT_MARKER"
            }

            throw GradleException("projectOrArtifact cannot find/replace project $path")
        }
    }

    private fun DependencySubstitution.replaceIfSnapshot() {
        val requested = this.requested
        if (requested is ModuleComponentSelector && requested.version == SNAPSHOT_MARKER) {
            val snapshotVersion = findSnapshotVersion(requested.group, requested.module)
            useTarget("${requested.group}:${requested.module}:$snapshotVersion")
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
            val metadataUrl = "${repos.snapshots}/$groupPath/$modulePath/maven-metadata.xml"
            URL(metadataUrl).openStream().use {
                val parsedMetadata = DOMBuilder.parse(it.reader())
                val versionNodes = parsedMetadata.getElementsByTagName("latest")
                if (versionNodes.length != 1) {
                    throw GradleException(
                        "AndroidXPlaygroundRootPlugin#findSnapshotVersion expected exactly one " +
                            "latest version in $metadataUrl, but got ${versionNodes.length}"
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
        repos.all.forEach { repoUrl ->
            maven {
                it.url = URI(repoUrl)
                it.metadataSources {
                    it.mavenPom()
                    it.artifact()
                }
            }
        }
        google()
        mavenCentral()
        @Suppress("DEPRECATION") // b/181908259
        jcenter()
    }

    private class PlaygroundRepositories(
        props: PlaygroundProperties
    ) {
        val snapshots =
            "https://androidx.dev/snapshots/builds/${props.snapshotBuildId}/artifacts/repository"
        val metalava = "https://androidx.dev/metalava/builds/${props.metalavaBuildId}/artifacts" +
            "/repo/m2repository"
        val doclava = "https://androidx.dev/dokka/builds/${props.dokkaBuildId}/artifacts/repository"
        val prebuilts = "https://androidx.dev/storage/prebuilts/androidx/internal/repository"
        val all = listOf(snapshots, metalava, doclava, prebuilts)
    }

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
        const val SNAPSHOT_MARKER = "REPLACE_WITH_SNAPSHOT"
    }
}
