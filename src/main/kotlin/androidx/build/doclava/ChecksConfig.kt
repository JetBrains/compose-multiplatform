/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.doclava

import java.io.Serializable

data class ChecksConfig(
    /**
     * List of Doclava error codes to treat as errors.
     * <p>
     * See {@link com.google.doclava.Errors} for a complete list of error codes.
     */
    val errors: List<Int>,
    /**
     * List of Doclava error codes to treat as warnings.
     * <p>
     * See {@link com.google.doclava.Errors} for a complete list of error codes.
     */
    val warnings: List<Int>,
    /**
     * List of Doclava error codes to ignore.
     * <p>
     * See {@link com.google.doclava.Errors} for a complete list of error codes.
     */
    val hidden: List<Int>,
    /** Message to display on check failure. */
    val onFailMessage: String? = null
) : Serializable

private const val MSG_HIDE_API =
        "If you are adding APIs that should be excluded from the public API surface,\n" +
                "consider using package or private visibility. If the API must have public\n" +
                "visibility, you may exclude it from public API by using the @hide javadoc\n" +
                "annotation paired with the @RestrictTo(LIBRARY_GROUP) code annotation."

val CHECK_API_CONFIG_RELEASE = ChecksConfig(
        onFailMessage =
        "Compatibility with previously released public APIs has been broken. Please\n" +
                "verify your change with Support API Council and provide error output,\n" +
                "including the error messages and associated SHAs.\n" +
                "\n" +
                "If you are removing APIs, they must be deprecated first before being removed\n" +
                "in a subsequent release.\n" +
                "\n" + MSG_HIDE_API,
        errors = (7..18).toList(),
        warnings = emptyList(),
        hidden = (2..6) + (19..30)
)

// Check that the API we're building hasn't changed from the development
// version. These types of changes require an explicit API file update.
val CHECK_API_CONFIG_DEVELOP = ChecksConfig(
        onFailMessage =
        "Public API definition has changed. Please run ./gradlew updateApi to confirm\n" +
                "these changes are intentional by updating the public API definition.\n" +
                "\n" + MSG_HIDE_API,
        errors = (2..30) - listOf(22),
        warnings = emptyList(),
        hidden = listOf(22)
)

// This is a patch or finalized release. Check that the API we're building
// hasn't changed from the current.
val CHECK_API_CONFIG_PATCH = CHECK_API_CONFIG_DEVELOP.copy(
        onFailMessage = "Public API definition may not change in finalized or patch releases.\n" +
                "\n" + MSG_HIDE_API)
