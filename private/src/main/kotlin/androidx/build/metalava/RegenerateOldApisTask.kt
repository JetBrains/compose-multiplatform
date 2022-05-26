/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build.metalava

import androidx.build.Version
import androidx.build.checkapi.getApiFileVersion
import androidx.build.checkapi.getVersionedApiLocation
import androidx.build.checkapi.isValidArtifactVersion
import androidx.build.getAndroidJar
import androidx.build.getCheckoutRoot
import androidx.build.java.JavaCompileInputs
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.ivyservice.DefaultLenientConfiguration
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/** Generate API signature text files using previously built .jar/.aar artifacts. */
@CacheableTask
abstract class RegenerateOldApisTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    @Input
    var generateRestrictToLibraryGroupAPIs = true

    @TaskAction
    fun exec() {
        val groupId = project.group.toString()
        val artifactId = project.name
        val internalPrebuiltsDir = File(project.getCheckoutRoot(), "prebuilts/androidx/internal")
        val projectPrebuiltsDir =
            File(internalPrebuiltsDir, groupId.replace(".", "/") + "/" + artifactId)

        val artifactVersions = listVersions(projectPrebuiltsDir)

        var prevApiFileVersion = getApiFileVersion(project.version as Version)
        for (artifactVersion in artifactVersions.reversed()) {
            val apiFileVersion = getApiFileVersion(artifactVersion)
            // If two artifacts correspond to the same API file, don't regenerate the
            // same api file again
            if (apiFileVersion != prevApiFileVersion) {
                regenerate(project.rootProject, groupId, artifactId, artifactVersion)
                prevApiFileVersion = apiFileVersion
            }
        }
    }

    // Returns all (valid) artifact versions that appear to exist in <dir>
    fun listVersions(dir: File): List<Version> {
        val pathNames: Array<String> = dir.list() ?: arrayOf()
        val files = pathNames.map({ name -> File(dir, name) })
        val subdirs = files.filter({ child -> child.isDirectory() })
        val versions = subdirs.map({ child -> Version(child.name) })
        val validVersions = versions.filter({ v -> isValidArtifactVersion(v) })
        return validVersions.sorted()
    }

    fun regenerate(
        runnerProject: Project,
        groupId: String,
        artifactId: String,
        version: Version
    ) {
        val mavenId = "$groupId:$artifactId:$version"
        val inputs: JavaCompileInputs?
        try {
            inputs = getFiles(runnerProject, mavenId)
        } catch (e: DefaultLenientConfiguration.ArtifactResolveException) {
            runnerProject.logger.info("Ignoring missing artifact $mavenId: $e")
            return
        }

        val outputApiLocation = project.getVersionedApiLocation(version)
        if (outputApiLocation.publicApiFile.exists()) {
            project.logger.lifecycle("Regenerating $mavenId")
            generateApi(
                project.getMetalavaClasspath(), inputs, outputApiLocation, ApiLintMode.Skip,
                generateRestrictToLibraryGroupAPIs, workerExecutor
            )
        }
    }

    fun getFiles(runnerProject: Project, mavenId: String): JavaCompileInputs {
        val jars = getJars(runnerProject, mavenId)
        val sources = getSources(runnerProject, mavenId + ":sources")

        return JavaCompileInputs(sources, jars, project.getAndroidJar())
    }

    fun getJars(runnerProject: Project, mavenId: String): FileCollection {
        val configuration = runnerProject.configurations.detachedConfiguration(
            runnerProject.dependencies.create("$mavenId")
        )
        val resolvedConfiguration = configuration.resolvedConfiguration.resolvedArtifacts
        val dependencyFiles = resolvedConfiguration.map({ artifact ->
            artifact.file
        })

        val jars = dependencyFiles.filter({ file -> file.name.endsWith(".jar") })
        val aars = dependencyFiles.filter({ file -> file.name.endsWith(".aar") })
        val classesJars = aars.map({ aar ->
            val tree = project.zipTree(aar)
            val classesJar = tree.matching { filter: PatternFilterable ->
                filter.include("classes.jar")
            }.single()
            classesJar
        })
        val embeddedLibs = getEmbeddedLibs(runnerProject, mavenId)
        val undeclaredJarDeps = getUndeclaredJarDeps(runnerProject, mavenId)
        return runnerProject.files(jars + classesJars + embeddedLibs + undeclaredJarDeps)
    }

    fun getUndeclaredJarDeps(runnerProject: Project, mavenId: String): FileCollection {
        if (mavenId.startsWith("androidx.wear:wear:")) {
            return runnerProject.files("wear/wear_stubs/com.google.android.wearable-stubs.jar")
        }
        return runnerProject.files()
    }

    fun getSources(runnerProject: Project, mavenId: String): FileCollection {
        val configuration = runnerProject.configurations.detachedConfiguration(
            runnerProject.dependencies.create(mavenId)
        )
        configuration.isTransitive = false

        val sanitizedMavenId = mavenId.replace(":", "-")
        val unzippedDir = File("${runnerProject.buildDir.path}/sources-unzipped/$sanitizedMavenId")
        runnerProject.copy({ copySpec ->
            copySpec.from(runnerProject.zipTree(configuration.singleFile))
            copySpec.into(unzippedDir)
        })
        return project.files(unzippedDir)
    }

    fun getEmbeddedLibs(runnerProject: Project, mavenId: String): Collection<File> {
        val configuration = runnerProject.configurations.detachedConfiguration(
            runnerProject.dependencies.create(mavenId)
        )
        configuration.isTransitive = false

        val sanitizedMavenId = mavenId.replace(":", "-")
        val unzippedDir = File("${runnerProject.buildDir.path}/aars-unzipped/$sanitizedMavenId")
        runnerProject.copy({ copySpec ->
            copySpec.from(runnerProject.zipTree(configuration.singleFile))
            copySpec.into(unzippedDir)
        })
        val libsDir = File(unzippedDir, "libs")
        if (libsDir.exists()) {
            return libsDir.listFiles().toList()
        }

        return listOf()
    }
}
