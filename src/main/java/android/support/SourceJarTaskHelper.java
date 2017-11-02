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

package android.support;

import com.android.build.gradle.LibraryExtension;
import com.android.builder.core.BuilderConstants;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.bundling.Jar;

/**
 * Helper class to handle creation of source jars.
 */
public class SourceJarTaskHelper {
    /**
     * Sets up a source jar task for an Android library project.
     */
    public static void setUpAndroidProject(Project project, LibraryExtension extension) {
        // Create sources jar for release builds
        extension.getLibraryVariants().all(libraryVariant -> {
            if (!libraryVariant.getBuildType().getName().equals(BuilderConstants.RELEASE)) {
                return; // Skip non-release builds.
            }

            Jar sourceJar = project.getTasks().create("sourceJarRelease", Jar.class);
            sourceJar.setPreserveFileTimestamps(false);
            sourceJar.setClassifier("sources");
            sourceJar.from(extension.getSourceSets().findByName("main").getJava().getSrcDirs());
            project.getArtifacts().add("archives", sourceJar);
        });
    }

    /**
     * Sets up a source jar task for a Java library project.
     */
    public static void setUpJavaProject(Project project) {
        Jar sourceJar = project.getTasks().create("sourceJar", Jar.class);
        sourceJar.setPreserveFileTimestamps(false);
        sourceJar.setClassifier("sources");
        JavaPluginConvention convention =
                project.getConvention().getPlugin(JavaPluginConvention.class);
        sourceJar.from(convention.getSourceSets().findByName("main").getAllSource().getSrcDirs());
        project.getArtifacts().add("archives", sourceJar);
    }
}
