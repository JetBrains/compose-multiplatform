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
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.RepeatableSpec
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorizedDurationBasedAnimationSpec
import androidx.compose.animation.tooling.TransitionInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.animation.states.TargetState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Animations can contain internal only transitions which should be ignored by tooling. */
internal val IGNORE_TRANSITIONS = listOf("TransformOriginInterruptionHandling")

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

/**
 * Creates [TransitionInfo] for [InfiniteTransition.TransitionAnimationState].
 */
internal fun <T, V : AnimationVector>
    InfiniteTransition.TransitionAnimationState<T, V>.createTransitionInfo(
    stepMs: Long = 1,
    endTimeMs: Long
): TransitionInfo {
    val startTimeMs: Long = 0
    val values: Map<Long, T> by lazy {
        val values: MutableMap<Long, T> = mutableMapOf()
        // Always add start and end points.
        values[startTimeMs] = this.animation.getValueFromNanos(
            millisToNanos(startTimeMs)
        )
        values[endTimeMs] = this.animation.getValueFromNanos(millisToNanos(endTimeMs))

        for (millis in startTimeMs..endTimeMs step stepMs) {
            values[millis] = this.animation.getValueFromNanos(millisToNanos(millis))
        }
        values
    }
    return TransitionInfo(
        label, animationSpec.javaClass.name,
        startTimeMs, endTimeMs, values
    )
}

/**
 * [parseParametersToValue] makes sure what [currentValue], [par1], [par2] have the same types and
 * returned [TargetState] always has correct and the same type as [currentValue].
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> parseParametersToValue(currentValue: T, par1: Any, par2: Any?): TargetState<T>? {

    currentValue ?: return null

    /** Check if [par1] and [par2] are not null and have the same type. */
    fun parametersAreValid(par1: Any?, par2: Any?): Boolean {
        return par1 != null && par2 != null && par1::class == par2::class
    }

    /** Check if all parameters have the same type. */
    fun parametersHasTheSameType(value: Any, par1: Any, par2: Any): Boolean {
        return value::class == par1::class && value::class == par2::class
    }

    fun getDp(par: Any): Dp? {
        return (par as? Dp) ?: (par as? Float)?.dp ?: (par as? Double)?.dp ?: (par as? Int)?.dp
    }

    fun parseDp(par1: Any, par2: Any?): TargetState<Dp>? {
        if (currentValue !is Dp || par2 == null) return null
        return if (par1 is Dp && par2 is Dp)
            TargetState(par1, par2) else {
            val dp1 = getDp(par1)
            val dp2 = getDp(par2)
            if (dp1 != null && dp2 != null)
                TargetState(dp1, dp2) else null
        }
    }
    // Dp could be presented as Float/Double/Int - try to parse it.
    parseDp(par1, par2)?.let { return it as TargetState<T> }

    if (!parametersAreValid(par1, par2)) return null

    if (parametersHasTheSameType(currentValue, par1, par2!!)) {
        return TargetState(par1 as T, par2 as T)
    }

    if (par1 is List<*> && par2 is List<*>) {
        try {
            return when (currentValue) {
                is IntSize -> TargetState(
                    IntSize(par1[0] as Int, par1[1] as Int),
                    IntSize(par2[0] as Int, par2[1] as Int)
                )

                is IntOffset -> TargetState(
                    IntOffset(par1[0] as Int, par1[1] as Int),
                    IntOffset(par2[0] as Int, par2[1] as Int)
                )

                is Size -> TargetState(
                    Size(par1[0] as Float, par1[1] as Float),
                    Size(par2[0] as Float, par2[1] as Float)
                )

                is Offset -> TargetState(
                    Offset(par1[0] as Float, par1[1] as Float),
                    Offset(par2[0] as Float, par2[1] as Float)
                )

                is Rect ->
                    TargetState(
                        Rect(
                            par1[0] as Float,
                            par1[1] as Float,
                            par1[2] as Float,
                            par1[3] as Float
                        ),
                        Rect(
                            par2[0] as Float,
                            par2[1] as Float,
                            par2[2] as Float,
                            par2[3] as Float
                        ),
                    )

                is Color -> TargetState(
                    Color(
                        par1[0] as Float,
                        par1[1] as Float,
                        par1[2] as Float,
                        par1[3] as Float
                    ),
                    Color(
                        par2[0] as Float,
                        par2[1] as Float,
                        par2[2] as Float,
                        par2[3] as Float
                    ),
                )

                is Dp ->
                    parseDp(par1[0]!!, par2[0]!!)

                else -> {
                    if (parametersAreValid(par1[0], par2[0]) &&
                        parametersHasTheSameType(currentValue, par1[0]!!, par2[0]!!)
                    ) TargetState(par1[0], par2[0])
                    else return null
                }
            } as TargetState<T>
        } catch (_: IndexOutOfBoundsException) {
            return null
        } catch (_: ClassCastException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        } catch (_: NullPointerException) {
            return null
        }
    }
    return null
}