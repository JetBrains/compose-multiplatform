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

package androidx.compose.animation.demos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Preview
@Composable
fun FlingGame() {
    Box(Modifier.fillMaxSize()) {
        Text("Throw me around, see what happens", Modifier.align(Alignment.Center))
        val anim = remember { Animatable(Offset(100f, 100f), Offset.VectorConverter) }
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                coroutineScope {
                    while (true) {
                        val pointerId = awaitPointerEventScope {
                            awaitFirstDown().run {
                                launch {
                                    anim.snapTo(position)
                                }
                                id
                            }
                        }
                        val velocityTracker = VelocityTracker()
                        awaitPointerEventScope {
                            drag(pointerId) {
                                launch {
                                    anim.snapTo(anim.value + it.positionChange())
                                }
                                velocityTracker.addPosition(
                                    it.uptimeMillis,
                                    it.position
                                )
                            }
                        }
                        val (x, y) = velocityTracker.calculateVelocity()
                        anim.updateBounds(
                            Offset(100f, 100f),
                            Offset(size.width.toFloat() - 100f, size.height.toFloat() - 100f)
                        )
                        launch {
                            var startVelocity = Offset(x, y)
                            do {
                                val result = anim.animateDecay(startVelocity, exponentialDecay())
                                startVelocity = result.endState.velocity

                                with(anim) {
                                    if (value.x == upperBound?.x || value.x == lowerBound?.x) {
                                        // x dimension hits bounds
                                        startVelocity = startVelocity.copy(x = -startVelocity.x)
                                    }
                                    if (value.y == upperBound?.y || value.y == lowerBound?.y) {
                                        // y dimension hits bounds
                                        startVelocity = startVelocity.copy(y = -startVelocity.y)
                                    }
                                }
                            } while (result.endReason == AnimationEndReason.BoundReached)
                        }
                    }
                }
            }.drawWithContent {
                drawCircle(Color(0xff3c1361), 100f, anim.value)
            }
        )
    }
}
