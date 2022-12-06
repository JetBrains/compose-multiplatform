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
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.TransitionInfo
import androidx.compose.ui.tooling.animation.InfiniteTransitionComposeAnimation
import androidx.compose.ui.tooling.animation.states.TargetState
import kotlin.math.max

/**
 * [ComposeAnimationClock] for [InfiniteTransition] animations.
 *
 *  @sample androidx.compose.animation.samples.InfiniteTransitionSample
 */
internal class InfiniteTransitionClock(
    override val animation: InfiniteTransitionComposeAnimation,
    private val maxDuration: () -> Long = { 0 }
) :
    ComposeAnimationClock<InfiniteTransitionComposeAnimation, TargetState<Any>> {

    /** [rememberInfiniteTransition] doesn't have a state. */
    override var state: TargetState<Any> = TargetState(0, 0)

    override fun setStateParameters(par1: Any, par2: Any?) {}

    override fun getAnimatedProperties(): List<ComposeAnimatedProperty> {
        return animation.animationObject.animations.mapNotNull {
            val value = it.value
            value ?: return@mapNotNull null
            ComposeAnimatedProperty(it.label, value)
        }.filter { !IGNORE_TRANSITIONS.contains(it.label) }
    }

    /** Max duration per iteration of the animation. */
    override fun getMaxDurationPerIteration(): Long {
        return nanosToMillis(animation.animationObject.animations.maxOfOrNull {
            it.getIterationDuration()
        } ?: 0)
    }

    /** Max duration of the animation. */
    override fun getMaxDuration(): Long {
        return max(getMaxDurationPerIteration(), maxDuration())
    }

    override fun getTransitions(stepMillis: Long): List<TransitionInfo> {
        val transition = animation.animationObject
        return transition.animations.map {
            it.createTransitionInfo(stepMillis, getMaxDuration())
        }.filter { !IGNORE_TRANSITIONS.contains(it.label) }.toList()
    }

    override fun setClockTime(animationTimeNanos: Long) {
        animation.setTimeNanos(animationTimeNanos)
    }

    private fun <T, V : AnimationVector> InfiniteTransition.TransitionAnimationState<T, V>
        .getIterationDuration(): Long {
        val repeatableSpec = animationSpec as InfiniteRepeatableSpec<T>
        // If animation has Reverse mode, include two iterations, otherwise just one.
        val repeats = if (repeatableSpec.repeatMode == RepeatMode.Reverse) 2 else 1
        val animation = repeatableSpec.animation.vectorize(typeConverter)
        return millisToNanos(animation.delayMillis.toLong() + animation.durationMillis * repeats)
    }
}