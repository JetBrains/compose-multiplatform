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

package androidx.compose.ui.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.isLinux
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.junit4.DesktopScreenshotTestRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(InternalTestApi::class)
class GraphicsLayerTest {
    @get:Rule
    val screenshotRule = DesktopScreenshotTestRule("compose/ui/ui-desktop/platform")

    @Test
    fun scale() {
        val snapshot = renderComposeScene(width = 40, height = 40) {
            Box(
                Modifier
                    .graphicsLayer(
                        scaleX = 2f,
                        scaleY = 0.5f,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )
                    .requiredSize(10f.dp, 10f.dp).background(Color.Red)
            )
            Box(
                Modifier
                    .graphicsLayer(
                        translationX = 10f,
                        translationY = 20f,
                        scaleX = 2f,
                        scaleY = 0.5f
                    )
                    .requiredSize(10f.dp, 10f.dp).background(Color.Blue)
            )
        }
        screenshotRule.write(snapshot)
    }

    @Composable
    fun testRotationBoxes(
        rotationX: Float = 0f,
        rotationY: Float = 0f,
        rotationZ: Float = 0f
    ) {
        val size = DpSize(10.dp, 10.dp)
        val backgroundBrush =
            Brush.verticalGradient(
                colors = listOf(Color.Red, Color.Blue)
            )
        Box(
            Modifier
                .graphicsLayer(
                    translationX = 0f,
                    translationY = 0f,
                    rotationX = rotationX,
                    rotationY = rotationY,
                    rotationZ = rotationZ,
                    )
                .requiredSize(size)
                .background(brush = backgroundBrush)
        )
        Box(
            Modifier
                .graphicsLayer(
                    translationX = 20f,
                    translationY = 0f,
                    rotationX = rotationX,
                    rotationY = rotationY,
                    rotationZ = rotationZ,
                    transformOrigin = TransformOrigin(0f, 0f),
                )
                .requiredSize(size)
                .background(brush = backgroundBrush)
        )
        Box(
            Modifier
                .graphicsLayer(
                    translationX = 0f,
                    translationY = 20f,
                    rotationX = rotationX,
                    rotationY = rotationY,
                    rotationZ = rotationZ,
                    cameraDistance = 0.1f
                )
                .requiredSize(size)
                .background(brush = backgroundBrush)
        )
        Box(
            Modifier
                .graphicsLayer(
                    translationX = 20f,
                    translationY = 20f,
                    rotationX = -rotationX,
                    rotationY = -rotationY,
                    rotationZ = -rotationZ,
                    cameraDistance = 0.1f
                )
                .requiredSize(size)
                .background(brush = backgroundBrush)
        )

    }

    @Test
    fun rotationX() {

        // TODO Remove once approximate comparison will be available. The problem: there is a difference
        //  in antialiasing between platforms. The golden screenshot currently matches CI behaviour.
        assumeTrue(isLinux)

        val snapshot = renderComposeScene(width = 40, height = 40) {
            testRotationBoxes(
                rotationX = 45f,
            )
        }
        screenshotRule.write(snapshot)
    }

    @Test
    fun rotationY() {

        // TODO Remove once approximate comparison will be available. The problem: there is a difference
        //  in antialiasing between platforms. The golden screenshot currently matches CI behaviour.
        assumeTrue(isLinux)

        val snapshot = renderComposeScene(width = 40, height = 40) {
            testRotationBoxes(
                rotationY = 45f,
            )
        }
        screenshotRule.write(snapshot)
    }
    @Test
    fun rotationZ() {

        // TODO Remove once approximate comparison will be available. The problem: there is a difference
        //  in antialiasing between platforms. The golden screenshot currently matches CI behaviour.
        assumeTrue(isLinux)

        val snapshot = renderComposeScene(width = 40, height = 40) {
            testRotationBoxes(
                rotationZ = 45f,
            )
        }
        screenshotRule.write(snapshot)
    }

    @Test
    fun rotationXYZ() {

        // TODO Remove once approximate comparison will be available. The problem: there is a difference
        //  in antialiasing between platforms. The golden screenshot currently matches CI behaviour.
        assumeTrue(isLinux)

        val snapshot = renderComposeScene(width = 40, height = 40) {
            testRotationBoxes(
                rotationX = 45f,
                rotationY = 45f,
                rotationZ = 45f,
            )
        }
        screenshotRule.write(snapshot)
    }

    @Test
    fun `nested layer transformations`() {
        val snapshot = renderComposeScene(width = 40, height = 40) {
            Box(
                Modifier
                    .graphicsLayer(rotationZ = 45f, translationX = 10f)
                    .requiredSize(20f.dp, 20f.dp).background(Color.Green)
            ) {
                Box(
                    Modifier
                        .graphicsLayer(rotationZ = 45f)
                        .requiredSize(20f.dp, 20f.dp).background(Color.Blue)
                )
            }
        }
        screenshotRule.write(snapshot)
    }

