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

package androidx.build.studio

import androidx.build.SupportConfig
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

/**
 * Task responsible for updating / installing the Studio version used in the current root project,
 * and launching it.
 */
abstract class StudioTask : DefaultTask() {

    // TODO: support -y and --update-only options? Can use @Option for this
    @TaskAction
    fun studiow() {
        StudioWrapper.create(project).run {
            update()
            launch(services)
        }
    }

    companion object {
        private const val STUDIO_TASK = "studio"

        fun Project.registerStudioTask() {
            tasks.register(STUDIO_TASK, StudioTask::class.java) {
                if (SupportConfig.isUiProject()) {
                    // Need to prepare the sandbox before we can run studio
                    it.dependsOn(":compose:compose-ide-plugin:prepareSandbox")
                }
            }
        }
    }
}
