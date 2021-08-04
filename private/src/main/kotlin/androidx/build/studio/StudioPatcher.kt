/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.build.studio

import org.gradle.api.Project
import java.io.File

typealias StudioPatcher = (
    studioTask: StudioTask,
    project: Project,
    studioInstallationDir: File
) -> Unit

/**
 * noop - leave studio unpatched by default
 */
val NoopStudioPatcher: StudioPatcher = fun (_: StudioTask, _: Project, _: File) {}

/**
 * Patch studio with the Performance Testing 202.6948.5 plugin, downloaded from Jetbrains
 */
val PerformancePluginStudioPatcher: StudioPatcher = fun (
    studioTask: StudioTask,
    project: Project,
    studioInstallationDir: File
) {
    val url = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=94393"
    val tmpZip = File("${studioInstallationDir.absolutePath}/performanceTestingPlugin.zip")
        .absolutePath

    println("Downloading $url to $tmpZip")
    project.exec { execSpec ->
        with(execSpec) {
            executable("curl")
            args("--location", url, "--output", tmpZip)
        }
    }

    val platformUtilities = StudioPlatformUtilities.get(project.rootDir, studioInstallationDir)
    val pluginsDir = with(platformUtilities) { studioTask.pluginsDirectory }
    File(pluginsDir, "performanceTesting").deleteRecursively()

    project.exec { execSpec ->
        with(execSpec) {
            executable("unzip")
            args(tmpZip, "-d", pluginsDir)
        }
    }
}
