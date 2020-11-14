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

package androidx.compose.animation

import androidx.compose.animation.core.AnimatedFloat
import androidx.compose.animation.core.AnimatedValue
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientAnimationClock

/**
 * The animatedValue effect creates an [AnimatedValue] and positionally memoizes it. When the
 * [AnimatedValue] object gets its value updated, components that rely on that value will be
 * automatically recomposed.
 *
 * @param initVal Initial value to set [AnimatedValue] to.
 * @param converter A value type converter for transforming any type T to an animatable type (i.e.
 *                  Floats, Vector2D, Vector3D, etc)
 * @param visibilityThreshold Visibility threshold for the animatedValue to consider itself
 * finished.
 */
@Composable
fun <T, V : AnimationVector> animatedValue(
    initVal: T,
    converter: TwoWayConverter<T, V>,
    visibilityThreshold: T? = null,
    clock: AnimationClockObservable = AmbientAnimationClock.current
): AnimatedValue<T, V> = clock.asDisposableClock().let { disposableClock ->
    remember(disposableClock) {
        AnimatedValueModel(initVal, converter, disposableClock, visibilityThreshold)
    }
}

/**
 * The animatedValue effect creates an [AnimatedFloat] and positionally memoizes it. When the
 * [AnimatedFloat] object gets its value updated, components that rely on that value will be
 * automatically recomposed.
 *
 * @param initVal Initial value to set [AnimatedFloat] to.
 */
@Composable
fun animatedFloat(
    initVal: Float,
    visibilityThreshold: Float = Spring.DefaultDisplacementThreshold,
    clock: AnimationClockObservable = AmbientAnimationClock.current
): AnimatedFloat = clock.asDisposableClock().let { disposableClock ->
    remember(disposableClock) { AnimatedFloatModel(initVal, disposableClock, visibilityThreshold) }
}

/**
 * The animatedValue effect creates an [AnimatedValue] of [Color] and positionally memoizes it. When
 * the [AnimatedValue] object gets its value updated, components that rely on that value will be
 * automatically recomposed.
 *
 * @param initVal Initial value to set [AnimatedValue] to.
 */
@Composable
fun animatedColor(
    initVal: Color,
    clock: AnimationClockObservable = AmbientAnimationClock.current
): AnimatedValue<Color, AnimationVector4D> = clock.asDisposableClock().let { disposableClock ->
    remember(disposableClock) {
        AnimatedValueModel(
            initialValue = initVal,
            typeConverter = (Color.VectorConverter)(initVal.colorSpace),
            clock = disposableClock
        )
    }
}

/**
 * Model class for [AnimatedValue]. This class tracks the value field change, so that composables
 * that read from this field can get promptly recomposed as the animation updates the value.
 *
 * @param initialValue The overridden value field that can only be mutated by animation
 * @param typeConverter The converter for converting any value of type [T] to an
 *                      [AnimationVector] type
 * @param clock The animation clock that will be used to drive the animation
 * @param visibilityThreshold Threshold at which the animation may round off to its target value.
 */
@Stable
class AnimatedValueModel<T, V : AnimationVector>(
    initialValue: T,
    typeConverter: TwoWayConverter<T, V>,
    clock: AnimationClockObservable,
    visibilityThreshold: T? = null
) : AnimatedValue<T, V>(typeConverter, clock, visibilityThreshold) {
    override var value: T by mutableStateOf(initialValue, structuralEqualityPolicy())
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
class AnimatedFloatModel(
    initialValue: Float,
    clock: AnimationClockObservable,
    visibilityThreshold: Float = Spring.DefaultDisplacementThreshold
) : AnimatedFloat(clock, visibilityThreshold) {
    override var value: Float by mutableStateOf(initialValue, structuralEqualityPolicy())
}