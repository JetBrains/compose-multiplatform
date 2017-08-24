/*
 * Copyright (C) 2017 The Android Open Source Project
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

import com.google.common.collect.ImmutableMap
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployer
import org.gradle.api.tasks.Upload

class SupportLibraryMavenUploader {
    static void apply(Project project, SupportLibraryExtension supportLibraryExtension) {
        project.apply(ImmutableMap.of("plugin", "maven"));

        // Set uploadArchives options.
        Upload uploadTask = (Upload) project.getTasks().getByName("uploadArchives");
        project.afterEvaluate {
            if (supportLibraryExtension.publish) {
                uploadTask.getRepositories().withType(MavenDeployer.class, new Action<MavenDeployer>() {
                    @Override
                    public void execute(MavenDeployer mavenDeployer) {
                        mavenDeployer.getPom().project {
                            name supportLibraryExtension.getName()
                            description supportLibraryExtension.getDescription()
                            url supportLibraryExtension.getUrl()
                            inceptionYear supportLibraryExtension.getInceptionYear()

                            licenses {
                                license {
                                    name 'The Apache Software License, Version 2.0'
                                    url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                                    distribution 'repo'
                                }

                                supportLibraryExtension.getLicenses().each {
                                    SupportLibraryExtension.License supportLicense ->
                                        license {
                                            name supportLicense.name
                                            url supportLicense.url
                                            distribution 'repo'
                                        }
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
            } else {
                uploadTask.enabled = false;
            }
        }
    }
}