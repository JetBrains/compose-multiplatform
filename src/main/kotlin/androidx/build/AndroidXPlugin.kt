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

import androidx.build.SupportConfig.BUILD_TOOLS_VERSION
import androidx.build.SupportConfig.COMPILE_SDK_VERSION
import androidx.build.SupportConfig.DEFAULT_MIN_SDK_VERSION
import androidx.build.SupportConfig.INSTRUMENTATION_RUNNER
import androidx.build.SupportConfig.TARGET_SDK_VERSION
import androidx.build.checkapi.ApiType
import androidx.build.checkapi.getCurrentApiLocation
import androidx.build.checkapi.getLastReleasedApiFileFromDir
import androidx.build.checkapi.hasApiFolder
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.dokka.Dokka.configureAndroidProjectForDokka
import androidx.build.dokka.Dokka.configureJavaProjectForDokka
import androidx.build.dokka.DokkaPublicDocs
import androidx.build.dokka.DokkaSourceDocs
import androidx.build.gradle.getByType
import androidx.build.gradle.isRoot
import androidx.build.jacoco.Jacoco
import androidx.build.license.CheckExternalDependencyLicensesTask
import androidx.build.license.configureExternalDependencyLicenseCheck
import androidx.build.metalava.Metalava.configureAndroidProjectForMetalava
import androidx.build.metalava.Metalava.configureJavaProjectForMetalava
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion.VERSION_1_7
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.withType
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * A plugin which enables all of the Gradle customizations for AndroidX.
 * This plugin reacts to other plugins being added and adds required and optional functionality.
 */
class AndroidXPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // This has to be first due to bad behavior by DiffAndDocs which is triggered on the root
        // project. It calls evaluationDependsOn on each subproject. This eagerly causes evaluation
        // *during* the root build.gradle evaluation. The subproject then applies this plugin (while
        // we're still halfway through applying it on the root). The check licenses code runs on the
        // subproject which then looks for the root project task to add itself as a dependency of.
        // Without the root project having created the task prior to DiffAndDocs running this fails.
        // TODO do not use evaluationDependsOn in DiffAndDocs to break this cycle!
        project.configureExternalDependencyLicenseCheck()

        if (project.isRoot) {
            project.configureRootProject()
        }

        val androidXExtension =
            project.extensions.create("androidx", AndroidXExtension::class.java, project)
        project.configureMavenArtifactUpload(androidXExtension)

        project.plugins.all {
            when (it) {
                is JavaPlugin,
                is JavaLibraryPlugin -> {
                    project.configureErrorProneForJava()
                    project.configureSourceJarForJava()
                    project.convention.getPlugin<JavaPluginConvention>().apply {
                        sourceCompatibility = VERSION_1_7
                        targetCompatibility = VERSION_1_7
                    }
                    project.hideJavadocTask()
                    val verifyDependencyVersionsTask = project.createVerifyDependencyVersionsTask()
                    verifyDependencyVersionsTask.configure {
                        it.dependsOn(project.tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME))
                    }
                    project.createCheckReleaseReadyTask(listOf(verifyDependencyVersionsTask))
                    project.configureNonAndroidProjectForLint(androidXExtension)
                    project.configureJavaProjectForDokka(androidXExtension)
                    project.configureJavaProjectForMetalava(androidXExtension)
                }
                is LibraryPlugin -> {
                    val extension = project.extensions.getByType<LibraryExtension>()
                    project.configureSourceJarForAndroid(extension)
                    project.configureAndroidCommonOptions(extension)
                    project.configureAndroidLibraryOptions(extension)
                    project.configureVersionFileWriter(extension)
                    project.configureResourceApiChecks()
                    val verifyDependencyVersionsTask = project.createVerifyDependencyVersionsTask()
                    val checkNoWarningsTask = project.tasks.register(CHECK_NO_WARNINGS_TASK)
                    // Only dump dependencies of published projects
                    if (project.extra.has("publish")) {
                        project.createDumpDependenciesTask()
                    }
                    project.createCheckReleaseReadyTask(listOf(verifyDependencyVersionsTask,
                        checkNoWarningsTask))
                    extension.libraryVariants.all { libraryVariant ->
                        verifyDependencyVersionsTask.configure { task ->
                            task.dependsOn(libraryVariant.javaCompileProvider)
                        }
                        checkNoWarningsTask.dependsOn(libraryVariant.javaCompileProvider)
                        project.gradle.taskGraph.whenReady { executionGraph ->
                            if (executionGraph.hasTask(checkNoWarningsTask.get())) {
                                libraryVariant.javaCompileProvider.configure { task ->
                                    task.options.compilerArgs.add("-Werror")
                                }
                            }
                        }
                    }
                    project.configureLint(extension.lintOptions, androidXExtension)
                    project.configureAndroidProjectForDokka(extension, androidXExtension)
                    project.configureAndroidProjectForMetalava(extension, androidXExtension)
                }
                is AppPlugin -> {
                    val extension = project.extensions.getByType<AppExtension>()
                    project.configureAndroidCommonOptions(extension)
                    project.configureAndroidApplicationOptions(extension)
                    // workaround for b/120487939
                    project.configurations.all {
                        // Gradle seems to crash on androidtest configurations
                        // preferring project modules...
                        if (!it.name.toLowerCase().contains("androidtest")) {
                            it.resolutionStrategy.preferProjectModules()
                        }
                    }
                }
            }
        }

        // Disable timestamps and ensure filesystem-independent archive ordering to maximize
        // cross-machine byte-for-byte reproducibility of artifacts.
        project.tasks.withType<Jar> {
            isReproducibleFileOrder = true
            isPreserveFileTimestamps = false
        }
    }

    private fun Project.configureRootProject() {
        val buildOnServerTask = tasks.create(BUILD_ON_SERVER_TASK)
        val buildTestApksTask = tasks.create(BUILD_TEST_APKS)
        project.configureDependencyGraphFileTask()
        var projectModules = ConcurrentHashMap<String, String>()
        project.extra.set("projects", projectModules)
        tasks.all { task ->
            if (task.name.startsWith(Release.DIFF_TASK_PREFIX) ||
                    "distDocs" == task.name ||
                    "partiallyDejetifyArchive" == task.name ||
                    CheckExternalDependencyLicensesTask.TASK_NAME == task.name) {
                buildOnServerTask.dependsOn(task)
            }
        }
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
            project.tasks.all { task ->
                // TODO remove androidTest from buildOnServer once test runners do not
                // expect them anymore. (wait for master)
                if ("assembleAndroidTest" == task.name ||
                        "assembleDebug" == task.name ||
                        ERROR_PRONE_TASK == task.name ||
                        ("lintDebug" == task.name &&
                        !project.rootProject.hasProperty("useMaxDepVersions"))) {
                    buildOnServerTask.dependsOn(task)
                }
                if ("assembleAndroidTest" == task.name ||
                        "assembleDebug" == task.name) {
                    buildTestApksTask.dependsOn(task)
                }
            }
        }

        val createCoverageJarTask = Jacoco.createCoverageJarTask(this)
        buildOnServerTask.dependsOn(createCoverageJarTask)
        buildTestApksTask.dependsOn(createCoverageJarTask)

        Release.createGlobalArchiveTask(this)
        val allDocsTask = DiffAndDocs.configureDiffAndDocs(this, projectDir,
                DacOptions("androidx", "ANDROIDX_DATA"),
                listOf(RELEASE_RULE))
        buildOnServerTask.dependsOn(allDocsTask)

        val jacocoUberJar = Jacoco.createUberJarTask(this)
        buildOnServerTask.dependsOn(jacocoUberJar)
        val checkSameVersionLibraryGroupsTask = project.tasks.register(
            CHECK_SAME_VERSION_LIBRARY_GROUPS,
            CheckSameVersionLibraryGroupsTask::class.java)
        buildOnServerTask.dependsOn(checkSameVersionLibraryGroupsTask)

        project.createClockLockTasks()

        AffectedModuleDetector.configure(gradle, this)

        // If useMaxDepVersions is set, iterate through all the project and substitute any androidx
        // artifact dependency with the local tip of tree version of the library.
        if (project.hasProperty("useMaxDepVersions")) {
            // This requires evaluating all sub-projects to create the module:project map
            // and project dependencies.
            evaluationDependsOnChildren()
            subprojects { project ->
                project.configurations.all { configuration ->
                    // Substitute only for debug configurations/tasks only because we can not
                    // change release dependencies after evaluation. Test hooks, buildOnServer
                    // and buildTestApks use the debug configurations as well.
                    if (project.extra.has("publish") && configuration.name
                            .toLowerCase().contains("debug")) {
                        configuration.resolutionStrategy.dependencySubstitution.apply {
                            for (e in projectModules) {
                                substitute(module(e.key)).with(project(e.value))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Project.configureAndroidCommonOptions(extension: BaseExtension) {
        extension.compileSdkVersion(COMPILE_SDK_VERSION)
        extension.buildToolsVersion = BUILD_TOOLS_VERSION
        // Expose the compilation SDK for use as the target SDK in test manifests.
        extension.defaultConfig.addManifestPlaceholders(
                mapOf("target-sdk-version" to TARGET_SDK_VERSION))

        extension.defaultConfig.testInstrumentationRunner = INSTRUMENTATION_RUNNER
        extension.testOptions.unitTests.isReturnDefaultValues = true

        extension.defaultConfig.minSdkVersion(DEFAULT_MIN_SDK_VERSION)
        afterEvaluate {
            val minSdkVersion = extension.defaultConfig.minSdkVersion.apiLevel
            check(minSdkVersion >= DEFAULT_MIN_SDK_VERSION) {
                "minSdkVersion $minSdkVersion lower than the default of $DEFAULT_MIN_SDK_VERSION"
            }
            project.configurations.all { configuration ->
                configuration.resolutionStrategy.eachDependency { dep ->
                    val target = dep.target
                    // Enforce the ban on declaring dependencies with version ranges.
                    if (isDependencyRange(target.version)) {
                        throw IllegalArgumentException(
                                "Dependency ${dep.target} declares its version as " +
                                        "version range ${dep.target.version} however the use of " +
                                        "version ranges is not allowed, please update the " +
                                        "dependency to list a fixed version.")
                    }
                }
            }
        }

        // Use a local debug keystore to avoid build server issues.
        extension.signingConfigs.getByName("debug").storeFile = SupportConfig.getKeystore(this)

        // Disable generating BuildConfig.java
        // TODO remove after https://issuetracker.google.com/72050365
        extension.variants.all {
            it.generateBuildConfigProvider.configure {
                it.enabled = false
            }
        }

        configureErrorProneForAndroid(extension.variants)

        // Enable code coverage for debug builds only if we are not running inside the IDE, since
        // enabling coverage reports breaks the method parameter resolution in the IDE debugger.
        extension.buildTypes.getByName("debug").isTestCoverageEnabled =
                !hasProperty("android.injected.invoked.from.ide") &&
                !isBenchmark()

        // Set the officially published version to be the release version with minimum dependency
        // versions.
        extension.defaultPublishConfig(Release.DEFAULT_PUBLISH_CONFIG)
    }

    private fun Project.configureAndroidLibraryOptions(extension: LibraryExtension) {
        extension.compileOptions.apply {
            setSourceCompatibility(VERSION_1_7)
            setTargetCompatibility(VERSION_1_7)
        }

        afterEvaluate {
            // Java 8 is only fully supported on API 24+ and not all Java 8 features are
            // binary compatible with API < 24
            val compilesAgainstJava8 = extension.compileOptions.sourceCompatibility > VERSION_1_7 ||
                    extension.compileOptions.targetCompatibility > VERSION_1_7
            val minSdkLessThan24 = extension.defaultConfig.minSdkVersion.apiLevel < 24
            if (compilesAgainstJava8 && minSdkLessThan24) {
                throw IllegalArgumentException(
                        "Libraries can only support Java 8 if minSdkVersion is 24 or higher")
            }
        }
    }

    private fun Project.configureAndroidApplicationOptions(extension: AppExtension) {
        extension.defaultConfig.apply {
            targetSdkVersion(TARGET_SDK_VERSION)

            versionCode = 1
            versionName = "1.0"
        }

        extension.compileOptions.apply {
            setSourceCompatibility(VERSION_1_8)
            setTargetCompatibility(VERSION_1_8)
        }

        extension.lintOptions.apply {
            isAbortOnError = true

            val baseline = lintBaseline
            if (baseline.exists()) {
                baseline(baseline)
            }
        }
    }

    private fun Project.createVerifyDependencyVersionsTask():
            TaskProvider<VerifyDependencyVersionsTask> {
        return project.tasks.register("verifyDependencyVersions",
                VerifyDependencyVersionsTask::class.java)
    }

    // Task that creates a json file of a project's dependencies
    private fun Project.createDumpDependenciesTask():
            TaskProvider<ListProjectDependencyVersionsTask> {
        return project.tasks.register("dumpDependencies",
            ListProjectDependencyVersionsTask::class.java)
    }

    // Task that creates a json file of the AndroidX dependency graph (all projects)
    private fun Project.configureDependencyGraphFileTask() {
        project.tasks.register("createDependencyGraphFile",
            DependencyGraphFileTask::class.java) { depGraphTask ->
            subprojects { project ->
                project.tasks.all { dumpDepTask ->
                    if ("dumpDependencies" == dumpDepTask.name &&
                        dumpDepTask is ListProjectDependencyVersionsTask) {
                        depGraphTask.dependsOn(dumpDepTask)
                        depGraphTask.projectDepDumpFiles.add(dumpDepTask.outputDepFile)
                    }
                }
            }
        }
    }

    companion object {
        const val BUILD_ON_SERVER_TASK = "buildOnServer"
        const val BUILD_TEST_APKS = "buildTestApks"
        const val CHECK_RELEASE_READY_TASK = "checkReleaseReady"
        const val CHECK_NO_WARNINGS_TASK = "checkNoWarnings"
        const val CHECK_SAME_VERSION_LIBRARY_GROUPS = "checkSameVersionLibraryGroups"
    }
}

fun Project.isBenchmark(): Boolean {
    // benchmark convention is to end name with "-benchmark"
    return name.endsWith("-benchmark")
}

fun Project.hideJavadocTask() {
    // Most tasks named "javadoc" are unused
    // So, few tasks named "javadoc" are interesting to developers
    // So, we don't want "javadoc" to appear in the output of `./gradlew tasks`
    // So, we set the group to null for any task named "javadoc"
    project.tasks.all { task ->
        if (task.name == "javadoc") {
            task.group = null
        }
    }
}

fun Project.addToProjectMap(group: String?) {
    if (group != null) {
        val module = "$group:${project.name}"
        val projectName = "${project.path}"
        var projectModules = project.rootProject.extra.get("projects")
                as ConcurrentHashMap<String, String>
        projectModules.put(module, projectName)
    }
}

private fun isDependencyRange(version: String?): Boolean {
    return ((version!!.startsWith("[") || version.startsWith("(")) &&
            (version.endsWith("]") || version.endsWith(")")) ||
            version.endsWith("+"))
}

private fun Project.createCheckResourceApiTask(): DefaultTask {
    return project.tasks.createWithConfig("checkResourceApi",
            CheckResourceApiTask::class.java) {
        newApiFile = getGenerateResourceApiFile()
        oldApiFile = project.getCurrentApiLocation().resourceFile
    }
}

private fun Project.createCheckReleaseReadyTask(taskProviderList: List<TaskProvider<out Task>>) {
    project.tasks.register(AndroidXPlugin.CHECK_RELEASE_READY_TASK) {
        for (taskProvider in taskProviderList) {
            it.dependsOn(taskProvider)
        }
    }
}

private fun Project.createUpdateResourceApiTask(): DefaultTask {
    return project.tasks.createWithConfig("updateResourceApi", UpdateResourceApiTask::class.java) {
        newApiFile = getGenerateResourceApiFile()
        oldApiFile = getLastReleasedApiFileFromDir(File(project.projectDir, "api/"),
                project.version(), true, false, ApiType.RESOURCEAPI)
        destApiFile = project.getCurrentApiLocation().resourceFile
    }
}

private fun Project.configureResourceApiChecks() {
    project.afterEvaluate {
        if (project.hasApiFolder()) {
            val checkResourceApiTask = project.createCheckResourceApiTask()
            val updateResourceApiTask = project.createUpdateResourceApiTask()
            project.tasks.all { task ->
                if (task.name == "assembleRelease") {
                    checkResourceApiTask.dependsOn(task)
                    updateResourceApiTask.dependsOn(task)
                } else if (task.name == "updateApi") {
                    task.dependsOn(updateResourceApiTask)
                }
            }
            project.rootProject.tasks.all { task ->
                if (task.name == AndroidXPlugin.BUILD_ON_SERVER_TASK) {
                    task.dependsOn(checkResourceApiTask)
                }
            }
        }
    }
}

private fun Project.getGenerateResourceApiFile(): File {
    return File(project.buildDir, "intermediates/public_res/release" +
            "/packageReleaseResources/public.txt")
}
