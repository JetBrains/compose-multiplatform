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

package android.support;

import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.AndroidSourceSet;

import com.google.common.collect.ImmutableMap;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Support library specific com.android.library plugin that sets common configurations needed for
 * support library modules.
 */
public class SupportLibraryPlugin implements Plugin<Project> {
    private static final String INSTRUMENTATION_RUNNER =
            "android.support.test.runner.AndroidJUnitRunner";

    @Override
    public void apply(Project project) {
        project.apply(ImmutableMap.of("plugin", "com.android.library"));
        LibraryExtension library =
                project.getExtensions().findByType(LibraryExtension.class);

        // Main sourceSet related options
        AndroidSourceSet mainSet = library.getSourceSets().findByName("main");
        mainSet.getManifest().srcFile("AndroidManifest.xml");

        // Set test related options
        library.getDefaultConfig().setTestInstrumentationRunner(INSTRUMENTATION_RUNNER);

        AndroidSourceSet sourceSet = library.getSourceSets().findByName("androidTest");
        sourceSet.setRoot("tests");
        sourceSet.getJava().srcDir("tests/src");
        sourceSet.getRes().srcDir("tests/res");
        sourceSet.getManifest().srcFile("tests/AndroidManifest.xml");

        // Set compile options
        library.getCompileOptions().setSourceCompatibility(JavaVersion.VERSION_1_7);
        library.getCompileOptions().setTargetCompatibility(JavaVersion.VERSION_1_7);
    }
}
