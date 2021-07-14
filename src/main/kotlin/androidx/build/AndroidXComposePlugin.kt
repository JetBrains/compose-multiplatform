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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestedExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

const val composeSourceOption =
    "plugin:androidx.compose.compiler.plugins.kotlin:sourceInformation=true"

/**
 * Plugin to apply common configuration for Compose projects.
 */
class AndroidXComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.all { plugin ->
            when (plugin) {
                is LibraryPlugin -> {
                    val library = project.extensions.findByType(LibraryExtension::class.java)
                        ?: throw Exception("Failed to find Android extension")

                    project.configureAndroidCommonOptions(library)
                }
                is AppPlugin -> {
                    val app = project.extensions.findByType(AppExtension::class.java)
                        ?: throw Exception("Failed to find Android extension")

                    project.configureAndroidCommonOptions(app)
                }
                is KotlinBasePluginWrapper -> {
                    val conf = project.configurations.create("kotlinPlugin")
                    val kotlinPlugin = conf.incoming.artifactView { view ->
                        view.attributes { attributes ->
                            attributes.attribute(
                                Attribute.of("artifactType", String::class.java),
                                ArtifactTypeDefinition.JAR_TYPE
                            )
                        }
                    }.files

                    project.tasks.withType(KotlinCompile::class.java).configureEach { compile ->
                        // TODO(b/157230235): remove when this is enabled by default
                        compile.kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
                        compile.inputs.files({ kotlinPlugin })
                            .withPropertyName("composeCompilerExtension")
                            .withNormalizer(ClasspathNormalizer::class.java)
                        compile.doFirst {
                            if (!kotlinPlugin.isEmpty) {
                                compile.kotlinOptions.freeCompilerArgs +=
                                    "-Xplugin=${kotlinPlugin.first()}"
                            }
                        }
                    }

                    project.afterEvaluate {
                        val androidXExtension =
                            project.extensions.findByType(AndroidXExtension::class.java)
                        if (androidXExtension != null) {
                            if (androidXExtension.publish.shouldPublish()) {
                                project.tasks.withType(KotlinCompile::class.java)
                                    .configureEach { compile ->
                                        compile.doFirst {
                                            if (!kotlinPlugin.isEmpty) {
                                                compile.kotlinOptions.freeCompilerArgs +=
                                                    listOf("-P", composeSourceOption)
                                            }
                                        }
                                    }
                            }
                        }
                    }

                    if (plugin is KotlinMultiplatformPluginWrapper) {
                        project.configureForMultiplatform()
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun Project.isMultiplatformEnabled(): Boolean {
            return properties.get(COMPOSE_MPP_ENABLED)?.toString()?.toBoolean() ?: false
        }

        /**
         * @param isMultiplatformEnabled whether this module has a corresponding
         * multiplatform configuration, or whether it is Android only
         */
        @JvmStatic
        @JvmOverloads
        fun Project.applyAndConfigureKotlinPlugin(
            isMultiplatformEnabled: Boolean = isMultiplatformEnabled()
        ) {
            if (isMultiplatformEnabled) {
                apply(plugin = "kotlin-multiplatform")
            } else {
                apply(plugin = "org.jetbrains.kotlin.android")
            }

            configureManifests()
            if (isMultiplatformEnabled) {
                configureForMultiplatform()
            } else {
                configureForKotlinMultiplatformSourceStructure()
            }

            tasks.withType(KotlinCompile::class.java).configureEach { compile ->
                // Needed to enable `expect` and `actual` keywords
                compile.kotlinOptions.freeCompilerArgs += "-Xmulti-platform"
            }
        }

        private fun Project.configureAndroidCommonOptions(testedExtension: TestedExtension) {
            testedExtension.defaultConfig.minSdk = 21

            val finalizeDsl: () -> Unit = {
                val isPublished = extensions.findByType(AndroidXExtension::class.java)
                    ?.type == LibraryType.PUBLISHED_LIBRARY

                @Suppress("DEPRECATION") // lintOptions methods
                testedExtension.lintOptions.apply {
                    // Too many Kotlin features require synthetic accessors - we want to rely on R8 to
                    // remove these accessors
                    disable("SyntheticAccessor")
                    // These lint checks are normally a warning (or lower), but we ignore (in AndroidX)
                    // warnings in Lint, so we make it an error here so it will fail the build.
                    // Note that this causes 'UnknownIssueId' lint warnings in the build log when
                    // Lint tries to apply this rule to modules that do not have this lint check, so
                    // we disable that check too
                    disable("UnknownIssueId")
                    error("ComposableNaming")
                    error("ComposableLambdaParameterNaming")
                    error("ComposableLambdaParameterPosition")
                    error("CompositionLocalNaming")
                    error("ComposableModifierFactory")
                    error("InvalidColorHexValue")
                    error("MissingColorAlphaChannel")
                    error("ModifierFactoryReturnType")
                    error("ModifierFactoryExtensionFunction")
                    error("ModifierParameter")
                    error("UnnecessaryComposedModifier")

                    // Paths we want to enable ListIterator checks for - for higher level
                    // libraries it won't have a noticeable performance impact, and we don't want
                    // developers reading high level library code to worry about this.
                    val listIteratorPaths = listOf(
                        "compose:foundation",
                        "compose:runtime",
                        "compose:ui",
                        "text"
                    )

                    // Paths we want to disable ListIteratorChecks for - these are not runtime
                    // libraries and so Iterator allocation is not relevant.
                    val ignoreListIteratorFilter = listOf(
                        "compose:ui:ui-test",
                        "compose:ui:ui-tooling",
                        "compose:ui:ui-inspection",
                    )

                    // Disable ListIterator if we are not in a matching path, or we are in an
                    // unpublished project
                    if (
                        listIteratorPaths.none { path.contains(it) } ||
                        ignoreListIteratorFilter.any { path.contains(it) } ||
                        !isPublished
                    ) {
                        disable("ListIterator")
                    }
                }
            }

            // TODO(aurimas): migrate away from this when upgrading to AGP 7.1.0-alpha03 or newer
            @Suppress("UnstableApiUsage", "DEPRECATION")
            extensions.findByType(
                com.android.build.api.extension.AndroidComponentsExtension::class.java
            )!!.finalizeDsl { finalizeDsl() }

            // TODO(148540713): remove this exclusion when Lint can support using multiple lint jars
            configurations.getByName("lintChecks").exclude(
                mapOf("module" to "lint-checks")
            )
            // TODO: figure out how to apply this to multiplatform modules
            dependencies.add(
                "lintChecks",
                project.dependencies.project(
                    mapOf(
                        "path" to ":compose:lint:internal-lint-checks",
                        "configuration" to "shadow"
                    )
                )
            )
        }

        private fun Project.configureManifests() {
            val libraryExtension = project.extensions.findByType<LibraryExtension>() ?: return
            libraryExtension.apply {
                sourceSets.findByName("main")!!.manifest
                    .srcFile("src/androidMain/AndroidManifest.xml")
                sourceSets.findByName("androidTest")!!.manifest
                    .srcFile("src/androidAndroidTest/AndroidManifest.xml")
            }
        }

        /**
         * General configuration for MPP projects. In the future, these workarounds should either be
         * generified and added to AndroidXPlugin, or removed as/when the underlying issues have been
         * resolved.
         */
        private fun Project.configureForKotlinMultiplatformSourceStructure() {
            val libraryExtension = project.extensions.findByType<LibraryExtension>() ?: return

            // TODO: b/148416113: AGP doesn't know about Kotlin-MPP's sourcesets yet, so add
            // them to its source directories (this fixes lint, and code completion in
            // Android Studio on versions >= 4.0canary8)
            libraryExtension.apply {
                sourceSets.findByName("main")?.apply {
                    java.srcDirs(
                        "src/commonMain/kotlin", "src/jvmMain/kotlin",
                        "src/androidMain/kotlin"
                    )
                    res.srcDirs(
                        "src/commonMain/resources",
                        "src/androidMain/res"
                    )
                    assets.srcDirs("src/androidMain/assets")

                    // Keep Kotlin files in java source sets so the source set is not empty when
                    // running unit tests which would prevent the tests from running in CI.
                    java.includes.add("**/*.kt")
                }
                sourceSets.findByName("test")?.apply {
                    java.srcDirs("src/test/kotlin")
                    res.srcDirs("src/test/res")

                    // Keep Kotlin files in java source sets so the source set is not empty when
                    // running unit tests which would prevent the tests from running in CI.
                    java.includes.add("**/*.kt")
                }
                sourceSets.findByName("androidTest")?.apply {
                    java.srcDirs("src/androidAndroidTest/kotlin")
                    res.srcDirs("src/androidAndroidTest/res")
                    assets.srcDirs("src/androidAndroidTest/assets")

                    // Keep Kotlin files in java source sets so the source set is not empty when
                    // running unit tests which would prevent the tests from running in CI.
                    java.includes.add("**/*.kt")
                }
            }
        }

        /**
         * General configuration for MPP projects. In the future, these workarounds should either be
         * generified and added to AndroidXPlugin, or removed as/when the underlying issues have been
         * resolved.
         */
        private fun Project.configureForMultiplatform() {
            val multiplatformExtension = checkNotNull(multiplatformExtension) {
                "Unable to configureForMultiplatform() when " +
                    "multiplatformExtension is null (multiplatform plugin not enabled?)"
            }

            /*
            The following configures source sets - note:

            1. The common unit test source set, commonTest, is included by default in both android
            unit and instrumented tests. This causes unnecessary duplication, so we explicitly do
            _not_ use commonTest, instead choosing to just use the unit test variant.
            TODO: Consider using commonTest for unit tests if a usable feature is added for
            https://youtrack.jetbrains.com/issue/KT-34662.

            2. The default (android) unit test source set is named 'androidTest', which conflicts / is
            confusing as this shares the same name / expected directory as AGP's 'androidTest', which
            represents _instrumented_ tests.
            TODO: Consider changing unitTest to androidLocalTest and androidAndroidTest to
            androidDeviceTest when https://github.com/JetBrains/kotlin/pull/2829 rolls in.
            */
            multiplatformExtension.sourceSets.all {
                // Allow all experimental APIs, since MPP projects are themselves experimental
                it.languageSettings.apply {
                    useExperimentalAnnotation("kotlin.Experimental")
                    useExperimentalAnnotation("kotlin.ExperimentalMultiplatform")
                }
            }

            afterEvaluate {
                if (multiplatformExtension.targets.findByName("jvm") != null) {
                    tasks.named("jvmTestClasses").also(::addToBuildOnServer)
                }
                if (multiplatformExtension.targets.findByName("desktop") != null) {
                    tasks.named("desktopTestClasses").also(::addToBuildOnServer)
                }
            }
        }
    }
}
