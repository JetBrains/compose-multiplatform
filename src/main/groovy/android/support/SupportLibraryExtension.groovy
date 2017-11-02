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

package android.support;

import org.gradle.api.Project

/**
 * Extension for {@link SupportAndroidLibraryPlugin} and {@link SupportJavaLibraryPlugin}.
 */
class SupportLibraryExtension {
    static final String ARCHITECTURE_URL =
            "https://developer.android.com/topic/libraries/architecture/index.html";
    static final String SUPPORT_URL =
            "http://developer.android.com/tools/extras/support-library.html";

    Project project
    String name;
    String description;
    String inceptionYear;
    String url = SUPPORT_URL;
    Collection<License> licenses = [];
    boolean java8Library = false;
    boolean legacySourceLocation = false;
    boolean publish = false;

    SupportLibraryExtension(Project project) {
        this.project = project
    }

    License license(Closure closure) {
        def license = project.configure(new License(), closure)
        licenses.add(license)
        return license
    }

    class License {
        String name;
        String url;

        void url(String p) {
            url = p
        }

        void name(String p) {
            name = p
        }
    }
}