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

package androidx.compose.animation.demos

import androidx.compose.animation.animatedFloat
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.ExponentialDecay
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TargetAnimation
import androidx.compose.animation.core.fling
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.gesture.rawDragGestureFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sign

@Composable
fun SwipeToDismissDemo() {
    Column {
        SwipeToDismiss()
        Text(
            "Swipe up to dismiss",
            fontSize = 30.sp,
            modifier = Modifier.padding(40.dp)
        )
    }
}

private val height = 1600f
private val itemHeight = 1600f * 2 / 3f
private val padding = 10f

@Composable
private fun SwipeToDismiss() {
    val itemBottom = animatedFloat(height)
    val index = remember { mutableStateOf(0) }
    val itemWidth = remember { mutableStateOf(0f) }
    val isFlinging = remember { mutableStateOf(false) }
    val modifier = Modifier.rawDragGestureFilter(
        dragObserver = object : DragObserver {
            override fun onStart(downPosition: Offset) {
                itemBottom.setBounds(0f, height)
                if (isFlinging.value && itemBottom.targetValue < 100f) {
                    reset()
                }
            }

            private fun reset() {
                itemBottom.snapTo(height)
                index.value--
                if (index.value < 0) {
                    index.value += pastelColors.size
                }
            }

            override fun onDrag(dragDistance: Offset): Offset {
                itemBottom.snapTo(itemBottom.targetValue + dragDistance.y)
                return dragDistance
            }

            fun adjustTarget(velocity: Float): (Float) -> TargetAnimation? {
                return { target: Float ->
                    // The velocity is fast enough to fly off screen
                    if (target <= 0) {
                        null
                    } else {
                        val animation = SpringSpec<Float>(
                            dampingRatio = 0.8f, stiffness = 300f
                        )
                        val projectedTarget = target + sign(velocity) * 0.2f * height
                        if (projectedTarget < 0.6 * height) {
                            TargetAnimation(0f, animation)
                        } else {
                            TargetAnimation(height, animation)
                        }
                    }
                }
            }

            override fun onStop(velocity: Offset) {
                isFlinging.value = true
                itemBottom.fling(
                    velocity.y,
                    ExponentialDecay(3.0f),
                    adjustTarget(velocity.y),
                    onEnd = { endReason, final, _ ->
                        isFlinging.value = false
                        if (endReason != AnimationEndReason.Interrupted && final == 0f) {
                            reset()
                        }
                    }
                )
            }
        }
    )

    val heightDp = with(DensityAmbient.current) { height.toDp() }

    Canvas(
        modifier.fillMaxWidth()
            .preferredHeight(heightDp)
            .onGloballyPositioned { coordinates ->
                itemWidth.value = coordinates.size.width * 2 / 3f
            }
    ) {
        val progress = 1 - itemBottom.value / height
        // TODO: this progress can be used to drive state transitions
        val alpha = 1f - FastOutSlowInEasing(progress)
        val horizontalOffset = progress * itemWidth.value
        drawLeftItems(horizontalOffset, itemWidth.value, itemHeight, index.value)
        drawDismissingItem(itemBottom.value, itemWidth.value, itemHeight, index.value + 1, alpha)
    }
}

private fun DrawScope.drawLeftItems(
    horizontalOffset: Float,
    width: Float,
    height: Float,
    index: Int
) {
    val offset = Offset(center.x - width * 1.5f + horizontalOffset + padding, size.height - height)
    val rectSize = Size(width - (2 * padding), height)
    drawRect(pastelColors[index % pastelColors.size], offset, rectSize)

    if (offset.x >= 0) {
        // draw another item
        drawRect(
            pastelColors[(index - 1 + pastelColors.size) % pastelColors.size],
            offset - Offset(width, 0.0f),
            rectSize
        )
    }
}

private fun DrawScope.drawDismissingItem(
    bottom: Float,
    width: Float,
    height: Float,
    index: Int,
    alpha: Float
) = drawRect(
    pastelColors[index % pastelColors.size],
    topLeft = Offset(center.x - width / 2 + padding, bottom - height),
    size = Size(width - (2 * padding), height),
    alpha = alpha
)

internal val pastelColors = listOf(
    Color(0xFFffd7d7),
    Color(0xFFffe9d6),
    Color(0xFFfffbd0),
    Color(0xFFe3ffd9),
    Color(0xFFd0fff8)
)
