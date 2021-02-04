/*
 * Copyright 2021 The Android Open Source Project
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

@Deprecated("Moved to androidx.compose.runtime in the compose:runtime artifact")
typealias MonotonicFrameClock = androidx.compose.runtime.MonotonicFrameClock

/**
 * Suspends until a new frame is requested, immediately invokes [onFrame] with the frame time
 * in nanoseconds in the calling context of frame dispatch, then resumes with the result from
 * [onFrame].
 */
@Deprecated(
    "Moved to androidx.compose.runtime",
    ReplaceWith("androidx.compose.runtime.withFrameNanos(onFrame)")
)
suspend fun <R> withFrameNanos(onFrame: (frameTimeMillis: Long) -> R): R =
    androidx.compose.runtime.withFrameNanos(onFrame)

/**
 * Suspends until a new frame is requested, immediately invokes [onFrame] with the frame time
 * in nanoseconds in the calling context of frame dispatch, then resumes with the result from
 * [onFrame].
 */
@Deprecated(
    "Moved to androidx.compose.runtime",
    ReplaceWith("androidx.compose.runtime.withFrameMillis(onFrame)")
)
suspend fun <R> withFrameMillis(onFrame: (frameTimeMillis: Long) -> R): R =
    androidx.compose.runtime.withFrameMillis(onFrame)
