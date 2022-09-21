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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.animation.AnimateXAsStateComposeAnimation
import androidx.compose.ui.tooling.animation.states.TargetState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

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

    @Suppress("UNCHECKED_CAST")
    override fun setStateParameters(par1: Any, par2: Any?) {

        fun parametersAreValid(par1: Any?, par2: Any?): Boolean {
            return currentValue != null &&
                par1 != null && par2 != null && par1::class == par2::class
        }

        fun parametersHasTheSameType(value: Any, par1: Any, par2: Any): Boolean {
            return value::class == par1::class && value::class == par2::class
        }

        if (!parametersAreValid(par1, par2)) return

        if (parametersHasTheSameType(currentValue!!, par1, par2!!)) {
            state = TargetState(par1 as T, par2 as T)
            return
        }

        if (par1 is List<*> && par2 is List<*>) {
            try {
                state = when (currentValue) {
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
                    else -> {
                        if (parametersAreValid(par1[0], par2[0]) &&
                            parametersHasTheSameType(currentValue!!, par1[0]!!, par2[0]!!)
                        ) TargetState(par1[0], par2[0])
                        else return
                    }
                } as TargetState<T>
            } catch (_: IndexOutOfBoundsException) {
                return
            } catch (_: ClassCastException) {
                return
            } catch (_: IllegalArgumentException) {
                return
            } catch (_: NullPointerException) {
                return
            }
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