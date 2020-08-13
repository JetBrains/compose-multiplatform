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

import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * This is an inaccurate implementation that will only be used when running linked against
 * Android SDK stubs in host-side tests. A real implementation should synchronize with the
 * device's default display's vsync rate.
 */
private object SdkStubsFallbackFrameClock : MonotonicFrameClock {
    private const val DefaultFrameDelay = 16L // milliseconds

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
        withContext(Dispatchers.Main) {
            delay(DefaultFrameDelay)
            onFrame(System.nanoTime())
        }
}

actual val DefaultMonotonicFrameClock: MonotonicFrameClock by lazy {
    // When linked against Android SDK stubs and running host-side tests, APIs such as
    // Looper.getMainLooper() that will never return null on a real device will return null.
    // This branch offers an alternative solution.
    if (Looper.getMainLooper() != null) AndroidUiDispatcher.Main[MonotonicFrameClock]!!
    else SdkStubsFallbackFrameClock
}
