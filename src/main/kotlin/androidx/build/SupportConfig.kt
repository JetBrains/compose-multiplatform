/*
 * Copyright 2017 The Android Open Source Project
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

object SupportConfig {
    const val DEFAULT_MIN_SDK_VERSION = 14
    const val INSTRUMENTATION_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
    const val BENCHMARK_INSTRUMENTATION_RUNNER = "androidx.benchmark.AndroidBenchmarkRunner"
    const val BUILD_TOOLS_VERSION = "28.0.3"

    /**
     * The Android SDK version to use for compilation.
     * <p>
     * Either an integer value or a pre-release platform code, prefixed with "android-" (ex.
     * "android-28" or "android-Q") as you would see within the SDK's platforms directory.
     */
    const val COMPILE_SDK_VERSION = "android-29"

    /**
     * The Android SDK version to use for targetSdkVersion meta-data.
     * <p>
     * Either an integer value (ex. 28), a pre-release platform code (ex. "Q") as you would see
     * within the SDK's platforms directory as android-<version>, or a released platform version
     * code as you would see within Build.VERSIONS.VERSION_CODE (ex. "HONEYCOMB" or "P").
     * <p>
     * <strong>Note:</strong> This must be set to an integer value or released platform version in
     * order for tests to run on devices running released versions of the Android OS. If this is
     * set to a pre-release version, tests will only be able to run on pre-release devices.
     */
    const val TARGET_SDK_VERSION = 28

    @JvmStatic
    fun getKeystore(project: Project): File {
        val supportRoot = (project.rootProject.property("ext") as ExtraPropertiesExtension)
                .get("supportRootFolder") as File
        return File(supportRoot, "development/keystore/debug.keystore")
    }

    @JvmStatic
    fun getSupportRoot(project: Project): File {
        val extension = (project.rootProject.property("ext") as ExtraPropertiesExtension)
        val file = extension.get("supportRootFolder") as File
        return file
    }

    @JvmStatic
    fun getPrebuiltsRootPath(project: Project): String {
        val reposProperties = (project.rootProject.property("ext") as ExtraPropertiesExtension)
            .get("repos") as Map<*, *>
        return reposProperties["prebuiltsRoot"].toString()
    }

    @JvmStatic
    fun getSupportRepoPath(project: Project): String {
        return project.getRepositoryDirectory().absolutePath
    }

    @JvmStatic
    fun getAGPVersion(project: Project): String {
        val studioProperties = (project.rootProject.property("ext") as ExtraPropertiesExtension)
            .let { it.get("build_versions") as Map<*, *> }
            .let { it["studio"] as Map<*, *> }
        return studioProperties["agp"].toString()
    }
}
