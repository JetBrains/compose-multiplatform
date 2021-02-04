/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.animation

import androidx.compose.animation.core.AnimatedFloat
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.platform.LocalAnimationClock

/**
 * The animatedValue effect creates an [AnimatedFloat] and positionally memoizes it. When the
 * [AnimatedFloat] object gets its value updated, components that rely on that value will be
 * automatically recomposed.
 *
 * @param initVal Initial value to set [AnimatedFloat] to.
 */
@Composable
@Deprecated(
    "animatedFloat has been deprecated. Please use remember { Animatable } instead",
    replaceWith = ReplaceWith(
        "remember { Animatable(initVal, visibilityThreshold) }",
        "androidx.compose.animation.core.Animatable",
        "androidx.compose.runtime.remember"
    )
)
fun animatedFloat(
    initVal: Float,
    visibilityThreshold: Float = Spring.DefaultDisplacementThreshold,
    clock: AnimationClockObservable = LocalAnimationClock.current
): AnimatedFloat = clock.asDisposableClock().let { disposableClock ->
    remember(disposableClock) { AnimatedFloatModel(initVal, disposableClock, visibilityThreshold) }
}

/**
 * Model class for [AnimatedFloat]. This class tracks the value field change, so that composables
 * that read from this field can get promptly recomposed as the animation updates the value.
 *
 * @param initialValue The overridden value field that can only be mutated by animation
 * @param clock The animation clock that will be used to drive the animation
 * @param visibilityThreshold a threshold to determine when the animation is considered close
 *                            enough to the target to terminate
 */
@Stable
private class AnimatedFloatModel(
    initialValue: Float,
    clock: AnimationClockObservable,
    visibilityThreshold: Float = Spring.DefaultDisplacementThreshold
) : AnimatedFloat(clock, visibilityThreshold) {
    override var value: Float by mutableStateOf(initialValue, structuralEqualityPolicy())
}