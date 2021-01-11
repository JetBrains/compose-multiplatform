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

import androidx.compose.animation.core.AnimationClockObservable

/**
 * Interface for animation clocks that can report their idleness and can switch between ticking
 * automatically (e.g., if it's driven by the main loop of the host) and ticking manually.
 *
 * An idle clock is one that is currently not driving any animations. Typically, that means a
 * clock where no observers are registered. The idleness can be retrieved by [isIdle].
 *
 * Use [pauseClock] to switch from automatic ticking to manual ticking, [resumeClock] to switch
 * from manual to automatic with; and manually tick the clock with [advanceClock].
 */
@ExperimentalTestApi
interface TestAnimationClock : AnimationClockObservable {
    /**
     * Whether the clock is idle or not. An idle clock is one that is not driving animations,
     * which happens (1) when no observers are observing this clock, or (2) when the clock is
     * paused.
     */
    val isIdle: Boolean

    /**
     * Pauses the automatic ticking of the clock. The clock shall only tick in response to
     * [advanceClock], and shall continue ticking automatically when [resumeClock] is called.
     * It's safe to call this method when the clock is already paused.
     */
    fun pauseClock()

    /**
     * Resumes the automatic ticking of the clock. It's safe to call this method when the clock
     * is already resumed.
     */
    fun resumeClock()

    /**
     * Whether the clock is [paused][pauseClock] or [not][resumeClock].
     */
    val isPaused: Boolean

    /**
     * Advances the clock by the given number of [milliseconds]. It is safe to call this method
     * both when the clock is paused and resumed.
     */
    fun advanceClock(milliseconds: Long)
}
