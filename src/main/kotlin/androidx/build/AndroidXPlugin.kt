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

import androidx.benchmark.gradle.BenchmarkPlugin
import androidx.build.AndroidXPlugin.Companion.CHECK_RELEASE_READY_TASK
import androidx.build.AndroidXPlugin.Companion.CHECK_RESOURCE_API_TASK
import androidx.build.AndroidXPlugin.Companion.TASK_TIMEOUT_MINUTES
import androidx.build.AndroidXPlugin.Companion.UPDATE_RESOURCE_API_TASK
import androidx.build.SupportConfig.BUILD_TOOLS_VERSION
import androidx.build.SupportConfig.COMPILE_SDK_VERSION
import androidx.build.SupportConfig.DEFAULT_MIN_SDK_VERSION
import androidx.build.SupportConfig.INSTRUMENTATION_RUNNER
import androidx.build.SupportConfig.TARGET_SDK_VERSION
import androidx.build.checkapi.ApiType
import androidx.build.checkapi.getApiFileDirectory
import androidx.build.checkapi.getRequiredCompatibilityApiFileFromDir
import androidx.build.checkapi.getVersionedApiLocation
import androidx.build.checkapi.hasApiFileDirectory
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.dokka.Dokka.configureAndroidProjectForDokka
import androidx.build.dokka.Dokka.configureJavaProjectForDokka
import androidx.build.gradle.getByType
import androidx.build.gradle.isRoot
import androidx.build.jacoco.Jacoco
import androidx.build.license.configureExternalDependencyLicenseCheck
import androidx.build.metalava.MetalavaTasks.configureAndroidProjectForMetalava
import androidx.build.metalava.MetalavaTasks.configureJavaProjectForMetalava
import androidx.build.metalava.UpdateApiTask
import androidx.build.studio.StudioTask
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApkVariant
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.time.Duration
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * A plugin which enables all of the Gradle customizations for AndroidX.
 * This plugin reacts to other plugins being added and adds required and optional functionality.
 */
class AndroidXPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project.isRoot) throw Exception("Root project should use AndroidXRootPlugin instead")
        // This has to be first due to bad behavior by DiffAndDocs which is triggered on the root
        // project. It calls evaluationDependsOn on each subproject. This eagerly causes evaluation
        // *during* the root build.gradle evaluation. The subproject then applies this plugin (while
        // we're still halfway through applying it on the root). The check licenses code runs on the
        // subproject which then looks for the root project task to add itself as a dependency of.
        // Without the root project having created the task prior to DiffAndDocs running this fails.
        // TODO(alanv): do not use evaluationDependsOn in DiffAndDocs to break this cycle!
        project.configureExternalDependencyLicenseCheck()

        val extension = project.extensions.create<AndroidXExtension>(EXTENSION_NAME, project)

        // This has to be first due to bad behavior by DiffAndDocs. It fails if this configuration
        // is called after DiffAndDocs.configureDiffAndDocs. b/129762955
        project.configureMavenArtifactUpload(extension)

        project.configureJacoco()

        // Perform different actions based on which plugins have been applied to the project.
        // Many of the actions overlap, ex. API tracking and documentation.
        project.plugins.all { plugin ->
            when (plugin) {
                is JavaPlugin -> configureWithJavaPlugin(project, extension)
                is LibraryPlugin -> configureWithLibraryPlugin(project, extension)
                is AppPlugin -> configureWithAppPlugin(project, extension)
                is KotlinBasePluginWrapper -> configureWithKotlinPlugin(project, plugin)
            }
        }

        project.configureKtlint()

        // Configure all Jar-packing tasks for hermetic builds.
        project.tasks.withType(Jar::class.java).configureEach { it.configureForHermeticBuild() }

        // copy host side test results to DIST
        project.tasks.withType(Test::class.java) { task -> configureTestTask(project, task) }

        project.configureTaskTimeouts()
    }

    /**
     * Disables timestamps and ensures filesystem-independent archive ordering to maximize
     * cross-machine byte-for-byte reproducibility of artifacts.
     */
    private fun Jar.configureForHermeticBuild() {
        isReproducibleFileOrder = true
        isPreserveFileTimestamps = false
    }

    private fun configureTestTask(project: Project, task: Test) {
        AffectedModuleDetector.configureTaskGuard(task)

        // Enable tracing to see results in command line
        task.testLogging.events = hashSetOf(TestLogEvent.FAILED, TestLogEvent.PASSED,
            TestLogEvent.SKIPPED, TestLogEvent.STANDARD_OUT)
        val report = task.reports.junitXml
        if (report.isEnabled) {
            val zipTask = project.tasks.register(
                "zipResultsOf${task.name.capitalize()}",
                Zip::class.java
            ) {
                it.destinationDirectory.set(project.getHostTestResultDirectory())
                it.archiveFileName.set("${project.asFilenamePrefix()}_${task.name}.zip")
            }
            if (project.hasProperty(TEST_FAILURES_DO_NOT_FAIL_TEST_TASK)) {
                task.ignoreFailures = true
            }
            task.finalizedBy(zipTask)
            task.doFirst {
                zipTask.configure {
                    it.from(report.destination)
                }
            }
        }
        task.systemProperty("robolectric.offline", "true")
        val robolectricDependencies =
            File(project.getPrebuiltsRoot(), "androidx/external/org/robolectric/android-all")
        task.systemProperty(
            "robolectric.dependency.dir",
            robolectricDependencies.absolutePath
        )
    }

    private fun configureWithKotlinPlugin(
        project: Project,
        plugin: KotlinBasePluginWrapper
    ) {
        project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
            task.kotlinOptions.jvmTarget = "1.8"
            project.configureCompilationWarnings(task)
        }
        if (plugin is KotlinMultiplatformPluginWrapper) {
            project.extensions.findByType<LibraryExtension>()?.apply {
                configureAndroidLibraryWithMultiplatformPluginOptions()
            }
        }
    }

    private fun configureWithAppPlugin(project: Project, extension: AndroidXExtension) {
        project.extensions.getByType<AppExtension>().apply {
            configureAndroidCommonOptions(project, extension)
            configureAndroidApplicationOptions(project)
        }
    }

    private fun configureWithLibraryPlugin(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        val libraryExtension = project.extensions.getByType<LibraryExtension>().apply {
            configureAndroidCommonOptions(project, androidXExtension)
            configureAndroidLibraryOptions(project, androidXExtension)
        }

        project.configureSourceJarForAndroid(libraryExtension)
        project.configureVersionFileWriter(libraryExtension, androidXExtension)
        project.configureResourceApiChecks(libraryExtension)
        project.addCreateLibraryBuildInfoFileTask(androidXExtension)

        val verifyDependencyVersionsTask = project.createVerifyDependencyVersionsTask()
        val checkReleaseReadyTasks = mutableListOf<TaskProvider<out Task>>()
        if (verifyDependencyVersionsTask != null) {
            checkReleaseReadyTasks.add(verifyDependencyVersionsTask)
        }
        if (checkReleaseReadyTasks.isNotEmpty()) {
            project.createCheckReleaseReadyTask(checkReleaseReadyTasks)
        }

        val reportLibraryMetrics = project.tasks.register<ReportLibraryMetricsTask>(
            REPORT_LIBRARY_METRICS_TASK, ReportLibraryMetricsTask::class.java)
        project.addToBuildOnServer(reportLibraryMetrics)
        libraryExtension.defaultPublishVariant { libraryVariant ->
            reportLibraryMetrics.configure {
                it.jarFiles.from(libraryVariant.packageLibraryProvider.map { zip ->
                    zip.inputs.files
                })
            }

            verifyDependencyVersionsTask?.configure { task ->
                task.dependsOn(libraryVariant.javaCompileProvider)
            }

            libraryVariant.javaCompileProvider.configure { task ->
                project.configureCompilationWarnings(task)
            }
        }

        // Standard lint, docs, and Metalava configuration for AndroidX projects.
        project.configureAndroidProjectForLint(libraryExtension.lintOptions, androidXExtension)
        project.configureAndroidProjectForDokka(libraryExtension, androidXExtension)
        project.configureAndroidProjectForMetalava(libraryExtension, androidXExtension)

        project.addToProjectMap(androidXExtension)
    }

    private fun configureWithJavaPlugin(project: Project, extension: AndroidXExtension) {
        project.configureErrorProneForJava()
        project.configureSourceJarForJava()

        // Force Java 1.8 source- and target-compatibilty for all Java libraries.
        val convention = project.convention.getPlugin<JavaPluginConvention>()
        convention.apply {
            sourceCompatibility = VERSION_1_8
            targetCompatibility = VERSION_1_8
        }

        project.tasks.withType(JavaCompile::class.java) { task ->
            project.configureCompilationWarnings(task)
        }

        project.hideJavadocTask()

        val verifyDependencyVersionsTask = project.createVerifyDependencyVersionsTask()
        verifyDependencyVersionsTask?.configure { task ->
            task.dependsOn(project.tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME))
        }

        project.addCreateLibraryBuildInfoFileTask(extension)
        if (verifyDependencyVersionsTask != null) {
            project.createCheckReleaseReadyTask(listOf(verifyDependencyVersionsTask))
        }

        // Standard lint, docs, and Metalava configuration for AndroidX projects.
        project.configureNonAndroidProjectForLint(extension)
        project.configureJavaProjectForDokka(extension)
        project.configureJavaProjectForMetalava(extension)

        project.afterEvaluate {
            if (extension.publish.shouldRelease()) {
                project.extra.set("publish", true)
            }
        }

        // Workaround for b/120487939 wherein Gradle's default resolution strategy prefers external
        // modules with lower versions over local projects with higher versions.
        project.configurations.all { configuration ->
            configuration.resolutionStrategy.preferProjectModules()
        }

        project.addToProjectMap(extension)
    }

    private fun TestedExtension.configureAndroidCommonOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        compileOptions.apply {
            sourceCompatibility = VERSION_1_8
            targetCompatibility = VERSION_1_8
        }

        // Force AGP to use our version of JaCoCo
        jacoco.version = Jacoco.VERSION
        compileSdkVersion(COMPILE_SDK_VERSION)
        buildToolsVersion = BUILD_TOOLS_VERSION
        defaultConfig.targetSdkVersion(TARGET_SDK_VERSION)

        defaultConfig.testInstrumentationRunner = INSTRUMENTATION_RUNNER

        // Enable code coverage for debug builds only if we are not running inside the IDE, since
        // enabling coverage reports breaks the method parameter resolution in the IDE debugger.
        buildTypes.getByName("debug").isTestCoverageEnabled =
            !project.hasProperty("android.injected.invoked.from.ide")

        testOptions.animationsDisabled = true
        testOptions.unitTests.isReturnDefaultValues = true

        defaultConfig.minSdkVersion(DEFAULT_MIN_SDK_VERSION)
        project.afterEvaluate {
            val minSdkVersion = defaultConfig.minSdkVersion!!.apiLevel
            check(minSdkVersion >= DEFAULT_MIN_SDK_VERSION) {
                "minSdkVersion $minSdkVersion lower than the default of $DEFAULT_MIN_SDK_VERSION"
            }
            project.configurations.all { configuration ->
                configuration.resolutionStrategy.eachDependency { dep ->
                    val target = dep.target
                    val version = target.version
                    // Enforce the ban on declaring dependencies with version ranges.
                    if (version != null && Version.isDependencyRange(version)) {
                        throw IllegalArgumentException(
                                "Dependency ${dep.target} declares its version as " +
                                        "version range ${dep.target.version} however the use of " +
                                        "version ranges is not allowed, please update the " +
                                        "dependency to list a fixed version.")
                    }
                }
            }

            if (androidXExtension.compilationTarget != CompilationTarget.DEVICE) {
                throw IllegalStateException(
                    "Android libraries must use a compilation target of DEVICE")
            }
        }

        val debugSigningConfig = signingConfigs.getByName("debug")
        // Use a local debug keystore to avoid build server issues.
        debugSigningConfig.storeFile = project.getKeystore()
        buildTypes.all { buildType ->
            // Sign all the builds (including release) with debug key
            buildType.signingConfig = debugSigningConfig
        }

        // Disable generating BuildConfig.java
        // TODO remove after https://issuetracker.google.com/72050365
        variants.all { variant ->
            variant.generateBuildConfigProvider.configure {
                it.enabled = false
            }
        }

        project.configureErrorProneForAndroid(variants)

        // Set the officially published version to be the debug version with minimum dependency
        // versions.
        defaultPublishConfig(Release.DEFAULT_PUBLISH_CONFIG)

        // workaround for b/120487939
        project.configurations.all { configuration ->
            // Gradle seems to crash on androidtest configurations
            // preferring project modules...
            if (!configuration.name.toLowerCase(Locale.US).contains("androidtest")) {
                configuration.resolutionStrategy.preferProjectModules()
            }
        }

        Jacoco.registerClassFilesTask(project, this)

        val buildTestApksTask = project.rootProject.tasks.named(BUILD_TEST_APKS_TASK)
        testVariants.all { variant ->
            buildTestApksTask.configure {
                it.dependsOn(variant.assembleProvider)
            }
            variant.configureApkCopy(project, this, true)
        }
    }

    private fun hasAndroidTestSourceCode(project: Project, extension: TestedExtension): Boolean {
        // check Java androidTest source set
        extension.sourceSets.findByName("androidTest")?.let { sourceSet ->
            // using getSourceFiles() instead of sourceFiles due to b/150800094
            if (!sourceSet.java.getSourceFiles().isEmpty) return true
        }

        // check kotlin-android androidTest source set
        project.extensions.findByType(KotlinAndroidProjectExtension::class.java)
            ?.sourceSets?.findByName("androidTest")?.let {
            if (it.kotlin.files.isNotEmpty()) return true
        }

        // check kotlin-multiplatform androidAndroidTest source set
        project.multiplatformExtension?.apply {
            sourceSets.findByName("androidAndroidTest")?.let {
                if (it.kotlin.files.isNotEmpty()) return true
            }
        }

        return false
    }

    private fun ApkVariant.configureApkCopy(
        project: Project,
        extension: TestedExtension,
        testApk: Boolean
    ) {
        packageApplicationProvider.configure { packageTask ->
            AffectedModuleDetector.configureTaskGuard(packageTask)
            packageTask.doLast {
                // Skip copying AndroidTest apks if they have no source code (no tests to run).
                if (testApk && !hasAndroidTestSourceCode(project, extension)) return@doLast

                project.copy {
                    it.from(packageTask.outputDirectory)
                    it.include("*.apk")
                    it.into(File(project.getDistributionDirectory(), "apks"))
                    it.rename { fileName ->
                        if (fileName.contains("media-compat-test") ||
                            fileName.contains("media2-test")) {
                            // Exclude media-compat-test-* and media2-test-* modules from
                            // existing support library presubmit tests.
                            fileName.replace("-debug-androidTest", "")
                        } else if (project.plugins.hasPlugin(BenchmarkPlugin::class.java)) {
                            // Exclude '-benchmark' modules from correctness tests
                            fileName.replace("-androidTest", "-androidBenchmark")
                        } else {
                            "${project.asFilenamePrefix()}_$fileName"
                        }
                    }
                }
            }
        }
    }

    private fun LibraryExtension.configureAndroidLibraryOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        project.configurations.all { config ->
            val isTestConfig = config.name.toLowerCase(Locale.US).contains("test")

            config.dependencyConstraints.configureEach { dependencyConstraint ->
                dependencyConstraint.apply {
                    // Remove strict constraints on test dependencies and listenablefuture:1.0
                    if (isTestConfig ||
                        group == "com.google.guava" &&
                        name == "listenablefuture" &&
                        version == "1.0") {
                        version { versionConstraint ->
                            versionConstraint.strictly("")
                        }
                    }
                }
            }
        }

        project.afterEvaluate {
            if (androidXExtension.publish.shouldRelease()) {
                project.extra.set("publish", true)
            }
            if (!project.rootProject.hasProperty(USE_MAX_DEP_VERSIONS)) {
                defaultPublishVariant { libraryVariant ->
                    libraryVariant.javaCompileProvider.configure { javaCompile ->
                        if (androidXExtension.failOnDeprecationWarnings) {
                            javaCompile.options.compilerArgs.add("-Xlint:deprecation")
                        }
                    }
                }
            }
        }
    }

    private fun TestedExtension.configureAndroidLibraryWithMultiplatformPluginOptions() {
        sourceSets.findByName("main")!!.manifest.srcFile("src/androidMain/AndroidManifest.xml")
        sourceSets.findByName("androidTest")!!
            .manifest.srcFile("src/androidAndroidTest/AndroidManifest.xml")
    }

    private fun AppExtension.configureAndroidApplicationOptions(project: Project) {
        defaultConfig.apply {
            versionCode = 1
            versionName = "1.0"
        }

        lintOptions.apply {
            isAbortOnError = true

            val baseline = project.lintBaseline
            if (baseline.exists()) {
                baseline(baseline)
            }
        }

        val buildTestApksTask = project.rootProject.tasks.named(BUILD_TEST_APKS_TASK)
        applicationVariants.all { variant ->
            // Using getName() instead of name due to b/150427408
            if (variant.buildType.getName() == "debug") {
                buildTestApksTask.configure {
                    it.dependsOn(variant.assembleProvider)
                }
            }
            variant.configureApkCopy(project, this, false)
        }
    }

    private fun Project.createVerifyDependencyVersionsTask():
            TaskProvider<VerifyDependencyVersionsTask>? {
        /**
         * Ignore -PuseMaxDepVersions when verifying dependency versions because it is a
         * hypothetical build which is only intended to check for forward compatibility.
         */
        if (hasProperty(USE_MAX_DEP_VERSIONS)) {
            return null
        }

        val taskProvider = tasks.register(
            "verifyDependencyVersions",
            VerifyDependencyVersionsTask::class.java
        )
        addToBuildOnServer(taskProvider)
        return taskProvider
    }

    // Task that creates a json file of a project's dependencies
    private fun Project.addCreateLibraryBuildInfoFileTask(extension: AndroidXExtension) {
        afterEvaluate {
            if (extension.publish.shouldRelease()) {
                // Only generate build info files for published libraries.
                val task = tasks.register(
                    CREATE_LIBRARY_BUILD_INFO_FILES_TASK,
                    CreateLibraryBuildInfoFileTask::class.java
                ) {
                    it.outputFile.set(File(project.getBuildInfoDirectory(),
                        "${group}_${name}_build_info.txt"))
                }
                rootProject.tasks.named(CREATE_LIBRARY_BUILD_INFO_FILES_TASK).configure {
                    it.dependsOn(task)
                }
                addTaskToAggregateBuildInfoFileTask(task)
            }
        }
    }

    private fun Project.addTaskToAggregateBuildInfoFileTask(
        task: TaskProvider<CreateLibraryBuildInfoFileTask>
    ) {
        rootProject.tasks.named(CREATE_AGGREGATE_BUILD_INFO_FILES_TASK).configure {
            val aggregateLibraryBuildInfoFileTask: CreateAggregateLibraryBuildInfoFileTask = it
                    as CreateAggregateLibraryBuildInfoFileTask
            aggregateLibraryBuildInfoFileTask.dependsOn(task)
            aggregateLibraryBuildInfoFileTask.libraryBuildInfoFiles.add(
                task.flatMap { task -> task.outputFile }
            )
        }
    }

    private fun Project.configureJacoco() {
        apply(plugin = "jacoco")
        configure<JacocoPluginExtension> {
            toolVersion = Jacoco.VERSION
        }

        val zipEcFilesTask = Jacoco.getZipEcFilesTask(this)

        tasks.withType(JacocoReport::class.java).configureEach { task ->
            zipEcFilesTask.get().dependsOn(task) // zip follows every jacocoReport task being run
            task.reports {
                it.xml.isEnabled = true
                it.html.isEnabled = false
                it.csv.isEnabled = false

                it.xml.destination = File(getHostTestCoverageDirectory(),
                    "${project.asFilenamePrefix()}.xml")
            }
        }
    }

    companion object {
        const val BUILD_ON_SERVER_TASK = "buildOnServer"
        const val BUILD_TEST_APKS_TASK = "buildTestApks"
        const val CHECK_RESOURCE_API_TASK = "checkResourceApi"
        const val UPDATE_RESOURCE_API_TASK = "updateResourceApi"
        const val CHECK_RELEASE_READY_TASK = "checkReleaseReady"
        const val CREATE_LIBRARY_BUILD_INFO_FILES_TASK = "createLibraryBuildInfoFiles"
        const val CREATE_AGGREGATE_BUILD_INFO_FILES_TASK = "createAggregateBuildInfoFiles"
        const val REPORT_LIBRARY_METRICS_TASK = "reportLibraryMetrics"

        const val EXTENSION_NAME = "androidx"

        /**
         * Fail the build if a non-Studio task runs for more than 30 minutes.
         */
        const val TASK_TIMEOUT_MINUTES = 30L

        /**
         * Setting this property indicates that a build is being performed to check for forward
         * compatibility.
         */
        // TODO(alanv): This property should be prefixed with `androidx.`.
        const val USE_MAX_DEP_VERSIONS = "useMaxDepVersions"
    }
}

