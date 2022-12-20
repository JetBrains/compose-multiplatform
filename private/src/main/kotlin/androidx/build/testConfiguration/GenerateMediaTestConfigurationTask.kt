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

    /**
     * output file where we write the sha256 of each APK we refer in tests.
     */
    @get:OutputFile
    abstract val shaReportOutput: RegularFileProperty

    /**
     * output file where we write the sha256 of each APK we refer in constained tests.
     */
    @get:OutputFile
    abstract val constrainedShaReportOutput: RegularFileProperty

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
        val testApkSha256Report = TestApkSha256Report()
        writeConfigFileContent(
            testApkSha256Report, clientToTApk, serviceToTApk, clientToTPath.get(),
            serviceToTPath.get(), clientToTServiceToT, false, false
        )
        writeConfigFileContent(
            testApkSha256Report, clientToTApk, servicePreviousApk, clientToTPath.get(),
            servicePreviousPath.get(), clientToTServicePrevious, false, true
        )
        writeConfigFileContent(
            testApkSha256Report, clientPreviousApk, serviceToTApk, clientPreviousPath.get(),
            serviceToTPath.get(), clientPreviousServiceToT, true, false
        )
        // write constrained configs as well
        writeConfigFileContent(
            testApkSha256Report, clientToTApk, serviceToTApk, clientToTPath.get(),
            serviceToTPath.get(), constrainedClientToTServiceToT, false, false, true
        )
        writeConfigFileContent(
            testApkSha256Report, clientToTApk, servicePreviousApk, clientToTPath.get(),
            servicePreviousPath.get(), constrainedClientToTServicePrevious, false, true, true
        )
        writeConfigFileContent(
            testApkSha256Report, clientPreviousApk, serviceToTApk, clientPreviousPath.get(),
            serviceToTPath.get(), constrainedClientPreviousServiceToT, true, false, true
        )
        testApkSha256Report.writeToFile(
            shaReportOutput.get().asFile
        )
        testApkSha256Report.writeToFile(
            constrainedShaReportOutput.get().asFile
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
        return outputFile.substringAfterLast("/")
            .renameApkForTesting(path, false)
    }

    private fun writeConfigFileContent(
        testApkSha256Report: TestApkSha256Report,
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
        val clientBuiltArtifact = clientApk.elements.single()
        val serviceBuiltArtifact = serviceApk.elements.single()
        val clientApkName = clientBuiltArtifact.resolveName(clientPath)
        val serviceApkName = serviceBuiltArtifact.resolveName(servicePath)
        testApkSha256Report.addFile(
            name = clientApkName,
            builtArtifact = clientBuiltArtifact
        )
        testApkSha256Report.addFile(
            name = serviceApkName,
            builtArtifact = serviceBuiltArtifact
        )
        configBuilder.clientApkName(clientApkName)
            .clientApplicationId(clientApk.applicationId)
            .serviceApkName(serviceApkName)
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
