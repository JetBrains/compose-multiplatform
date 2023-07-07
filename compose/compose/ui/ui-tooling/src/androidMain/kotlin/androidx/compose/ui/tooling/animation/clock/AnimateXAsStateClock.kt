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

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.TransitionInfo
import androidx.compose.ui.tooling.animation.AnimateXAsStateComposeAnimation
import androidx.compose.ui.tooling.animation.states.TargetState

/**
 * [ComposeAnimationClock] for [AnimateXAsStateComposeAnimation].
 */
internal class AnimateXAsStateClock<T, V : AnimationVector>(
    override val animation: AnimateXAsStateComposeAnimation<T, V>
) :
    ComposeAnimationClock<AnimateXAsStateComposeAnimation<T, V>, TargetState<T>> {

    override var state = TargetState(
        animation.animationObject.value,
        animation.animationObject.value
    )
        set(value) {
            field = value
            currAnimation = getCurrentAnimation()
            setClockTime(0)
        }

    private var currentValue: T = animation.toolingState.value
        private set(value) {
            field = value
            animation.toolingState.value = value
        }

    private var currAnimation: TargetBasedAnimation<T, V> = getCurrentAnimation()

    override fun setStateParameters(par1: Any, par2: Any?) {
        parseParametersToValue(currentValue, par1, par2)?.let {
            state = it
        }
    }

    override fun getAnimatedProperties(): List<ComposeAnimatedProperty> {
        return listOf(ComposeAnimatedProperty(animation.label, currentValue as Any))
    }

    override fun getMaxDurationPerIteration(): Long {
        return nanosToMillis(currAnimation.durationNanos)
    }

    override fun getMaxDuration(): Long {
        return nanosToMillis(currAnimation.durationNanos)
    }

    override fun getTransitions(stepMillis: Long): List<TransitionInfo> {
        return listOf(
            currAnimation.createTransitionInfo(
                animation.label, animation.animationSpec, stepMillis
            )
        )
    }

    private var clockTimeNanos = 0L
        set(value) {
            field = value
            currentValue = currAnimation.getValueFromNanos(value)
        }

    override fun setClockTime(animationTimeNanos: Long) {
        clockTimeNanos = animationTimeNanos
    }

    private fun getCurrentAnimation(): TargetBasedAnimation<T, V> {
        return TargetBasedAnimation(
            animationSpec = animation.animationSpec,
            initialValue = state.initial,
            targetValue = state.target,
            typeConverter = animation.animationObject.typeConverter,
            initialVelocity = animation.animationObject.velocity
        )
    }
}