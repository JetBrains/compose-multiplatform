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
    const val INSTRUMENTATION_RUNNER = "android.support.test.runner.AndroidJUnitRunner"
    const val BUILD_TOOLS_VERSION = "27.0.3"
    const val CURRENT_SDK_VERSION = 28

    fun getKeystore(project: Project): File {
        val supportRoot = (project.rootProject.property("ext") as ExtraPropertiesExtension)
                .get("supportRootFolder") as File
        return File(supportRoot, "development/keystore/debug.keystore")
    }

    fun getLintBaseline(project: Project): File {
        return File(project.projectDir, "/lint-baseline.xml")
    }
}
