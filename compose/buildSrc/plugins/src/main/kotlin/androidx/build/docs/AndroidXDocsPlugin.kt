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

package androidx.build.docs

import androidx.build.getSupportRootFolder
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin that allows to build documentation for a given set of prebuilt and tip of tree projects.
 *
 * The actual implementation is in AndroidXDocsImplPlugin.
 * This extracts this logic out of the classpath so that individual tasks can't access this logic
 * so Gradle can know that changes to this logic doesn't need to automatically invalidate every task
 */
class AndroidXDocsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val supportRoot = project.getSupportRootFolder()
        project.apply(
            mapOf<String, String>(
                "from" to "$supportRoot/buildSrc/apply/applyAndroidXDocsImplPlugin.gradle"
            )
        )
    }
}
