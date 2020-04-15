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
    const val BUILD_TOOLS_VERSION = "29.0.3"

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
    const val TARGET_SDK_VERSION = 29

    @JvmStatic
    fun isUiProject() = System.getenv("DIST_SUBDIR") == "/ui"

    @JvmStatic
    fun getJavaToolsJarPath() = System.getenv("JAVA_TOOLS_JAR")
}

fun Project.getExternalProjectPath(): File {
    return File(project.getCheckoutRoot(), "external")
}

fun Project.getKeystore(): File {
    return File(project.getSupportRootFolder(), "development/keystore/debug.keystore")
}

fun Project.getPrebuiltsRoot(): File {
    val ext = project.rootProject.property("ext") as ExtraPropertiesExtension
    val reposProperties = ext.get("repos") as Map<*, *>
    return File(reposProperties["prebuiltsRoot"].toString())
}
