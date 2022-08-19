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

import com.google.gson.annotations.SerializedName

/**
 * Helper data class to store the metadata information for each library/path.
 */
data class MetadataEntry(
    @SerializedName("groupId")
    val groupId: String,

    @SerializedName("artifactId")
    val artifactId: String,

    @SerializedName("releaseNotesUrl")
    val releaseNotesUrl: String,

    // TODO (b/241582234): Remove when bug is resolved.
    //
    // This will no longer be used once Dackka is updated, but is currently needed as Dackka
    // expects this key to be present.
    @SerializedName("sourceDir")
    val sourceDir: String = "TBD/SOURCE/DIR",
)