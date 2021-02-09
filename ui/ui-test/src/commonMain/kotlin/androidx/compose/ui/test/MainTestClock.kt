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

package androidx.compose.ui.test

/**
 * Clock that drives frames and recompositions in compose tests.
 *
 * This clock is responsible for driving all the recompositions and all subscribers via
 * withFrameNanos. Such subscribers are animations, transition and gestures. These clock do not
 * control measure and draw events.
 */
interface MainTestClock {
    /**
     * The current time of this clock in milliseconds.
     */
    val currentTime: Long

    /**
     * Whether the clock should be auto-advanced by the testing framework anytime it needs to
     * advance more frames to ensure idleness of compose or processing of gestures.
     *
     * When false, the clock can be only advanced manually via APIs on this object or by gestures.
     *
     * By default this is true.
     */
    var autoAdvance: Boolean

    /**
     * Advances the main clock by the duration of one frame.
     */
    fun advanceTimeByFrame()

    /**
     * Advances the clock by the given duration.
     *
     * Important: The duration will always be rounded up to the nearest duration that is a
     * multiplier of the frame duration. This is to make sure that any changes get recomposed to
     * avoid confusing states. This behavior can be turned off via [ignoreFrameDuration].
     *
     * @param milliseconds The minimal duration to advance the main clock by. Will be rounded up
     * to the nearest frame duration.
     * @param ignoreFrameDuration Whether to avoid rounding up the [milliseconds] to the nearest
     * multiplier of a frame duration.
     */
    fun advanceTimeBy(milliseconds: Long, ignoreFrameDuration: Boolean = false)

    /**
     * Advances the clock until the given condition is satisfied.
     *
     * Note that the given condition should be only checking state that can be affected by this
     * clock. Any condition that depends on measure or draw should use more general concept such
     * as [waitUntil][androidx.compose.ui.test.junit4.ComposeTestRule.waitUntil].
     *
     * @param timeoutMillis The time after which this method throws an exception if the given
     * condition is not satisfied. This is the simulated time not the wall clock or cpu time.
     *
     * @throws ComposeTimeoutException the condition is not satisfied after [timeoutMillis].
     */
    fun advanceTimeUntil(timeoutMillis: Long = 1_000, condition: () -> Boolean)
}

/**
 * Thrown in cases where Compose test can't satisfy a condition in a defined time limit.
 */
class ComposeTimeoutException(message: String?) : Throwable(message)