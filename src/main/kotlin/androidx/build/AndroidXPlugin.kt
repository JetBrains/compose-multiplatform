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

import androidx.build.SupportConfig.BUILD_TOOLS_VERSION
import androidx.build.SupportConfig.CURRENT_SDK_VERSION
import androidx.build.SupportConfig.DEFAULT_MIN_SDK_VERSION
import androidx.build.SupportConfig.INSTRUMENTATION_RUNNER
import androidx.build.gradle.getByType
import androidx.build.gradle.isRoot
import androidx.build.license.configureExternalDependencyLicenseCheck
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
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
        // This has to be first due to bad behavior by DiffAndDocs which is triggered on the root
        // project. It calls evaluationDependsOn on each subproject. This eagerly causes evaluation
        // *during* the root build.gradle evaluation. The subproject then applies this plugin (while
        // we're still halfway through applying it on the root). The check licenses code runs on the
        // subproject which then looks for the root project task to add itself as a dependency of.
        // Without the root project having created the task prior to DiffAndDocs running this fails.
        // TODO do not use evaluationDependsOn in DiffAndDocs to break this cycle!
        project.configureExternalDependencyLicenseCheck()

        if (project.isRoot) {
            project.configureRootProject()
        }

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
                    project.configureSourceJarForAndroid(extension)
                    project.configureAndroidCommonOptions(extension)
                    project.configureAndroidLibraryOptions(extension)
                }
                is AppPlugin -> {
                    val extension = project.extensions.getByType<AppExtension>()
                    project.configureAndroidCommonOptions(extension)
                    project.configureAndroidApplicationOptions(extension)
                }
            }
        }

        // Disable timestamps and ensure filesystem-independent archive ordering to maximize
        // cross-machine byte-for-byte reproducibility of artifacts.
        project.tasks.withType<Jar> {
            isReproducibleFileOrder = true
            isPreserveFileTimestamps = false
        }
    }

    private fun Project.configureRootProject() {
        Release.createGlobalArchiveTask(this)

        val supportRootFolder = if (name == "app-toolkit") projectDir.parentFile else projectDir

        val allDocsTask = DiffAndDocs.configureDiffAndDocs(this, supportRootFolder,
                DacOptions("androidx", "ANDROIDX_DATA"),
                listOf(RELEASE_RULE))

        tasks.getByName(BUILD_ON_SERVER_TASK).dependsOn(allDocsTask)
    }

    private fun Project.configureAndroidCommonOptions(extension: BaseExtension) {
        extension.compileSdkVersion(CURRENT_SDK_VERSION)
        extension.buildToolsVersion = BUILD_TOOLS_VERSION

        // Expose the compilation SDK for use as the target SDK in test manifests.
        extension.defaultConfig.addManifestPlaceholders(
                mapOf("target-sdk-version" to CURRENT_SDK_VERSION))

        extension.defaultConfig.testInstrumentationRunner = INSTRUMENTATION_RUNNER
        extension.testOptions.unitTests.isReturnDefaultValues = true

        extension.defaultConfig.minSdkVersion(DEFAULT_MIN_SDK_VERSION)
        afterEvaluate {
            val minSdkVersion = extension.defaultConfig.minSdkVersion.apiLevel
            check(minSdkVersion >= DEFAULT_MIN_SDK_VERSION) {
                "minSdkVersion $minSdkVersion lower than the default of $DEFAULT_MIN_SDK_VERSION"
            }
        }

        // Use a local debug keystore to avoid build server issues.
        extension.signingConfigs.getByName("debug").storeFile = SupportConfig.getKeystore(this)

        // Disable generating BuildConfig.java
        extension.variants.all {
            it.generateBuildConfig.enabled = false
        }

        configureErrorProneForAndroid(extension.variants)

        // Enable code coverage for debug builds only if we are not running inside the IDE, since
        // enabling coverage reports breaks the method parameter resolution in the IDE debugger.
        extension.buildTypes.getByName("debug").isTestCoverageEnabled =
                !hasProperty("android.injected.invoked.from.ide")
    }

    private fun Project.configureAndroidLibraryOptions(extension: LibraryExtension) {
        extension.compileOptions.apply {
            setSourceCompatibility(VERSION_1_7)
            setTargetCompatibility(VERSION_1_7)
        }

        afterEvaluate {
            // Java 8 is only fully supported on API 24+ and not all Java 8 features are
            // binary compatible with API < 24
            val compilesAgainstJava8 = extension.compileOptions.sourceCompatibility > VERSION_1_7 ||
                    extension.compileOptions.targetCompatibility > VERSION_1_7
            val minSdkLessThan24 = extension.defaultConfig.minSdkVersion.apiLevel < 24
            if (compilesAgainstJava8 && minSdkLessThan24) {
                throw IllegalArgumentException(
                        "Libraries can only support Java 8 if minSdkVersion is 24 or higher")
            }
        }

        val isCoreSupportLibrary = rootProject.name == "support"
        configurations.all { configuration ->
            if (isCoreSupportLibrary && name != "annotations") {
                // While this usually happens naturally due to normal project dependencies, force
                // evaluation on the annotations project in case the below substitution is the only
                // dependency to this project. See b/70650240 on what happens when this is missing.
                evaluationDependsOn(":annotation")

                // In projects which compile as part of the "core" support libraries (which include
                // the annotations), replace any transitive pointer to the deployed Maven
                // coordinate version of annotations with a reference to the local project. These
                // usually originate from test dependencies and otherwise cause multiple copies on
                // the classpath. We do not do this for non-"core" projects as they need to
                // depend on the Maven coordinate variant.
                configuration.resolutionStrategy.dependencySubstitution.apply {
                    substitute(module("androidx.annotation:annotation"))
                            .with(project(":annotation"))
                }
            }
        }
    }

    private fun Project.configureAndroidApplicationOptions(extension: AppExtension) {
        extension.defaultConfig.apply {
            targetSdkVersion(CURRENT_SDK_VERSION)

            versionCode = 1
            versionName = "1.0"
        }

        extension.compileOptions.apply {
            setSourceCompatibility(VERSION_1_8)
            setTargetCompatibility(VERSION_1_8)
        }

        extension.lintOptions.apply {
            isAbortOnError = true

            val baseline = lintBaseline
            if (baseline.exists()) {
                baseline(baseline)
            }
        }
    }

    companion object {
        const val BUILD_ON_SERVER_TASK = "buildOnServer"
    }
}
