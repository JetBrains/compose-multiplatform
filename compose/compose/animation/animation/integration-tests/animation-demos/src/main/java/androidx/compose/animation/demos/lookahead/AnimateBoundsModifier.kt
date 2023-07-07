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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LookaheadLayoutScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

context(LookaheadLayoutScope)
fun Modifier.animateBounds(
    modifier: Modifier,
    sizeAnimationSpec: FiniteAnimationSpec<IntSize> = spring(
        Spring.DampingRatioNoBouncy,
        Spring.StiffnessMediumLow
    ),
    positionAnimationSpec: FiniteAnimationSpec<IntOffset> = spring(
        Spring.DampingRatioNoBouncy,
        Spring.StiffnessMediumLow
    ),
) = composed {
    val coroutineScope = rememberCoroutineScope()
    var placementOffset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }

    val offsetAnimation = remember {
        DeferredAnimation(coroutineScope, IntOffset.VectorConverter)
    }
    val sizeAnimation = remember {
        DeferredAnimation(coroutineScope, IntSize.VectorConverter)
    }
    val outerSizeAnimation = remember {
        DeferredAnimation(coroutineScope, IntSize.VectorConverter)
    }
    // The measure logic in `intermediateLayout` is skipped in the lookahead pass, as
    // intermediateLayout is expected to produce intermediate stages of a layout transform.
    // When the measure block is invoked after lookahead pass, the lookahead size of the
    // child will be accessible as a parameter to the measure block.
    this
        .intermediateLayout { measurable, constraints, lookaheadSize ->
            outerSizeAnimation.updateTarget(lookaheadSize, sizeAnimationSpec)
            val (w, h) = outerSizeAnimation.value ?: lookaheadSize
            measurable
                .measure(constraints)
                .run {
                    layout(w, h) {
                        place(0, 0)
                    }
                }
        }
        .then(modifier)
        .onPlaced { lookaheadScopeCoordinates, layoutCoordinates ->
            // This block of code has the LookaheadCoordinates of the LookaheadLayout
            // as the first parameter, and the coordinates of this modifier as the second
            // parameter.

            // localLookaheadPositionOf returns the *target* position of this
            // modifier in the LookaheadLayout's local coordinates.
            val targetOffset = lookaheadScopeCoordinates
                .localLookaheadPositionOf(
                    layoutCoordinates
                )
                .round()
            offsetAnimation.updateTarget(targetOffset, positionAnimationSpec)

            // localPositionOf returns the *current* position of this
            // modifier in the LookaheadLayout's local coordinates.
            placementOffset = lookaheadScopeCoordinates
                .localPositionOf(
                    layoutCoordinates, Offset.Zero
                )
                .round()
        }
        .intermediateLayout { measurable, _, lookaheadSize ->
            // When layout changes, the lookahead pass will calculate a new final size for the
            // child modifier. This lookahead size can be used to animate the size
            // change, such that the animation starts from the current size and gradually
            // change towards `lookaheadSize`.
            sizeAnimation.updateTarget(lookaheadSize, sizeAnimationSpec)
            // Reads the animation size if the animation is set up. Otherwise (i.e. first
            // frame), use the lookahead size without animation.
            val (width, height) = sizeAnimation.value ?: lookaheadSize
            // Creates a fixed set of constraints using the animated size
            val animatedConstraints = Constraints.fixed(width, height)
            // Measure child/children with animated constraints.
            val placeable = measurable.measure(animatedConstraints)
            layout(placeable.width, placeable.height) {
                // offsetAnimation will animate the target position whenever it changes.
                // In order to place the child at the animated position, we need to offset
                // the child based on the target and current position in LookaheadLayout.
                val (x, y) = offsetAnimation.value?.let { it - placementOffset }
                // If offsetAnimation has not been set up yet (i.e. in the first frame),
                // skip the animation
                    ?: (offsetAnimation.target!! - placementOffset)
                placeable.place(x, y)
            }
        }
}

// Experimenting with a way to initialize animation during measurement && only take the last target
// change in a frame (if the target was changed multiple times in the same frame) as the
// animation target.
internal class DeferredAnimation<T, V : AnimationVector>(
    coroutineScope: CoroutineScope,
    vectorConverter: TwoWayConverter<T, V>
) {
    val value: T?
        get() = animatable?.value ?: target
    var target: T? by mutableStateOf(null)
        private set
    private var animationSpec: FiniteAnimationSpec<T> = spring()
    private var animatable: Animatable<T, V>? = null

    init {
        coroutineScope.launch {
            snapshotFlow { target }.collect { target ->
                if (target != null && target != animatable?.targetValue) {
                    animatable?.run {
                        launch { animateTo(target, animationSpec) }
                    } ?: Animatable(target, vectorConverter).let {
                        animatable = it
                    }
                }
            }
        }
    }

    fun updateTarget(targetValue: T, animationSpec: FiniteAnimationSpec<T>) {
        target = targetValue
        this.animationSpec = animationSpec
    }
}