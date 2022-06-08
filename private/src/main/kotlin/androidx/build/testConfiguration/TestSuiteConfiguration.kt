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

@file:Suppress("UnstableApiUsage") // Incubating AGP APIs

package androidx.build.testConfiguration

import androidx.build.AndroidXExtension
import androidx.build.AndroidXImplPlugin
import androidx.build.AndroidXImplPlugin.Companion.ZIP_CONSTRAINED_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.AndroidXImplPlugin.Companion.ZIP_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.asFilenamePrefix
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.getConstrainedTestConfigDirectory
import androidx.build.getSupportRootFolder
import androidx.build.getTestConfigDirectory
import androidx.build.hasAndroidTestSourceCode
import androidx.build.hasBenchmarkPlugin
import androidx.build.isPresubmitBuild
import androidx.build.renameApkForTesting
import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.getByType
import java.io.File

/**
 * Creates and configures the test config generation task for a project. Configuration includes
 * populating the task with relevant data from the first 4 params, and setting whether the task
 * is enabled.
 *
 * @param overrideProject Allows the config task for one project to get registered to an
 * alternative project. Default is for the project to register the new config task to itself
 */
fun Project.createTestConfigurationGenerationTask(
    variantName: String,
    artifacts: Artifacts,
    minSdk: Int,
    testRunner: String,
    overrideProject: Project = this
) {
    val xmlName = "${path.asFilenamePrefix()}$variantName.xml"
    rootProject.tasks.named("createModuleInfo").configure {
        it as ModuleInfoGenerator
        it.testModules.add(
            TestModule(
                name = xmlName,
                path = listOf(projectDir.toRelativeString(getSupportRootFolder()))
            )
        )
    }
    val generateTestConfigurationTask = overrideProject.tasks.register(
        "${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}$variantName",
        GenerateTestConfigurationTask::class.java
    ) { task ->
        task.testFolder.set(artifacts.get(SingleArtifact.APK))
        task.testLoader.set(artifacts.getBuiltArtifactsLoader())
        task.outputXml.fileValue(File(getTestConfigDirectory(), xmlName))
        task.constrainedOutputXml.fileValue(File(getConstrainedTestConfigDirectory(), xmlName))
        task.presubmit.set(isPresubmitBuild())
        // Disable work tests on < API 18: b/178127496
        if (path.startsWith(":work:")) {
            task.minSdk.set(maxOf(18, minSdk))
        } else {
            task.minSdk.set(minSdk)
        }
        val hasBenchmarkPlugin = hasBenchmarkPlugin()
        task.hasBenchmarkPlugin.set(hasBenchmarkPlugin)
        if (hasBenchmarkPlugin) {
            task.benchmarkRunAlsoInterpreted.set(
                extensions.getByType<AndroidXExtension>().benchmarkRunAlsoInterpreted
            )
        }
        task.testRunner.set(testRunner)
        task.testProjectPath.set(path)
        val detector = AffectedModuleDetector.getInstance(project)
        task.affectedModuleDetectorSubset.set(
            project.provider {
                detector.getSubset(task)
            }
        )
        AffectedModuleDetector.configureTaskGuard(task)
    }
    // Disable xml generation for projects that have no test sources
    this.afterEvaluate {
        generateTestConfigurationTask.configure {
            it.enabled = this.hasAndroidTestSourceCode()
        }
    }
    this.rootProject.tasks.findByName(ZIP_TEST_CONFIGS_WITH_APKS_TASK)!!
        .dependsOn(generateTestConfigurationTask)
    this.rootProject.tasks.findByName(ZIP_CONSTRAINED_TEST_CONFIGS_WITH_APKS_TASK)!!
        .dependsOn(generateTestConfigurationTask)
}

/**
 * Further configures the test config generation task for a project. This only gets called when
 * there is a test app in addition to the instrumentation app, and the only thing it configures is
 * the location of the testapp.
 *
 * @param overrideProject Allows the config task for one project to get registered to an
 * alternative project. Default is for the project to register the new config task to itself
 */
fun Project.addAppApkToTestConfigGeneration(overrideProject: Project = this) {
    if (project.isMacrobenchmarkTarget()) {
        return
    }

    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        onVariants(selector().withBuildType("debug")) { appVariant ->
            overrideProject.tasks.named(
                AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK +
                    "${appVariant.name}AndroidTest"
            ) { configTask ->
                configTask as GenerateTestConfigurationTask
                configTask.appFolder.set(appVariant.artifacts.get(SingleArtifact.APK))
                configTask.appLoader.set(appVariant.artifacts.getBuiltArtifactsLoader())
                configTask.appProjectPath.set(overrideProject.path)
            }
        }
    }
}

/**
 * Configures the test zip task to include the project's apk
 */
