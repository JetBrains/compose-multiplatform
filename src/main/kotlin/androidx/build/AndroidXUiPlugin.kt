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

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Plugin to apply options across all of the androidx.ui projects
 */
class AndroidXUiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.all {
            when (it) {
                is LibraryPlugin -> {
                    val library = project.extensions.findByType(LibraryExtension::class.java)
                        ?: throw Exception("Failed to find Android extension")

                    library.defaultConfig.apply {
                        minSdkVersion(21)
                        targetSdkVersion(29)
                    }

                    library.lintOptions.apply {
                        // Too many Kotlin features require synthetic accessors - we want to rely on R8 to
                        // remove these accessors
                        disable("SyntheticAccessor")
                    }
                }
                is KotlinBasePluginWrapper -> {
                    val conf = project.configurations.create("kotlinPlugin")

                    project.tasks.withType(KotlinCompile::class.java).configureEach { compile ->
                        compile.dependsOn(conf)
                        compile.doFirst {
                            if (!conf.isEmpty) {
                                compile.kotlinOptions.freeCompilerArgs +=
                                    "-Xplugin=${conf.files.first()}"
                            }
                        }
                    }
                }
            }
        }
    }
}
