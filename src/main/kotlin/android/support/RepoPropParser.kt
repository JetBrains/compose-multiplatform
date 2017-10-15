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

package android.support

import org.gradle.api.GradleException
import java.io.File

private const val FRAMEWORKS_SUPPORT = "platform/frameworks/support"

/**
 * parses repo.prop
 * expected format is:
 * <project1 path> <revision sha1>
 *     ....
 * <projectN path> <revision shaN>
 */
private fun parseRepoPropFile(file: File): Map<String, String> {
    val result = mutableMapOf<String, String>()
    file.forEachLine { line ->
        val split = line.split(' ')
        if (split.size != 2) {
            throw GradleException("Invalid format for repo.prop, line: $line")
        }
        val project = split[0]
        val sha = split[1]
        result[project] = sha
    }
    return result
}

fun frameworksSupportSHA(file: File): String {
    return parseRepoPropFile(file).getOrElse(FRAMEWORKS_SUPPORT, {
        throw GradleException("$FRAMEWORKS_SUPPORT wasn't found in $file")
    })
}