fun addToTestZips(project: Project, packageTask: PackageAndroidArtifact) {
    project.rootProject.tasks.named(ZIP_TEST_CONFIGS_WITH_APKS_TASK) { task ->
        task as Zip
        val projectPath = project.path
        val hasBenchmarkPlugin = project.providers.provider { project.hasBenchmarkPlugin() }
        task.from(packageTask.outputDirectory) {
            it.include("*.apk")
            it.duplicatesStrategy = DuplicatesStrategy.FAIL
            it.rename { fileName ->
                fileName.renameApkForTesting(projectPath, hasBenchmarkPlugin.get())
            }
        }
        task.dependsOn(packageTask)
    }
    project.rootProject.tasks.named(ZIP_CONSTRAINED_TEST_CONFIGS_WITH_APKS_TASK) { task ->
        task as Zip
        val projectPath = project.path
        val hasBenchmarkPlugin = project.providers.provider { project.hasBenchmarkPlugin() }
        task.from(packageTask.outputDirectory) {
            it.include("*.apk")
            it.duplicatesStrategy = DuplicatesStrategy.FAIL
            it.rename { fileName ->
                fileName.renameApkForTesting(projectPath, hasBenchmarkPlugin.get())
            }
        }
        task.dependsOn(packageTask)
    }
}

private fun getOrCreateMediaTestConfigTask(project: Project, isMedia2: Boolean):
    TaskProvider<GenerateMediaTestConfigurationTask> {
        val mediaPrefix = getMediaConfigTaskPrefix(isMedia2)
        val parentProject = project.parent!!
        if (!parentProject.tasks.withType(GenerateMediaTestConfigurationTask::class.java)
            .names.contains(
                    "support-$mediaPrefix-test${
                    AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK
                    }"
                )
        ) {
            val task = parentProject.tasks.register(
                "support-$mediaPrefix-test${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}",
                GenerateMediaTestConfigurationTask::class.java
            ) { task ->
                AffectedModuleDetector.configureTaskGuard(task)
                val detector = AffectedModuleDetector.getInstance(project)
                task.affectedModuleDetectorSubset.set(
                    project.provider {
                        detector.getSubset(task)
                    }
                )
            }
            project.rootProject.tasks.findByName(ZIP_TEST_CONFIGS_WITH_APKS_TASK)!!
                .dependsOn(task)
            project.rootProject.tasks.findByName(ZIP_CONSTRAINED_TEST_CONFIGS_WITH_APKS_TASK)!!
                .dependsOn(task)
            return task
        } else {
            return parentProject.tasks.withType(GenerateMediaTestConfigurationTask::class.java)
                .named(
                    "support-$mediaPrefix-test${
                    AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK
                    }"
                )
        }
    }

private fun getMediaConfigTaskPrefix(isMedia2: Boolean): String {
    return if (isMedia2) "media2" else "media"
}

fun Project.createOrUpdateMediaTestConfigurationGenerationTask(
    variantName: String,
    artifacts: Artifacts,
    minSdk: Int,
    testRunner: String,
    isMedia2: Boolean
) {
    val mediaPrefix = getMediaConfigTaskPrefix(isMedia2)
    val mediaTask = getOrCreateMediaTestConfigTask(this, isMedia2)
    mediaTask.configure {
        it as GenerateMediaTestConfigurationTask
        if (this.name.contains("client")) {
            if (this.name.contains("previous")) {
                it.clientPreviousFolder.set(artifacts.get(SingleArtifact.APK))
                it.clientPreviousLoader.set(artifacts.getBuiltArtifactsLoader())
                it.clientPreviousPath.set(this.path)
            } else {
                it.clientToTFolder.set(artifacts.get(SingleArtifact.APK))
                it.clientToTLoader.set(artifacts.getBuiltArtifactsLoader())
                it.clientToTPath.set(this.path)
            }
        } else {
            if (this.name.contains("previous")) {
                it.servicePreviousFolder.set(artifacts.get(SingleArtifact.APK))
                it.servicePreviousLoader.set(artifacts.getBuiltArtifactsLoader())
                it.servicePreviousPath.set(this.path)
            } else {
                it.serviceToTFolder.set(artifacts.get(SingleArtifact.APK))
                it.serviceToTLoader.set(artifacts.getBuiltArtifactsLoader())
                it.serviceToTPath.set(this.path)
            }
        }
        it.clientPreviousServiceToT.fileValue(
            File(
                this.getTestConfigDirectory(),
                "${mediaPrefix}ClientPreviousServiceToT$variantName.xml"
            )
        )
        it.clientToTServicePrevious.fileValue(
            File(
                this.getTestConfigDirectory(),
                "${mediaPrefix}ClientToTServicePrevious$variantName.xml"
            )
        )
        it.clientToTServiceToT.fileValue(
            File(
                this.getTestConfigDirectory(),
                "${mediaPrefix}ClientToTServiceToT$variantName.xml"
            )
        )
        it.constrainedClientPreviousServiceToT.fileValue(
            File(
                this.getConstrainedTestConfigDirectory(),
                "${mediaPrefix}ClientPreviousServiceToT$variantName.xml"
            )
        )
        it.constrainedClientToTServicePrevious.fileValue(
            File(
                this.getConstrainedTestConfigDirectory(),
                "${mediaPrefix}ClientToTServicePrevious$variantName.xml"
            )
        )
        it.constrainedClientToTServiceToT.fileValue(
            File(
                this.getConstrainedTestConfigDirectory(),
                "${mediaPrefix}ClientToTServiceToT$variantName.xml"
            )
        )
        it.minSdk.set(minSdk)
        it.testRunner.set(testRunner)
        it.presubmit.set(isPresubmitBuild())
        AffectedModuleDetector.configureTaskGuard(it)
    }
}