/**
 * Hides a project's Javadoc tasks from the output of `./gradlew tasks` by setting their group to
 * `null`.
 *
 * AndroidX projects do not use the Javadoc task for docs generation, so we don't want them
 * cluttering up the task overview.
 */
private fun Project.hideJavadocTask() {
    tasks.withType(Javadoc::class.java).configureEach {
        if (it.name == "javadoc") {
            it.group = null
        }
    }
}

private fun Project.addToProjectMap(extension: AndroidXExtension) {
    // TODO(alanv): Move this out of afterEvaluate
    afterEvaluate {
        if (extension.publish.shouldRelease()) {
            val group = extension.mavenGroup?.group
            if (group != null) {
                val module = "$group:$name"
                @Suppress("UNCHECKED_CAST")
                val projectModules = getProjectsMap()
                projectModules[module] = path
            }
        }
    }
}

val Project.multiplatformExtension
    get() = extensions.findByType(KotlinMultiplatformExtension::class.java)

/**
 * Creates the [CHECK_RESOURCE_API_TASK], which verifies the AAPT-generated resource API file
 * against the checked-in resource API file.
 */
private fun Project.createCheckResourceApiTask(): TaskProvider<CheckResourceApiTask> {
    return tasks.register(CHECK_RESOURCE_API_TASK, CheckResourceApiTask::class.java) { task ->
        task.newApiFile = getGenerateResourceApiFile()
        task.oldApiFile = getVersionedApiLocation().resourceFile
        task.cacheEvenIfNoOutputs()
    }
}

