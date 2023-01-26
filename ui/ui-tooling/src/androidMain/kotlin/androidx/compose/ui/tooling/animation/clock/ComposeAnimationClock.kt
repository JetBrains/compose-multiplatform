/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.tooling.animation.clock

import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.TransitionInfo
import androidx.compose.ui.tooling.animation.states.ComposeAnimationState

/** Clock to control one individual [ComposeAnimation] */
internal interface ComposeAnimationClock<T : ComposeAnimation, TState : ComposeAnimationState> {
    val animation: T
    var state: TState

    /** Get duration of the animation (ms).*/
    fun getMaxDuration(): Long

    /**
     * Get duration of one iteration for the animation (ms).
     * Applicable for repeatable animations with multiple iterations.
     * For non-repeatable animations [getMaxDuration] and [getMaxDurationPerIteration] are the same.
     * TODO(b/177895209) Add support for repeatable animations.
     */
    fun getMaxDurationPerIteration(): Long

    /**
     * Get the list of [ComposeAnimatedProperty].
     * It changes everytime time if [state] or clock time is changed.
     */
    fun getAnimatedProperties(): List<ComposeAnimatedProperty>

    /**
     * Get the list of [TransitionInfo].
     * It changes if [state] is changed.
     */
    fun getTransitions(stepMillis: Long): List<TransitionInfo>

    /**
     * Set clock time for the animation.
     */
    fun setClockTime(animationTimeNanos: Long)

    /**
     * Setter for [state] can be removed.
     * [setStateParameters] allows to update [state] in that case.
     */
    fun setStateParameters(par1: Any, par2: Any? = null)
}