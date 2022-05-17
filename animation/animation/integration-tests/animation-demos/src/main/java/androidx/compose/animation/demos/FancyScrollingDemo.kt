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

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

const val DEBUG = false

@Preview
@Composable
fun FancyScrollingDemo() {
    Column {
        Text(
            "<== Scroll horizontally ==>",
            fontSize = 20.sp,
            modifier = Modifier.padding(40.dp)
        )
        val animScroll = remember { Animatable(0f) }
        val itemWidth = remember { mutableStateOf(0f) }
        val scope = rememberCoroutineScope()
        val modifier = Modifier.draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { delta ->
                // Snap to new drag position
                scope.launch {
                    animScroll.snapTo(animScroll.value + delta)
                }
            },

            onDragStopped = { velocity: Float ->

                // Uses default decay animation to calculate where the fling will settle,
                // and adjust that position as needed. The target animation will be used for
                // animating to the adjusted target.
                scope.launch {
                    val decay = exponentialDecay<Float>()
                    val target = decay.calculateTargetValue(animScroll.value, velocity)
                    // Adjust the target position to center align the item
                    var rem = target % itemWidth.value
                    if (rem < 0) {
                        rem += itemWidth.value
                    }
                    animScroll.animateTo(
                        targetValue = target - rem,
                        initialVelocity = velocity,
                        animationSpec = SpringSpec(dampingRatio = 2.0f, stiffness = 100f)
                    )
                }
            }
        )

        Canvas(modifier.fillMaxWidth().height(400.dp)) {
            val width = size.width / 2f
            val scroll = animScroll.value + width / 2
            itemWidth.value = width
            if (DEBUG) {
                Log.w(
                    "Anim",
                    "Drawing items with updated" +
                        " Scroll: ${animScroll.value}"
                )
            }
            drawItems(scroll, width, size.height)
        }
    }
}

private fun DrawScope.drawItems(
    scrollPosition: Float,
    width: Float,
    height: Float
) {
    var startingPos = scrollPosition % width
    if (startingPos > 0) {
        startingPos -= width
    }
    var startingColorIndex =
        ((scrollPosition - startingPos) / width).roundToInt().rem(pastelColors.size)
    if (startingColorIndex < 0) {
        startingColorIndex += pastelColors.size
    }

    val size = Size(width - 20, height)
    drawRect(
        pastelColors[startingColorIndex],
        topLeft = Offset(startingPos + 10, 0f),
        size = size
    )

    drawRect(
        pastelColors[(startingColorIndex + pastelColors.size - 1) % pastelColors.size],
        topLeft = Offset(startingPos + width + 10, 0.0f),
        size = size
    )

    drawRect(
        pastelColors[(startingColorIndex + pastelColors.size - 2) % pastelColors.size],
        topLeft = Offset(startingPos + width * 2 + 10, 0.0f),
        size = size
    )
}

private val colors = listOf(
    Color(0xFFffd9d9),
    Color(0xFFffa3a3),
    Color(0xFFff7373),
    Color(0xFFff3b3b),
    Color(0xFFce0000),
    Color(0xFFff3b3b),
    Color(0xFFff7373),
    Color(0xFFffa3a3)
)
