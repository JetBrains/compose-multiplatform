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

import com.android.build.gradle.LibraryPlugin
import com.google.common.collect.ImmutableMap
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.maven.MavenDeployer
import org.gradle.api.tasks.Upload

class SupportLibraryMavenUploader {
    static void apply(Project project, SupportLibraryExtension supportLibraryExtension) {
        project.afterEvaluate {
            if (supportLibraryExtension.publish) {
                if (supportLibraryExtension.mavenGroup == null) {
                    throw Exception("You must specify mavenGroup for " + project.name + " project");
                }
                if (supportLibraryExtension.mavenVersion == null) {
                    throw Exception("You must specify mavenVersion for " + project.name + " project");
                }
                project.group = supportLibraryExtension.mavenGroup
                project.version = supportLibraryExtension.mavenVersion.toString()
            }
        }

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

                        uploadTask.doFirst {
                            Set<ProjectDependency> allDeps = new HashSet<>();
                            collectDependenciesForConfiguration(allDeps, project, "api");
                            collectDependenciesForConfiguration(allDeps, project, "implementation");
                            collectDependenciesForConfiguration(allDeps, project, "compile");

                            mavenDeployer.getPom().whenConfigured {
                                it.dependencies.forEach { dep ->
                                    if (isAndroidProject(dep.groupId, dep.artifactId, allDeps)) {
                                        dep.type = "aar"
                                    }
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

    private static void collectDependenciesForConfiguration(Set<ProjectDependency> dependencies,
            Project project, String name) {
        def config = project.configurations.findByName(name);
        if (config != null) {
            config.dependencies.withType(ProjectDependency.class).forEach {
                dep -> dependencies.add(dep)
            }
        }
    }

    private static boolean isAndroidProject(String groupId, String artifactId, Set<ProjectDependency> deps) {
        for (ProjectDependency dep : deps) {
            if (dep.group == groupId && dep.name == artifactId) {
                return dep.getDependencyProject().plugins.hasPlugin(LibraryPlugin.class)
            }
        }
        return false;
    }
}