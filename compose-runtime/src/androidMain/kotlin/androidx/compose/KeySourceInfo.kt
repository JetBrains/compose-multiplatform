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

package androidx.compose

internal val keyInfo = mutableMapOf<Int, String>()

private fun findSourceKey(key: Any): Int? =
    when (key) {
        is Int -> key
        is JoinedKey -> {
            key.left?.let { findSourceKey(it) } ?: key.right?.let { findSourceKey(it) }
        }
        else -> null
    }

internal actual fun recordSourceKeyInfo(key: Any) {
    val sk = findSourceKey(key)
    sk?.let {
        keyInfo.getOrPut(sk, {
            val stack = Thread.currentThread().stackTrace
            // On Android the frames looks like:
            //  0: getThreadStackTrace() (native method)
            //  1: getStackTrace()
            //  2: recordSourceKeyInfo()
            //  3: start()
            //  4: start()
            //  5: startRestartGroup() or startReplaceableGroup() or startNode() or startDefaults()
            //  6: <calling method>
            // On a desktop VM this looks like:
            //  0: getStackTrace()
            //  1: recordSourceKey()
            //  2: start()
            //  3: startRestartGroup() or startReplaceableGroup() or startNode() or startDefaults()
            //  4: non-inline call/emit?
            //  4 or 5: <calling method>
            val frame = stack
                .drop(3) // The first 3 elements are never interesting to us
                .dropWhile { it.className.startsWith("androidx.compose.Composer") } // Drop all
                // start*
                .drop(1) // Drop non-inline call/emit
                .dropWhile { it.lineNumber == -1 } // Drop if we do not have source info
                .first()
            "${frame.className}.${frame.methodName} (${frame.fileName}:${frame.lineNumber})"
        })
    }
}

@InternalComposeApi
actual fun keySourceInfoOf(key: Any): String? = keyInfo[key]
