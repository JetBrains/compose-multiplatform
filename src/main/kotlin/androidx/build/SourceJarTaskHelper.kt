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
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.named

/**
 * Sets up a source jar task for an Android library project.
 */
fun Project.configureSourceJarForAndroid(extension: LibraryExtension) {
    extension.defaultPublishVariant { variant ->
        val sourceJar = tasks.register("sourceJar${variant.name.capitalize()}", Jar::class.java) {
            it.archiveClassifier.set("sources")
            it.from(extension.sourceSets.getByName("main").java.srcDirs)
        }
        registerSourcesVariant(sourceJar)
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
    registerSourcesVariant(sourceJar)
}

private fun Project.registerSourcesVariant(sourceJar: TaskProvider<Jar>) {
    configurations.create("sourcesElements") { gradleVariant ->
        gradleVariant.isVisible = false
        gradleVariant.isCanBeResolved = false
        gradleVariant.attributes.attribute(
            Usage.USAGE_ATTRIBUTE,
            objects.named(Usage.JAVA_RUNTIME)
        )
        gradleVariant.getAttributes().attribute(
            Category.CATEGORY_ATTRIBUTE,
            objects.named(Category.DOCUMENTATION)
        )
        gradleVariant.getAttributes().attribute(
            Bundling.BUNDLING_ATTRIBUTE,
            objects.named(Bundling.EXTERNAL)
        )
        gradleVariant.getAttributes().attribute(
            DocsType.DOCS_TYPE_ATTRIBUTE,
            objects.named(DocsType.SOURCES)
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