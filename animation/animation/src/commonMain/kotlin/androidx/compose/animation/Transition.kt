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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Creates a [Color] animation as a part of the given [Transition]. This means the lifecycle
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, CompositionLocals, themes, etc. If the target value changes
 * when the [Transition] already reached its [targetState][Transition.targetState],
 * the [Transition] will run an animation to ensure the new target value is reached smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animations for
 * each pair of initialState and targetState. [FiniteAnimationSpec] can be used to describe such
 * animations, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 *
 * @sample androidx.compose.animation.samples.GestureAnimationSample
 *
 * @see Transition.animateValue
 * @see androidx.compose.animation.core.Transition
 * @see androidx.compose.animation.core.updateTransition
 */
@Composable
inline fun <S> Transition<S>.animateColor(
    noinline transitionSpec:
        @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Color> = { spring() },
    label: String = "ColorAnimation",
    targetValueByState: @Composable() (state: S) -> Color
): State<Color> {
    val colorSpace = targetValueByState(targetState).colorSpace
    val typeConverter = remember(colorSpace) {
        Color.VectorConverter(colorSpace)
    }

    return animateValue(typeConverter, transitionSpec, label, targetValueByState)
}

/**
 * Creates a Color animation that runs infinitely as a part of the given [InfiniteTransition].
 *
 * Once the animation is created, it will run from [initialValue] to [targetValue] and repeat.
 * Depending on the [RepeatMode] of the provided [animationSpec], the animation could either
 * restart after each iteration (i.e. [RepeatMode.Restart]), or reverse after each iteration (i.e
 * . [RepeatMode.Reverse]).
 *
 * If [initialValue] or [targetValue] is changed at any point during the animation, the animation
 * will be restarted with the new initial/targetValue. __Note__: this means animation continuity
 * will *not* be preserved when changing either [initialValue] or [targetValue].
 *
 * @sample androidx.compose.animation.samples.InfiniteTransitionSample
 *
 * @see InfiniteTransition.animateValue
 * @see InfiniteRepeatableSpec
 */
@Composable
fun InfiniteTransition.animateColor(
    initialValue: Color,
    targetValue: Color,
    animationSpec: InfiniteRepeatableSpec<Color>
): State<Color> {
    val converter = remember {
        (Color.VectorConverter)(targetValue.colorSpace)
    }
    return animateValue(initialValue, targetValue, converter, animationSpec)
}
