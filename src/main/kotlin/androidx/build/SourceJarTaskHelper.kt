/*
 * Copyright 2017 The Android Open Source Project
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
import com.android.builder.core.BuilderConstants

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar

/**
 * Sets up a source jar task for an Android library project.
 */
fun setUpSoureJarTaskForAndroidProject(project: Project, extension: LibraryExtension) {
    // Create sources jar for release builds
    extension.libraryVariants.all { libraryVariant ->
        if (libraryVariant.buildType.name != BuilderConstants.RELEASE) {
            return@all // Skip non-release builds.
        }

        val sourceJar = project.tasks.create("sourceJarRelease", Jar::class.java)
        sourceJar.isPreserveFileTimestamps = false
        sourceJar.classifier = "sources"
        sourceJar.from(extension.sourceSets.findByName("main")!!.java.srcDirs)
        project.artifacts.add("archives", sourceJar)
    }
}

/**
 * Sets up a source jar task for a Java library project.
 */
fun setUpSourceJarTaskForJavaProject(project: Project) {
    val sourceJar = project.tasks.create("sourceJar", Jar::class.java)
    sourceJar.isPreserveFileTimestamps = false
    sourceJar.classifier = "sources"
    val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
    sourceJar.from(convention.sourceSets.findByName("main")!!.allSource.srcDirs)
    project.artifacts.add("archives", sourceJar)
}
