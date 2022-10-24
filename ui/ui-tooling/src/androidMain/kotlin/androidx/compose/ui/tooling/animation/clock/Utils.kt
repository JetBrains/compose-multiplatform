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

import androidx.compose.animation.core.Animation
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.RepeatableSpec
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorizedDurationBasedAnimationSpec
import androidx.compose.animation.tooling.TransitionInfo

/**
 * Converts the given time in nanoseconds to milliseconds, rounding up when needed.
 */
internal fun nanosToMillis(timeNs: Long) = (timeNs + 999_999) / 1_000_000

/**
 * Converts the given time in milliseconds to nanoseconds.
 */
internal fun millisToNanos(timeMs: Long) = timeMs * 1_000_000L

/**
 * Return all the animations of a [Transition], as well as all the animations of its every
 * descendant [Transition]s.
 */
internal fun Transition<*>.allAnimations(): List<Transition<*>.TransitionAnimationState<*, *>> {
    val descendantAnimations = transitions.flatMap { it.allAnimations() }
    return animations + descendantAnimations
}

/**
 * Creates [TransitionInfo] from [Transition.TransitionAnimationState].
 * * [TransitionInfo.startTimeMillis] is an animation delay if it has one.
 * * [TransitionInfo.endTimeMillis] is an animation duration as it's already includes the delay.
 * * [TransitionInfo.specType] is a java class name of the spec.
 * * [TransitionInfo.values] a map of animation values from [TransitionInfo.startTimeMillis]
 * to [TransitionInfo.endTimeMillis] with [stepMs] sampling.
 */
internal fun <T, V : AnimationVector, S>
    Transition<S>.TransitionAnimationState<T, V>.createTransitionInfo(stepMs: Long = 1):
    TransitionInfo = animation.createTransitionInfo(label, animationSpec, stepMs)

/**
 * Creates [TransitionInfo] for [Animation].
 * * [TransitionInfo.startTimeMillis] is an animation delay if it has one.
 * * [TransitionInfo.endTimeMillis] is an animation duration as it's already includes the delay.
 * * [TransitionInfo.specType] is a java class name of the spec.
 * * [TransitionInfo.values] a map of animation values from [TransitionInfo.startTimeMillis]
 * to [TransitionInfo.endTimeMillis] with [stepMs] sampling.
 */
internal fun <T, V : AnimationVector>
    Animation<T, V>.createTransitionInfo(
    label: String,
    animationSpec: AnimationSpec<T>,
    stepMs: Long = 1
): TransitionInfo {
    val endTimeMs = nanosToMillis(this.durationNanos)
    val startTimeMs: Long by lazy {
        when (animationSpec) {
            is TweenSpec<*> -> animationSpec.delay
            is SnapSpec<*> -> animationSpec.delay
            is KeyframesSpec<*> -> animationSpec.config.delayMillis
            is RepeatableSpec<*> -> {
                if (animationSpec.initialStartOffset.offsetType == StartOffsetType.Delay)
                    animationSpec.initialStartOffset.offsetMillis
                else 0L
            }
            is InfiniteRepeatableSpec<*> -> {
                if (animationSpec.initialStartOffset.offsetType == StartOffsetType.Delay)
                    animationSpec.initialStartOffset.offsetMillis
                else 0L
            }
            is VectorizedDurationBasedAnimationSpec<*> -> animationSpec.delayMillis
            else -> 0L
        }.toLong()
    }
    val values: Map<Long, T> by lazy {
        val values: MutableMap<Long, T> = mutableMapOf()
        // Always add start and end points.
        values[startTimeMs] = this.getValueFromNanos(
            millisToNanos(startTimeMs)
        )
        values[endTimeMs] = this.getValueFromNanos(millisToNanos(endTimeMs))

        for (millis in startTimeMs..endTimeMs step stepMs) {
            values[millis] = this.getValueFromNanos(millisToNanos(millis))
        }
        values
    }
    return TransitionInfo(
        label, animationSpec.javaClass.name,
        startTimeMs, endTimeMs, values
    )
}