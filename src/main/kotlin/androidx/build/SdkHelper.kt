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

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import java.io.File
import java.util.Locale

/**
 * Writes the appropriate SDK path to local.properties file in specified location.
 */
fun Project.writeSdkPathToLocalPropertiesFile() {
    val sdkPath = project.getSdkPath()
    if (sdkPath.exists()) {
        // This must be the project's real root directory (ex. fw/support/ui) rather than the
        // canonical root obtained via getSupportRootFolder().
        val props = File(project.rootDir, "local.properties")
        // Gradle always separates directories with '/' regardless of the OS, so convert here.
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
        throw Exception("Unable to find SDK prebuilts at $sdkPath. If you are not using a " +
                "standard repo-based checkout, please follow the checkout instructions at " +
                "go/androidx-onboarding.")
    }
}

/**
 * Returns the root project's platform-specific SDK path as a file.
 */
fun Project.getSdkPath(): File {
    val sdkPath = androidxSdkPath()
    if (sdkPath != null) {
        return File(sdkPath)
    }

    val osName = System.getProperty("os.name").toLowerCase(Locale.US)
    val isMacOsX = osName.contains("mac os x") ||
            osName.contains("darwin") ||
            osName.contains("osx")
    val platform = if (isMacOsX) "darwin" else "linux"

    // By convention, the SDK prebuilts live under the root checkout directory.
    return File(project.getCheckoutRoot(), "prebuilts/fullsdk-$platform")
}

/**
 * Sets the path to the canonical root project directory, e.g. {@code frameworks/support}.
 */
fun Project.setSupportRootFolder(rootDir: File) {
    val extension = project.rootProject.property("ext") as ExtraPropertiesExtension
    return extension.set("supportRootFolder", rootDir)
}

/**
 * Returns the path to the canonical root project directory, e.g. {@code frameworks/support}.
 */
fun Project.getSupportRootFolder(): File {
    // When the `ui` project is merged, this can be simplified to `project.rootDir`.
    val extension = project.rootProject.property("ext") as ExtraPropertiesExtension
    return extension.get("supportRootFolder") as File
}

/**
 * Returns the path to the checkout's root directory, e.g. where {@code repo init} was run.
 * <p>
 * This method assumes that the canonical root project directory is {@code frameworks/support}.
 */
fun Project.getCheckoutRoot(): File {
    return project.getSupportRootFolder().parentFile.parentFile
}
