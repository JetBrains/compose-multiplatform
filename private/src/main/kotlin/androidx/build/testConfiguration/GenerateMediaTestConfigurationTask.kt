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
import androidx.build.renameApkForTesting
import com.android.build.api.variant.BuiltArtifact
import com.android.build.api.variant.BuiltArtifacts
import com.android.build.api.variant.BuiltArtifactsLoader
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Writes three configuration files to test combinations of media client & service in
 * <a href=https://source.android.com/devices/tech/test_infra/tradefed/testing/through-suite/android-test-structure>AndroidTest.xml</a>
 * format that gets zipped alongside the APKs to be tested. The combinations are of previous and
 * tip-of-tree versions client and service. We want to test every possible pairing that includes
 * tip-of-tree.
 *
 * This config gets ingested by Tradefed.
 */
@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class GenerateMediaTestConfigurationTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val clientToTFolder: DirectoryProperty

    @get:Internal
    abstract val clientToTLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val clientPreviousFolder: DirectoryProperty

    @get:Internal
    abstract val clientPreviousLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val serviceToTFolder: DirectoryProperty

    @get:Internal
    abstract val serviceToTLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val servicePreviousFolder: DirectoryProperty

    @get:Internal
    abstract val servicePreviousLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val affectedModuleDetectorSubset: Property<ProjectSubset>

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

    @get:Input
    abstract val presubmit: Property<Boolean>

    @get:OutputFile
    abstract val clientPreviousServiceToT: RegularFileProperty

    @get:OutputFile
    abstract val clientToTServicePrevious: RegularFileProperty

    @get:OutputFile
    abstract val clientToTServiceToT: RegularFileProperty

    @get:OutputFile
    abstract val constrainedClientPreviousServiceToT: RegularFileProperty

    @get:OutputFile
    abstract val constrainedClientToTServicePrevious: RegularFileProperty

    @get:OutputFile
    abstract val constrainedClientToTServiceToT: RegularFileProperty

    @get:OutputFile
    abstract val jsonClientPreviousServiceToTClientTests: RegularFileProperty

    @get:OutputFile
    abstract val jsonClientPreviousServiceToTServiceTests: RegularFileProperty

    @get:OutputFile
    abstract val jsonClientToTServicePreviousClientTests: RegularFileProperty

    @get:OutputFile
    abstract val jsonClientToTServicePreviousServiceTests: RegularFileProperty

    @get:OutputFile
    abstract val jsonClientToTServiceToTClientTests: RegularFileProperty

    @get:OutputFile
    abstract val jsonClientToTServiceToTServiceTests: RegularFileProperty

    @TaskAction
    fun generateAndroidTestZip() {
        val clientToTApk = resolveApk(clientToTFolder, clientToTLoader)
        val clientPreviousApk = resolveApk(clientPreviousFolder, clientPreviousLoader)
        val serviceToTApk = resolveApk(serviceToTFolder, serviceToTLoader)
        val servicePreviousApk = resolveApk(
            servicePreviousFolder, servicePreviousLoader
        )
        writeConfigFileContent(
            clientApk = clientToTApk,
            serviceApk = serviceToTApk,
            clientPath = clientToTPath.get(),
            servicePath = serviceToTPath.get(),
            xmlOutputFile = clientToTServiceToT,
            jsonClientOutputFile = jsonClientToTServiceToTClientTests,
            jsonServiceOutputFile = jsonClientToTServiceToTServiceTests,
            isClientPrevious = false,
            isServicePrevious = false
        )
        writeConfigFileContent(
            clientApk = clientToTApk,
            serviceApk = servicePreviousApk,
            clientPath = clientToTPath.get(),
            servicePath = servicePreviousPath.get(),
            xmlOutputFile = clientToTServicePrevious,
            jsonClientOutputFile = jsonClientToTServicePreviousClientTests,
            jsonServiceOutputFile = jsonClientToTServicePreviousServiceTests,
            isClientPrevious = false,
            isServicePrevious = true
        )
        writeConfigFileContent(
            clientApk = clientPreviousApk,
            serviceApk = serviceToTApk,
            clientPath = clientPreviousPath.get(),
            servicePath = serviceToTPath.get(),
            xmlOutputFile = clientPreviousServiceToT,
            jsonClientOutputFile = jsonClientPreviousServiceToTClientTests,
            jsonServiceOutputFile = jsonClientPreviousServiceToTServiceTests,
            isClientPrevious = true,
            isServicePrevious = false
        )
        // write constrained configs as well
        writeConfigFileContent(
            clientApk = clientToTApk,
            serviceApk = serviceToTApk,
            clientPath = clientToTPath.get(),
            servicePath = serviceToTPath.get(),
            xmlOutputFile = constrainedClientToTServiceToT,
            jsonClientOutputFile = jsonClientToTServiceToTClientTests,
            jsonServiceOutputFile = jsonClientToTServiceToTServiceTests,
            isClientPrevious = false,
            isServicePrevious = false,
            isConstrained = true
        )
        writeConfigFileContent(
            clientApk = clientToTApk,
            serviceApk = servicePreviousApk,
            clientPath = clientToTPath.get(),
            servicePath = servicePreviousPath.get(),
            xmlOutputFile = constrainedClientToTServicePrevious,
            jsonClientOutputFile = jsonClientToTServicePreviousClientTests,
            jsonServiceOutputFile = jsonClientToTServicePreviousServiceTests,
            isClientPrevious = false,
            isServicePrevious = true,
            isConstrained = true
        )
        writeConfigFileContent(
            clientApk = clientPreviousApk,
            serviceApk = serviceToTApk,
            clientPath = clientPreviousPath.get(),
            servicePath = serviceToTPath.get(),
            xmlOutputFile = constrainedClientPreviousServiceToT,
            jsonClientOutputFile = jsonClientPreviousServiceToTClientTests,
            jsonServiceOutputFile = jsonClientPreviousServiceToTServiceTests,
            isClientPrevious = true,
            isServicePrevious = false,
            isConstrained = true
        )
    }

    private fun resolveApk(
        apkFolder: DirectoryProperty,
        apkLoader: Property<BuiltArtifactsLoader>
    ): BuiltArtifacts {
        return apkLoader.get().load(apkFolder.get())
            ?: throw RuntimeException("Cannot load required APK for task: $name")
    }

    private fun BuiltArtifact.resolveName(path: String): String {
        return outputFile.substringAfterLast("/").renameApkForTesting(path)
    }

    private fun writeConfigFileContent(
        clientApk: BuiltArtifacts,
        serviceApk: BuiltArtifacts,
        clientPath: String,
        servicePath: String,
        xmlOutputFile: RegularFileProperty,
        jsonClientOutputFile: RegularFileProperty,
        jsonServiceOutputFile: RegularFileProperty,
        isClientPrevious: Boolean,
        isServicePrevious: Boolean,
        isConstrained: Boolean = false
    ) {
        val configBuilder = MediaConfigBuilder()
        configBuilder.configName(xmlOutputFile.asFile.get().name)
        val clientBuiltArtifact = clientApk.elements.single()
        val serviceBuiltArtifact = serviceApk.elements.single()
        val clientApkName = clientBuiltArtifact.resolveName(clientPath)
        val serviceApkName = serviceBuiltArtifact.resolveName(servicePath)
        configBuilder.clientApkName(clientApkName)
            .clientApkSha256(sha256(File(clientBuiltArtifact.outputFile)))
            .clientApplicationId(clientApk.applicationId)
            .serviceApkName(serviceApkName)
            .serviceApkSha256(sha256(File(serviceBuiltArtifact.outputFile)))
            .serviceApplicationId(serviceApk.applicationId)
            .minSdk(minSdk.get().toString())
            .testRunner(testRunner.get())
            .isClientPrevious(isClientPrevious)
            .isServicePrevious(isServicePrevious)
            .tag("androidx_unit_tests")
            .tag("media_compat")
        when (affectedModuleDetectorSubset.get()) {
            ProjectSubset.DEPENDENT_PROJECTS -> {
                if (isConstrained) {
                    configBuilder.runAllTests(false)
                } else {
                    configBuilder.runAllTests(true)
                }
            }
            ProjectSubset.NONE -> {
                if (presubmit.get()) {
                    configBuilder.runAllTests(false)
                } else {
                    configBuilder.runAllTests(true)
                }
            }
            else -> {
                configBuilder.runAllTests(true)
            }
        }

        createOrFail(xmlOutputFile).writeText(configBuilder.build())
        if (!isConstrained) {
            createOrFail(jsonClientOutputFile).writeText(
                configBuilder.buildJson(forClient = true)
            )
            createOrFail(jsonServiceOutputFile).writeText(
                configBuilder.buildJson(forClient = false)
            )
        }
    }
}
