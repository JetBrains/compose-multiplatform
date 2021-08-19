/*
 * Copyright 2021 The Android Open Source Project
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

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This plugin needs to be applied to the root of an AndroidX build
 *
 * The actual implementation is in AndroidXRootImplPlugin.
 * This extracts this logic out of the classpath so that individual tasks can't access this logic
 * so Gradle can know that changes to this logic doesn't need to automatically invalidate every task
 */
abstract class AndroidXRootPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.apply(mapOf<String, String>("from" to "${project.getSupportRootFolder()}/buildSrc/apply/applyAndroidXRootImplPlugin.gradle"))
    }
}
