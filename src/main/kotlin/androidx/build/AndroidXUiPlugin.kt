/*
 * Copyright 2019 The Android Open Source Project
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

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin to apply options across all of the androidx.ui projects
 */
class AndroidXUiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val library = project.extensions.findByType(LibraryExtension::class.java)
            ?: throw Exception("Failed to find Android extension")

        library.defaultConfig.apply {
            minSdkVersion(21)
        }

        library.lintOptions.apply {
            // Too many Kotlin features require synthetic accessors - we want to rely on R8 to
            // remove these accessors
            disable("SyntheticAccessor")
        }
    }
}