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

import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.dokka.DokkaPublicDocs
import androidx.build.dokka.DokkaSourceDocs
import androidx.build.gmaven.GMavenVersionChecker
import androidx.build.gradle.isRoot
import androidx.build.jacoco.Jacoco
import androidx.build.license.CheckExternalDependencyLicensesTask
import androidx.build.studio.StudioTask.Companion.registerStudioTask
import androidx.build.uptodatedness.TaskUpToDateValidator
import com.android.build.gradle.api.AndroidBasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.extra
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class AndroidXRootPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (!project.isRoot) {
            throw Exception("This plugin should only be applied to root project")
        }
        project.configureRootProject()
    }

    private fun Project.configureRootProject() {
        // This has to be first due to bad behavior by DiffAndDocs which is triggered on the root
        // project. It calls evaluationDependsOn on each subproject. This eagerly causes evaluation
        // *during* the root build.gradle evaluation. The subproject then applies this plugin (while
        // we're still halfway through applying it on the root). The check licenses code runs on the
        // subproject which then looks for the root project task to add itself as a dependency of.
        // Without the root project having created the task prior to DiffAndDocs running this fails.
        // TODO(alanv): do not use evaluationDependsOn in DiffAndDocs to break this cycle!
        // Create an empty task in the root which will depend on all the per-project child tasks.
        // TODO have the normal license check run here so it catches the buildscript classpath.
        tasks.register(CheckExternalDependencyLicensesTask.TASK_NAME)

        setDependencyVersions()
        configureKtlintCheckFile()
        configureCheckInvalidSuppress()

        val buildOnServerTask = tasks.create(
            AndroidXPlugin.BUILD_ON_SERVER_TASK,
            BuildOnServer::class.java
        )
        buildOnServerTask.dependsOn(
            tasks.register(
                AndroidXPlugin.CREATE_AGGREGATE_BUILD_INFO_FILES_TASK,
                CreateAggregateLibraryBuildInfoFileTask::class.java
            )
        )
        buildOnServerTask.dependsOn(
            tasks.register(AndroidXPlugin.CREATE_LIBRARY_BUILD_INFO_FILES_TASK)
        )

        extra.set("versionChecker", GMavenVersionChecker(logger))
        val createArchiveTask = Release.getGlobalFullZipTask(this)
        buildOnServerTask.dependsOn(createArchiveTask)
        val partiallyDejetifyArchiveTask = partiallyDejetifyArchiveTask(
            createArchiveTask.get().archiveFile)
        if (partiallyDejetifyArchiveTask != null)
            buildOnServerTask.dependsOn(partiallyDejetifyArchiveTask)

        val projectModules = ConcurrentHashMap<String, String>()
        extra.set("projects", projectModules)
        buildOnServerTask.dependsOn(tasks.named(CheckExternalDependencyLicensesTask.TASK_NAME))
        subprojects { project ->
            if (project.path == ":docs-runner") {
                project.tasks.all { task ->
                    if (DokkaPublicDocs.ARCHIVE_TASK_NAME == task.name ||
                        DokkaSourceDocs.ARCHIVE_TASK_NAME == task.name) {
                        buildOnServerTask.dependsOn(task)
                    }
                }
                return@subprojects
            }
            project.plugins.withType(AndroidBasePlugin::class.java) {
                buildOnServerTask.dependsOn("${project.path}:assembleDebug")
                buildOnServerTask.dependsOn("${project.path}:assembleAndroidTest")
                if (!project.rootProject.hasProperty(AndroidXPlugin.USE_MAX_DEP_VERSIONS) &&
                    project.path != ":docs-fake"
                ) {
                    buildOnServerTask.dependsOn("${project.path}:lintDebug")
                }
            }
            project.plugins.withType(JavaPlugin::class.java) {
                buildOnServerTask.dependsOn("${project.path}:jar")
            }
        }

        if (partiallyDejetifyArchiveTask != null) {
            project(":jetifier-standalone").afterEvaluate { standAloneProject ->
                partiallyDejetifyArchiveTask.configure {
                    it.dependsOn(standAloneProject.tasks.named("installDist"))
                }
                createArchiveTask.configure {
                    it.dependsOn(standAloneProject.tasks.named("dist"))
                }
            }
        }

        val buildTestApks = tasks.register(AndroidXPlugin.BUILD_TEST_APKS_TASK)
        if (project.isCoverageEnabled()) {
            val createCoverageJarTask = Jacoco.createCoverageJarTask(this)
            buildTestApks.configure {
                it.dependsOn(createCoverageJarTask)
            }
            buildOnServerTask.dependsOn(createCoverageJarTask)
            buildOnServerTask.dependsOn(Jacoco.createZipEcFilesTask(this))
            buildOnServerTask.dependsOn(Jacoco.createUberJarTask(this))
        }

        if (project.isDocumentationEnabled()) {
            val allDocsTask = DiffAndDocs.configureDiffAndDocs(
                this,
                DacOptions("androidx", "ANDROIDX_DATA"),
                listOf(RELEASE_RULE)
            )
            buildOnServerTask.dependsOn(allDocsTask)
        }

        AffectedModuleDetector.configure(gradle, this)

        // If useMaxDepVersions is set, iterate through all the project and substitute any androidx
        // artifact dependency with the local tip of tree version of the library.
        if (hasProperty(AndroidXPlugin.USE_MAX_DEP_VERSIONS)) {
            // This requires evaluating all sub-projects to create the module:project map
            // and project dependencies.
            evaluationDependsOnChildren()
            subprojects { subproject ->
                // TODO(153485458) remove most of these exceptions
                if (subproject.name != "docs-fake" &&
                    !subproject.name.contains("hilt") &&
                    subproject.name != "camera-testapp-timing" &&
                    subproject.name != "room-testapp" &&
                    subproject.name != "support-media2-test-client-previous" &&
                    subproject.name != "support-media2-test-service-previous") {

                    subproject.configurations.all { configuration ->
                        configuration.resolutionStrategy.dependencySubstitution.apply {
                            for (e in projectModules) {
                                substitute(module(e.key)).with(project(e.value))
                            }
                        }
                    }
                }
            }
        }

        registerStudioTask()

        TaskUpToDateValidator.setup(project)

        project.tasks.register("listTaskOutputs", ListTaskOutputsTask::class.java) { task ->
            task.setOutput(File(project.getDistributionDirectory(), "task_outputs.txt"))
            task.removePrefix(project.getCheckoutRoot().path)
        }
        publishInspectionArtifacts()
    }

    private fun Project.setDependencyVersions() {
        val buildVersions = (project.rootProject.property("ext") as ExtraPropertiesExtension)
            .let { it.get("build_versions") as Map<*, *> }

        fun getVersion(key: String) = checkNotNull(buildVersions[key]) {
            "Could not find a version for `$key`"
        }.toString()

        androidx.build.dependencies.kotlinVersion = getVersion("kotlin")
        androidx.build.dependencies.kotlinCoroutinesVersion = getVersion("kotlin_coroutines")
        androidx.build.dependencies.agpVersion = getVersion("agp")
        androidx.build.dependencies.lintVersion = getVersion("lint")
    }
}