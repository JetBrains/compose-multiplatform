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

package androidx.build.dackka

import java.io.Serializable

/**
 * Helper data class to store the metadata information for each library/path.
 */
data class MetadataEntry(
    val groupId: String,
    val artifactId: String,
    val releaseNotesUrl: String,
    val sourceDir: String,
) : Serializable {

    /**
     * Transforms the contents of this data class into a [Map] for future conversion to JSON
     *
     * @return the contents of this data class as a [Map].
     */
    fun toMap(): Map<String, String> {
        return mapOf(
            "groupId" to groupId,
            "artifactId" to artifactId,
            "releaseNotesUrl" to releaseNotesUrl,
            "sourceDir" to sourceDir
        )
    }
}