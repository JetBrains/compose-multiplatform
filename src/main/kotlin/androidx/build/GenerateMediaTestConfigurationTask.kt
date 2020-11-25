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

import com.android.build.api.variant.BuiltArtifacts
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

const val MEDIA_TEMPLATE = """<?xml version="1.0" encoding="utf-8"?>
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
        <option name="test-suite-tag" value="androidx_unit_tests_suite" />
        <option name="config-descriptor:metadata" key="applicationId"
            value="CLIENT_APPLICATION_ID;SERVICE_APPLICATION_ID" />
        <option name="wifi:disable" value="true" />
        <include name="google/unbundled/common/setup" />
        <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
        <option name="cleanup-apks" value="true" />
        <option name="test-file-name" value="CLIENT_FILE_NAME" />
        <option name="test-file-name" value="SERVICE_FILE_NAME" />
        </target_preparer>
        <test class="com.android.tradefed.testtype.AndroidJUnitTest">
        <option name="runner" value="TEST_RUNNER"/>
        <option name="package" value="CLIENT_APPLICATION_ID" />
        INSTRUMENTATION_ARGS
        </test>
        <test class="com.android.tradefed.testtype.AndroidJUnitTest">
        <option name="runner" value="TEST_RUNNER"/>
        <option name="package" value="SERVICE_APPLICATION_ID" />
        INSTRUMENTATION_ARGS
        </test>
        </configuration>"""

const val CLIENT_PREVIOUS = """
    <option name="instrumentation-arg" key="client_version" value="previous" />
"""
const val CLIENT_TOT = """
    <option name="instrumentation-arg" key="client_version" value="tot" />
"""
const val SERVICE_PREVIOUS = """
    <option name="instrumentation-arg" key="service_version" value="previous" />
"""
const val SERVICE_TOT = """
    <option name="instrumentation-arg" key="service_version" value="tot" />
"""

/**
 * Writes three configuration files to test combinations of media client & service in
 * <a href=https://source.android.com/devices/tech/test_infra/tradefed/testing/through-suite/android-test-structure>AndroidTest.xml</a>
 * format that gets zipped alongside the APKs to be tested. The combinations are of previous and
 * tip-of-tree versions client and service. We want to test every possible pairing that includes
 * tip-of-tree.
 *
 * This config gets ingested by Tradefed.
 */
abstract class GenerateMediaTestConfigurationTask : DefaultTask() {

    @get:InputFiles
    abstract val clientToTFolder: DirectoryProperty

    @get:Internal
    abstract val clientToTLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    abstract val clientPreviousFolder: DirectoryProperty

    @get:Internal
    abstract val clientPreviousLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    abstract val serviceToTFolder: DirectoryProperty

    @get:Internal
    abstract val serviceToTLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    abstract val servicePreviousFolder: DirectoryProperty

    @get:Internal
    abstract val servicePreviousLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val clientToTPath: Property<String>

    @get:Input
    abstract val clientPreviousPath: Property<String>

    @get:Input
    abstract val serviceToTPath: Property<String>

    @get:Input
    abstract val servicePreviousPath: Property<String>

    @get:Input
    abstract val minSdk: Property<Int>

    @get:Input
    abstract val testRunner: Property<String>

    @get:OutputFile
    abstract val clientPreviousServiceToT: RegularFileProperty

    @get:OutputFile
    abstract val clientToTServicePrevious: RegularFileProperty

    @get:OutputFile
    abstract val clientToTServiceToT: RegularFileProperty

    @TaskAction
    fun generateAndroidTestZip() {
        val clientToTApk = resolveApk(clientToTFolder, clientToTLoader)
        val clientPreviousApk = resolveApk(clientPreviousFolder, clientPreviousLoader)
        val serviceToTApk = resolveApk(serviceToTFolder, serviceToTLoader)
        val servicePreviousApk = resolveApk(
            servicePreviousFolder, servicePreviousLoader
        )
        writeConfigFileContent(
            clientToTApk, serviceToTApk, clientToTPath.get(),
            serviceToTPath.get(), clientToTServiceToT
        )
        writeConfigFileContent(
            clientToTApk, servicePreviousApk, clientToTPath.get(),
            servicePreviousPath.get(), clientToTServicePrevious
        )
        writeConfigFileContent(
            clientPreviousApk, serviceToTApk, clientPreviousPath.get(),
            serviceToTPath.get(), clientPreviousServiceToT
        )
    }

    private fun resolveApk(
        apkFolder: DirectoryProperty,
        apkLoader: Property<BuiltArtifactsLoader>
    ): BuiltArtifacts {
        return apkLoader.get().load(apkFolder.get())
            ?: throw RuntimeException("Cannot load APK for $name")
    }

    private fun resolveName(apk: BuiltArtifacts, path: String): String {
        return apk.elements.single().outputFile.substringAfterLast("/")
            .renameApkForTesting(path, false)
    }

    private fun writeConfigFileContent(
        clientApk: BuiltArtifacts,
        serviceApk: BuiltArtifacts,
        clientPath: String,
        servicePath: String,
        outputFile: RegularFileProperty
    ) {
        val instrumentationArgs =
            if (clientPath.contains("previous")) {
                if (servicePath.contains("previous")) {
                    CLIENT_PREVIOUS + SERVICE_PREVIOUS
                } else {
                    CLIENT_PREVIOUS + SERVICE_TOT
                }
            } else if (servicePath.contains("previous")) {
                CLIENT_TOT + SERVICE_PREVIOUS
            } else {
                CLIENT_TOT + SERVICE_TOT
            }
        var configContent: String = MEDIA_TEMPLATE
        configContent = configContent
            .replace("CLIENT_FILE_NAME", resolveName(clientApk, clientPath))
            .replace("SERVICE_FILE_NAME", resolveName(serviceApk, servicePath))
            .replace("CLIENT_APPLICATION_ID", clientApk.applicationId)
            .replace("SERVICE_APPLICATION_ID", serviceApk.applicationId)
            .replace("MIN_SDK", minSdk.get().toString())
            .replace("TEST_RUNNER", testRunner.get())
            .replace("INSTRUMENTATION_ARGS", instrumentationArgs)
        val resolvedOutputFile: File = outputFile.asFile.get()
        if (!resolvedOutputFile.exists()) {
            if (!resolvedOutputFile.createNewFile()) {
                throw RuntimeException(
                    "Failed to create test configuration file: $outputFile"
                )
            }
        }
        resolvedOutputFile.writeText(configContent)
    }
}
