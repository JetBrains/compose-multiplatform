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

package androidx.compose.animation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Bounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

private val defaultAnimation = spring<Float>()

/**
 * Fire-and-forget animation function for [Float]. This Composable function is overloaded for
 * different parameter types such as [Dp], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight whe [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateFloatAsState] returns a [State] object. The value of the state object will continuously
 * be updated by the animation until the animation finishes.
 *
 * Note, [animateFloatAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [animatedFloat][androidx.compose.animation.animatedFloat] for cancelable
 * animations.
 *
 * @sample androidx.compose.animation.core.samples.AlphaAnimationSample
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. [spring]
 *                      will be used by default.
 * @param visibilityThreshold An optional threshold for deciding when the animation value is
 *                            considered close enough to the targetValue.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 * @return A [State] object, the value of which is updated by animation.
 */
@Composable
fun animateFloatAsState(
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = defaultAnimation,
    visibilityThreshold: Float = 0.01f,
    finishedListener: ((Float) -> Unit)? = null
): State<Float> {
    val resolvedAnimSpec =
        if (animationSpec === defaultAnimation) {
            remember(visibilityThreshold) { spring(visibilityThreshold = visibilityThreshold) }
        } else {
            animationSpec
        }
    val animationState: AnimationState<Float, AnimationVector1D> = remember {
        AnimationState(targetValue)
    }

    val currentEndListener by rememberUpdatedState(finishedListener)
    LaunchedEffect(targetValue, animationSpec) {
        animationState.animateTo(
            targetValue,
            resolvedAnimSpec,
            // If the previous animation was interrupted (i.e. not finished), make it sequential.
            !animationState.isFinished
        )
        currentEndListener?.invoke(animationState.value)
    }
    return animationState
}

/**
 * Fire-and-forget animation function for [Dp]. This Composable function is overloaded for
 * different parameter types such as [Float], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight whe [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateDpAsState] returns a [State] object. The value of the state object will continuously be
 * updated by the animation until the animation finishes.
 *
 * Note, [animateDpAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [animatedValue][androidx.compose.animation.animatedValue] for cancelable
 * animations.
 *
 * @sample androidx.compose.animation.core.samples.DpAnimationSample
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 * @return A [State] object, the value of which is updated by animation.
 */
@Composable
fun animateDpAsState(
    targetValue: Dp,
    animationSpec: AnimationSpec<Dp> = dpDefaultSpring,
    finishedListener: ((Dp) -> Unit)? = null
): State<Dp> {
    return animateValueAsState(
        targetValue,
        Dp.VectorConverter,
        animationSpec,
        finishedListener = finishedListener
    )
}

private val dpDefaultSpring = spring<Dp>(visibilityThreshold = Dp.VisibilityThreshold)

/**
 * Fire-and-forget animation function for [Size]. This Composable function is overloaded for
 * different parameter types such as [Dp], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight whe [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateSizeAsState] returns a [State] object. The value of the state object will continuously be
 * updated by the animation until the animation finishes.
 *
 * Note, [animateSizeAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [animatedValue][androidx.compose.animation.animatedValue] for cancelable
 * animations.
 *
 *     val size: Size by animateSizeAsState(
 *         if (selected) Size(20f, 20f) else Size(10f, 10f))
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 * @return A [State] object, the value of which is updated by animation.
 */
@Composable
fun animateSizeAsState(
    targetValue: Size,
    animationSpec: AnimationSpec<Size> = sizeDefaultSpring,
    finishedListener: ((Size) -> Unit)? = null
): State<Size> {
    return animateValueAsState(
        targetValue,
        Size.VectorConverter,
        animationSpec,
        finishedListener = finishedListener
    )
}

private val sizeDefaultSpring = spring(visibilityThreshold = Size.VisibilityThreshold)

