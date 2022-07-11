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

import androidx.build.AndroidXImplPlugin.Companion.ZIP_CONSTRAINED_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.AndroidXImplPlugin.Companion.ZIP_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.gradle.isRoot
import androidx.build.license.CheckExternalDependencyLicensesTask
import androidx.build.playground.VerifyPlaygroundGradleConfigurationTask
import androidx.build.studio.StudioTask.Companion.registerStudioTask
import androidx.build.testConfiguration.registerOwnersServiceTasks
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import androidx.build.uptodatedness.TaskUpToDateValidator
import com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.extra

abstract class AndroidXRootImplPlugin : Plugin<Project> {
    @Suppress("UnstableApiUsage")
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

        // If we're running inside Studio, validate the Android Gradle Plugin version.
        val expectedAgpVersion = System.getenv("EXPECTED_AGP_VERSION")
        if (properties.containsKey("android.injected.invoked.from.ide")) {
            if (expectedAgpVersion != ANDROID_GRADLE_PLUGIN_VERSION) {
                throw GradleException(
                    """
                    Please close and restart Android Studio.

                    Expected AGP version \"$expectedAgpVersion\" does not match actual AGP version
                    \"$ANDROID_GRADLE_PLUGIN_VERSION\". This happens when AGP is updated while
                    Studio is running and can be fixed by restarting Studio.
                    """.trimIndent()
                )
            }
        }

        val buildOnServerTask = tasks.create(
            BUILD_ON_SERVER_TASK,
            BuildOnServerTask::class.java
        )
        buildOnServerTask.cacheEvenIfNoOutputs()
        buildOnServerTask.distributionDirectory = getDistributionDirectory()
        buildOnServerTask.repositoryDirectory = getRepositoryDirectory()
        buildOnServerTask.buildId = getBuildId()
        buildOnServerTask.dependsOn(
            tasks.register(
                AndroidXImplPlugin.CREATE_AGGREGATE_BUILD_INFO_FILES_TASK,
                CreateAggregateLibraryBuildInfoFileTask::class.java
            )
        )
        buildOnServerTask.dependsOn(
            tasks.register(AndroidXImplPlugin.CREATE_LIBRARY_BUILD_INFO_FILES_TASK)
        )

        VerifyPlaygroundGradleConfigurationTask.createIfNecessary(project)?.let {
            buildOnServerTask.dependsOn(it)
        }

        val createArchiveTask = Release.getGlobalFullZipTask(this)
        buildOnServerTask.dependsOn(createArchiveTask)

        buildOnServerTask.dependsOn(
            tasks.register(
                "saveSystemStats",
                SaveSystemStatsTask::class.java
            ) { task ->
                task.outputFile.set(File(project.getDistributionDirectory(), "system_stats.txt"))
            }
        )

        extra.set("projects", ConcurrentHashMap<String, String>())
        subprojects { project ->
            project.afterEvaluate {
                if (project.plugins.hasPlugin(LibraryPlugin::class.java) ||
                    project.plugins.hasPlugin(AppPlugin::class.java)
                ) {

                    buildOnServerTask.dependsOn("${project.path}:assembleRelease")
                    if (!project.usingMaxDepVersions()) {
                        project.agpVariants.all { variant ->
                            // in AndroidX, release and debug variants are essentially the same,
                            // so we don't run the lintRelease task on the build server
                            if (!variant.name.lowercase(Locale.getDefault()).contains("release")) {
                                val taskName = "lint${variant.name.replaceFirstChar {
                                    if (it.isLowerCase()) {
                                        it.titlecase(Locale.getDefault())
                                    } else {
                                        it.toString()
                                    }
                                }}"
                                buildOnServerTask.dependsOn("${project.path}:$taskName")
                            }
                        }
                    }
                }
            }
            project.plugins.withType(JavaPlugin::class.java) {
                buildOnServerTask.dependsOn("${project.path}:jar")
            }

            project.tasks.register("validateProperties", ValidatePropertiesTask::class.java)
        }
        project.configureRootProjectForLint()

        tasks.register(AndroidXImplPlugin.BUILD_TEST_APKS_TASK)

        // NOTE: this task is used by the Github CI as well. If you make any changes here,
        // please update the .github/workflows files as well, if necessary.
        project.tasks.register(
            ZIP_TEST_CONFIGS_WITH_APKS_TASK, Zip::class.java
        ) {
            it.destinationDirectory.set(project.getDistributionDirectory())
            it.archiveFileName.set("androidTest.zip")
            it.from(project.getTestConfigDirectory())
            // We're mostly zipping a bunch of .apk files that are already compressed
            it.entryCompression = ZipEntryCompression.STORED
            // Archive is greater than 4Gb :O
            it.isZip64 = true
        }
        project.tasks.register(
            ZIP_CONSTRAINED_TEST_CONFIGS_WITH_APKS_TASK, Zip::class.java
        ) {
            it.destinationDirectory.set(project.getDistributionDirectory())
            it.archiveFileName.set("constrainedAndroidTest.zip")
            it.from(project.getConstrainedTestConfigDirectory())
            // We're mostly zipping a bunch of .apk files that are already compressed
            it.entryCompression = ZipEntryCompression.STORED
            // Archive is greater than 4Gb :O
            it.isZip64 = true
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
                                val requested = dep.requested
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

    private fun Project.setDependencyVersions() {
        androidx.build.dependencies.kotlinVersion = getVersionByName("kotlin")
        androidx.build.dependencies.kotlinNativeVersion = getVersionByName("kotlinNative")
        androidx.build.dependencies.kspVersion = getVersionByName("ksp")
        androidx.build.dependencies.agpVersion = getVersionByName("androidGradlePlugin")
        androidx.build.dependencies.guavaVersion = getVersionByName("guavaJre")
    }
}
