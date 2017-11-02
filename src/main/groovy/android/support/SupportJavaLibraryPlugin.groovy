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
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

/**
 * Support java library specific plugin that sets common configurations needed for
 * support library modules.
 */
class SupportJavaLibraryPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        SupportLibraryExtension supportLibraryExtension =
                project.extensions.create("supportLibrary", SupportLibraryExtension, project);
        SupportLibraryMavenUploader.apply(project, supportLibraryExtension);

        project.apply(ImmutableMap.of("plugin", "java"));
        project.afterEvaluate {
            project.compileJava {
                def version = supportLibraryExtension.java8Library ?
                    JavaVersion.VERSION_1_8 : JavaVersion.VERSION_1_7
                sourceCompatibility = version
                targetCompatibility = version
            }
        }

        SourceJarTaskHelper.setUpJavaProject(project);
    }
}