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

import androidx.build.license.CheckExternalDependencyLicensesTask
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar

class SupportKotlinLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val supportLibraryExtension = project.extensions.create("supportLibrary",
                SupportLibraryExtension::class.java, project)
        apply(project, supportLibraryExtension)
        project.apply(mapOf("plugin" to "kotlin"))
        project.apply(mapOf("plugin" to "kotlin-kapt"))

        project.afterEvaluate {
            val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
            if (supportLibraryExtension.java8Library) {
                convention.sourceCompatibility = JavaVersion.VERSION_1_8
                convention.targetCompatibility = JavaVersion.VERSION_1_8
            } else {
                convention.sourceCompatibility = JavaVersion.VERSION_1_7
                convention.targetCompatibility = JavaVersion.VERSION_1_7
            }

            project.tasks.withType(Jar::class.java) { jarTask ->
                jarTask.setReproducibleFileOrder(true)
                jarTask.setPreserveFileTimestamps(false)
            }

        }

        CheckExternalDependencyLicensesTask.configure(project)
    }
}
