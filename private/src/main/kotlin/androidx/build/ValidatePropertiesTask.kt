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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Validates that all properties of the project can be computed successfully.
 * This is similar to the built-in ":properties" task of type PropertyReportTask within
 * Gradle itself, but does not output them to stdout.
 * In practice, this should ensure that Android Studio sync is able to succeed
 */
@DisableCachingByDefault(because = "Too many inputs to be feasible to cache, and also runs quickly")
abstract class ValidatePropertiesTask : DefaultTask() {
    @TaskAction
    fun exec() {
        for (entry in project.properties.toMap()) {
            if (entry.key != "properties") {
                // query the entry's value and discard it
                entry.value.toString()
            }
        }
    }
}
