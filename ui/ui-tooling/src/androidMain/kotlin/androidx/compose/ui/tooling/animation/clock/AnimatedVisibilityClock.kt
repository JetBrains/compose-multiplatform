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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.TransitionInfo
import androidx.compose.ui.tooling.animation.AnimatedVisibilityComposeAnimation
import androidx.compose.ui.tooling.animation.states.AnimatedVisibilityState

/**
 * [ComposeAnimationClock] for [AnimatedVisibility] animations.
 *
 * Note: [AnimatedVisibility] extension function for Transition will be controlled by
 * [TransitionClock] instead.
 *
 * @sample androidx.compose.animation.samples.AnimatedVisibilityWithBooleanVisibleParamNoReceiver
 */
internal class AnimatedVisibilityClock(override val animation: AnimatedVisibilityComposeAnimation) :
    ComposeAnimationClock<AnimatedVisibilityComposeAnimation, AnimatedVisibilityState> {

    override var state: AnimatedVisibilityState =
        if (animation.animationObject.currentState) {
            AnimatedVisibilityState.Exit
        } else {
            AnimatedVisibilityState.Enter
        }
        set(value) {
            field = value
            setClockTime(0)
        }

    override fun setStateParameters(par1: Any, par2: Any?) {
        state = par1 as AnimatedVisibilityState
    }

    override fun getMaxDurationPerIteration(): Long {
        return nanosToMillis(animation.childTransition?.totalDurationNanos ?: return 0)
    }

    override fun getMaxDuration(): Long {
        return nanosToMillis(animation.childTransition?.totalDurationNanos ?: return 0)
    }

    override fun setClockTime(animationTimeNanos: Long) {
        animation.animationObject.let {
            val (current, target) = state.toCurrentTargetPair()
            it.setPlaytimeAfterInitialAndTargetStateEstablished(current, target, animationTimeNanos)
        }
    }

    override fun getTransitions(stepMillis: Long): List<TransitionInfo> {
        animation.childTransition?.let { child ->
            return child.allAnimations().map {
                it.createTransitionInfo(stepMillis)
            }.sortedBy { it.label }.filter { !IGNORE_TRANSITIONS.contains(it.label) }
        }
        return emptyList()
    }

    override fun getAnimatedProperties(): List<ComposeAnimatedProperty> {
        animation.childTransition?.let { child ->
            return child.allAnimations().mapNotNull {
                ComposeAnimatedProperty(it.label, it.value ?: return@mapNotNull null)
            }.sortedBy { it.label }.filter { !IGNORE_TRANSITIONS.contains(it.label) }
        }
        return emptyList()
    }

    private fun AnimatedVisibilityState.toCurrentTargetPair() =
        if (this == AnimatedVisibilityState.Enter) false to true else true to false
}