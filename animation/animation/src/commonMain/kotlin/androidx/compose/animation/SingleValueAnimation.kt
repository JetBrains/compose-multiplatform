/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Fire-and-forget animation function for [Color]. This Composable function is overloaded for
 * different parameter types such as [Dp], [Float], [Int], [Size], [Offset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight when [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateColorAsState] returns a [State] object. The value of the state object will
 * continuously be updated by the animation until the animation finishes.
 *
 * Note, [animateColorAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [Animatable][androidx.compose.animation.Animatable] for cancelable animations.
 *
 * @sample androidx.compose.animation.samples.ColorAnimationSample
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time,
 *                      [spring] by default
 * @param finishedListener An optional listener to get notified when the animation is finished.
 */
@Composable
fun animateColorAsState(
    targetValue: Color,
    animationSpec: AnimationSpec<Color> = colorDefaultSpring,
    finishedListener: ((Color) -> Unit)? = null
): State<Color> {
    val converter = remember(targetValue.colorSpace) {
        (Color.VectorConverter)(targetValue.colorSpace)
    }
    return animateValueAsState(
        targetValue, converter, animationSpec, finishedListener = finishedListener
    )
}

private val colorDefaultSpring = spring<Color>()

/**
 * This [Animatable] function creates a Color value holder that automatically
 * animates its value when the value is changed via [animateTo]. [Animatable] supports value
 * change during an ongoing value change animation. When that happens, a new animation will
 * transition [Animatable] from its current value (i.e. value at the point of interruption) to the
 * new target. This ensures that the value change is *always* continuous using [animateTo]. If
 * [spring] animation (i.e. default animation) is used with [animateTo], the velocity change will
 * be guaranteed to be continuous as well.
 *
 * Unlike [AnimationState], [Animatable] ensures mutual exclusiveness on its animation. To
 * do so, when a new animation is started via [animateTo] (or [animateDecay]), any ongoing
 * animation job will be cancelled via a
 * [CancellationException][kotlinx.coroutines.CancellationException].
 *
 * [Animatable] also supports animating data types other than [Color], such as Floats and generic
 * types. See [androidx.compose.animation.core.Animatable] for other variants.
 *
 * @sample androidx.compose.animation.samples.AnimatableColor
 *
 * @param initialValue initial value of the [Animatable]
 */
fun Animatable(initialValue: Color): Animatable<Color, AnimationVector4D> =
    Animatable(initialValue, (Color.VectorConverter)(initialValue.colorSpace))