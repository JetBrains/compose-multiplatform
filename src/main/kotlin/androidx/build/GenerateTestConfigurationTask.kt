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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

const val CONFIG_DIRECTORY = "test_xml_configs"
private const val APK_FILE_EXTENSION = ".apk"
private const val CONFIG_FILE_EXTENSION = ".xml"

private const val TEMPLATE = """<?xml version="1.0" encoding="utf-8"?> 
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
        <option name="test-suite-tag" value="androidx_unit_tests" />
        <option name="wifi:disable" value="true" />
        <include name="google/unbundled/common/setup" />
        <target_preparer class="com.android.tradefed.targetprep.suite.SuiteApkInstaller">
        <option name="cleanup-apks" value="true" />
        <option name="test-file-name" value="TEST_FILE_NAME" />
        </target_preparer>
        <test class="com.android.tradefed.testtype.AndroidJUnitTest">
        <option name="runner" value="androidx.test.runner.AndroidJUnitRunner"/>
        <option name="package" value="APPLICATION_ID" />
        <option name="size" value="small"/>
        </test>
        </configuration>"""

/**
Writes a set of AndroidTest.xml configuration files that gets zipped
alongside the APKs to be tested.

The configs will
 */
abstract class GenerateTestConfigurationTask : DefaultTask() {

    @get:OutputFiles
    val outputXmlFiles: MutableCollection<File> by lazy {
        computeOutputs()
    }

    @get:Internal
    /**
     * Map of the apk to the application id
     */
    val apkPackageMap = hashMapOf<File, String>()

    @Input
    fun getInputPaths(): Collection<String> {
        return apkPackageMap.keys.toList().map { f -> f.name }
    }

    @get:Internal
    /**
     * Maps the output file to the input file
     */
    val fileMap = hashMapOf<File, File>()

    private fun mapOutputsToInputs() {
        val distDir = project.getDistributionDirectory()
        val configDir = File(distDir.canonicalPath, CONFIG_DIRECTORY)
        val configFiles = mutableListOf<File>()
        apkPackageMap.keys.forEach { apkFile ->
            val configFile = File(
                configDir,
                apkFile.name.replace(APK_FILE_EXTENSION, CONFIG_FILE_EXTENSION)
            )
            configFiles.add(configFile)
            fileMap[configFile] = apkFile
        }
    }

    private fun computeOutputs(): MutableCollection<File> {
        mapOutputsToInputs()
        return fileMap.keys
    }

    @TaskAction
    fun generateAndroidTestZip() {
        writeConfigFileContent()
    }

    private fun writeConfigFileContent() {
        if (fileMap.keys.size == 0) {
            mapOutputsToInputs()
        }
        fileMap.forEach { (configFile, apkFile) ->
            val configContent = TEMPLATE.replace("TEST_FILE_NAME", apkFile.name)
                .replace("APPLICATION_ID", apkPackageMap[apkFile]!!)
            configFile.writeText(configContent)
        }
    }
}
