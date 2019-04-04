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

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ComponentModuleMetadataDetails
import org.gradle.kotlin.dsl.apply

/**
 * Support library specific com.android.library plugin that sets common configurations needed for
 * support library modules.
 */
class SupportAndroidLibraryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<AndroidXPlugin>()

        val androidXExtension = project.extensions.getByType(AndroidXExtension::class.java)

        // Workaround for concurrentfuture
        project.dependencies.modules.module("com.google.guava:listenablefuture") {
            (it as ComponentModuleMetadataDetails).replacedBy(
                "com.google.guava:guava", "guava contains listenablefuture")
        }

        project.afterEvaluate {
            // workaround for b/120487939
            project.configurations.all {
                // Gradle seems to crash an androidtest configurations preferring project modules...
                if (!it.name.toLowerCase().contains("androidtest")) {
                    it.resolutionStrategy.preferProjectModules()
                }
            }
            val library = project.extensions.findByType(LibraryExtension::class.java)
                    ?: return@afterEvaluate

            if (androidXExtension.compilationTarget != CompilationTarget.DEVICE) {
                throw IllegalStateException(
                        "Android libraries must use a compilation target of DEVICE")
            }

            library.libraryVariants.all { libraryVariant ->
                if (libraryVariant.getBuildType().getName().equals("debug")) {
                    libraryVariant.javaCompileProvider.configure { javaCompile ->
                        if (androidXExtension.failOnUncheckedWarnings) {
                            javaCompile.options.compilerArgs.add("-Xlint:unchecked")
                        }
                        if (androidXExtension.failOnDeprecationWarnings) {
                            javaCompile.options.compilerArgs.add("-Xlint:deprecation")
                        }
                    }
                }
            }
        }

        project.apply<LibraryPlugin>()
    }
}
