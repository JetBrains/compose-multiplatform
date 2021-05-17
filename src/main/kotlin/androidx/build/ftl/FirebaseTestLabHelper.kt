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

package androidx.build.ftl

import androidx.build.gradle.isRoot
import com.android.build.gradle.TestedExtension
import org.gradle.api.Project

/**
 * Helper class to setup Firebase Test Lab for instrumentation tests
 */
internal class FirebaseTestLabHelper(
    private val rootProject: Project
) {
    init {
        check(rootProject.isRoot) {
            "FTL Utilities can only be created for root projects"
        }
    }

    private val anchorTask by lazy {
        rootProject.tasks.register(ANCHOR_TASK_NAME) {
            it.description = "Anchor task that depends on all firebase test lab tests"
            it.group = "Verification"
        }
    }

    fun setupFTL(project: Project) {
        AGP_PLUGIN_IDS.forEach { agpPluginId ->
            // using base plugin at this stage does not work as base plugin is applied before the
            // Android Extension is created.
            // see the comment on [AGP_PLUGIN_IDS] for details.
            project.pluginManager.withPlugin(agpPluginId) {
                project.extensions.findByType(TestedExtension::class.java)?.let {
                    configure(project, it)
                }
            }
        }
    }

    private fun configure(project: Project, testedExtension: TestedExtension) {
        testedExtension.testVariants.all { testVariant ->
            RunTestOnFTLTask.create(project, testVariant)?.let { ftlTask ->
                anchorTask.configure { it.dependsOn(ftlTask) }
            }
        }
    }

    companion object {
        const val ANCHOR_TASK_NAME = "firebaseTestLabTests"

        /**
         * AGP base plugin is applied before the extension is created so instead we use plugin
         * ids here.
         * see: https://github.com/google/ksp/issues/314
         * see: https://github.com/google/ksp/pull/318
         */
        private val AGP_PLUGIN_IDS = listOf(
            "com.android.application",
            // TODO enable library and dynamic feature when we can synthesize
            //  an APK for them
            //  "com.android.library",
            //  "com.android.dynamic-feature"
        )
    }
}