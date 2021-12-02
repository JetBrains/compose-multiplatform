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

import org.gradle.process.ExecOperations
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Function to place the required studio archive at destinationPath, in preparation for
 * extracting later.
 */
typealias StudioArchiveCreator = (
    execOperations: ExecOperations,
    studioVersion: String,
    filename: String,
    destinationPath: String
) -> Unit

/**
 * Downloads a Studio archive from the official download URL, and places it at destinationPath.
 */
val UrlArchiveCreator: StudioArchiveCreator = fun (
    execOperations: ExecOperations,
    studioVersion: String,
    filename: String,
    destinationPath: String
) {
    val url = "https://dl.google.com/dl/android/studio/ide-zips/$studioVersion/$filename"

    val tmpDownloadPath = File("$destinationPath.tmp").absolutePath

    println("Downloading $url to $tmpDownloadPath")
    execOperations.exec { execSpec ->
        with(execSpec) {
            executable("curl")
            args(url, "--output", tmpDownloadPath)
        }
    }

    // Renames temp archive to the final archive name
    Files.move(Paths.get(tmpDownloadPath), Paths.get(destinationPath))
}
