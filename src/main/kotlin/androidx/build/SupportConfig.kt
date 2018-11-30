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
    const val BUILD_TOOLS_VERSION = "28.0.3"

    /**
     * The Android SDK version to use for compilation.
     *
     * Either an integer value or a pre-release platform code, prefixed with "android-" (ex.
     * "android-28" or "android-Q") as you would see within the SDK's platforms directory.
     */
    const val COMPILE_SDK_VERSION = "android-28"

    /**
     * The Android SDK version to use for targetSdkVersion meta-data.
     *
     * Either an integer value (ex. 28), a pre-release platform code (ex. "Q") as you would see
     * within the SDK's platforms directory as android-<version>, or a released platform version
     * code as you would see within Build.VERSIONS.VERSION_CODE (ex. "HONEYCOMB" or "P").
     */
    const val TARGET_SDK_VERSION = "28"

    fun getKeystore(project: Project): File {
        val supportRoot = (project.rootProject.property("ext") as ExtraPropertiesExtension)
                .get("supportRootFolder") as File
        return File(supportRoot, "development/keystore/debug.keystore")
    }
}
