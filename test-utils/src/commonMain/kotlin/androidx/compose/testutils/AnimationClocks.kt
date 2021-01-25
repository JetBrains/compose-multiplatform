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

package androidx.compose.testutils

import androidx.compose.animation.core.ManualFrameClock
import androidx.compose.animation.core.MonotonicFrameAnimationClock
import androidx.compose.animation.core.advanceClockMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.coroutines.CoroutineContext

/**
 * Creates a [MonotonicFrameAnimationClock] from the given [coroutineContext]'s clock. A new
 * coroutine scope is created from the [coroutineContext].
 *
 * @see MonotonicFrameAnimationClock
 */
fun monotonicFrameAnimationClockOf(
    coroutineContext: CoroutineContext
): MonotonicFrameAnimationClock =
    MonotonicFrameAnimationClock(
        CoroutineScope(coroutineContext)
    )

/**
 * Advances the clock on the main dispatcher.
 *
 * @see ManualFrameClock.advanceClock
 */
suspend fun ManualFrameClock.advanceClockOnMainThread(nanos: Long) {
    withContext(Dispatchers.Main) {
        advanceClock(nanos)
        // Give awaiters the chance to await again
        yield()
    }
}

/**
 * Advances the clock on the main dispatcher
 *
 * @see ManualFrameClock.advanceClock
 */
suspend fun ManualFrameClock.advanceClockOnMainThreadMillis(millis: Long) {
    withContext(Dispatchers.Main) {
        advanceClockMillis(millis)
        // Give awaiters the chance to await again
        yield()
    }
}
