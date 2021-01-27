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

package androidx.compose.runtime

import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import java.awt.DisplayMode
import java.awt.GraphicsEnvironment

internal actual object Trace {
    actual fun beginSection(name: String): Any? {
        // Do nothing.
        return null
    }

    actual fun endSection(token: Any?) {
        // Do nothing.
    }
}

internal val keyInfo = mutableMapOf<Int, String>()

private fun findSourceKey(key: Any): Int? =
    when (key) {
        is Int -> key
        is JoinedKey -> {
            key.left?.let { findSourceKey(it) } ?: key.right?.let { findSourceKey(it) }
        }
        else -> null
    }

// TODO(igotti): likely incorrect for the desktop.
internal actual fun recordSourceKeyInfo(key: Any) {
    val sk = findSourceKey(key)
    sk?.let {
        keyInfo.getOrPut(
            sk,
            {
                val stack = Thread.currentThread().stackTrace
                // On Android the frames looks like:
                //  0: getThreadStackTrace() (native method)
                //  1: getStackTrace()
                //  2: recordSourceKey()
                //  3: start()
                //  4: startGroup() or startNode()
                //  5: non-inline call/emit?
                //  5 or 6: <calling method>
                // On a desktop VM this looks like:
                //  0: getStackTrace()
                //  1: recordSourceKey()
                //  2: start()
                //  3: startGroup() or startNode()
                //  4: non-inline call/emit?
                //  4 or 5: <calling method>
                // If the stack method at 4 is startGroup assume we want 5 instead.
                val frameNumber = stack[4].let {
                    if (it.methodName == "startGroup" || it.methodName == "startNode") 5 else 4
                }
                val frame = stack[frameNumber].let {
                    if (it.methodName == "call" || it.methodName == "emit")
                        stack[frameNumber + 1]
                    else
                        stack[frameNumber]
                }
                "${frame.className}.${frame.methodName} (${frame.fileName}:${frame.lineNumber})"
            }
        )
    }
}

@InternalComposeApi
actual fun keySourceInfoOf(key: Any): String? = keyInfo[key]

actual fun resetSourceInfo() {
    keyInfo.clear()
}

// TODO(igotti): do we need actual processing for those?
actual annotation class MainThread()
actual annotation class CheckResult(actual val suggest: String)

// TODO implement local Recomposer in each Window, so each Window can have own MonotonicFrameClock.
//  It is needed for smooth animations and for the case when user have multiple windows on multiple
//  monitors with different refresh rates.
//  see https://github.com/JetBrains/compose-jb/issues/137
actual val DefaultMonotonicFrameClock: MonotonicFrameClock by lazy {
    object : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(
            onFrame: (Long) -> R
        ): R {
            if (GraphicsEnvironment.isHeadless()) {
                yield()
            } else {
                delay(1000L / getFramesPerSecond())
            }
            return onFrame(System.nanoTime())
        }

        private fun getFramesPerSecond(): Int {
            val refreshRate = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .screenDevices.maxOfOrNull { it.displayMode.refreshRate }
                ?: DisplayMode.REFRESH_RATE_UNKNOWN
            return if (refreshRate != DisplayMode.REFRESH_RATE_UNKNOWN) refreshRate else 60
        }
    }
}
