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

import java.io.File

/**
 * Writes the appropriate SDK path to local.properties file.
 */
fun setSdkInLocalPropertiesFile(supportRoot: File) {
    setSdkInLocalPropertiesFile(supportRoot, supportRoot)
}

/**
 * Writes the appropriate SDK path to local.properties file in specified location.
 */
fun setSdkInLocalPropertiesFile(supportRoot: File, propertiesFile: File) {
    val sdkPath = getSdkPath(supportRoot)
    if (sdkPath.exists()) {
        val props = File(propertiesFile, "local.properties")
        // gradle always deliminate directories with '/' regardless of the OS.
        // So convert deliminator here.
        val gradlePath = sdkPath.absolutePath.replace(File.separator, "/")
        var expectedContents = "sdk.dir=$gradlePath"
        expectedContents += "\ncmake.dir=$gradlePath/cmake"
        expectedContents += "\nndk.dir=$gradlePath/ndk"
        if (!props.exists() || props.readText(Charsets.UTF_8).trim() != expectedContents) {
            props.printWriter().use { out ->
                out.println(expectedContents)
            }
            println("updated local.properties")
        }
    } else {
        throw Exception("You are using non androidx-master-dev checkout. You need to check out " +
                "androidx-master-dev to work on support library. See go/androidx for details.")
    }
}

/**
 * Returns the appropriate SDK path.
 */
fun getSdkPath(supportRoot: File): File {
    val osName = System.getProperty("os.name").toLowerCase()
    val isMacOsX = osName.contains("mac os x") || osName.contains("darwin") ||
            osName.contains("osx")
    val platform = if (isMacOsX) "darwin" else "linux"
    // Making an assumption that prebuilts directory is in $supportRoot/../../prebuilts/
    return File(supportRoot.parentFile.parentFile, "prebuilts/fullsdk-$platform")
}