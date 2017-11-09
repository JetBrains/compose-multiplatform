/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.LibraryVariant
import com.android.builder.core.BuilderConstants
import com.google.common.collect.ImmutableMap
import net.ltgt.gradle.errorprone.ErrorProneBasePlugin
import net.ltgt.gradle.errorprone.ErrorProneToolChain
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * Support library specific com.android.library plugin that sets common configurations needed for
 * support library modules.
 */
class SupportAndroidLibraryPlugin implements Plugin<Project> {
    private static final String INSTRUMENTATION_RUNNER =
            "android.support.test.runner.AndroidJUnitRunner";

    @Override
    public void apply(Project project) {
        KotlinNoOp noOp = new KotlinNoOp()
        noOp.noOp()
        SupportLibraryExtension supportLibraryExtension =
                project.extensions.create("supportLibrary", SupportLibraryExtension, project);
        SupportLibraryMavenUploader.apply(project, supportLibraryExtension);

        project.afterEvaluate {
            LibraryExtension library = project.extensions.findByType(LibraryExtension.class);

            if (supportLibraryExtension.legacySourceLocation) {
                library.sourceSets {
                    main {
                        // We use a non-standard manifest path.
                        manifest.srcFile 'AndroidManifest.xml'
                    }

                    androidTest {
                        // We use a non-standard test directory structure.
                        root 'tests'
                        java.srcDir 'tests/src'
                        res.srcDir 'tests/res'
                        manifest.srcFile 'tests/AndroidManifest.xml'
                    }
                }
            }

            // Java 8 is only fully supported on API 24+ and not all Java 8 features are binary
            // compatible with API < 24, so use Java 7 for both source AND target.
            final JavaVersion javaVersion;
            if (supportLibraryExtension.java8Library) {
                if (library.defaultConfig.minSdkVersion.apiLevel < 24) {
                    throw new IllegalArgumentException("Libraries can only support Java 8 if "
                            + "minSdkVersion is 24 or higher");
                }
                javaVersion = JavaVersion.VERSION_1_8
            } else {
                javaVersion = JavaVersion.VERSION_1_7
            }

            library.compileOptions {
                sourceCompatibility javaVersion
                targetCompatibility javaVersion
            }
        }

        VersionFileWriterTask.setUpAndroidLibrary(project);

        project.apply(ImmutableMap.of("plugin", "com.android.library"));
        project.apply(ImmutableMap.of("plugin", ErrorProneBasePlugin.class));

        LibraryExtension library = project.extensions.findByType(LibraryExtension.class);

        library.compileSdkVersion project.currentSdk

        library.defaultConfig {
            // Update the version meta-data in each Manifest.
            addManifestPlaceholders(["target-sdk-version": project.currentSdk])

            // Set test related options.
            testInstrumentationRunner INSTRUMENTATION_RUNNER
        }

        library.signingConfigs {
            debug {
                // Use a local debug keystore to avoid build server issues.
                storeFile project.rootProject.init.debugKeystore
            }
        }

        // Always lint check NewApi as fatal.
        library.lintOptions {
            abortOnError true
            ignoreWarnings true

            // Write output directly to the console (and nowhere else).
            textOutput 'stderr'
            textReport true
            htmlReport false
            //xmlReport false

            // Format output for convenience.
            explainIssues true
            noLines false
            quiet true

            // Always fail on NewApi.
            error 'NewApi'
        }

        // Set baseline file for all legacy lint warnings.
        if (System.getenv("GRADLE_PLUGIN_VERSION") != null) {
            library.lintOptions.check 'NewApi'
        } else {
            library.lintOptions.baseline new File(project.projectDir, "/lint-baseline.xml")
        }

        if (project.rootProject.usingFullSdk) {
            // Library projects don't run lint by default, so set up dependency.
            project.uploadArchives.dependsOn "lintRelease"
        }

        SourceJarTaskHelper.setUpAndroidProject(project, library);

        final ErrorProneToolChain toolChain = ErrorProneToolChain.create(project);
        library.getBuildTypes().create("errorProne")
        library.getLibraryVariants().all(new Action<LibraryVariant>() {
            @Override
            void execute(LibraryVariant libraryVariant) {
                if (libraryVariant.getBuildType().getName().equals("errorProne")) {
                    libraryVariant.getJavaCompile().setToolChain(toolChain);

                    libraryVariant.getJavaCompile().options.compilerArgs += [
                            '-XDcompilePolicy=simple', // Workaround for b/36098770

                            // Enforce the following checks.
                            //'-Xep:MissingOverride:ERROR',
                            '-Xep:NarrowingCompoundAssignment:ERROR',
                            '-Xep:ClassNewInstance:ERROR',
                            '-Xep:ClassCanBeStatic:ERROR',
                            '-Xep:SynchronizeOnNonFinalField:ERROR',
                            '-Xep:OperatorPrecedence:ERROR'
                    ]
                }
            }
        })
    }
}
