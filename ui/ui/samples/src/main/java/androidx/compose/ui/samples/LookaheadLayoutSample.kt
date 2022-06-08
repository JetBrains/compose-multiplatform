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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadLayout
import androidx.compose.ui.layout.LookaheadLayoutScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlinx.coroutines.launch

@Sampled
@Composable
fun LookaheadLayoutSample() {
    // Creates a custom modifier that animates the constraints and measure child/children with them.
    // It is built on top of `Modifier.intermediateLayout`, which allows access to the target size
    // of the layout. A resize animation will be created to animate to the target size. Fixed
    // constraints created based on the animation value will be used to measure child/children, so
    // that all the children gradually change their size to fit in the animated constraints.
    fun Modifier.animateConstraints(lookaheadScope: LookaheadLayoutScope) = composed {
        var sizeAnimation: Animatable<IntSize, AnimationVector2D>? by remember {
            mutableStateOf(null)
        }
        var targetSize: IntSize? by remember { mutableStateOf(null) }
        // Create a `LaunchEffect` to handle target size change. This avoids creating side effects
        // from measure/layout phase.
        LaunchedEffect(Unit) {
            snapshotFlow { targetSize }.collect { target ->
                if (target != null && target != sizeAnimation?.targetValue) {
                    sizeAnimation?.run {
                        launch { animateTo(target) }
                    } ?: Animatable(target, IntSize.VectorConverter).let {
                        sizeAnimation = it
                    }
                }
            }
        }
        with(lookaheadScope) {
            // The measure logic in `intermediateLayout` is skipped in the lookahead pass, as
            // intermediateLayout is expected to produce intermediate stages of a layout transform.
            // When the measure block is invoked after lookahead pass, the lookahead size of the
            // child will be accessible as a parameter to the measure block.
            this@composed.intermediateLayout { measurable, _, lookaheadSize ->
                // When layout changes, the lookahead pass will calculate a new final size for the
                // child modifier. This lookahead size can be used to animate the size
                // change, such that the animation starts from the current size and gradually
                // change towards `lookaheadSize`.
                targetSize = lookaheadSize
                // Reads the animation size if the animation is set up. Otherwise (i.e. first
                // frame), use the lookahead size without animation.
                val (width, height) = sizeAnimation?.value ?: lookaheadSize
                // Creates a fixed set of constraints using the animated size
                val animatedConstraints = Constraints.fixed(width, height)
                // Measure child/children with animated constraints.
                val placeable = measurable.measure(animatedConstraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }
    }

    LookaheadLayout(
        content = {
            var fullWidth by remember { mutableStateOf(false) }
            Row(
                (if (fullWidth) Modifier.fillMaxWidth() else Modifier.width(100.dp))
                    .height(200.dp)
                    // Use the custom modifier created above to animate the constraints passed
                    // to the child, and therefore resize children in an animation.
                    .animateConstraints(this@LookaheadLayout)
                    .clickable { fullWidth = !fullWidth }) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Red)
                )
                Box(
                    Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .background(Color.Yellow)
                )
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val maxWidth: Int = placeables.maxOf { it.width }
        val maxHeight = placeables.maxOf { it.height }
        // Position the children.
        layout(maxWidth, maxHeight) {
            placeables.forEach {
                it.place(0, 0)
            }
        }
    }
}

@Sampled
@Composable
fun LookaheadLayoutCoordinatesSample() {
    // Creates a custom modifier to animate the local position of the layout within the
    // LookaheadLayout, whenever there's a change in the layout.
    fun Modifier.animatePlacementInScope(lookaheadScope: LookaheadLayoutScope) = composed {
        var offsetAnimation: Animatable<IntOffset, AnimationVector2D>? by remember {
            mutableStateOf(
                null
            )
        }

        var placementOffset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }
        var targetOffset: IntOffset? by remember {
            mutableStateOf(null)
        }
        // Create a `LaunchEffect` to handle target size change. This avoids creating side effects
        // from measure/layout phase.
        LaunchedEffect(Unit) {
            snapshotFlow {
                targetOffset
            }.collect { target ->
                if (target != null && target != offsetAnimation?.targetValue) {
                    offsetAnimation?.run {
                        launch { animateTo(target) }
                    } ?: Animatable(target, IntOffset.VectorConverter).let {
                        offsetAnimation = it
                    }
                }
            }
        }
        with(lookaheadScope) {
            this@composed
                .onPlaced { lookaheadScopeCoordinates, layoutCoordinates ->
                    // This block of code has the LookaheadCoordinates of the LookaheadLayout
                    // as the first parameter, and the coordinates of this modifier as the second
                    // parameter.

                    // localLookaheadPositionOf returns the *target* position of this
                    // modifier in the LookaheadLayout's local coordinates.
                    targetOffset = lookaheadScopeCoordinates.localLookaheadPositionOf(
                        layoutCoordinates
                    ).round()
                    // localPositionOf returns the *current* position of this
                    // modifier in the LookaheadLayout's local coordinates.
                    placementOffset = lookaheadScopeCoordinates.localPositionOf(
                        layoutCoordinates, Offset.Zero
                    ).round()
                }
                // The measure logic in `intermediateLayout` is skipped in the lookahead pass, as
                // intermediateLayout is expected to produce intermediate stages of a layout
                // transform. When the measure block is invoked after lookahead pass, the lookahead
                // size of the child will be accessible as a parameter to the measure block.
                .intermediateLayout { measurable, constraints, _ ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        // offsetAnimation will animate the target position whenever it changes.
                        // In order to place the child at the animated position, we need to offset
                        // the child based on the target and current position in LookaheadLayout.
                        val (x, y) = offsetAnimation?.run { value - placementOffset }
                        // If offsetAnimation has not been set up yet (i.e. in the first frame),
                        // skip the animation
                            ?: (targetOffset!! - placementOffset)
                        placeable.place(x, y)
                    }
                }
        }
    }

    val colors = listOf(
        Color(0xffff6f69), Color(0xffffcc5c), Color(0xff264653), Color(0xff2a9d84)
    )

    // Creates movable content containing 4 boxes. They will be put either in a [Row] or in a
    // [Column] depending on the state
    val items = remember {
        movableContentWithReceiverOf<LookaheadLayoutScope> {
            colors.forEach { color ->
                Box(
                    Modifier
                        .padding(15.dp)
                        .size(100.dp, 80.dp)
                        .animatePlacementInScope(this)
                        .background(color, RoundedCornerShape(20))
                )
            }
        }
    }

    var isInColumn by remember { mutableStateOf(true) }
    LookaheadLayout(
        content = {
            // As the items get moved between Column and Row, their positions in LookaheadLayout
            // will change. The `animatePlacementInScope` modifier created above will
            // observe that final position change via `localLookaheadPositionOf`, and create
            // a position animation.
            if (isInColumn) {
                Column(Modifier.fillMaxSize()) {
                    items()
                }
            } else {
                Row { items() }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .clickable { isInColumn = !isInColumn }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val maxWidth: Int = placeables.maxOf { it.width }
        val maxHeight = placeables.maxOf { it.height }
        // Position the children.
        layout(maxWidth, maxHeight) {
            placeables.forEach {
                it.place(0, 0)
            }
        }
    }
}
