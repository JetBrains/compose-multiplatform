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

import androidx.build.gradle.getByType
import androidx.build.license.configureExternalDependencyLicenseCheck
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.JavaVersion.VERSION_1_7
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.withType

/**
 * A plugin which enables all of the Gradle customizations for AndroidX.
 * This plugin reacts to other plugins being added and adds required and optional functionality.
 */
class AndroidXPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.all {
            when (it) {
                is JavaPlugin,
                is JavaLibraryPlugin -> {
                    project.configureErrorProneForJava()
                    project.configureSourceJarForJava()

                    project.convention.getPlugin<JavaPluginConvention>().apply {
                        sourceCompatibility = VERSION_1_7
                        targetCompatibility = VERSION_1_7
                    }
                }
                is LibraryPlugin -> {
                    val extension = project.extensions.getByType<LibraryExtension>()

                    project.configureErrorProneForAndroid(extension.libraryVariants)
                    project.configureSourceJarForAndroid(extension)

                    extension.compileOptions.apply {
                        setSourceCompatibility(VERSION_1_7)
                        setTargetCompatibility(VERSION_1_7)
                    }

                    project.afterEvaluate {
                        // Java 8 is only fully supported on API 24+ and not all Java 8 features are
                        // binary compatible with API < 24
                        val compilesAgainstJava8 =
                                extension.compileOptions.sourceCompatibility >= VERSION_1_8 ||
                                        extension.compileOptions.targetCompatibility >= VERSION_1_8
                        val minSdkLessThan24 = extension.defaultConfig.minSdkVersion.apiLevel < 24
                        if (compilesAgainstJava8 && minSdkLessThan24) {
                            throw IllegalArgumentException("Libraries can only support Java 8 if " +
                                    "minSdkVersion is 24 or higher")
                        }
                    }
                }
                is AppPlugin -> {
                    val extension = project.extensions.getByType<AppExtension>()
                    project.configureErrorProneForAndroid(extension.applicationVariants)
                }
            }
        }

        project.configureExternalDependencyLicenseCheck()

        // Disable timestamps and ensure filesystem-independent archive ordering to maximize
        // cross-machine byte-for-byte reproducibility of artifacts.
        project.tasks.withType<Jar> {
            isReproducibleFileOrder = true
            isPreserveFileTimestamps = false
        }
    }
}
