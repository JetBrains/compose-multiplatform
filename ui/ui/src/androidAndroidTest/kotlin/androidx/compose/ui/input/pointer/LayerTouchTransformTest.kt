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

package androidx.compose.ui.input.pointer

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.TransformOrigin
import androidx.compose.ui.drawBehind
import androidx.compose.ui.drawLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.pressIndicatorGestureFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.down
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performGesture
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class LayerTouchTransformTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testTransformTouchEventConsumed() {
        val testTag = "transformedComposable"
        var latch: CountDownLatch? = null
        rule.setContent {
            val pressed = remember { mutableStateOf(false) }
            val onStart: (Offset) -> Unit = {
                pressed.value = true
            }

            val onStop = {
                pressed.value = false
            }

            val color = if (pressed.value) {
                Color.Red
            } else {
                Color.Blue
            }

            val background = Modifier.drawBehind {
                drawRect(Color.Gray)
            }

            val latchDrawModifier = Modifier.drawBehind { latch?.countDown() }

            val containerDp = (200.0f / DensityAmbient.current.density).dp
            val boxDp = (50.0f / DensityAmbient.current.density).dp

            val offsetX = (270.0f / DensityAmbient.current.density).dp
            val offsetY = (120.0f / DensityAmbient.current.density).dp
            Box(Modifier.testTag(testTag)) {
                SimpleLayout(
                    modifier = Modifier.fillMaxSize().offset(offsetX, offsetY)
                ) {
                    SimpleLayout(modifier = background.then(Modifier.preferredSize(containerDp))) {
                        SimpleLayout(
                            modifier = Modifier
                                .drawLayer(
                                    translationX = 50.0f,
                                    translationY = 30.0f,
                                    rotationZ = 45.0f,
                                    scaleX = 2.0f,
                                    scaleY = 0.5f,
                                    transformOrigin = TransformOrigin(1.0f, 1.0f)
                                ).drawBehind {
                                    drawRect(color)
                                }
                                .then(latchDrawModifier)
                                .preferredSize(boxDp)
                                .pressIndicatorGestureFilter(onStart, onStop, onStop)
                        )
                    }
                }
            }
        }

        // Touch position outside the bounds of the target composable
        // however, after transformations, this point will be within
        // its bounds

        val mappedPosition = Offset(342.0f, 168.0f)
        val node = rule.onNodeWithTag(testTag).performGesture { down(mappedPosition) }

        latch = CountDownLatch(1).apply {
            await(5, TimeUnit.SECONDS)
        }

        node.captureToBitmap().apply {
            Assert.assertEquals(
                Color.Red.toArgb(),
                getPixel(
                    mappedPosition.x.toInt(),
                    mappedPosition.y.toInt()
                )
            )
        }
    }
}

@Composable
fun SimpleLayout(modifier: Modifier, children: @Composable () -> Unit = emptyContent()) {
    Layout(
        children,
        modifier
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }
        var containerWidth = constraints.minWidth
        var containerHeight = constraints.minHeight
        placeables.forEach {
            containerWidth = max(containerWidth, it.width)
            containerHeight = max(containerHeight, it.height)
        }
        layout(containerWidth, containerHeight) {
            placeables.forEach {
                it.placeRelative(0, 0)
            }
        }
    }
}