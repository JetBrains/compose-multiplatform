/*
 * Copyright 2019 The Android Open Source Project
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

import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * Studio version information used for setting up the correct version of Android Studio.
 */
class StudioVersions(
    val studioVersion: String,
    val ideaMajorVersion: String,
    val studioBuildNumber: String
) {
    companion object {
        fun loadFrom(supportRoot: File): StudioVersions {
            val versionsFile = File(supportRoot, "buildSrc/studio_versions.properties")
            val inputStream = FileInputStream(versionsFile)
            val properties = Properties()
            properties.load(inputStream)
            return StudioVersions(
                properties.get("studio_version")!!.toString(),
                properties.get("idea_major_version")!!.toString(),
                properties.get("studio_build_number")!!.toString()
            )
        }
    }
}
