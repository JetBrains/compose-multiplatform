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

package androidx.inspection.gradle

import com.android.build.api.variant.Variant
import java.io.File
import java.util.Locale
import org.gradle.api.Project

internal fun Variant.taskName(baseName: String) =
    "$baseName${name.replaceFirstChar(Char::titlecase)}"

internal fun Project.taskWorkingDir(
    variant: Variant,
    baseName: String
): File {
    val inspectionDir = File(project.buildDir, "androidx_inspection")
    return File(File(inspectionDir, baseName), variant.name)
}

// Functions below will be removed once registerGenerateProguardDetectionFileTask is migrated
// that needs newly added function "addGeneratedSourceDirectory"
@Suppress("DEPRECATION") // BaseVariant
internal fun com.android.build.gradle.api.BaseVariant.taskName(baseName: String) =
    "$baseName${name.capitalize(Locale.ENGLISH)}"

@Suppress("DEPRECATION") // BaseVariant
internal fun Project.taskWorkingDir(
    variant: com.android.build.gradle.api.BaseVariant,
    baseName: String
): File {
    val inspectionDir = File(project.buildDir, "androidx_inspection")
    return File(File(inspectionDir, baseName), variant.dirName)
}