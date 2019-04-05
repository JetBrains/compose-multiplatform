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
import org.gradle.kotlin.dsl.getPlugin

/**
 * Sets up a source jar task for an Android library project.
 */
fun Project.configureSourceJarForAndroid(extension: LibraryExtension) {
    extension.libraryVariants.all { variant ->
        if (variant.buildType.name != BuilderConstants.RELEASE) {
            return@all // Skip non-release builds.
        }

        val sourceJar = tasks.register("sourceJar${variant.name.capitalize()}", Jar::class.java) {
            it.archiveClassifier.set("sources")
            it.from(extension.sourceSets.getByName("main").java.srcDirs)
        }
        artifacts.add("archives", sourceJar)
    }
}

/**
 * Sets up a source jar task for a Java library project.
 */
fun Project.configureSourceJarForJava() {
    val sourceJar = tasks.register("sourceJar", Jar::class.java) {
        it.archiveClassifier.set("sources")

        val convention = convention.getPlugin<JavaPluginConvention>()
        it.from(convention.sourceSets.getByName("main").allSource.srcDirs)
    }
    artifacts.add("archives", sourceJar)
}
