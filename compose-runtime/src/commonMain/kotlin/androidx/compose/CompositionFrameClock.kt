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

package androidx.compose

/**
 * Provides a time source for display frames for use in composition.
 * This may be used for matching timing with the refresh rate of a display
 * or otherwise synchronizing with a desired frame rate of composition updates.
 */
interface CompositionFrameClock {
    /**
     * Suspends until a new frame is requested, immediately invokes [onFrame] with the frame time
     * in nanoseconds in the calling context of frame dispatch, then resumes with the result from
     * [onFrame].
     *
     * `frameTimeNanos` should be used when calculating animation time deltas from frame to frame
     * as it may be normalized to the target time for the frame, not necessarily a direct,
     * "now" value.
     *
     * The time base of the value provided by [withFrameNanos] is implementation defined.
     * Time values provided are monotonically increasing; after a call to [withFrameNanos]
     * completes it must not provide the same value again for a subsequent call.
     */
    suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R

    /**
     * Suspends until a new frame is requested, immediately invokes [onFrame] with the frame time
     * in nanoseconds in the calling context of frame dispatch, then resumes with the result from
     * [onFrame].
     *
     * `frameTimeNanos` should be used when calculating animation time deltas from frame to frame
     * as it may be normalized to the target time for the frame, not necessarily a direct,
     * "now" value.
     *
     * The time base of the value provided by [withFrameNanos] is implementation defined.
     * Time values provided are monotonically increasing; after a call to [withFrameNanos]
     * completes it must not provide the same value again for a subsequent call.
     */
    @Deprecated("renamed to withFrameNanos", ReplaceWith("withFrameNanos(onFrame)"))
    suspend fun <R> awaitFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
        withFrameNanos(onFrame)
}

/**
 * Suspends until a new frame is requested, immediately invokes [onFrame] with the frame time
 * in nanoseconds in the calling context of frame dispatch, then resumes with the result from
 * [onFrame].
 *
 * `frameTimeNanos` should be used when calculating animation time deltas from frame to frame
 * as it may be normalized to the target time for the frame, not necessarily a direct,
 * "now" value.
 *
 * The time base of the value provided by [CompositionFrameClock.withFrameMillis] is
 * implementation defined. Time values provided are monotonically increasing; after a call to
 * [CompositionFrameClock.withFrameMillis] completes it must not provide the same value again for
 * a subsequent call.
 */
@Suppress("UnnecessaryLambdaCreation")
suspend inline fun <R> CompositionFrameClock.withFrameMillis(
    crossinline onFrame: (frameTimeMillis: Long) -> R
): R = withFrameNanos { onFrame(it / 1_000_000L) }

/**
 * Suspends until a new frame is requested, immediately invokes [onFrame] with the frame time
 * in nanoseconds in the calling context of frame dispatch, then resumes with the result from
 * [onFrame].
 *
 * `frameTimeNanos` should be used when calculating animation time deltas from frame to frame
 * as it may be normalized to the target time for the frame, not necessarily a direct,
 * "now" value.
 *
 * The time base of the value provided by [awaitFrameNanos] is implementation defined.
 * Time values provided are monotonically increasing; after a call to [awaitFrameNanos]
 * completes it must not provide the same value again for a subsequent call.
 */
@Deprecated(
    "renamed to withFrameMillis",
    ReplaceWith("withFrameMillis(onFrame)", "androidx.compose.withFrameMillis")
)
suspend inline fun <R> CompositionFrameClock.awaitFrameMillis(
    crossinline onFrame: (frameTimeMillis: Long) -> R
): R = withFrameMillis(onFrame)

/**
 * Suspends until a new frame is requested, returning the frame time in nanoseconds.
 * This value should be used when calculating animation time deltas from frame to frame
 * as it may be normalized to the target time for the frame, not necessarily a direct,
 * "now" value.
 *
 * The time base of the value returned by [awaitFrameNanos] is implementation defined.
 * Time values returned are monotonically increasing; after a call to [awaitFrameNanos]
 * returns it must not return the same value again for a subsequent call.
 */
@Deprecated(
    "callers will resume after missing the frame on most dispatchers",
    ReplaceWith("withFrameNanos { it }")
)
suspend fun CompositionFrameClock.awaitFrameNanos(): Long = withFrameNanos { it }

/**
 * Suspends until a new frame is requested, returning the frame time in milliseconds.
 * This value should be used when calculating animation time deltas from frame to frame
 * as it may be normalized to the target time for the frame, not necessarily a direct,
 * "now" value.
 *
 * The time base of the value returned by [awaitFrameMillis] is implementation defined.
 * Time values returned are monotonically increasing; after a call to [awaitFrameMillis]
 * returns it must not return the same value again for a subsequent call.
 */
@Deprecated(
    "callers will resume after missing the frame on most dispatchers",
    ReplaceWith("withFrameMillis { it }", "androidx.compose.withFrameMillis")
)
suspend fun CompositionFrameClock.awaitFrameMillis(): Long = withFrameMillis { it }
