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

internal actual object Trace {
    actual fun beginSection(name: String): Any? {
        // Do nothing.
        return null
    }

    actual fun endSection(token: Any?) {
        // Do nothing.
    }
}

// TODO(igotti): do we need actual processing for those?
actual annotation class CheckResult(actual val suggest: String)

/**
 * Clock with fixed delay between frames (16ms), independent from any display/window.
 *
 * It is used by [withFrameNanos] and [withFrameMillis] if one is not present
 * in the calling [kotlin.coroutines.CoroutineContext].
 *
 * Use it only where you don't need to show animation in a window.
 *
 * If you need a frame clock for changing the state of an animation that should be displayed to
 * user, use [MonotonicFrameClock] that is bound to the current window. You can access it using
 * [LaunchedEffect]:
 * ```
 * LaunchedEffect {
 *   val frameClock = coroutineContext[MonotonicFrameClock]
 * }
 * ```
 *
 * Or using [rememberCoroutineScope]:
 * ```
 * val scope = rememberCoroutineScope()
 * val frameClock = scope.coroutineContext[MonotonicFrameClock]
 * ```
 *
 * If [withFrameNanos] / [withFrameMillis] runs inside the coroutine scope
 * obtained using [LaunchedEffect] or [rememberCoroutineScope] they also use
 * [MonotonicFrameClock] which is bound to the current window.
 */
actual val DefaultMonotonicFrameClock: MonotonicFrameClock get() = SixtyFpsMonotonicFrameClock

private object SixtyFpsMonotonicFrameClock : MonotonicFrameClock {
    private const val fps = 60

    override suspend fun <R> withFrameNanos(
        onFrame: (Long) -> R
    ): R {
        delay(1000L / fps)
        return onFrame(System.nanoTime())
    }
}
