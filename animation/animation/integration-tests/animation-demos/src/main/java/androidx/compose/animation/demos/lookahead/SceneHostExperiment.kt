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
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LookaheadLayout
import androidx.compose.ui.layout.LookaheadLayoutScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

@Composable
fun SceneHost(modifier: Modifier = Modifier, content: @Composable SceneScope.() -> Unit) {
    LookaheadLayout(
        modifier = modifier,
        content = {
            val sceneScope = remember { SceneScope(this) }
            sceneScope.content()
        },
        measurePolicy = { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val maxWidth: Int = placeables.maxOf { it.width }
            val maxHeight = placeables.maxOf { it.height }
            // Position the children.
            layout(maxWidth, maxHeight) {
                placeables.forEach {
                    it.place(0, 0)
                }
            }
        })
}

private const val debugSharedElement = false

class SceneScope internal constructor(
    lookaheadLayoutScope: LookaheadLayoutScope
) : LookaheadLayoutScope by lookaheadLayoutScope {
    fun Modifier.sharedElement(): Modifier = composed {
        var offsetAnimation: Animatable<IntOffset, AnimationVector2D>?
            by remember { mutableStateOf(null) }
        var sizeAnimation: Animatable<IntSize, AnimationVector2D>?
            by remember { mutableStateOf(null) }

        var placementOffset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }
        var targetOffset: IntOffset? by remember {
            mutableStateOf(null)
        }
        var targetSize: IntSize? by remember {
            mutableStateOf(null)
        }

        LaunchedEffect(Unit) {
            launch {
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
            launch {
                snapshotFlow {
                    targetSize
                }.collect { target ->
                    if (target != null && target != sizeAnimation?.targetValue) {
                        sizeAnimation?.run {
                            launch { animateTo(target) }
                        } ?: Animatable(target, IntSize.VectorConverter).let {
                            sizeAnimation = it
                        }
                    }
                }
            }
        }
        this
            .onPlaced { lookaheadScopeCoordinates, layoutCoordinates ->
                targetOffset =
                    lookaheadScopeCoordinates.localLookaheadPositionOf(layoutCoordinates).round()
                placementOffset = lookaheadScopeCoordinates.localPositionOf(
                    layoutCoordinates, Offset.Zero
                ).round()
            }
            .drawBehind {
                if (debugSharedElement) {
                    drawRect(
                        color = Color.Black,
                        style = Stroke(2f),
                        topLeft = (targetOffset!! - placementOffset).toOffset(),
                        size = targetSize!!.toSize()
                    )
                }
            }
            .intermediateLayout { measurable, _, lookaheadSize ->
                targetSize = lookaheadSize
                val (width, height) = sizeAnimation?.value ?: lookaheadSize
                val animatedConstraints = Constraints(
                    minWidth = width,
                    maxWidth = width,
                    minHeight = height,
                    maxHeight = height
                )
                val placeable = measurable.measure(animatedConstraints)
                layout(placeable.width, placeable.height) {
                    val (x, y) = offsetAnimation?.run {
                        value - placementOffset
                    } ?: (targetOffset!! - placementOffset)
                    placeable.place(x, y)
                }
            }
    }

    fun Modifier.animateSizeAndSkipToFinalLayout() = composed {
        var sizeAnimation: Animatable<IntSize, AnimationVector2D>? by remember {
            mutableStateOf(null)
        }
        var targetSize: IntSize? by remember { mutableStateOf(null) }
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
        this
            .drawBehind {
                if (debugSharedElement) {
                    drawRect(
                        color = Color.Black,
                        style = Stroke(2f),
                        topLeft = Offset.Zero,
                        size = targetSize!!.toSize()
                    )
                }
            }
            .intermediateLayout { measurable, constraints, lookaheadSize ->
                targetSize = lookaheadSize
                val (width, height) = sizeAnimation?.value ?: lookaheadSize
                val placeable = measurable.measure(
                    Constraints.fixed(lookaheadSize.width, lookaheadSize.height)
                )
                // Make sure the content is aligned to topStart
                val wrapperWidth = width.coerceIn(constraints.minWidth, constraints.maxWidth)
                val wrapperHeight = height.coerceIn(constraints.minHeight, constraints.maxHeight)
                layout(width, height) {
                    placeable.place(-(wrapperWidth - width) / 2, -(wrapperHeight - height) / 2)
                }
            }
    }
}