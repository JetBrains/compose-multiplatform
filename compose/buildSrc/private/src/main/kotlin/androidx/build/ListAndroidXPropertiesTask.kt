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

package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Lists recognized properties whose names start with "androidx"
 */
@DisableCachingByDefault(because = "Too many inputs to cache, and runs quickly anyway")
abstract class ListAndroidXPropertiesTask() : DefaultTask() {
    init {
        group = "Help"
        description = "Lists AndroidX-specific properties (specifiable via -Pandroidx.*)"
    }

    @TaskAction
    fun exec() {
        project.logger.lifecycle(ALL_ANDROIDX_PROPERTIES.joinToString("\n"))
        project.logger.lifecycle("See AndroidXGradleProperties.kt for more information")
    }
}
