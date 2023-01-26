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
import org.gradle.kotlin.dsl.extra

/**
 * Setting this property enables multiplatform builds of Compose
 */
const val COMPOSE_MPP_ENABLED = "androidx.compose.multiplatformEnabled"

class Multiplatform {
    companion object {
        fun Project.isMultiplatformEnabled(): Boolean {
            return findProperty(COMPOSE_MPP_ENABLED)?.toString()?.toBoolean() ?: false
        }

        fun setEnabledForProject(project: Project, enabled: Boolean) {
            project.extra.set(COMPOSE_MPP_ENABLED, enabled)
        }

        /**
         * Returns true if kotlin native targets should be enabled.
         */
        @JvmStatic
        fun isKotlinNativeEnabled(project: Project): Boolean {
            return "KMP".equals(System.getenv()["ANDROIDX_PROJECTS"], ignoreCase = true) ||
                ProjectLayoutType.isPlayground(project) ||
                project.providers.gradleProperty("androidx.kmp.native.enabled")
                    .orNull?.toBoolean() == true
        }
    }
}
