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

import androidx.build.SupportConfig.INSTRUMENTATION_RUNNER
import androidx.build.license.CheckExternalDependencyLicensesTask
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.LintOptions
import com.android.build.gradle.tasks.GenerateBuildConfig
import net.ltgt.gradle.errorprone.ErrorProneBasePlugin
import net.ltgt.gradle.errorprone.ErrorProneToolChain
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * Support library specific com.android.library plugin that sets common configurations needed for
 * support library modules.
 */
class SupportAndroidLibraryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val supportLibraryExtension = project.extensions.create("supportLibrary",
                SupportLibraryExtension::class.java, project)
        apply(project, supportLibraryExtension)
        CheckExternalDependencyLicensesTask.configure(project)
        val isCoreSupportLibrary = project.rootProject.name == "support"

        project.afterEvaluate {
            val library = project.extensions.findByType(LibraryExtension::class.java)
                    ?: return@afterEvaluate

            library.defaultConfig.minSdkVersion(supportLibraryExtension.minSdkVersion)

            // Java 8 is only fully supported on API 24+ and not all Java 8 features are binary
            // compatible with API < 24, so use Java 7 for both source AND target.
            val javaVersion: JavaVersion
            if (supportLibraryExtension.java8Library) {
                if (library.defaultConfig.minSdkVersion.apiLevel < 24) {
                    throw IllegalArgumentException("Libraries can only support Java 8 if "
                            + "minSdkVersion is 24 or higher")
                }
                javaVersion = JavaVersion.VERSION_1_8
            } else {
                javaVersion = JavaVersion.VERSION_1_7
            }

            library.compileOptions.setSourceCompatibility(javaVersion)
            library.compileOptions.setTargetCompatibility(javaVersion)

            VersionFileWriterTask.setUpAndroidLibrary(project, library)
            DiffAndDocs.registerAndroidProject(project, library, supportLibraryExtension)

            library.libraryVariants.all { libraryVariant ->
                if (libraryVariant.getBuildType().getName().equals("debug")) {
                    @Suppress("DEPRECATION")
                    val javaCompile = libraryVariant.javaCompile
                    if (supportLibraryExtension.failOnUncheckedWarnings) {
                        javaCompile.options.compilerArgs.add("-Xlint:unchecked")
                    }
                    if (supportLibraryExtension.failOnDeprecationWarnings) {
                        javaCompile.options.compilerArgs.add("-Xlint:deprecation")
                    }
                    javaCompile.options.compilerArgs.add("-Werror")
                }
            }
        }

        project.apply(mapOf("plugin" to "com.android.library"))
        project.apply(mapOf("plugin" to ErrorProneBasePlugin::class.java))

        project.afterEvaluate {
            project.tasks.all({
                if (it is GenerateBuildConfig) {
                    // Disable generating BuildConfig.java
                    it.enabled = false
                }
            })
        }

        project.configurations.all { configuration ->
            if (isCoreSupportLibrary && project.name != "annotations") {
                // While this usually happens naturally due to normal project dependencies, force
                // evaluation on the annotations project in case the below substitution is the only
                // dependency to this project. See b/70650240 on what happens when this is missing.
                project.evaluationDependsOn(":annotation")

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

        val library = project.extensions.findByType(LibraryExtension::class.java)
                ?: throw Exception("Failed to find Android extension")

        library.compileSdkVersion(SupportConfig.CURRENT_SDK_VERSION)

        library.buildToolsVersion = SupportConfig.BUILD_TOOLS_VERSION

        // Update the version meta-data in each Manifest.
        library.defaultConfig.addManifestPlaceholders(
                mapOf("target-sdk-version" to SupportConfig.CURRENT_SDK_VERSION))

        // Set test runner.
        library.defaultConfig.testInstrumentationRunner = INSTRUMENTATION_RUNNER

        library.testOptions.unitTests.isReturnDefaultValues = true

        // Use a local debug keystore to avoid build server issues.
        library.signingConfigs.findByName("debug")?.storeFile =
                SupportConfig.getKeystore(project)

        project.afterEvaluate {
            setUpLint(library.lintOptions, SupportConfig.getLintBaseline(project),
                    (supportLibraryExtension.mavenVersion?.isFinalApi()) ?: false)
        }

        project.tasks.getByName("uploadArchives").dependsOn("lintRelease")

        setUpSoureJarTaskForAndroidProject(project, library)

        val toolChain = ErrorProneToolChain.create(project)
        project.dependencies.add("errorprone", ERROR_PRONE_VERSION)
        library.libraryVariants.all { libraryVariant ->
            if (libraryVariant.getBuildType().getName().equals("debug")) {
                @Suppress("DEPRECATION")
                libraryVariant.javaCompile.configureWithErrorProne(toolChain)
            }
        }
    }
}

private fun setUpLint(lintOptions: LintOptions, baseline: File, verifyTranslations: Boolean) {
    // Always lint check NewApi as fatal.
    lintOptions.isAbortOnError = true
    lintOptions.isIgnoreWarnings = true

    // Skip lintVital tasks on assemble. We explicitly run lintRelease for libraries.
    lintOptions.isCheckReleaseBuilds = false

    // Write output directly to the console (and nowhere else).
    lintOptions.textOutput("stderr")
    lintOptions.textReport = true
    lintOptions.htmlReport = false

    // Format output for convenience.
    lintOptions.isExplainIssues = true
    lintOptions.isNoLines = false
    lintOptions.isQuiet = true

    lintOptions.fatal("NewApi")
    lintOptions.fatal("ObsoleteSdkInt")
    lintOptions.fatal("VisibleForTests")
    lintOptions.fatal("NoHardKeywords")

    if (verifyTranslations) {
        lintOptions.fatal("MissingTranslation")
    } else {
        lintOptions.disable("MissingTranslation")
    }

    // Set baseline file for all legacy lint warnings.
    if (baseline.exists()) {
        lintOptions.baseline(baseline)
    }
}
