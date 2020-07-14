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

package androidx.compose.dispatch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * TODO: a more appropriate implementation that matches the vsync rate of the default display.
 * The current implementation will result in clock skew over time as resuming from delay() is not
 * guaranteed to be precise or frame-accurate.
 */
private object MainDispatcherFrameClock : MonotonicFrameClock {
    private const val DefaultFrameDelay = 16L // milliseconds

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
        withContext(Dispatchers.Main) {
            delay(DefaultFrameDelay)
            onFrame(System.nanoTime())
        }
}

actual val DefaultMonotonicFrameClock: MonotonicFrameClock
    get() = MainDispatcherFrameClock