    @Test
    fun clip() {
        val snapshot = renderComposeScene(width = 40, height = 40) {
            Box(
                Modifier
                    .graphicsLayer(
                        translationX = 10f,
                        translationY = 10f,
                        transformOrigin = TransformOrigin(0f, 0f),
                        clip = false
                    )
                    .requiredSize(10f.dp, 10f.dp).background(Color.Red)
            ) {
                Box(
                    Modifier
                        .graphicsLayer(
                            transformOrigin = TransformOrigin(0f, 0f),
                            clip = false
                        )
                        .requiredSize(20f.dp, 2f.dp)
                        .background(Color.Blue)
                )
            }

            Box(
                Modifier
                    .graphicsLayer(
                        translationX = 10f,
                        translationY = 30f,
                        transformOrigin = TransformOrigin(0f, 0f),
                        clip = true
                    )
                    .requiredSize(10f.dp, 10f.dp).background(Color.Red)
            ) {
                Box(
                    Modifier
                        .graphicsLayer(
                            transformOrigin = TransformOrigin(0f, 0f),
                            clip = false
                        )
                        .requiredSize(20f.dp, 2f.dp)
                        .background(Color.Blue)
                )
            }

            Box(
                Modifier
                    .graphicsLayer(
                        translationX = 30f,
                        translationY = 10f,
                        transformOrigin = TransformOrigin(0f, 0f),
                        clip = true,
                        shape = RoundedCornerShape(5.dp)
                    )
                    .requiredSize(10f.dp, 10f.dp).background(Color.Red)
            ) {
                Box(
                    Modifier
                        .graphicsLayer(
                            transformOrigin = TransformOrigin(0f, 0f),
                            clip = false
                        )
                        .requiredSize(20f.dp, 2f.dp)
                        .background(Color.Blue)
                )
            }
        }
        screenshotRule.write(snapshot)
    }

    @Test
    fun alpha() {
        val snapshot = renderComposeScene(width = 40, height = 40) {
            Box(
                Modifier
                    .padding(start = 5.dp)
                    .graphicsLayer(
                        translationX = -5f,
                        translationY = 5f,
                        transformOrigin = TransformOrigin(0f, 0f),
                        alpha = 0.5f
                    )
                    .requiredSize(10f.dp, 10f.dp)
                    .background(Color.Green)
            ) {
                // This box will be clipped (because if we use alpha, we draw into
                // intermediate buffer)
                Box(
                    Modifier
                        .requiredSize(30f.dp, 30f.dp)
                        .background(Color.Blue)
                )
            }

            Box(
                Modifier
                    .padding(start = 15.dp)
                    .graphicsLayer(alpha = 0.5f)
                    .requiredSize(15f.dp, 15f.dp)
                    .background(Color.Red)
            ) {
                Box(
                    Modifier
                        .graphicsLayer(alpha = 0.5f)
                        .requiredSize(10f.dp, 10f.dp)
                        .background(Color.Blue)
                )
            }

            Box(
                Modifier
                    .graphicsLayer(
                        alpha = 0f
                    )
                    .requiredSize(10f.dp, 10f.dp)
                    .background(Color.Blue)
            )
        }
        screenshotRule.write(snapshot)
    }

    @Test
    fun elevation() {
        val snapshot = renderComposeScene(width = 40, height = 40) {
            Box(
                Modifier
                    .graphicsLayer(shadowElevation = 5f)
                    .requiredSize(20f.dp, 20f.dp)
            )
            Box(
                Modifier
                    .graphicsLayer(translationX = 20f, shadowElevation = 5f)
                    .requiredSize(20f.dp, 20f.dp)
            ) {
                Box(
                    Modifier
                        .requiredSize(20f.dp, 20f.dp)
                        .background(Color.Blue)
                )
            }
            Box(
                Modifier
                    .graphicsLayer(translationY = 20f, alpha = 0.8f, shadowElevation = 5f)
                    .requiredSize(20f.dp, 20f.dp)
            ) {
                Box(
                    Modifier
                        .requiredSize(20f.dp, 20f.dp)
                        .background(Color.Red)
                )
            }
            Box(
                Modifier
                    .graphicsLayer(
                        translationX = 20f,
                        translationY = 20f,
                        shadowElevation = 5f,
                        alpha = 0.8f
                    )
                    .requiredSize(20f.dp, 20f.dp)
            )
        }
        screenshotRule.write(snapshot)
    }
}