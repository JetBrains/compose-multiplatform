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

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

const val BUILD_ON_SERVER_TASK = "buildOnServer"

/**
 * Configures the root project's buildOnServer task to run the specified task.
 */
fun <T : Task> Project.addToBuildOnServer(taskProvider: TaskProvider<T>) {
    rootProject.tasks.named(BUILD_ON_SERVER_TASK).configure {
        it.dependsOn(taskProvider)
    }
}

/**
 * Configures the root project's buildOnServer task to run the specified task.
 */
fun <T : Task> Project.addToBuildOnServer(taskPath: String) {
    rootProject.tasks.named(BUILD_ON_SERVER_TASK).configure {
        it.dependsOn(taskPath)
    }
}
