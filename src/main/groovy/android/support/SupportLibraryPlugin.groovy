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
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.LibraryVariant
import com.android.builder.core.BuilderConstants
import com.google.common.collect.ImmutableMap
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployer
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar

/**
 * Support library specific com.android.library plugin that sets common configurations needed for
 * support library modules.
 */
class SupportLibraryPlugin implements Plugin<Project> {
    private static final String INSTRUMENTATION_RUNNER =
            "android.support.test.runner.AndroidJUnitRunner";

    @Override
    public void apply(Project project) {
        SupportLibraryExtension supportLibraryExtension =
                project.getExtensions().create("supportLibrary", SupportLibraryExtension);

        project.apply(ImmutableMap.of("plugin", "com.android.library"));
        LibraryExtension library =
                project.getExtensions().findByType(LibraryExtension.class);

        library.setCompileSdkVersion(project.ext.currentSdk)

        // Main sourceSet related options
        AndroidSourceSet mainSet = library.getSourceSets().findByName("main");
        mainSet.getManifest().srcFile("AndroidManifest.xml");

        // Update the version meta-data in each Manifest
        library.getDefaultConfig().addManifestPlaceholders(
                ["support-version": project.rootProject.ext.supportVersion])

        // Set test related options
        library.getDefaultConfig().setTestInstrumentationRunner(INSTRUMENTATION_RUNNER);

        library.sourceSets.androidTest {
            root "tests"
            java.srcDir "tests/src"
            res.srcDir "tests/res"
            manifest.srcFile "tests/AndroidManifest.xml"
        }

        // Set compile options
        library.getCompileOptions().setSourceCompatibility(JavaVersion.VERSION_1_7);
        library.getCompileOptions().setTargetCompatibility(JavaVersion.VERSION_1_7);

        // Create sources jar for release builds
        library.getLibraryVariants().all(new Action<LibraryVariant>() {
            @Override
            public void execute(LibraryVariant libraryVariant) {
                if (!libraryVariant.getBuildType().getName().equals(BuilderConstants.RELEASE)) {
                    return; // Skip non-release builds.
                }

                Jar sourceJar = project.getTasks().create("sourceJarRelease", Jar.class);
                sourceJar.setClassifier("sources");
                sourceJar.from(library.getSourceSets().findByName("main").getJava().getSrcDirs());
                project.getArtifacts().add("archives", sourceJar);
            }
        });

        // Set uploadArchives options
        Upload uploadTask = (Upload) project.getTasks().getByName("uploadArchives");
        project.afterEvaluate {
            uploadTask.repositories {
                mavenDeployer {
                    repository(url: project.uri(project.rootProject.ext.supportRepoOut))
                }
            };
            uploadTask.getRepositories().withType(MavenDeployer.class, new Action<MavenDeployer>() {
                @Override
                public void execute(MavenDeployer mavenDeployer) {
                    mavenDeployer.getPom().project {
                        name supportLibraryExtension.getName()
                        description supportLibraryExtension.getDescription()
                        url 'http://developer.android.com/tools/extras/support-library.html'
                        inceptionYear supportLibraryExtension.getInceptionYear()

                        licenses {
                            license {
                                name 'The Apache Software License, Version 2.0'
                                url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                                distribution 'repo'
                            }
                        }

                        scm {
                            url "http://source.android.com"
                            connection "scm:git:https://android.googlesource.com/platform/frameworks/support"
                        }
                        developers {
                            developer {
                                name 'The Android Open Source Project'
                            }
                        }
                    }
                }
            });
        }
    }
}
