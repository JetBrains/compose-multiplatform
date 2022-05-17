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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Preview
@Composable
fun SwipeToDismissDemo() {
    Column {
        var index by remember { mutableStateOf(0) }
        Box(Modifier.requiredHeight(300.dp).fillMaxWidth()) {
            Box(
                Modifier.swipeToDismiss(index).align(Alignment.BottomCenter).requiredSize(150.dp)
                    .background(pastelColors[index])
            )
        }
        Text(
            "Swipe up to dismiss",
            fontSize = 30.sp,
            modifier = Modifier.padding(40.dp).align(Alignment.CenterHorizontally)
        )
        Button(
            onClick = {
                index = (index + 1) % pastelColors.size
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("New Card")
        }
    }
}

private fun Modifier.swipeToDismiss(index: Int): Modifier = composed {
    val animatedOffset = remember { Animatable(0f) }
    val height = remember { mutableStateOf(0) }
    LaunchedEffect(index) {
        animatedOffset.snapTo(0f)
    }
    this.pointerInput(Unit) {
        coroutineScope {
            while (true) {
                val pointerId = awaitPointerEventScope {
                    awaitFirstDown().id
                }
                height.value = size.height
                val velocityTracker = VelocityTracker()
                awaitPointerEventScope {
                    verticalDrag(pointerId) {
                        launch {
                            animatedOffset.snapTo(
                                animatedOffset.value + it.positionChange().y
                            )
                        }
                        velocityTracker.addPosition(
                            it.uptimeMillis,
                            it.position
                        )
                    }
                }
                val velocity = velocityTracker.calculateVelocity().y
                launch {
                    // Either fling out of the sight, or snap back
                    val decay = splineBasedDecay<Float>(this@pointerInput)
                    if (decay.calculateTargetValue(
                            animatedOffset.value,
                            velocity
                        ) >= -size.height
                    ) {
                        // Not enough velocity to be dismissed
                        animatedOffset.animateTo(0f, initialVelocity = velocity)
                    } else {
                        animatedOffset.updateBounds(
                            lowerBound = -size.height.toFloat()
                        )
                        animatedOffset.animateDecay(velocity, decay)
                    }
                }
            }
        }
    }.offset { IntOffset(0, animatedOffset.value.roundToInt()) }
        .graphicsLayer(alpha = calculateAlpha(animatedOffset.value, height.value))
}

private fun calculateAlpha(offset: Float, size: Int): Float {
    if (size <= 0) return 1f
    val alpha = (offset + size) / size
    return alpha.coerceIn(0f, 1f)
}

internal val pastelColors = listOf(
    Color(0xFFffd7d7),
    Color(0xFFffe9d6),
    Color(0xFFfffbd0),
    Color(0xFFe3ffd9),
    Color(0xFFd0fff8)
)
