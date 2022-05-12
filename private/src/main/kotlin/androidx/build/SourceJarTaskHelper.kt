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
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import java.util.Locale

/**
 * Sets up a source jar task for an Android library project.
 */
fun Project.configureSourceJarForAndroid(extension: LibraryExtension) {
    extension.defaultPublishVariant { variant ->
        val sourceJar = tasks.register(
            "sourceJar${variant.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }}",
            Jar::class.java
        ) {
            it.archiveClassifier.set("sources")
            it.from(extension.sourceSets.getByName("main").java.srcDirs)
            // Do not allow source files with duplicate names, information would be lost otherwise.
            it.duplicatesStrategy = DuplicatesStrategy.FAIL
        }
        registerSourcesVariant(sourceJar)
    }
    project.afterEvaluate {
        // we can only tell if a project is multiplatform after it is configured
        if (it.multiplatformExtension != null && it.extra.has("publish")) {
            extension.defaultPublishVariant { variant ->
                val kotlinExt = project.extensions.getByName("kotlin") as KotlinProjectExtension
                val sourceJar =
                    project.tasks.named(
                        "sourceJar${variant.name.replaceFirstChar {
                            if (it.isLowerCase()) {
                                it.titlecase(Locale.getDefault())
                            } else it.toString()
                        }}",
                        Jar::class.java
                    )
                // multiplatform projects use different source sets, so we need to modify the task
                sourceJar.configure { sourceJarTask ->
                    // use an inclusion list of source sets, because that is the preferred policy
                    sourceJarTask.from(kotlinExt.sourceSets.getByName("commonMain").kotlin.srcDirs)
                    sourceJarTask.from(kotlinExt.sourceSets.getByName("androidMain").kotlin.srcDirs)
                }
            }
        }
    }
}

/**
 * Sets up a source jar task for a Java library project.
 */
fun Project.configureSourceJarForJava() {
    val sourceJar = tasks.register("sourceJar", Jar::class.java) { task ->
        task.archiveClassifier.set("sources")

        // Do not allow source files with duplicate names, information would be lost otherwise.
        // Different sourceSets in KMP should use different platform infixes, see b/203764756
        task.duplicatesStrategy = DuplicatesStrategy.FAIL

        extensions.findByType(JavaPluginExtension::class.java)?.let { extension ->
            // Since KotlinPlugin applies JavaPlugin, it's possible for JavaPlugin to exist, but
            // not to have "main".  Eventually, we should stop expecting to grab sourceSets by name
            // (b/235828421)
            extension.sourceSets.findByName("main")?.let {
                task.from(it.allSource.srcDirs)
            }
        }

        extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { extension ->
            for (sourceSetName in listOf("commonMain", "jvmMain")) {
                extension.sourceSets.findByName(sourceSetName)?.let { sourceSet ->
                    task.from(sourceSet.kotlin.srcDirs)
                }
            }
        }
    }
    registerSourcesVariant(sourceJar)
}

private fun Project.registerSourcesVariant(sourceJar: TaskProvider<Jar>) {
    configurations.create("sourcesElements") { gradleVariant ->
        gradleVariant.isVisible = false
        gradleVariant.isCanBeResolved = false
        gradleVariant.attributes.attribute(
            Usage.USAGE_ATTRIBUTE,
            objects.named<Usage>(Usage.JAVA_RUNTIME)
        )
        gradleVariant.attributes.attribute(
            Category.CATEGORY_ATTRIBUTE,
            objects.named<Category>(Category.DOCUMENTATION)
        )
        gradleVariant.attributes.attribute(
            Bundling.BUNDLING_ATTRIBUTE,
            objects.named<Bundling>(Bundling.EXTERNAL)
        )
        gradleVariant.attributes.attribute(
            DocsType.DOCS_TYPE_ATTRIBUTE,
            objects.named<DocsType>(DocsType.SOURCES)
        )
        gradleVariant.outgoing.artifact(sourceJar)

        registerAsComponentForPublishing(gradleVariant)
    }
}

private fun Project.registerAsComponentForPublishing(gradleVariant: Configuration) {
    // Android Library project 'release' component
    val release = components.findByName("release")
    if (release is AdhocComponentWithVariants) {
        release.addVariantsFromConfiguration(gradleVariant) { }
    }
    // Java Library project 'java' component
    val javaComponent = components.findByName("java")
    if (javaComponent is AdhocComponentWithVariants) {
        javaComponent.addVariantsFromConfiguration(gradleVariant) { }
    }
}
