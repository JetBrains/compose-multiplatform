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

import androidx.build.dokka.kmpDocs.DokkaAnalysisPlatform
import com.android.build.gradle.LibraryExtension
import com.google.gson.GsonBuilder
import java.io.File
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
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import androidx.build.dokka.kmpDocs.docsPlatform
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

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
                task.from(it.allSource.sourceDirectories)
            }
        }

        extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { extension ->
            for (sourceSetName in listOf("commonMain", "jvmMain")) {
                extension.sourceSets.findByName(sourceSetName)?.let { sourceSet ->
                    task.from(sourceSet.kotlin.sourceDirectories)
                }
            }
        }
    }
    registerSourcesVariant(sourceJar)
}

fun Project.configureSourceJarForMultiplatform() {
    val extension = multiplatformExtension ?: throw GradleException(
        "Unable to find multiplatform extension while configuring multiplatform source JAR"
    )
    val metadataFile = buildDir.resolve(
        PROJECT_STRUCTURE_METADATA_FILEPATH
    )
    val multiplatformMetadataTask = tasks.register(
        "createMultiplatformMetadata",
        CreateMultiplatformMetadata::class.java
    ) {
        it.metadataFile = metadataFile
        it.sourceSetJson = createSourceSetMetadata(extension)
    }
    val sourceJar = tasks.register("multiplatformSourceJar", Jar::class.java) { task ->
        task.dependsOn(multiplatformMetadataTask)
        task.archiveClassifier.set("multiplatform-sources")

        // Do not allow source files with duplicate names, information would be lost otherwise.
        // Different sourceSets in KMP should use different platform infixes, see b/203764756
        task.duplicatesStrategy = DuplicatesStrategy.FAIL
        extension.targets.flatMap {
            it.mainCompilation().allKotlinSourceSets
        }.toSet().forEach { sourceSet ->
            task.from(sourceSet.kotlin.srcDirs) { copySpec ->
                copySpec.into(sourceSet.name)
            }
        }
        task.metaInf.from(metadataFile)
    }
    registerMultiplatformSourcesVariant(sourceJar)
}

internal val Project.multiplatformUsage
    get() = objects.named<Usage>("androidx-multiplatform-docs")

private fun Project.registerMultiplatformSourcesVariant(sourceJar: TaskProvider<Jar>) {
    registerSourcesVariant("androidxSourcesElements", sourceJar, multiplatformUsage)
}

private fun Project.registerSourcesVariant(sourceJar: TaskProvider<Jar>) {
    registerSourcesVariant("sourcesElements", sourceJar, objects.named(Usage.JAVA_RUNTIME))
}

private fun Project.registerSourcesVariant(
    configurationName: String,
    sourceJar: TaskProvider<Jar>,
    usage: Usage
) {
    configurations.create(configurationName) { gradleVariant ->
        gradleVariant.isVisible = false
        gradleVariant.isCanBeResolved = false
        gradleVariant.attributes.attribute(
            Usage.USAGE_ATTRIBUTE,
            usage
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

/**
 * Finds the main compilation for a source set, usually called 'main' but for android we need to
 * search for 'debug' instead.
 */
private fun KotlinTarget.mainCompilation() =
    compilations.findByName(MAIN_COMPILATION_NAME) ?: compilations.getByName("debug")

/**
 * Writes a metadata file to the given [metadataFile] location for all multiplatform Kotlin source
 * sets including their dependencies and analysisPlatform. This is consumed when we are reading
 * source JARs so that we can pass the correct inputs to Dackka.
 */
@CacheableTask
abstract class CreateMultiplatformMetadata : DefaultTask() {
    @Input
    lateinit var sourceSetJson: String

    @OutputFile
    lateinit var metadataFile: File

    @TaskAction
    fun execute() {
        metadataFile.apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(sourceSetJson)
        }
    }
}

fun createSourceSetMetadata(extension: KotlinMultiplatformExtension): String {
    val commonMain = extension.sourceSets.getByName("commonMain")
    val sourceSetsByName = mutableMapOf(
        "commonMain" to mapOf(
            "name" to commonMain.name,
            "dependencies" to commonMain.dependsOn.map { it.name },
            "analysisPlatform" to DokkaAnalysisPlatform.COMMON.jsonName
        )
    )
    extension.targets.forEach { target ->
        target.mainCompilation().allKotlinSourceSets.forEach {
            sourceSetsByName.getOrPut(it.name) {
                mapOf(
                    "name" to it.name,
                    "dependencies" to it.dependsOn.map { it.name },
                    "analysisPlatform" to target.docsPlatform().jsonName
                )
            }
        }
    }
    val sourceSetMetadata = mutableMapOf(
        "sourceSets" to sourceSetsByName.values
    )
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(sourceSetMetadata)
}

internal const val PROJECT_STRUCTURE_METADATA_FILENAME = "kotlin-project-structure-metadata.json"

private const val PROJECT_STRUCTURE_METADATA_FILEPATH =
    "project_structure_metadata/$PROJECT_STRUCTURE_METADATA_FILENAME"