/**
 * Creates the [CHECK_RELEASE_READY_TASK], which aggregates tasks that must pass for a
 * project to be considered ready for public release.
 */
private fun Project.createCheckReleaseReadyTask(taskProviderList: List<TaskProvider<out Task>>) {
    tasks.register(CHECK_RELEASE_READY_TASK) {
        for (taskProvider in taskProviderList) {
            it.dependsOn(taskProvider)
        }
    }
}

private fun Project.createUpdateResourceApiTask(): TaskProvider<UpdateResourceApiTask> {
    return tasks.register(UPDATE_RESOURCE_API_TASK, UpdateResourceApiTask::class.java) { task ->
        task.newApiFile = getGenerateResourceApiFile()
        task.oldApiFile = getRequiredCompatibilityApiFileFromDir(project.getApiFileDirectory(),
                version(), ApiType.RESOURCEAPI)
        task.destApiFile = getVersionedApiLocation().resourceFile
    }
}

private fun Project.getGenerateResourceApiFile(): File {
    // TODO(alanv): Find a stable API contract to use when referencing this file.
    return File(buildDir, "intermediates/public_res/release/public.txt")
}

@Suppress("UNCHECKED_CAST")
fun Project.getProjectsMap(): ConcurrentHashMap<String, String> {
    return rootProject.extra.get("projects") as ConcurrentHashMap<String, String>
}

