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
            serviceToTPath.get(), clientToTServiceToT, false, false
        )
        writeConfigFileContent(
            clientToTApk, servicePreviousApk, clientToTPath.get(),
            servicePreviousPath.get(), clientToTServicePrevious, false, true
        )
        writeConfigFileContent(
            clientPreviousApk, serviceToTApk, clientPreviousPath.get(),
            serviceToTPath.get(), clientPreviousServiceToT, true, false
        )
        // write constrained configs as well
        writeConfigFileContent(
            clientToTApk, serviceToTApk, clientToTPath.get(),
            serviceToTPath.get(), constrainedClientToTServiceToT, false, false, true
        )
        writeConfigFileContent(
            clientToTApk, servicePreviousApk, clientToTPath.get(),
            servicePreviousPath.get(), constrainedClientToTServicePrevious, false, true, true
        )
        writeConfigFileContent(
            clientPreviousApk, serviceToTApk, clientPreviousPath.get(),
            serviceToTPath.get(), constrainedClientPreviousServiceToT, true, false, true
        )
    }

    private fun resolveApk(
        apkFolder: DirectoryProperty,
        apkLoader: Property<BuiltArtifactsLoader>
    ): BuiltArtifacts {
        return apkLoader.get().load(apkFolder.get())
            ?: throw RuntimeException("Cannot load required APK for task: $name")
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
        outputFile: RegularFileProperty,
        isClientPrevious: Boolean,
        isServicePrevious: Boolean,
        isConstrained: Boolean = false
    ) {
        val configBuilder = MediaConfigBuilder()
        configBuilder.clientApkName(resolveName(clientApk, clientPath))
            .clientApplicationId(clientApk.applicationId)
            .serviceApkName(resolveName(serviceApk, servicePath))
            .serviceApplicationId(serviceApk.applicationId)
            .minSdk(minSdk.get().toString())
            .testRunner(testRunner.get())
            .isClientPrevious(isClientPrevious)
            .isServicePrevious(isServicePrevious)
            .tag("androidx_unit_tests")
            .tag("media_compat")
        val isPresubmit = presubmit.get()
        configBuilder.isPostsubmit(!isPresubmit)
        when (affectedModuleDetectorSubset.get()) {
            ProjectSubset.DEPENDENT_PROJECTS -> {
                if (isConstrained) {
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
            else -> {
                configBuilder.runAllTests(true)
            }
        }

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
