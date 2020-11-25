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

package androidx.compose.runtime.dispatch

import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import java.awt.DisplayMode
import java.awt.GraphicsEnvironment

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