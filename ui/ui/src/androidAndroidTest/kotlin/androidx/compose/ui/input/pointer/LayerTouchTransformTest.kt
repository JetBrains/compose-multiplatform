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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class LayerTouchTransformTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testTransformTouchEventConsumed() {
        val testTag = "transformedComposable"
        var latch = CountDownLatch(1)
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

            val latchDrawModifier = Modifier.drawBehind { latch.countDown() }

            with(LocalDensity.current) {
                val containerDp = 200f.toDp()
                val boxDp = 50f.toDp()

                val offsetX = 270f.toDp()
                val offsetY = 120f.toDp()
                Box(Modifier.testTag(testTag)) {
                    SimpleLayout(
                        modifier = Modifier.fillMaxSize().offset(offsetX, offsetY)
                    ) {
                        SimpleLayout(modifier = background.then(Modifier.size(containerDp))) {
                            SimpleLayout(
                                modifier = Modifier
                                    .graphicsLayer(
                                        scaleX = 2.0f,
                                        scaleY = 0.5f,
                                        translationX = 50.0f,
                                        translationY = 30.0f,
                                        rotationZ = 45.0f,
                                        transformOrigin = TransformOrigin(1.0f, 1.0f)
                                    ).drawBehind {
                                        drawRect(color)
                                    }
                                    .then(latchDrawModifier)
                                    .size(boxDp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                onStart.invoke(it)
                                                val success = tryAwaitRelease()
                                                if (success) onStop.invoke() else onStop.invoke()
                                            }
                                        )
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        assertTrue(latch.await(5, TimeUnit.SECONDS))

        // Touch position outside the bounds of the target composable
        // however, after transformations, this point will be within
        // its bounds

        latch = CountDownLatch(1)
        val mappedPosition = Offset(342.0f, 168.0f)
        val node = rule.onNodeWithTag(testTag).performTouchInput { down(mappedPosition) }

        rule.waitForIdle()
        assertTrue(latch.await(5, TimeUnit.SECONDS))

        node.captureToImage().asAndroidBitmap().apply {
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
fun SimpleLayout(modifier: Modifier, content: @Composable () -> Unit = {}) {
    Layout(
        content,
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