/**
 * Fire-and-forget animation function for [Bounds]. This Composable function is overloaded for
 * different parameter types such as [Dp], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight whe [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateBoundsAsState] returns a [State] object. The value of the state object will
 * continuously be
 * updated by the animation until the animation finishes.
 *
 * Note, [animateBoundsAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [animatedValue][androidx.compose.animation.animatedValue] for cancelable
 * animations.
 *
 *    val bounds: Bounds by animateBoundsAsState(
 *        if (collapsed) Bounds(0.dp, 0.dp, 10.dp, 20.dp) else Bounds(0.dp, 0.dp, 100.dp, 200.dp))
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 * @return A [State] object, the value of which is updated by animation.
 */
@Composable
fun animateBoundsAsState(
    targetValue: Bounds,
    animationSpec: AnimationSpec<Bounds> = boundsDefaultSpring,
    finishedListener: ((Bounds) -> Unit)? = null
): State<Bounds> {
    return animateValueAsState(
        targetValue,
        Bounds.VectorConverter,
        animationSpec,
        finishedListener = finishedListener
    )
}

private val boundsDefaultSpring = spring(visibilityThreshold = Bounds.VisibilityThreshold)

/**
 * Fire-and-forget animation function for [Offset]. This Composable function is overloaded for
 * different parameter types such as [Dp], [Color][androidx.compose.ui.graphics.Color], [Float],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight whe [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateOffsetAsState] returns a [State] object. The value of the state object will
 * continuously be updated by the animation until the animation finishes.
 *
 * Note, [animateOffsetAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [animatedValue][androidx.compose.animation.animatedValue] for cancelable
 * animations.
 *
 * @sample androidx.compose.animation.core.samples.AnimateOffsetSample
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 * @return A [State] object, the value of which is updated by animation.
 */
@Composable
fun animateOffsetAsState(
    targetValue: Offset,
    animationSpec: AnimationSpec<Offset> = offsetDefaultSpring,
    finishedListener: ((Offset) -> Unit)? = null
): State<Offset> {
    return animateValueAsState(
        targetValue, Offset.VectorConverter, animationSpec, finishedListener = finishedListener
    )
}

private val offsetDefaultSpring = spring(visibilityThreshold = Offset.VisibilityThreshold)

/**
 * Fire-and-forget animation function for [Rect]. This Composable function is overloaded for
 * different parameter types such as [Dp], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight whe [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateRectAsState] returns a [State] object. The value of the state object will continuously be
 * updated by the animation until the animation finishes.
 *
 * Note, [animateRectAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [animatedValue][androidx.compose.animation.animatedValue] for cancelable
 * animations.
 *
 *    val bounds: Rect by animateRectAsState(
 *        if (enabled) Rect(0f, 0f, 100f, 100f) else Rect(8f, 8f, 80f, 80f))
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 * @return A [State] object, the value of which is updated by animation.
 */
@Composable
fun animateRectAsState(
    targetValue: Rect,
    animationSpec: AnimationSpec<Rect> = rectDefaultSpring,
    finishedListener: ((Rect) -> Unit)? = null
): State<Rect> {
    return animateValueAsState(
        targetValue, Rect.VectorConverter, animationSpec, finishedListener = finishedListener
    )
}

private val rectDefaultSpring = spring(visibilityThreshold = Rect.VisibilityThreshold)

/**
 * Fire-and-forget animation function for [Int]. This Composable function is overloaded for
 * different parameter types such as [Dp], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight whe [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateIntAsState] returns a [State] object. The value of the state object will continuously be
 * updated by the animation until the animation finishes.
 *
 * Note, [animateIntAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [animatedValue][androidx.compose.animation.animatedValue] for cancelable
 * animations.
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 * @return A [State] object, the value of which is updated by animation.
 */
@Composable
fun animateIntAsState(
    targetValue: Int,
    animationSpec: AnimationSpec<Int> = intDefaultSpring,
    finishedListener: ((Int) -> Unit)? = null
): State<Int> {
    return animateValueAsState(
        targetValue, Int.VectorConverter, animationSpec, finishedListener = finishedListener
    )
}