private fun Project.getOrCreateMacrobenchmarkConfigTask(variantName: String):
    TaskProvider<GenerateTestConfigurationTask> {
        val parentProject = this.parent!!
        return if (
            parentProject.tasks.withType(GenerateTestConfigurationTask::class.java).isEmpty()
        ) {
            parentProject.tasks.register(
                "${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}$variantName",
                GenerateTestConfigurationTask::class.java
            )
        } else {
            parentProject.tasks.withType(GenerateTestConfigurationTask::class.java)
                .named("${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}$variantName")
        }
    }

private fun Project.configureMacrobenchmarkConfigTask(
    variantName: String,
    artifacts: Artifacts,
    minSdk: Int,
    testRunner: String
) {
    val configTask = getOrCreateMacrobenchmarkConfigTask(variantName)
    if (path.endsWith("macrobenchmark")) {
        configTask.configure { task ->
            task.testFolder.set(artifacts.get(SingleArtifact.APK))
            task.testLoader.set(artifacts.getBuiltArtifactsLoader())
            task.outputXml.fileValue(
                File(
                    this.getTestConfigDirectory(),
                    "${this.path.asFilenamePrefix()}$variantName.xml"
                )
            )
            task.constrainedOutputXml.fileValue(
                File(
                    this.getTestConfigDirectory(),
                    "${this.path.asFilenamePrefix()}$variantName.xml"
                )
            )
            task.minSdk.set(minSdk)
            task.hasBenchmarkPlugin.set(this.hasBenchmarkPlugin())
            task.testRunner.set(testRunner)
            task.testProjectPath.set(this.path)
            task.presubmit.set(isPresubmitBuild())
            val detector = AffectedModuleDetector.getInstance(project)
            task.affectedModuleDetectorSubset.set(
                project.provider {
                    detector.getSubset(task)
                }
            )

            AffectedModuleDetector.configureTaskGuard(task)
        }
        // Disable xml generation for projects that have no test sources
        this.afterEvaluate {
            configTask.configure {
                it.enabled = this.hasAndroidTestSourceCode()
            }
        }
        this.rootProject.tasks.findByName(ZIP_TEST_CONFIGS_WITH_APKS_TASK)!!
            .dependsOn(configTask)
        this.rootProject.tasks.findByName(ZIP_CONSTRAINED_TEST_CONFIGS_WITH_APKS_TASK)!!
            .dependsOn(configTask)
    } else if (isMacrobenchmarkTarget()) {
        configTask.configure { task ->
            task.appFolder.set(artifacts.get(SingleArtifact.APK))
            task.appLoader.set(artifacts.getBuiltArtifactsLoader())
            task.appProjectPath.set(path)
        }
    }
}

/**
 * Tells whether this project is the macrobenchmark-target project
 */
fun Project.isMacrobenchmarkTarget(): Boolean {
    return path.endsWith("macrobenchmark-target")
}

fun Project.configureTestConfigGeneration(baseExtension: BaseExtension) {
    extensions.getByType(AndroidComponentsExtension::class.java).apply {
        onVariants { variant ->
            var name: String? = null
            var artifacts: Artifacts? = null
            when {
                variant is HasAndroidTest -> {
                    name = variant.androidTest?.name
                    artifacts = variant.androidTest?.artifacts
                }
                project.plugins.hasPlugin("com.android.test") -> {
                    name = variant.name
                    artifacts = variant.artifacts
                }
            }
            if (name == null || artifacts == null) {
                return@onVariants
            }
            when {
                path.contains("media2:media2-session:version-compat-tests:") -> {
                    createOrUpdateMediaTestConfigurationGenerationTask(
                        name,
                        artifacts,
                        baseExtension.defaultConfig.minSdk!!,
                        baseExtension.defaultConfig.testInstrumentationRunner!!,
                        isMedia2 = true
                    )
                }
                path.contains("media:version-compat-tests:") -> {
                    createOrUpdateMediaTestConfigurationGenerationTask(
                        name,
                        artifacts,
                        baseExtension.defaultConfig.minSdk!!,
                        baseExtension.defaultConfig.testInstrumentationRunner!!,
                        isMedia2 = false
                    )
                }
                path.endsWith("macrobenchmark") ||
                    isMacrobenchmarkTarget() -> {
                    configureMacrobenchmarkConfigTask(
                        name,
                        artifacts,
                        baseExtension.defaultConfig.minSdk!!,
                        baseExtension.defaultConfig.testInstrumentationRunner!!
                    )
                }
                else -> {
                    createTestConfigurationGenerationTask(
                        name,
                        artifacts,
                        baseExtension.defaultConfig.minSdk!!,
                        baseExtension.defaultConfig.testInstrumentationRunner!!
                    )
                }
            }
        }
    }
}