/**
 * Configures an Android library project to track and validate its public resource API surface.
 */
private fun Project.configureResourceApiChecks(extension: LibraryExtension) {
    // TODO(alanv): Fix this to occur during normal configuration.
    afterEvaluate { project ->
        // Only configure resource API checks for projects that are already tracking APIs.
        // TODO(alanv): Migrate to check the AndroidX extension for "should generate API files".
        if (project.hasApiFileDirectory()) {
            val checkResourceApiTask = createCheckResourceApiTask()
            val updateResourceApiTask = createUpdateResourceApiTask()

            // Configure the check- and update- resource API tasks to depend on Java compilation,
            // after which we expect the AAPT-generated public.txt file to be available.
            extension.defaultPublishVariant { libraryVariant ->
                // TODO(alanv): These should probably depend on public.txt as an input file.
                checkResourceApiTask.configure { it.dependsOn(libraryVariant.javaCompileProvider) }
                updateResourceApiTask.configure { it.dependsOn(libraryVariant.javaCompileProvider) }
            }

            // Ensure that this task runs as part of updateApi and buildOnServer
            tasks.withType(UpdateApiTask::class.java).configureEach { task ->
                task.dependsOn(updateResourceApiTask)
            }
            addToBuildOnServer(checkResourceApiTask)
        }
    }
}

