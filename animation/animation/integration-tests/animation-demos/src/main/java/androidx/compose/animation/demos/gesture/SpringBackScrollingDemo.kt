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

package androidx.compose.animation.demos.gesture

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.isFinished
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Preview
@Composable
fun SpringBackScrollingDemo() {
    Column(Modifier.fillMaxHeight()) {
        Text(
            "<== Scroll horizontally ==>",
            modifier = Modifier.padding(40.dp),
            fontSize = 20.sp
        )

        var scrollPosition by remember { mutableStateOf(0f) }
        val itemWidth = remember { mutableStateOf(0f) }
        val mutatorMutex = remember { MutatorMutex() }
        var animation by remember { mutableStateOf(AnimationState(scrollPosition)) }

        val gesture = Modifier.pointerInput(Unit) {
            coroutineScope {
                while (true) {
                    val pointerId = awaitPointerEventScope {
                        awaitFirstDown().id
                    }
                    val velocityTracker = VelocityTracker()
                    mutatorMutex.mutate(MutatePriority.UserInput) {
                        awaitPointerEventScope {
                            horizontalDrag(pointerId) {
                                scrollPosition += it.positionChange().x
                                velocityTracker.addPosition(
                                    it.uptimeMillis,
                                    it.position
                                )
                            }
                        }
                    }
                    val velocity = velocityTracker.calculateVelocity().x
                    // Now finger lifted, get fling going
                    launch {
                        mutatorMutex.mutate {
                            animation = AnimationState(scrollPosition, velocity)
                            val target = exponentialDecay<Float>()
                                .calculateTargetValue(scrollPosition, velocity)
                            val springBackTarget: Float = calculateSpringBackTarget(
                                target,
                                velocity,
                                itemWidth.value
                            )

                            animation.animateDecay(exponentialDecay()) {
                                scrollPosition = this.value
                                // Spring back as soon as the target position is crossed.
                                if ((this.velocity > 0 && value > springBackTarget) ||
                                    (this.velocity < 0 && value < springBackTarget)
                                ) {
                                    cancelAnimation()
                                }
                            }

                            // The previous animation is either finished or interrupted (via
                            // cancelAnimation(). If interrupted, spring back.
                            if (!animation.isFinished) {
                                animation.animateTo(
                                    springBackTarget,
                                    SpringSpec(
                                        dampingRatio = 0.8f,
                                        stiffness = 200f
                                    ),
                                    sequentialAnimation = true
                                ) {
                                    scrollPosition = this.value
                                }
                            }
                        }
                    }
                }
            }
        }
        Canvas(gesture.fillMaxWidth().height(400.dp)) {
            itemWidth.value = size.width / 2f
            if (DEBUG) {
                println(
                    "Anim, Spring back scrolling, redrawing with new" +
                        " scroll value: $scrollPosition"
                )
            }
            drawRects(scrollPosition)
        }
    }
}

private fun calculateSpringBackTarget(target: Float, velocity: Float, itemWidth: Float): Float {
    var rem = target % itemWidth
    if (velocity < 0) {
        if (rem > 0) {
            rem -= itemWidth
        }
    } else {
        if (rem < 0) {
            rem += itemWidth
        }
    }
    return target - rem
}

private fun DrawScope.drawRects(animScroll: Float) {
    val width = size.width / 2f
    val scroll = animScroll + width / 2
    var startingPos = scroll % width
    if (startingPos > 0) {
        startingPos -= width
    }
    var startingColorIndex = ((scroll - startingPos) / width).roundToInt().rem(pastelColors.size)
    if (startingColorIndex < 0) {
        startingColorIndex += pastelColors.size
    }

    val rectSize = Size(width - 20.0f, size.height)

    drawRect(
        pastelColors[startingColorIndex],
        topLeft = Offset(startingPos + 10, 0f),
        size = rectSize
    )

    drawRect(
        pastelColors[(startingColorIndex + pastelColors.size - 1) % pastelColors.size],
        topLeft = Offset(startingPos + width + 10, 0f),
        size = rectSize
    )

    drawRect(
        pastelColors[(startingColorIndex + pastelColors.size - 2) % pastelColors.size],
        topLeft = Offset(startingPos + width * 2 + 10, 0.0f),
        size = rectSize
    )
}

private val colors = listOf(
    Color(0xFFdaf8e3),
    Color(0xFF97ebdb),
    Color(0xFF00c2c7),
    Color(0xFF0086ad),
    Color(0xFF005582),
    Color(0xFF0086ad),
    Color(0xFF00c2c7),
    Color(0xFF97ebdb)
)
