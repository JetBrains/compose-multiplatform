/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.build.metalava.Metalava
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.extra

/**
 * Support java library specific plugin that sets common configurations needed for
 * support library modules.
 */
class SupportJavaLibraryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<AndroidXPlugin>()

        val androidXExtension = project.extensions.getByType(AndroidXExtension::class.java)

        project.apply(mapOf("plugin" to "java"))
        project.afterEvaluate {
            // workaround for b/120487939
            project.configurations.all {
                it.resolutionStrategy.preferProjectModules()
            }
            if (androidXExtension.publish) {
                project.extra.set("publish", true)
                project.addToProjectMap(androidXExtension.mavenGroup?.group)
            }
            if (androidXExtension.useMetalava) {
                Metalava.registerJavaProject(project, androidXExtension)
            } else {
                DiffAndDocs.get(project).registerJavaProject(project, androidXExtension)
            }
        }
    }
}
