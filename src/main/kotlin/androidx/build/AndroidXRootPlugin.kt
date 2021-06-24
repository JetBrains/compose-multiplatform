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

import androidx.build.AndroidXPlugin.Companion.ZIP_CONSTRAINED_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.AndroidXPlugin.Companion.ZIP_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.gradle.isRoot
import androidx.build.license.CheckExternalDependencyLicensesTask
import androidx.build.playground.VerifyPlaygroundGradlePropertiesTask
import androidx.build.studio.StudioTask.Companion.registerStudioTask
import androidx.build.testConfiguration.registerOwnersServiceTasks
import androidx.build.uptodatedness.TaskUpToDateValidator
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.extra
import java.io.File
import java.util.concurrent.ConcurrentHashMap

abstract class AndroidXRootPlugin : Plugin<Project> {
    @get:javax.inject.Inject
    abstract val registry: BuildEventsListenerRegistry

    override fun apply(project: Project) {
        if (!project.isRoot) {
            throw Exception("This plugin should only be applied to root project")
        }
        project.configureRootProject()
    }

    private fun Project.configureRootProject() {
        project.validateAllAndroidxArgumentsAreRecognized()
        tasks.register("listAndroidXProperties", ListAndroidXPropertiesTask::class.java)
        setDependencyVersions()
        configureKtlintCheckFile()
        tasks.register(CheckExternalDependencyLicensesTask.TASK_NAME)

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

        VerifyPlaygroundGradlePropertiesTask.createIfNecessary(project)?.let {
            buildOnServerTask.dependsOn(it)
        }

        val createArchiveTask = Release.getGlobalFullZipTask(this)
        buildOnServerTask.dependsOn(createArchiveTask)
        val partiallyDejetifyArchiveTask = partiallyDejetifyArchiveTask(
            createArchiveTask.get().archiveFile
        )
        if (partiallyDejetifyArchiveTask != null)
            buildOnServerTask.dependsOn(partiallyDejetifyArchiveTask)

        buildOnServerTask.dependsOn(
            tasks.register(
                "saveSystemStats",
                SaveSystemStatsTask::class.java
            ) { task ->
                task.outputFile.set(File(project.getDistributionDirectory(), "system_stats.txt"))
            }
        )

        extra.set("projects", ConcurrentHashMap<String, String>())
        buildOnServerTask.dependsOn(tasks.named(CheckExternalDependencyLicensesTask.TASK_NAME))
        // Anchor task that invokes running all subprojects :validateProperties tasks which ensure that
        // Android Studio sync is able to succeed.
        val validateAllProperties = tasks.register("validateAllProperties")
        subprojects { project ->
            // Add a method for each sub project where they can declare an optional
            // dependency on a project or its latest snapshot artifact.
            // In AndroidX build, this is always enforsed to the project while in Playground
            // builds, they are converted to the latest SNAPSHOT artifact if the project is
            // not included in that playground. see: AndroidXPlaygroundRootPlugin
            project.extra.set(
                PROJECT_OR_ARTIFACT_EXT_NAME,
                KotlinClosure1<String, Project>(
                    function = {
                        // this refers to the first parameter of the closure.
                        project.project(this)
                    }
                )
            )
            project.afterEvaluate {
                if (project.plugins.hasPlugin(LibraryPlugin::class.java) ||
                    project.plugins.hasPlugin(AppPlugin::class.java)
                ) {

                    buildOnServerTask.dependsOn("${project.path}:assembleRelease")
                    if (!project.usingMaxDepVersions()) {
                        project.agpVariants.all { variant ->
                            // in AndroidX, release and debug variants are essentially the same,
                            // so we don't run the lintRelease task on the build server
                            if (!variant.name.toLowerCase().contains("release")) {
                                val taskName = "lint${variant.name.capitalize()}"
                                buildOnServerTask.dependsOn("${project.path}:$taskName")
                            }
                        }
                    }
                }
            }
            project.plugins.withType(JavaPlugin::class.java) {
                buildOnServerTask.dependsOn("${project.path}:jar")
            }

            val validateProperties = project.tasks.register(
                "validateProperties",
                ValidatePropertiesTask::class.java
            )
            validateAllProperties.configure {
                it.dependsOn(validateProperties)
            }
        }

        if (partiallyDejetifyArchiveTask != null) {
            project(":jetifier:jetifier-standalone").afterEvaluate { standAloneProject ->
                partiallyDejetifyArchiveTask.configure {
                    it.dependsOn(standAloneProject.tasks.named("installDist"))
                }
                createArchiveTask.configure {
                    it.dependsOn(standAloneProject.tasks.named("dist"))
                }
            }
        }

        tasks.register(AndroidXPlugin.BUILD_TEST_APKS_TASK)

        project.tasks.register(
            ZIP_TEST_CONFIGS_WITH_APKS_TASK, Zip::class.java
        ) {
            it.destinationDirectory.set(project.getDistributionDirectory())
            it.archiveFileName.set("androidTest.zip")
            it.from(project.getTestConfigDirectory())
            // We're mostly zipping a bunch of .apk files that are already compressed
            it.entryCompression = ZipEntryCompression.STORED
        }
        project.tasks.register(
            ZIP_CONSTRAINED_TEST_CONFIGS_WITH_APKS_TASK, Zip::class.java
        ) {
            it.destinationDirectory.set(project.getDistributionDirectory())
            it.archiveFileName.set("constrainedAndroidTest.zip")
            it.from(project.getConstrainedTestConfigDirectory())
            // We're mostly zipping a bunch of .apk files that are already compressed
            it.entryCompression = ZipEntryCompression.STORED
        }

        AffectedModuleDetector.configure(gradle, this)

        // Needs to be called before evaluationDependsOnChildren in usingMaxDepVersions block
        publishInspectionArtifacts()
        registerOwnersServiceTasks()

        // If useMaxDepVersions is set, iterate through all the project and substitute any androidx
        // artifact dependency with the local tip of tree version of the library.
        if (project.usingMaxDepVersions()) {
            // This requires evaluating all sub-projects to create the module:project map
            // and project dependencies.
            allprojects { project2 ->
                // evaluationDependsOnChildren isn't transitive so we must call it on each project
                project2.evaluationDependsOnChildren()
            }
            val projectModules = getProjectsMap()
            subprojects { subproject ->
                // TODO(153485458) remove most of these exceptions
                if (!subproject.name.contains("hilt") &&
                    subproject.name != "docs-public" &&
                    subproject.name != "docs-tip-of-tree" &&
                    subproject.name != "camera-testapp-timing" &&
                    subproject.name != "room-testapp" &&
                    !(
                        subproject.path.contains
                        ("media2:media2-session:version-compat-tests:client-previous")
                        ) &&
                    !(
                        subproject.path.contains
                        ("media2:media2-session:version-compat-tests:service-previous")
                        )
                ) {
                    subproject.configurations.all { configuration ->
                        configuration.resolutionStrategy.dependencySubstitution.apply {
                            all { dep ->
                                val requested = dep.getRequested()
                                if (requested is ModuleComponentSelector) {
                                    val module = requested.group + ":" + requested.module
                                    if (projectModules.containsKey(module)) {
                                        dep.useTarget(project(projectModules[module]!!))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        registerStudioTask()

        TaskUpToDateValidator.setup(project, registry)

        project.tasks.register("listTaskOutputs", ListTaskOutputsTask::class.java) { task ->
            task.setOutput(File(project.getDistributionDirectory(), "task_outputs.txt"))
            task.removePrefix(project.getCheckoutRoot().path)
        }
    }

    @Suppress("UnstableApiUsage")
    private fun Project.setDependencyVersions() {
        val libs = project.extensions.getByType(
            VersionCatalogsExtension::class.java
        ).find("libs").get()
        fun getVersion(key: String): String {
            val version = libs.findVersion(key)
            return if (version.isPresent) {
                version.get().requiredVersion
            } else {
                throw GradleException("Could not find a version for `$key`")
            }
        }
        androidx.build.dependencies.kotlinVersion = getVersion("kotlin")
        androidx.build.dependencies.kspVersion = getVersion("ksp")
        androidx.build.dependencies.agpVersion = getVersion("androidGradlePlugin")
        androidx.build.dependencies.guavaVersion = getVersion("guavaJre")
    }

    companion object {
        const val PROJECT_OR_ARTIFACT_EXT_NAME = "projectOrArtifact"
    }
}