private val intDefaultSpring = spring(visibilityThreshold = Int.VisibilityThreshold)

/**
 * Fire-and-forget animation function for [IntOffset]. This Composable function is overloaded for
 * different parameter types such as [Dp], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight whe [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateIntOffsetAsState] returns a [State] object. The value of the state object will
 * continuously be updated by the animation until the animation finishes.
 *
 * Note, [animateIntOffsetAsState] cannot be canceled/stopped without removing this composable
 * function from the tree. See [animatedValue][androidx.compose.animation.animatedValue] for
 * cancelable animations.
 *
 * @sample androidx.compose.animation.core.samples.AnimateOffsetSample
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 * @return A [State] object, the value of which is updated by animation.
 */
@Composable
fun animateIntOffsetAsState(
    targetValue: IntOffset,
    animationSpec: AnimationSpec<IntOffset> = intOffsetDefaultSpring,
    finishedListener: ((IntOffset) -> Unit)? = null
): State<IntOffset> {
    return animateValueAsState(
        targetValue, IntOffset.VectorConverter, animationSpec, finishedListener = finishedListener
    )
}

private val intOffsetDefaultSpring = spring(visibilityThreshold = IntOffset.VisibilityThreshold)

/**
 * Fire-and-forget animation function for [IntSize]. This Composable function is overloaded for
 * different parameter types such as [Dp], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight whe [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateIntSizeAsState] returns a [State] object. The value of the state object will continuously
 * be updated by the animation until the animation finishes.
 *
 * Note, [animateIntSizeAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [animatedValue][androidx.compose.animation.animatedValue] for cancelable
 * animations.
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 * @return A [State] object, the value of which is updated by animation.
 */
@Composable
fun animateIntSizeAsState(
    targetValue: IntSize,
    animationSpec: AnimationSpec<IntSize> = intSizeDefaultSpring,
    finishedListener: ((IntSize) -> Unit)? = null
): State<IntSize> {
    return animateValueAsState(
        targetValue, IntSize.VectorConverter, animationSpec, finishedListener = finishedListener
    )
}

private val intSizeDefaultSpring = spring(visibilityThreshold = IntSize.VisibilityThreshold)

/**
 * Fire-and-forget animation function for any value. This Composable function is overloaded for
 * different parameter types such as [Dp], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight whe [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateValueAsState] returns a [State] object. The value of the state object will continuously be
 * updated by the animation until the animation finishes.
 *
 * Note, [animateValueAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [animatedValue][androidx.compose.animation.animatedValue] for cancelable
 * animations.
 *
 * @sample androidx.compose.animation.core.samples.ArbitraryValueTypeTransitionSample
 *
 *     data class MySize(val width: Dp, val height: Dp)
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param visibilityThreshold An optional threshold to define when the animation value can be
 *                            considered close enough to the targetValue to end the animation.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 * @return A [State] object, the value of which is updated by animation.
 */
@Composable
fun <T, V : AnimationVector> animateValueAsState(
    targetValue: T,
    typeConverter: TwoWayConverter<T, V>,
    animationSpec: AnimationSpec<T> = remember {
        spring(visibilityThreshold = visibilityThreshold)
    },
    visibilityThreshold: T? = null,
    finishedListener: ((T) -> Unit)? = null
): State<T> {
    val animationState: AnimationState<T, V> = remember(typeConverter) {
        AnimationState(typeConverter, targetValue)
    }

    val listener by rememberUpdatedState(finishedListener)
    LaunchedEffect(targetValue, animationSpec) {
        animationState.animateTo(
            targetValue,
            animationSpec,
            // If the previous animation was interrupted (i.e. not finished), make it sequential.
            !animationState.isFinished
        )
        listener?.invoke(animationState.value)
    }
    return animationState
}