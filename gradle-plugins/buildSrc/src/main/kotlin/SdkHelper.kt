/*
 * Copyright 2023 The Android Open Source Project
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

import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.util.*

/**
 * Writes the appropriate SDK path to local.properties file of android sample projects (e.g. for tests).
 */
fun Project.setAndroidSDKForTestProjects(projects: List<String>) {
    val sdkPath = project.getSdkPath()
    // Gradle always separates directories with '/' regardless of the OS, so convert here.
    val gradlePath = sdkPath.absolutePath.replace(File.separator, "/")
    if (sdkPath.exists()) {
        // This must be the project's real root directory (ex. fw/support/ui) rather than the
        // canonical root obtained via getSupportRootFolder().
        for (projectRawPath in projects) {
            val projectPath = File(project.rootDir, projectRawPath)
            val props = File(projectPath, "local.properties")
            var expectedContents = "sdk.dir=$gradlePath"
            expectedContents += "\ncmake.dir=$gradlePath/native-build-tools"
            if (!props.exists() || props.readText(Charsets.UTF_8).trim() != expectedContents) {
                props.printWriter().use { out ->
                    out.println(expectedContents)
                }
                println("updated local.properties")
            }
        }
    } else {
        throw Exception(
            "Unable to find SDK prebuilts at $sdkPath."
        )
    }
}

/**
 * Returns the root project's platform-specific SDK path as a file.
 */
fun Project.getSdkPath(): File {
    val os = getOperatingSystem()
    return if (os == OperatingSystem.WINDOWS) {
        getSdkPathFromEnvironmentVariable()
    } else {
        val platform = if (os == OperatingSystem.MAC) "darwin" else "linux"
        val folder = rootProject.projectDir.resolve("android-sdk/$platform")
        check(folder.exists()) {
            "Android SDK folder $folder doesn't exist. " +
                    "Call ./android-sdk/downloadAndroidSdk before opening the project"
        }
        folder
    }
}

private fun getSdkPathFromEnvironmentVariable(): File {
    // check for environment variables, in the order AGP checks
    listOf("ANDROID_HOME", "ANDROID_SDK_ROOT").forEach {
        val envValue = System.getenv(it)
        if (envValue != null) {
            val sdkDirectory = File(envValue)
            if (sdkDirectory.isDirectory) {
                return sdkDirectory
            }
        }
    }
    // only print the error for SDK ROOT since ANDROID_HOME is deprecated but we first check
    // it because it is prioritized according to the documentation
    throw GradleException("ANDROID_SDK_ROOT environment variable is not set")
}

enum class OperatingSystem {
    LINUX,
    WINDOWS,
    MAC
}

fun getOperatingSystem(): OperatingSystem {
    val os = System.getProperty("os.name").lowercase(Locale.US)
    return when {
        os.contains("mac os x") -> OperatingSystem.MAC
        os.contains("darwin") -> OperatingSystem.MAC
        os.contains("osx") -> OperatingSystem.MAC
        os.startsWith("win") -> OperatingSystem.WINDOWS
        os.startsWith("linux") -> OperatingSystem.LINUX
        else -> throw GradleException("Unsupported operating system $os")
    }
}