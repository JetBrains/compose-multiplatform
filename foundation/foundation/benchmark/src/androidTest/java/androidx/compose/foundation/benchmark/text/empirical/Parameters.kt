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

package androidx.compose.foundation.benchmark.text.empirical

object AllApps {
    /**
     * For all apps, the vast majority of text is <64 characters.
     *
     * Examples of typical text
     *
     * "OK"
     * "Close"
     * "Click below to learn more"
     */
    val TextLengths: Array<Any> = arrayOf(2, 16, 32, 64)
}

object ChatApps {
    /**
     * For chat apps, strings tend to be longer due to user generated content.
     */
    val TextLengths: Array<Any> = arrayOf(256, 512)
}

fun generateCacheableStringOf(size: Int): String {
    var workingSize = size
    val builder = StringBuilder(size)
    while (workingSize > 0) {
        repeat(Integer.min(8, workingSize) - 1) {
            builder.append("a")
            workingSize--
        }
        if (workingSize > 1) {
            builder.append(" ")
        } else {
            builder.append("a")
        }
        workingSize--
    }
    return builder.toString()
}