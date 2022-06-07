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

package androidx.build.testConfiguration

import androidx.build.dependencyTracker.ProjectSubset
import androidx.build.isPresubmitBuild
import androidx.build.renameApkForTesting
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Writes a configuration file in
 * <a href=https://source.android.com/devices/tech/test_infra/tradefed/testing/through-suite/android-test-structure>AndroidTest.xml</a>
 * format that gets zipped alongside the APKs to be tested.
 * This config gets ingested by Tradefed.
 */
@CacheableTask
abstract class GenerateTestConfigurationTask : DefaultTask() {

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val appFolder: DirectoryProperty

    @get:Internal
    abstract val appLoader: Property<BuiltArtifactsLoader>

    @get:Input
    @get:Optional
    abstract val appProjectPath: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testFolder: DirectoryProperty

    @get:Internal
    abstract val testLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val testProjectPath: Property<String>

    @get:Input
    abstract val minSdk: Property<Int>

    @get:Input
    abstract val hasBenchmarkPlugin: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val benchmarkRunAlsoInterpreted: Property<Boolean>

    @get:Input
    abstract val testRunner: Property<String>

    @get:Input
    abstract val affectedModuleDetectorSubset: Property<ProjectSubset>

    @get:OutputFile
    abstract val outputXml: RegularFileProperty

    @get:OutputFile
    abstract val constrainedOutputXml: RegularFileProperty

    @TaskAction
    fun generateAndroidTestZip() {
        writeConfigFileContent(constrainedOutputXml, true)
        writeConfigFileContent(outputXml)
    }

    private fun writeConfigFileContent(
        outputFile: RegularFileProperty,
        isConstrained: Boolean = false
    ) {
        /*
        Testing an Android Application project involves 2 APKS: an application to be instrumented,
        and a test APK. Testing an Android Library project involves only 1 APK, since the library
        is bundled inside the test APK, meaning it is self instrumenting. We add extra data to
        configurations testing Android Application projects, so that both APKs get installed.
         */
        val configBuilder = ConfigBuilder()
        if (appLoader.isPresent) {
            val appApk = appLoader.get().load(appFolder.get())
                ?: throw RuntimeException("Cannot load required APK for task: $name")
            // We don't need to check hasBenchmarkPlugin because benchmarks shouldn't have test apps
            val appName = appApk.elements.single().outputFile.substringAfterLast("/")
                .renameApkForTesting(appProjectPath.get(), hasBenchmarkPlugin = false)
            // TODO(b/178776319): Clean up this hardcoded hack
            if (appProjectPath.get().contains("macrobenchmark-target")) {
                configBuilder.appApkName(appName.replace("debug-androidTest", "release"))
            } else {
                configBuilder.appApkName(appName)
            }
        }
        val isPresubmit = isPresubmitBuild()
        configBuilder.isPostsubmit(!isPresubmit)
        // Will be using the constrained configs for all devices api 26 and below.
        // Don't attempt to remove APKs after testing. We can't remove the apk on API < 27 due to a
        // platform crash that occurs when handling a PACKAGE_CHANGED broadcast after the package has
        // been removed. See b/37264334.
        if (isConstrained) {
            configBuilder.cleanupApks(false)
        }
        when (affectedModuleDetectorSubset.get()) {
            ProjectSubset.DEPENDENT_PROJECTS -> {
                // Don't ever run full tests of RV if it is dependent, since they take > 45 minutes
                if (isConstrained || testProjectPath.get().contains("recyclerview")) {
                    configBuilder.runAllTests(false)
                } else {
                    configBuilder.runAllTests(true)
                }
            }
            ProjectSubset.NONE -> {
                if (isPresubmit) {
                    configBuilder.runAllTests(false)
                } else {
                    configBuilder.runAllTests(true)
                }
            }
            // in all other cases, if we are building this config we want to run all the tests
            else -> {
                configBuilder.runAllTests(true)
            }
        }
        // This section adds metadata tags that will help filter runners to specific modules.
        if (hasBenchmarkPlugin.get()) {
            configBuilder.isBenchmark(true)
            if (configBuilder.isPostsubmit) {
                if (benchmarkRunAlsoInterpreted.get()) {
                    configBuilder.tag("microbenchmarks_interpreted")
                }
                configBuilder.tag("microbenchmarks")
            } else {
                // in presubmit, we treat micro benchmarks as regular correctness tests as
                // they run with dryRunMode to check crashes don't happen, without measurement
                configBuilder.tag("androidx_unit_tests")
            }
        } else if (testProjectPath.get().endsWith("macrobenchmark")) {
            // macro benchmarks do not have a dryRunMode, so we don't run them in presubmit
            configBuilder.tag("macrobenchmarks")
        } else {
            configBuilder.tag("androidx_unit_tests")
            if (testProjectPath.get().startsWith(":compose:")) {
                configBuilder.tag("compose")
            } else if (testProjectPath.get().startsWith(":wear:")) {
                configBuilder.tag("wear")
            }
        }
        val testApk = testLoader.get().load(testFolder.get())
            ?: throw RuntimeException("Cannot load required APK for task: $name")
        val testName = testApk.elements.single().outputFile
            .substringAfterLast("/")
            .renameApkForTesting(testProjectPath.get(), hasBenchmarkPlugin.get())
        configBuilder.testApkName(testName)
            .applicationId(testApk.applicationId)
            .minSdk(minSdk.get().toString())
            .testRunner(testRunner.get())

        val resolvedOutputFile: File = outputFile.asFile.get()
        if (!resolvedOutputFile.exists()) {
            if (!resolvedOutputFile.createNewFile()) {
                throw RuntimeException(
                    "Failed to create test configuration file: $resolvedOutputFile"
                )
            }
        }
        resolvedOutputFile.writeText(configBuilder.build())
    }
}