/**
 * Configures all non-Studio tasks in a project (see b/153193718 for background) to time out after
 * [TASK_TIMEOUT_MINUTES].
 */
private fun Project.configureTaskTimeouts() {
    tasks.configureEach { t ->
        // skip adding a timeout for some tasks that both take a long time and
        // that we can count on the user to monitor
        if (t !is StudioTask) {
            t.timeout.set(Duration.ofMinutes(TASK_TIMEOUT_MINUTES))
        }
    }
}

private fun Project.configureCompilationWarnings(task: JavaCompile) {
    if (hasProperty(ALL_WARNINGS_AS_ERRORS)) {
        task.options.compilerArgs.add("-Werror")
        task.options.compilerArgs.add("-Xlint:unchecked")
    }
}

private fun Project.configureCompilationWarnings(task: KotlinCompile) {
    if (hasProperty(ALL_WARNINGS_AS_ERRORS)) {
        task.kotlinOptions.allWarningsAsErrors = true
    }
    task.kotlinOptions.freeCompilerArgs += listOf("-Xskip-runtime-version-check")
}

/**
 * Returns a string that is a valid filename and loosely based on the project name
 * The value returned for each project will be distinct
 */
private fun Project.asFilenamePrefix(): String {
    return project.path.substring(1).replace(':', '-')
}
