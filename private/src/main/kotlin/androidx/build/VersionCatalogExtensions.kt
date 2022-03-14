/*
 * Copyright 2022 The Android Open Source Project
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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension

fun Project.getLibraryByName(name: String): MinimalExternalModuleDependency {
    val libs = project.extensions.getByType(
        VersionCatalogsExtension::class.java
    ).find("libs").get()
    val library = libs.findLibrary(name)
    return if (library.isPresent) {
        library.get().get()
    } else {
        throw GradleException("Could not find a library for `$name`")
    }
}

fun Project.getVersionByName(name: String): String {
    val libs = project.extensions.getByType(
        VersionCatalogsExtension::class.java
    ).find("libs").get()
    val version = libs.findVersion(name)
    return if (version.isPresent) {
        version.get().requiredVersion
    } else {
        throw GradleException("Could not find a version for `$name`")
    }
}
