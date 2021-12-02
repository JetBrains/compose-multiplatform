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

import org.gradle.api.Project

enum class StudioType {
    ANDROIDX,
    PLAYGROUND;

    companion object {
        /**
         * Returns the Studio type for the project's studio task
         */
        @JvmStatic
        fun from(project: Project): StudioType {
            val value = project.findProperty(STUDIO_TYPE)?.toString()
            return when (value) {
                "playground" -> StudioType.PLAYGROUND
                null, "androidx" -> StudioType.ANDROIDX
                else -> error("Invalid project type $value")
            }
        }

        /**
         * @return `true` if running in a Playground (Github) setup, `false` otherwise.
         */
        @JvmStatic
        fun isPlayground(project: Project): Boolean {
            return StudioType.from(project) == StudioType.PLAYGROUND
        }
    }
}
