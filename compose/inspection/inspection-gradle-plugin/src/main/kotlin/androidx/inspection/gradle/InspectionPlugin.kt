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

package androidx.inspection.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.LibraryExtension
import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.ProtobufPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.StopExecutionException
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension

/**
 * A plugin which, when present, ensures that intermediate inspector
 * resources are generated at build time
 */
@Suppress("SyntheticAccessor")
class InspectionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        var foundLibraryPlugin = false
        var foundReleaseVariant = false
        val extension = project.extensions.create<InspectionExtension>(EXTENSION_NAME, project)

        val publishInspector = project.configurations.create("publishInspector") {
            it.isCanBeConsumed = true
            it.isCanBeResolved = false
            it.setupInspectorAttribute()
        }

        val publishNonDexedInspector = project.configurations.create("publishNonDexedInspector") {
            it.isCanBeConsumed = true
            it.isCanBeResolved = false
            it.setupNonDexedInspectorAttribute()
        }

        project.pluginManager.withPlugin("com.android.library") {
            foundLibraryPlugin = true
            val libExtension = project.extensions.getByType(LibraryExtension::class.java)
            includeMetaInfServices(libExtension)
            val componentsExtension =
                project.extensions.findByType(AndroidComponentsExtension::class.java)
                    ?: throw GradleException("android plugin must be used")
            componentsExtension.onVariants { variant: Variant ->
                if (variant.name == "release") {
                    foundReleaseVariant = true
                    val unzip = project.registerUnzipTask(variant)
                    val shadowJar = project.registerShadowDependenciesTask(
                        variant, extension.name, unzip
                    )
                    val bundleTask = project.registerBundleInspectorTask(
                        variant, libExtension, extension.name, shadowJar
                    )

                    publishNonDexedInspector.outgoing.variants {
                        val configVariant = it.create("inspectorNonDexedJar")
                        configVariant.artifact(shadowJar)
                    }

                    publishInspector.outgoing.variants {
                        val configVariant = it.create("inspectorJar")
                        configVariant.artifact(bundleTask)
                    }
                }
            }
            libExtension.sourceSets.findByName("main")!!.resources.srcDirs(
                File(project.rootDir, "src/main/proto")
            )
        }

        project.apply(plugin = "com.google.protobuf")
        project.plugins.all {
            if (it is ProtobufPlugin) {
                val protobufExtension = project.extensions.getByType(ProtobufExtension::class.java)
                protobufExtension.apply {
                    protoc {
                        it.artifact = project.getLibraryByName("protobufCompiler").toString()
                    }
                    generateProtoTasks {
                        it.all().forEach { task: GenerateProtoTask ->
                            task.builtins.create("java") { options ->
                                options.option("lite")
                            }
                        }
                    }
                }
            }
        }

        project.dependencies {
            add("implementation", project.getLibraryByName("protobufLite"))
        }

        project.afterEvaluate {
            if (!foundLibraryPlugin) {
                throw StopExecutionException(
                    """A required plugin, com.android.library, was not found.
                        The androidx.inspection plugin currently only supports android library
                        modules, so ensure that com.android.library is applied in the project
                        build.gradle file."""
                        .trimIndent()
                )
            }
            if (!foundReleaseVariant) {
                throw StopExecutionException(
                    "The androidx.inspection plugin requires " +
                        "release build variant."
                )
            }
        }
    }
}

private fun Project.getLibraryByName(name: String): MinimalExternalModuleDependency {
    val libs = project.extensions.getByType(
        VersionCatalogsExtension::class.java
    ).find("libs").get()
    val library = libs.findLibrary(name)
    return if (library.isPresent) {
        library.get().get()
    } else {
        throw GradleException("Could not find a library for `$name`")
    }
}

private fun includeMetaInfServices(library: LibraryExtension) {
    library.sourceSets.getByName("main").resources.include("META-INF/services/*")
    library.sourceSets.getByName("main").resources.include("**/*.proto")
}

/**
 * Use this function in [libraryProject] to include inspector that will be compiled into
 * inspector.jar and packaged in the library's aar.
 *
 * @param libraryProject project that is inspected and which aar will host inspector.jar .
 * E.g. work-runtime
 * @param inspectorProjectPath project path of the inspector, that will be compiled into the
 * inspector.jar. E.g. :work:work-inspection
 */
@ExperimentalStdlibApi
fun packageInspector(libraryProject: Project, inspectorProjectPath: String) {
    val inspectorProject = libraryProject.rootProject.findProject(inspectorProjectPath)
    if (inspectorProject == null) {
        check(libraryProject.property("androidx.studio.type") == "playground") {
            "Cannot find $inspectorProjectPath. This is optional only for playground builds."
        }
        // skip setting up inspector project
        return
    }
    val consumeInspector = libraryProject.createConsumeInspectionConfiguration()

    libraryProject.dependencies {
        add(consumeInspector.name, inspectorProject)
    }
    val consumeInspectorFiles = libraryProject.files(consumeInspector)

    generateProguardDetectionFile(libraryProject)
    val libExtension = libraryProject.extensions.getByType(LibraryExtension::class.java)
    libExtension.libraryVariants.all { variant ->
        variant.packageLibraryProvider.configure { zip ->
            zip.from(consumeInspectorFiles)
            zip.rename {
                if (it == consumeInspectorFiles.asFileTree.singleFile.name) {
                    "inspector.jar"
                } else it
            }
        }
    }
}

fun Project.createConsumeInspectionConfiguration(): Configuration =
    configurations.create("consumeInspector") {
        it.setupInspectorAttribute()
    }

private fun Configuration.setupInspectorAttribute() {
    attributes {
        it.attribute(Attribute.of("inspector", String::class.java), "inspectorJar")
    }
}

fun Project.createConsumeNonDexedInspectionConfiguration(): Configuration =
    configurations.create("consumeNonDexedInspector") {
        it.setupNonDexedInspectorAttribute()
    }

private fun Configuration.setupNonDexedInspectorAttribute() {
    attributes {
        it.attribute(Attribute.of("inspector-undexed", String::class.java), "inspectorUndexedJar")
    }
}

@ExperimentalStdlibApi
private fun generateProguardDetectionFile(libraryProject: Project) {
    val libExtension = libraryProject.extensions.getByType(LibraryExtension::class.java)
    libExtension.libraryVariants.all { variant ->
        libraryProject.registerGenerateProguardDetectionFileTask(variant)
    }
}

const val EXTENSION_NAME = "inspection"

open class InspectionExtension(@Suppress("UNUSED_PARAMETER") project: Project) {
    /**
     * Name of built inspector artifact, if not provided it is equal to project's name.
     */
    var name: String? = null
}
