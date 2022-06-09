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
import androidx.build.AndroidXImplPlugin.Companion.CHECK_RELEASE_READY_TASK
import androidx.build.AndroidXImplPlugin.Companion.TASK_TIMEOUT_MINUTES
import androidx.build.Release.DEFAULT_PUBLISH_CONFIG
import androidx.build.SupportConfig.BUILD_TOOLS_VERSION
import androidx.build.SupportConfig.COMPILE_SDK_VERSION
import androidx.build.SupportConfig.DEFAULT_MIN_SDK_VERSION
import androidx.build.SupportConfig.INSTRUMENTATION_RUNNER
import androidx.build.SupportConfig.TARGET_SDK_VERSION
import androidx.build.checkapi.JavaApiTaskConfig
import androidx.build.checkapi.KmpApiTaskConfig
import androidx.build.checkapi.LibraryApiTaskConfig
import androidx.build.checkapi.configureProjectForApiTasks
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.gradle.isRoot
import androidx.build.license.configureExternalDependencyLicenseCheck
import androidx.build.resources.configurePublicResourcesStub
import androidx.build.studio.StudioTask
import androidx.build.testConfiguration.addAppApkToTestConfigGeneration
import androidx.build.testConfiguration.addToTestZips
import androidx.build.testConfiguration.configureTestConfigGeneration
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ManagedVirtualDevice
import com.android.build.api.dsl.TestOptions
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestExtension
import com.android.build.gradle.TestPlugin
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.internal.lint.AndroidLintTask
import com.android.build.gradle.internal.tasks.AnalyticsRecordingTask
import com.android.build.gradle.internal.tasks.ListingFileRedirectTask
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.time.Duration
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeHostTestRun
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.testing.KotlinTaskTestRun

/**
 * A plugin which enables all of the Gradle customizations for AndroidX.
 * This plugin reacts to other plugins being added and adds required and optional functionality.
 */
class AndroidXImplPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project.isRoot)
            throw Exception("Root project should use AndroidXRootImplPlugin instead")
        val extension = project.extensions.create<AndroidXExtension>(EXTENSION_NAME, project)
        // Perform different actions based on which plugins have been applied to the project.
        // Many of the actions overlap, ex. API tracking.
        project.plugins.all { plugin ->
            when (plugin) {
                is JavaPlugin -> configureWithJavaPlugin(project, extension)
                is LibraryPlugin -> configureWithLibraryPlugin(project, extension)
                is AppPlugin -> configureWithAppPlugin(project, extension)
                is TestPlugin -> configureWithTestPlugin(project, extension)
                is KotlinBasePluginWrapper -> configureWithKotlinPlugin(project, extension, plugin)
            }
        }

        project.configureKtlint()

        // Configure all Jar-packing tasks for hermetic builds.
        project.tasks.withType(Jar::class.java).configureEach { it.configureForHermeticBuild() }
        project.tasks.withType(Copy::class.java).configureEach { it.configureForHermeticBuild() }

        // copy host side test results to DIST
        project.tasks.withType(Test::class.java) { task -> configureTestTask(project, task) }

        project.configureTaskTimeouts()
        project.configureMavenArtifactUpload(extension)
        project.configureExternalDependencyLicenseCheck()
        project.configureProjectStructureValidation(extension)
        project.configureProjectVersionValidation(extension)
        project.registerProjectOrArtifact()

        project.configurations.create("samples")
    }

    private fun Project.registerProjectOrArtifact() {
        // Add a method for each sub project where they can declare an optional
        // dependency on a project or its latest snapshot artifact.
        if (!StudioType.isPlayground(this)) {
            // In AndroidX build, this is always enforced to the project
            extra.set(
                PROJECT_OR_ARTIFACT_EXT_NAME,
                KotlinClosure1<String, Project>(
                    function = {
                        // this refers to the first parameter of the closure.
                        project(this)
                    }
                )
            )
        } else {
            // In Playground builds, they are converted to the latest SNAPSHOT artifact if the
            // project is not included in that playground.
            extra.set(
                PROJECT_OR_ARTIFACT_EXT_NAME,
                KotlinClosure1<String, Any>(
                    function = {
                        AndroidXPlaygroundRootImplPlugin.projectOrArtifact(rootProject, this)
                    }
                )
            )
        }
    }

    /**
     * Disables timestamps and ensures filesystem-independent archive ordering to maximize
     * cross-machine byte-for-byte reproducibility of artifacts.
     */
    private fun Jar.configureForHermeticBuild() {
        isReproducibleFileOrder = true
        isPreserveFileTimestamps = false
    }

    private fun Copy.configureForHermeticBuild() {
        duplicatesStrategy = DuplicatesStrategy.FAIL
    }

    private fun configureTestTask(project: Project, task: Test) {
        AffectedModuleDetector.configureTaskGuard(task)

        // Robolectric 1.7 increased heap size requirements, see b/207169653.
        task.maxHeapSize = "3g"

        val xmlReportDestDir = project.getHostTestResultDirectory()
        val archiveName = "${project.path}:${task.name}.zip"
        if (project.isDisplayTestOutput()) {
            // Enable tracing to see results in command line
            task.testLogging.apply {
                events = hashSetOf(
                    TestLogEvent.FAILED, TestLogEvent.PASSED,
                    TestLogEvent.SKIPPED, TestLogEvent.STANDARD_OUT
                )
                showExceptions = true
                showCauses = true
                showStackTraces = true
                exceptionFormat = TestExceptionFormat.FULL
            }
        } else {
            task.testLogging.apply {
                showExceptions = false
                // Disable all output, including the names of the failing tests, by specifying
                // that the minimum granularity we're interested in is this very high number
                // (which is higher than the current maximum granularity that Gradle offers (3))
                minGranularity = 1000
            }
            val testTaskName = task.name
            val capitalizedTestTaskName = testTaskName.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }

            val zipHtmlTask = project.tasks.register(
                "zipHtmlResultsOf$capitalizedTestTaskName",
                Zip::class.java
            ) {
                val destinationDirectory = File("$xmlReportDestDir-html")
                it.destinationDirectory.set(destinationDirectory)
                it.archiveFileName.set(archiveName)
                it.from(project.file(task.reports.html.outputLocation))
                it.doLast { zip ->
                    // If the test itself didn't display output, then the report task should
                    // remind the user where to find its output
                    zip.logger.lifecycle(
                        "Html results of $testTaskName zipped into " +
                            "$destinationDirectory/$archiveName"
                    )
                }
            }
            task.finalizedBy(zipHtmlTask)
            val xmlReport = task.reports.junitXml
            if (xmlReport.required.get()) {
                val zipXmlTask = project.tasks.register(
                    "zipXmlResultsOf$capitalizedTestTaskName",
                    Zip::class.java
                ) {
                    it.destinationDirectory.set(xmlReportDestDir)
                    it.archiveFileName.set(archiveName)
                    it.from(project.file(xmlReport.outputLocation))
                }
                val ignoreFailuresProperty = project.providers.gradleProperty(
                    TEST_FAILURES_DO_NOT_FAIL_TEST_TASK
                )
                if (ignoreFailuresProperty.isPresent) {
                    task.ignoreFailures = true
                }
                task.finalizedBy(zipXmlTask)
            }
        }
        if (!StudioType.isPlayground(project)) { // For non-playground setup use robolectric offline
            task.systemProperty("robolectric.offline", "true")
            val robolectricDependencies =
                File(
                    project.getPrebuiltsRoot(),
                    "androidx/external/org/robolectric/android-all-instrumented"
                )
            task.systemProperty(
                "robolectric.dependency.dir",
                robolectricDependencies.absolutePath
            )
        }
    }

    private fun configureWithKotlinPlugin(
        project: Project,
        extension: AndroidXExtension,
        plugin: KotlinBasePluginWrapper
    ) {
        project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
            task.kotlinOptions.jvmTarget = "1.8"
            task.kotlinOptions.freeCompilerArgs += listOf(
                "-Xskip-metadata-version-check"
            )
        }
        project.afterEvaluate {
            val isAndroidProject = project.plugins.hasPlugin(LibraryPlugin::class.java) ||
                project.plugins.hasPlugin(AppPlugin::class.java)
            // Explicit API mode is broken for Android projects
            // https://youtrack.jetbrains.com/issue/KT-37652
            if (extension.shouldEnforceKotlinStrictApiMode() && !isAndroidProject) {
                project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
                    // Workaround for https://youtrack.jetbrains.com/issue/KT-37652
                    if (task.name.endsWith("TestKotlin")) return@configureEach
                    task.kotlinOptions.freeCompilerArgs += listOf("-Xexplicit-api=strict")
                }
            }
        }
        if (plugin is KotlinMultiplatformPluginWrapper) {
            project.configureKonanDirectory()
            project.extensions.findByType<LibraryExtension>()?.apply {
                configureAndroidLibraryWithMultiplatformPluginOptions()
            }
            project.configureKmpBuildOnServer()
        }
    }

    @Suppress("UnstableApiUsage") // AGP DSL APIs
    private fun configureWithAppPlugin(project: Project, androidXExtension: AndroidXExtension) {
        project.extensions.getByType<AppExtension>().apply {
            configureAndroidBaseOptions(project, androidXExtension)
            configureAndroidApplicationOptions(project)
        }

        project.extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
            onVariants { it.configureLicensePackaging() }
            finalizeDsl {
                project.configureAndroidProjectForLint(it.lint, androidXExtension)
            }
        }
    }

    private fun configureWithTestPlugin(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        project.extensions.getByType<TestExtension>().apply {
            configureAndroidBaseOptions(project, androidXExtension)
        }

        project.configureJavaCompilationWarnings(androidXExtension)

        project.addToProjectMap(androidXExtension)
    }

    private fun HasAndroidTest.configureLicensePackaging() {
        androidTest?.packaging?.resources?.apply {
            // Workaround a limitation in AGP that fails to merge these META-INF license files.
            pickFirsts.add("/META-INF/AL2.0")
            // In addition to working around the above issue, we exclude the LGPL2.1 license as we're
            // approved to distribute code via AL2.0 and the only dependencies which pull in LGPL2.1
            // are currently dual-licensed with AL2.0 and LGPL2.1. The affected dependencies are:
            //   - net.java.dev.jna:jna:5.5.0
            excludes.add("/META-INF/LGPL2.1")
        }
    }

    @Suppress("UnstableApiUsage", "DEPRECATION") // AGP DSL APIs
    private fun configureWithLibraryPlugin(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        val libraryExtension = project.extensions.getByType<LibraryExtension>().apply {
            configureAndroidBaseOptions(project, androidXExtension)
            configureAndroidLibraryOptions(project, androidXExtension)

            // Make sure the main Kotlin source set doesn't contain anything under src/main/kotlin.
            val mainKotlinSrcDir = (sourceSets.findByName("main")?.kotlin
                as com.android.build.gradle.api.AndroidSourceDirectorySet)
                .srcDirs
                .filter { it.name == "kotlin" }
                .getOrNull(0)
            if (mainKotlinSrcDir?.isDirectory == true) {
                throw GradleException(
                    "Invalid project structure! AndroidX does not support \"kotlin\" as a " +
                        "top-level source directory for libraries, use \"java\" instead: " +
                        mainKotlinSrcDir.path
                )
            }
        }

        // Remove the lint and column attributes from generated lint baseline XML.
        project.tasks.withType(AndroidLintTask::class.java).configureEach { task ->
            if (task.name.startsWith("updateLintBaseline")) {
                task.doLast {
                    task.outputs.files.find { it.name == "lint-baseline.xml" }?.let { file ->
                        file.writeText(removeLineAndColumnAttributes(file.readText()))
                    }
                }
            }
        }

        // Remove the android:targetSdkVersion element from the manifest used for AARs.
        project.extensions.getByType<LibraryAndroidComponentsExtension>().onVariants { variant ->
            project.tasks.register(
                variant.name + "AarManifestTransformer",
                AarManifestTransformerTask::class.java
            ).let { taskProvider ->
                variant.artifacts.use(taskProvider)
                    .wiredWithFiles(
                        AarManifestTransformerTask::aarFile,
                        AarManifestTransformerTask::updatedAarFile
                    )
                    .toTransform(SingleArtifact.AAR)
            }
        }

        project.extensions.getByType<com.android.build.api.dsl.LibraryExtension>().apply {
            publishing {
                singleVariant(DEFAULT_PUBLISH_CONFIG)
            }
        }

        project.extensions.getByType<LibraryAndroidComponentsExtension>().apply {
            beforeVariants(selector().withBuildType("release")) { variant ->
                variant.enableUnitTest = false
            }
            onVariants { it.configureLicensePackaging() }
            finalizeDsl { project.configureAndroidProjectForLint(it.lint, androidXExtension) }
        }

        project.configurePublicResourcesStub(libraryExtension)
        project.configureSourceJarForAndroid(libraryExtension)
        project.configureVersionFileWriter(libraryExtension, androidXExtension)
        project.addCreateLibraryBuildInfoFileTask(androidXExtension)
        project.configureJavaCompilationWarnings(androidXExtension)

        project.configureDependencyVerification(androidXExtension) { taskProvider ->
            libraryExtension.defaultPublishVariant { libraryVariant ->
                taskProvider.configure { task ->
                    task.dependsOn(libraryVariant.javaCompileProvider)
                }
            }
        }

        val reportLibraryMetrics = project.configureReportLibraryMetricsTask()
        project.addToBuildOnServer(reportLibraryMetrics)
        libraryExtension.defaultPublishVariant { libraryVariant ->
            reportLibraryMetrics.configure {
                it.jarFiles.from(
                    libraryVariant.packageLibraryProvider.map { zip ->
                        zip.inputs.files
                    }
                )
            }
        }

        // Standard docs, resource API, and Metalava configuration for AndroidX projects.
        project.configureProjectForApiTasks(
            LibraryApiTaskConfig(libraryExtension),
            androidXExtension
        )

        project.addToProjectMap(androidXExtension)
    }

    private fun configureWithJavaPlugin(project: Project, extension: AndroidXExtension) {
        project.configureErrorProneForJava()
        project.configureSourceJarForJava()

        // Force Java 1.8 source- and target-compatibility for all Java libraries.
        val javaExtension = project.extensions.getByType<JavaPluginExtension>()
        javaExtension.apply {
            sourceCompatibility = VERSION_1_8
            targetCompatibility = VERSION_1_8
        }

        project.configureJavaCompilationWarnings(extension)

        project.hideJavadocTask()

        project.configureDependencyVerification(extension) { taskProvider ->
            taskProvider.configure { task ->
                task.dependsOn(project.tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME))
            }
        }

        project.addCreateLibraryBuildInfoFileTask(extension)

        // Standard lint, docs, and Metalava configuration for AndroidX projects.
        project.configureNonAndroidProjectForLint(extension)
        val apiTaskConfig = if (project.multiplatformExtension != null) {
            KmpApiTaskConfig
        } else {
            JavaApiTaskConfig
        }
        project.configureProjectForApiTasks(apiTaskConfig, extension)

        project.afterEvaluate {
            if (extension.type.publish.shouldRelease()) {
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

    private fun Project.configureProjectStructureValidation(
        extension: AndroidXExtension
    ) {
        // AndroidXExtension.mavenGroup is not readable until afterEvaluate.
        afterEvaluate {
            val mavenGroup = extension.mavenGroup
            val isProbablyPublished = extension.type == LibraryType.PUBLISHED_LIBRARY ||
                extension.type == LibraryType.UNSET
            if (mavenGroup != null && isProbablyPublished) {
                validateProjectStructure(mavenGroup.group)
            }
        }
    }

    private fun Project.configureProjectVersionValidation(
        extension: AndroidXExtension
    ) {
        // AndroidXExtension.mavenGroup is not readable until afterEvaluate.
        afterEvaluate {
            extension.validateMavenVersion()
        }
    }

    @Suppress("UnstableApiUsage") // Usage of ManagedVirtualDevice
    private fun TestOptions.configureVirtualDevices() {
        managedDevices.devices.register<ManagedVirtualDevice>("pixel2api29") {
            device = "Pixel 2"
            apiLevel = 29
            systemImageSource = "aosp"
        }
        managedDevices.devices.register<ManagedVirtualDevice>("pixel2api30") {
            device = "Pixel 2"
            apiLevel = 30
            systemImageSource = "aosp"
        }
        managedDevices.devices.register<ManagedVirtualDevice>("pixel2api31") {
            device = "Pixel 2"
            apiLevel = 31
            systemImageSource = "aosp"
        }
    }

    private fun BaseExtension.configureAndroidBaseOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        compileOptions.apply {
            sourceCompatibility = VERSION_1_8
            targetCompatibility = VERSION_1_8
        }

        compileSdkVersion(COMPILE_SDK_VERSION)
        buildToolsVersion = BUILD_TOOLS_VERSION
        defaultConfig.targetSdk = TARGET_SDK_VERSION
        ndkVersion = SupportConfig.NDK_VERSION

        defaultConfig.testInstrumentationRunner = INSTRUMENTATION_RUNNER

        testOptions.animationsDisabled = true
        testOptions.unitTests.isReturnDefaultValues = true
        testOptions.configureVirtualDevices()

        // Include resources in Robolectric tests as a workaround for b/184641296 and
        // ensure the build directory exists as a workaround for b/187970292.
        testOptions.unitTests.isIncludeAndroidResources = true
        if (!project.buildDir.exists()) project.buildDir.mkdirs()

        defaultConfig.minSdk = DEFAULT_MIN_SDK_VERSION
        project.afterEvaluate {
            val minSdkVersion = defaultConfig.minSdk!!
            check(minSdkVersion >= DEFAULT_MIN_SDK_VERSION) {
                "minSdkVersion $minSdkVersion lower than the default of $DEFAULT_MIN_SDK_VERSION"
            }
            check(compileSdkVersion == COMPILE_SDK_VERSION) {
                "compileSdkVersion must not be explicitly specified, was \"$compileSdkVersion\""
            }
            project.configurations.all { configuration ->
                configuration.resolutionStrategy.eachDependency { dep ->
                    val target = dep.target
                    val version = target.version
                    // Enforce the ban on declaring dependencies with version ranges.
                    // Note: In playground, this ban is exempted to allow unresolvable prebuilts
                    // to automatically get bumped to snapshot versions via version range
                    // substitution.
                    if (version != null && Version.isDependencyRange(version) &&
                        project.rootProject.rootDir == project.getSupportRootFolder()
                    ) {
                        throw IllegalArgumentException(
                            "Dependency ${dep.target} declares its version as " +
                                "version range ${dep.target.version} however the use of " +
                                "version ranges is not allowed, please update the " +
                                "dependency to list a fixed version."
                        )
                    }
                }
            }

            if (androidXExtension.type.compilationTarget != CompilationTarget.DEVICE) {
                throw IllegalStateException(
                    "${androidXExtension.type.name} libraries cannot apply the android plugin, as" +
                        " they do not target android devices"
                )
            }
        }

        val debugSigningConfig = signingConfigs.getByName("debug")
        // Use a local debug keystore to avoid build server issues.
        debugSigningConfig.storeFile = project.getKeystore()
        buildTypes.all { buildType ->
            // Sign all the builds (including release) with debug key
            buildType.signingConfig = debugSigningConfig
        }

        project.configureErrorProneForAndroid(variants)

        // workaround for b/120487939
        project.configurations.all { configuration ->
            // Gradle seems to crash on androidtest configurations
            // preferring project modules...
            if (!configuration.name.lowercase(Locale.US).contains("androidtest")) {
                configuration.resolutionStrategy.preferProjectModules()
            }
        }

        project.configureTestConfigGeneration(this)

        val buildTestApksTask = project.rootProject.tasks.named(BUILD_TEST_APKS_TASK)
        when (this) {
            is TestedExtension -> testVariants
            // app module defines variants for test module
            is TestExtension -> applicationVariants
            else -> throw IllegalStateException("Unsupported plugin type")
        }.all { variant ->
            buildTestApksTask.configure {
                it.dependsOn(variant.assembleProvider)
            }
            variant.configureApkZipping(project, true)
        }

        // AGP warns if we use project.buildDir (or subdirs) for CMake's generated
        // build files (ninja build files, CMakeCache.txt, etc.). Use a staging directory that
        // lives alongside the project's buildDir.
        externalNativeBuild.cmake.buildStagingDirectory =
            File(project.buildDir, "../nativeBuildStaging")

        // disable analytics recording
        // It's always out-of-date, and we don't release any apps in this repo
        project.tasks.withType(AnalyticsRecordingTask::class.java).configureEach { task ->
            task.enabled = false
        }
    }

    /**
     * Configures the ZIP_TEST_CONFIGS_WITH_APKS_TASK to include the test apk if applicable
     */
    @Suppress("DEPRECATION") // ApkVariant
    private fun com.android.build.gradle.api.ApkVariant.configureApkZipping(
        project: Project,
        testApk: Boolean
    ) {
        packageApplicationProvider.get().let { packageTask ->
            AffectedModuleDetector.configureTaskGuard(packageTask)
            // Skip copying AndroidTest apks if they have no source code (no tests to run).
            if (!testApk || project.hasAndroidTestSourceCode()) {
                addToTestZips(project, packageTask)
            }
        }
        project.tasks.withType(ListingFileRedirectTask::class.java).forEach {
            AffectedModuleDetector.configureTaskGuard(it)
        }
    }

    private fun LibraryExtension.configureAndroidLibraryOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        // Note, this should really match COMPILE_SDK_VERSION, however
        // this API takes an integer and we are unable to set it to a
        // pre-release SDK.
        defaultConfig.aarMetadata.minCompileSdk = TARGET_SDK_VERSION
        project.configurations.all { config ->
            val isTestConfig = config.name.lowercase(Locale.US).contains("test")

            config.dependencyConstraints.configureEach { dependencyConstraint ->
                dependencyConstraint.apply {
                    // Remove strict constraints on test dependencies and listenablefuture:1.0
                    if (isTestConfig ||
                        group == "com.google.guava" &&
                        name == "listenablefuture" &&
                        version == "1.0"
                    ) {
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
        }
    }

    private fun TestedExtension.configureAndroidLibraryWithMultiplatformPluginOptions() {
        sourceSets.findByName("main")!!.manifest.srcFile("src/androidMain/AndroidManifest.xml")
        sourceSets.findByName("androidTest")!!
            .manifest.srcFile("src/androidAndroidTest/AndroidManifest.xml")
    }

    /**
     * Sets the konan distribution url to the prebuilts directory.
     */
    private fun Project.configureKonanDirectory() {
        if (StudioType.isPlayground(this)) {
            return // playground does not use prebuilts
        }
        overrideKotlinNativeCompilerRepository()
        tasks.withType(KotlinNativeCompile::class.java).configureEach {
            // use relative path so it doesn't affect gradle remote cache.
            val relativePath = getKonanPrebuiltsFolder().relativeTo(rootProject.projectDir).path
            it.kotlinOptions.freeCompilerArgs += listOf(
                "-Xoverride-konan-properties=dependenciesUrl=file:$relativePath"
            )
        }
    }

    /**
     * Until kotlin 1.7, we cannot set the repository URL where KMP plugin downloads the kotlin
     * native compiler. This method implements a workaround for 1.6.21.
     * We hijack the repository added by NativeCompilerDownloader to make it point to the konan
     * prebuilts directory.
     * After kotlin 1.7, we should use nativeBaseDownloadUrl property:
     * https://github.com/JetBrains/kotlin/blob/025a21761b326767207b4a373593a3c2d24b8056/libraries/
     *   tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/
     *   KotlinProperties.kt#L223
     */
    private fun Project.overrideKotlinNativeCompilerRepository() {
        this.repositories.whenObjectAdded {
            if (it is IvyArtifactRepository) {
                if (it.url.host == "download.jetbrains.com" &&
                    it.url.path.contains("kotlin/native/builds")
                ) {
                    val fileUrl = project.getKonanPrebuiltsFolder().resolve(
                        "nativeCompilerPrebuilts"
                    ).resolve(
                        it.url.path.substringAfter("kotlin/native/builds/")
                    ).canonicalFile
                    check(fileUrl.exists()) {
                        val konanVersion = getVersionByName("kotlinNative")
                        """
                        Missing kotlin native compiler prebuilt in $fileUrl. If you are updating
                        kotlin version, please add the new compiler to the prebuilts/androidx/konan
                        repository. You can download them by invoking the following script:

                        ../../prebuilts/androidx/konan/download-native-compiler-prebuilts.sh $konanVersion

                        Please don't forget to commit that version into the konan repository.
                        """.trimIndent()
                    }
                    it.url = fileUrl.toURI()
                }
            }
        }
    }

    private fun Project.configureKmpBuildOnServer() {
        val kmpExtension = checkNotNull(
            project.extensions.findByType<KotlinMultiplatformExtension>()
        ) {
            """
            Project ${project.path} applies kotlin multiplatform plugin but we cannot find the
            KotlinMultiplatformExtension.
            """.trimIndent()
        }
        // Add all "configured" native tests to the buildOnServer task.
        // Note that we don't check if platform is enabled in flags because it wouldn't be
        // configured in the first place if the target is not enabled
        kmpExtension.testableTargets.all { kotlinTarget ->
            kotlinTarget.testRuns.all { kotlinTestRun ->
                // Need to check for both KotlinNativeHostTest (to ensure it runs on host, not on
                // an emulator or simulator) and also KotlinTaskTestRun to ensure it has a task.
                // Unfortunately, there is no parent interface/class that covers both cases.
                if (kotlinTestRun is KotlinNativeHostTestRun &&
                    kotlinTestRun is KotlinTaskTestRun<*, *>) {
                    project.addToBuildOnServer(kotlinTestRun.executionTask)
                }
            }
        }
    }

    private fun AppExtension.configureAndroidApplicationOptions(project: Project) {
        defaultConfig.apply {
            versionCode = 1
            versionName = "1.0"
        }

        project.addAppApkToTestConfigGeneration()

        val buildTestApksTask = project.rootProject.tasks.named(BUILD_TEST_APKS_TASK)
        applicationVariants.all { variant ->
            // Using getName() instead of name due to b/150427408
            if (variant.buildType.name == "debug") {
                buildTestApksTask.configure {
                    it.dependsOn(variant.assembleProvider)
                }
            }
            variant.configureApkZipping(project, false)
        }
    }

    private fun Project.configureDependencyVerification(
        extension: AndroidXExtension,
        taskConfigurator: (TaskProvider<VerifyDependencyVersionsTask>) -> Unit
    ) {
        afterEvaluate {
            if (extension.type != LibraryType.SAMPLES) {
                val verifyDependencyVersionsTask = project.createVerifyDependencyVersionsTask()
                if (verifyDependencyVersionsTask != null) {
                    project.createCheckReleaseReadyTask(listOf(verifyDependencyVersionsTask))
                    taskConfigurator(verifyDependencyVersionsTask)
                }
            }
        }
    }

    // Task that creates a json file of a project's dependencies
    private fun Project.addCreateLibraryBuildInfoFileTask(extension: AndroidXExtension) {
        afterEvaluate {
            if (extension.publish.shouldRelease()) {
                // Only generate build info files for published libraries.
                val task = CreateLibraryBuildInfoFileTask.setup(project, extension)

                rootProject.tasks.named(CreateLibraryBuildInfoFileTask.TASK_NAME).configure {
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

    companion object {
        const val BUILD_TEST_APKS_TASK = "buildTestApks"
        const val CHECK_RELEASE_READY_TASK = "checkReleaseReady"
        const val CREATE_LIBRARY_BUILD_INFO_FILES_TASK = "createLibraryBuildInfoFiles"
        const val CREATE_AGGREGATE_BUILD_INFO_FILES_TASK = "createAggregateBuildInfoFiles"
        const val GENERATE_TEST_CONFIGURATION_TASK = "GenerateTestConfiguration"
        const val REPORT_LIBRARY_METRICS_TASK = "reportLibraryMetrics"
        const val ZIP_TEST_CONFIGS_WITH_APKS_TASK = "zipTestConfigsWithApks"
        const val ZIP_CONSTRAINED_TEST_CONFIGS_WITH_APKS_TASK = "zipConstrainedTestConfigsWithApks"

        const val TASK_GROUP_API = "API"

        const val EXTENSION_NAME = "androidx"

        /**
         * Fail the build if a non-Studio task runs longer than expected
         */
        const val TASK_TIMEOUT_MINUTES = 60L
    }
}

private const val PROJECTS_MAP_KEY = "projects"
private const val ACCESSED_PROJECTS_MAP_KEY = "accessedProjectsMap"

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

                if (project.rootProject.extra.has(ACCESSED_PROJECTS_MAP_KEY)) {
                    throw GradleException(
                        "Attempted to add $project to project map after " +
                            "the contents of the map were accessed"
                    )
                }
                @Suppress("UNCHECKED_CAST")
                val projectModules = project.rootProject.extra.get(PROJECTS_MAP_KEY)
                    as ConcurrentHashMap<String, String>
                projectModules[module] = path
            }
        }
    }
}

val Project.multiplatformExtension
    get() = extensions.findByType(KotlinMultiplatformExtension::class.java)

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

@Suppress("UNCHECKED_CAST")
fun Project.getProjectsMap(): ConcurrentHashMap<String, String> {
    project.rootProject.extra.set(ACCESSED_PROJECTS_MAP_KEY, true)
    return rootProject.extra.get(PROJECTS_MAP_KEY) as ConcurrentHashMap<String, String>
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

private fun Project.configureJavaCompilationWarnings(androidXExtension: AndroidXExtension) {
    afterEvaluate {
        project.tasks.withType(JavaCompile::class.java).configureEach { task ->
            // If we're running a hypothetical test build confirming that tip-of-tree versions
            // are compatible, then we're not concerned about warnings
            if (!project.usingMaxDepVersions()) {
                task.options.compilerArgs.add("-Xlint:unchecked")
                if (androidXExtension.failOnDeprecationWarnings) {
                    task.options.compilerArgs.add("-Xlint:deprecation")
                }
            }
        }
    }
}

/**
 * Guarantees unique names for the APKs, and modifies some of the suffixes. The APK name is used
 * to determine what gets run by our test runner
 */
fun String.renameApkForTesting(projectPath: String, hasBenchmarkPlugin: Boolean): String {
    val name =
        if (projectPath.contains("media") && projectPath.contains("version-compat-tests")) {
            // Exclude media*:version-compat-tests modules from
            // existing support library presubmit tests.
            this.replace("-debug-androidTest", "")
        } else if (hasBenchmarkPlugin) {
            this.replace("-androidTest", "-androidBenchmark")
        } else if (projectPath.endsWith("macrobenchmark")) {
            this.replace("-androidTest", "-androidMacrobenchmark")
        } else {
            this
        }
    return "${projectPath.asFilenamePrefix()}_$name"
}

fun Project.hasBenchmarkPlugin(): Boolean {
    return this.plugins.hasPlugin(BenchmarkPlugin::class.java)
}

/**
 * Returns a string that is a valid filename and loosely based on the project name
 * The value returned for each project will be distinct
 */
fun String.asFilenamePrefix(): String {
    return this.substring(1).replace(':', '-')
}

/**
 * Sets the specified [task] as a dependency of the top-level `check` task, ensuring that it runs
 * as part of `./gradlew check`.
 */
fun <T : Task> Project.addToCheckTask(task: TaskProvider<T>) {
    project.tasks.named("check").configure {
        it.dependsOn(task)
    }
}

/**
 * Expected to be called in afterEvaluate when all extensions are available
 */
internal fun Project.hasAndroidTestSourceCode(): Boolean {
    // com.android.test modules keep test code in main sourceset
    extensions.findByType(TestExtension::class.java)?.let { extension ->
        extension.sourceSets.findByName("main")?.let { sourceSet ->
            if (!sourceSet.java.getSourceFiles().isEmpty) return true
        }
        // check kotlin-android main source set
        extensions.findByType(KotlinAndroidProjectExtension::class.java)
            ?.sourceSets?.findByName("main")?.let {
                if (it.kotlin.files.isNotEmpty()) return true
            }
        // Note, don't have to check for kotlin-multiplatform as it is not compatible with
        // com.android.test modules
    }

    // check Java androidTest source set
    extensions.findByType(TestedExtension::class.java)
        ?.sourceSets
        ?.findByName("androidTest")
        ?.let { sourceSet ->
            // using getSourceFiles() instead of sourceFiles due to b/150800094
            if (!sourceSet.java.getSourceFiles().isEmpty) return true
        }

    // check kotlin-android androidTest source set
    extensions.findByType(KotlinAndroidProjectExtension::class.java)
        ?.sourceSets?.findByName("androidTest")?.let {
            if (it.kotlin.files.isNotEmpty()) return true
        }

    // check kotlin-multiplatform androidAndroidTest source set
    multiplatformExtension?.apply {
        sourceSets.findByName("androidAndroidTest")?.let {
            if (it.kotlin.files.isNotEmpty()) return true
        }
    }

    return false
}

private const val GROUP_PREFIX = "androidx."

/**
 * Validates the project structure against Jetpack guidelines.
 */
fun Project.validateProjectStructure(groupId: String) {
    if (!project.isValidateProjectStructureEnabled()) {
        return
    }

    val shortGroupId = if (groupId.startsWith(GROUP_PREFIX)) {
        groupId.substring(GROUP_PREFIX.length)
    } else {
        groupId
    }

    // Fully-qualified Gradle project name should match the Maven coordinate.
    val expectName = ":${shortGroupId.replace(".",":")}:${project.name}"
    val actualName = project.path
    if (expectName != actualName) {
        throw GradleException(
            "Invalid project structure! Expected $expectName as project name, found $actualName"
        )
    }

    // Project directory should match the Maven coordinate.
    val expectDir = shortGroupId.replace(".", File.separator) +
        "${File.separator}${project.name}"
    val actualDir = project.projectDir.toRelativeString(project.getSupportRootFolder())
    if (expectDir != actualDir) {
        throw GradleException(
            "Invalid project structure! Expected $expectDir as project directory, found $actualDir"
        )
    }
}

/**
 * Validates the Maven version against Jetpack guidelines.
 */
fun AndroidXExtension.validateMavenVersion() {
    val mavenGroup = mavenGroup
    val mavenVersion = mavenVersion
    val forcedVersion = mavenGroup?.atomicGroupVersion
    if (forcedVersion != null && forcedVersion == mavenVersion) {
        throw GradleException(
            """
            Unnecessary override of same-group library version

            Project version is already set to $forcedVersion by same-version group
            ${mavenGroup.group}.

            To fix this error, remove "mavenVersion = ..." from your build.gradle
            configuration.
            """.trimIndent()
        )
    }
}

/**
 * Removes the line and column attributes from the [baseline].
 */
fun removeLineAndColumnAttributes(baseline: String): String = baseline.replace(
    "\\s*(line|column)=\"\\d+?\"".toRegex(),
    ""
)

const val PROJECT_OR_ARTIFACT_EXT_NAME = "projectOrArtifact"
