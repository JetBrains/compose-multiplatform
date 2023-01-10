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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Transition
import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.TransitionInfo
import androidx.compose.ui.tooling.animation.TransitionBasedAnimation
import androidx.compose.ui.tooling.animation.states.TargetState

/**
 * [ComposeAnimationClock] for [Transition] and [AnimatedContent] animations.
 * This clock also controls extension functions such as:
 * * Transition.AnimatedVisibility
 * * Transition.Crossfade
 * * Transition.AnimatedContent
 *
 *  @sample androidx.compose.animation.core.samples.GestureAnimationSample
 *  @sample androidx.compose.animation.samples.AnimatedVisibilityLazyColumnSample
 *  @sample androidx.compose.animation.samples.CrossfadeSample
 *  @sample androidx.compose.animation.samples.TransitionExtensionAnimatedContentSample
 *  @sample androidx.compose.animation.samples.AnimateIncrementDecrementSample
 */
internal class TransitionClock<T>(override val animation: TransitionBasedAnimation<T>) :
    ComposeAnimationClock<TransitionBasedAnimation<T>, TargetState<T>> {

    override var state = TargetState(
        animation.animationObject.currentState,
        animation.animationObject.targetState
    )
        set(value) {
            field = value
            setClockTime(0)
        }

    override fun setStateParameters(par1: Any, par2: Any?) {
        parseParametersToValue(state.initial, par1, par2)?.let {
            state = it
        }
    }

    override fun getAnimatedProperties(): List<ComposeAnimatedProperty> {
        // In case the transition have child transitions, make sure to return their
        // descendant animations as well.
        return animation.animationObject.allAnimations().mapNotNull {
            val value = it.value
            value ?: return@mapNotNull null
            ComposeAnimatedProperty(it.label, value)
        }.filter { !IGNORE_TRANSITIONS.contains(it.label) }
    }

    override fun getMaxDurationPerIteration(): Long {
        return nanosToMillis(animation.animationObject.totalDurationNanos)
    }

    override fun getMaxDuration(): Long {
        return nanosToMillis(animation.animationObject.totalDurationNanos)
    }

    override fun getTransitions(stepMillis: Long): List<TransitionInfo> {
        val transition = animation.animationObject
        return transition.allAnimations().map {
            it.createTransitionInfo(stepMillis)
        }.filter { !IGNORE_TRANSITIONS.contains(it.label) }
    }

    override fun setClockTime(animationTimeNanos: Long) {
        animation.animationObject.setPlaytimeAfterInitialAndTargetStateEstablished(
            state.initial, state.target, animationTimeNanos
        )
    }
}