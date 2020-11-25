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

import androidx.build.dependencyTracker.ProjectSubset
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

const val TEMPLATE = """<?xml version="1.0" encoding="utf-8"?>
        <!-- Copyright (C) 2019 The Android Open Source Project
        Licensed under the Apache License, Version 2.0 (the "License")
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
        -->
        <configuration description="Runs tests for the module">
        <object type="module_controller" class="com.android.tradefed.testtype.suite.module.MinApiLevelModuleController">
            <option name="min-api-level" value="MIN_SDK" />
        </object>
        <option name="test-suite-tag" value="TEST_SUITE_TAG" />
        <option name="config-descriptor:metadata" key="applicationId" value="APPLICATION_ID" />
        <option name="wifi:disable" value="true" />
        <include name="google/unbundled/common/setup" />
        <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
        <option name="cleanup-apks" value="true" />
        <option name="test-file-name" value="TEST_FILE_NAME" />
        <option name="test-file-name" value="APP_FILE_NAME" />
        </target_preparer>
        <test class="com.android.tradefed.testtype.AndroidJUnitTest">
        <option name="runner" value="TEST_RUNNER"/>
        <option name="package" value="APPLICATION_ID" />
        </test>
        </configuration>"""

const val SELF_INSTRUMENTING_TEMPLATE = """<?xml version="1.0" encoding="utf-8"?>
        <!-- Copyright (C) 2019 The Android Open Source Project
        Licensed under the Apache License, Version 2.0 (the "License")
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
        -->
        <configuration description="Runs tests for the module">
        <object type="module_controller" class="com.android.tradefed.testtype.suite.module.MinApiLevelModuleController">
            <option name="min-api-level" value="MIN_SDK" />
        </object>
        <option name="test-suite-tag" value="TEST_SUITE_TAG" />
        <option name="config-descriptor:metadata" key="applicationId" value="APPLICATION_ID" />
        <option name="wifi:disable" value="true" />
        <include name="google/unbundled/common/setup" />
        <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
        <option name="cleanup-apks" value="true" />
        <option name="test-file-name" value="TEST_FILE_NAME" />
        </target_preparer>
        TEST_BLOCK
        </configuration>"""

const val FULL_TEST = """
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">
        <option name="runner" value="TEST_RUNNER"/>
        <option name="package" value="APPLICATION_ID" />
    </test>
"""

const val DEPENDENT_TESTS = """
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">
        <option name="runner" value="TEST_RUNNER"/>
        <option name="package" value="APPLICATION_ID" />
        <option name="size" value="small" />
        <option name="test-timeout" value="300" />
    </test>
    <test class="com.android.tradefed.testtype.AndroidJUnitTest">
        <option name="runner" value="TEST_RUNNER"/>
        <option name="package" value="APPLICATION_ID" />
        <option name="size" value="medium" />
        <option name="test-timeout" value="1500" />
    </test>
"""

/**
 * Writes a configuration file in
 * <a href=https://source.android.com/devices/tech/test_infra/tradefed/testing/through-suite/android-test-structure>AndroidTest.xml</a>
 * format that gets zipped alongside the APKs to be tested.
 * This config gets ingested by Tradefed.
 */
abstract class GenerateTestConfigurationTask : DefaultTask() {

    @get:InputFiles
    @get:Optional
    abstract val appFolder: DirectoryProperty

    @get:Internal
    abstract val appLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    abstract val testFolder: DirectoryProperty

    @get:Internal
    abstract val testLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val minSdk: Property<Int>

    @get:Input
    abstract val hasBenchmarkPlugin: Property<Boolean>

    @get:Input
    abstract val testRunner: Property<String>

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val affectedModuleDetectorSubset: Property<ProjectSubset>

    @get:OutputFile
    abstract val outputXml: RegularFileProperty

    @TaskAction
    fun generateAndroidTestZip() {
        writeConfigFileContent()
    }

    private fun writeConfigFileContent() {
        /*
        Testing an Android Application project involves 2 APKS: an application to be instrumented,
        and a test APK. Testing an Android Library project involves only 1 APK, since the library
        is bundled inside the test APK, meaning it is self instrumenting. We add extra data to
        configurations testing Android Application projects, so that both APKs get installed.
         */
        var configContent: String = if (appLoader.isPresent) {
            val appApk = appLoader.get().load(appFolder.get())
                ?: throw RuntimeException("Cannot load application APK for $name")
            val appName = appApk.elements.single().outputFile.substringAfterLast("/")
                .renameApkForTesting(projectPath.get(), hasBenchmarkPlugin.get())
            TEMPLATE.replace("APP_FILE_NAME", appName)
        } else {
            SELF_INSTRUMENTING_TEMPLATE
        }
        configContent = when (affectedModuleDetectorSubset.get()) {
            ProjectSubset.CHANGED_PROJECTS, ProjectSubset.ALL_AFFECTED_PROJECTS -> {
                configContent.replace("TEST_BLOCK", FULL_TEST)
            }
            ProjectSubset.DEPENDENT_PROJECTS -> {
                configContent.replace("TEST_BLOCK", DEPENDENT_TESTS)
            }
            else -> {
                throw IllegalStateException(
                    "$name should not be running if the AffectedModuleDetector is returning " +
                        "${affectedModuleDetectorSubset.get()} for this project."
                )
            }
        }
        val tag = if (hasBenchmarkPlugin.get()) "MetricTests" else "androidx_unit_tests"
        val testApk = testLoader.get().load(testFolder.get())
            ?: throw RuntimeException("Cannot load test APK for $name")
        val testName = testApk.elements.single().outputFile
            .substringAfterLast("/")
            .renameApkForTesting(projectPath.get(), hasBenchmarkPlugin.get())
        configContent = configContent.replace("TEST_FILE_NAME", testName)
            .replace("APPLICATION_ID", testApk.applicationId)
            .replace("MIN_SDK", minSdk.get().toString())
            .replace("TEST_SUITE_TAG", tag)
            .replace("TEST_RUNNER", testRunner.get())
        val resolvedOutputFile: File = outputXml.asFile.get()
        if (!resolvedOutputFile.exists()) {
            if (!resolvedOutputFile.createNewFile()) {
                throw RuntimeException(
                    "Failed to create test configuration file: $outputXml"
                )
            }
        }
        resolvedOutputFile.writeText(configContent)
    }
}
