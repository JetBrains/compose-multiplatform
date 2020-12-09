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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.TestComposeWindow
import androidx.compose.ui.test.junit4.DesktopScreenshotTestRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GraphicsLayerTest {
    @get:Rule
    val screenshotRule = DesktopScreenshotTestRule("ui/ui-desktop/core")

    @Test
    fun scale() {
        val window = TestComposeWindow(width = 40, height = 40)
        window.setContent {
            Box(
                Modifier
                    .graphicsLayer(
                        scaleX = 2f,
                        scaleY = 0.5f,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )
                    .size(10f.dp, 10f.dp).background(Color.Red)
            )
            Box(
                Modifier
                    .graphicsLayer(
                        translationX = 10f,
                        translationY = 20f,
                        scaleX = 2f,
                        scaleY = 0.5f
                    )
                    .size(10f.dp, 10f.dp).background(Color.Blue)
            )
        }
        screenshotRule.snap(window.surface)
    }

    @Test
    fun rotationZ() {
        val window = TestComposeWindow(width = 40, height = 40)
        window.setContent {
            Box(
                Modifier
                    .graphicsLayer(
                        translationX = 10f,
                        rotationZ = 90f,
                        scaleX = 2f,
                        scaleY = 0.5f,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )
                    .size(10f.dp, 10f.dp).background(Color.Red)
            )
            Box(
                Modifier
                    .graphicsLayer(
                        translationX = 10f,
                        translationY = 20f,
                        rotationZ = 45f
                    )
                    .size(10f.dp, 10f.dp).background(Color.Blue)
            )
        }
        screenshotRule.snap(window.surface)
    }

    @Test
    fun rotationX() {
        val window = TestComposeWindow(width = 40, height = 40)

        window.setContent {
            Box(
                Modifier
                    .graphicsLayer(rotationX = 45f)
                    .size(10f.dp, 10f.dp).background(Color.Blue)
            )
            Box(
                Modifier
                    .graphicsLayer(
                        translationX = 20f,
                        transformOrigin = TransformOrigin(0f, 0f),
                        rotationX = 45f
                    )
                    .size(10f.dp, 10f.dp).background(Color.Blue)
            )
        }
        screenshotRule.snap(window.surface)
    }

    @Test
    fun rotationY() {
        val window = TestComposeWindow(width = 40, height = 40)
        window.setContent {
            Box(
                Modifier
                    .graphicsLayer(rotationY = 45f)
                    .size(10f.dp, 10f.dp).background(Color.Blue)
            )
            Box(
                Modifier
                    .graphicsLayer(
                        translationX = 20f,
                        transformOrigin = TransformOrigin(0f, 0f),
                        rotationY = 45f
                    )
                    .size(10f.dp, 10f.dp).background(Color.Blue)
            )
        }
        screenshotRule.snap(window.surface)
    }

    @Test
    fun `nested layer transformations`() {
        val window = TestComposeWindow(width = 40, height = 40)
        window.setContent {
            Box(
                Modifier
                    .graphicsLayer(rotationZ = 45f, translationX = 10f)
                    .size(20f.dp, 20f.dp).background(Color.Green)
            ) {
                Box(
                    Modifier
                        .graphicsLayer(rotationZ = 45f)
                        .size(20f.dp, 20f.dp).background(Color.Blue)
                )
            }
        }
        screenshotRule.snap(window.surface)
    }

    @Test
    fun clip() {
        val window = TestComposeWindow(width = 40, height = 40)
        window.setContent {
            Box(
                Modifier
                    .graphicsLayer(
                        translationX = 10f,
                        translationY = 10f,
                        transformOrigin = TransformOrigin(0f, 0f),
                        clip = false
                    )
                    .size(10f.dp, 10f.dp).background(Color.Red)
            ) {
                Box(
                    Modifier
                        .graphicsLayer(
                            transformOrigin = TransformOrigin(0f, 0f),
                            clip = false
                        )
                        .size(20f.dp, 2f.dp)
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
                    .size(10f.dp, 10f.dp).background(Color.Red)
            ) {
                Box(
                    Modifier
                        .graphicsLayer(
                            transformOrigin = TransformOrigin(0f, 0f),
                            clip = false
                        )
                        .size(20f.dp, 2f.dp)
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
                    .size(10f.dp, 10f.dp).background(Color.Red)
            ) {
                Box(
                    Modifier
                        .graphicsLayer(
                            transformOrigin = TransformOrigin(0f, 0f),
                            clip = false
                        )
                        .size(20f.dp, 2f.dp)
                        .background(Color.Blue)
                )
            }
        }
        screenshotRule.snap(window.surface)
    }

    @Test
    fun alpha() {
        val window = TestComposeWindow(width = 40, height = 40)
        window.setContent {
            Box(
                Modifier
                    .padding(start = 5.dp)
                    .graphicsLayer(
                        translationX = -5f,
                        translationY = 5f,
                        transformOrigin = TransformOrigin(0f, 0f),
                        alpha = 0.5f
                    )
                    .size(10f.dp, 10f.dp)
                    .background(Color.Green)
            ) {
                // This box will be clipped (because if we use alpha, we draw into
                // intermediate buffer)
                Box(
                    Modifier
                        .size(30f.dp, 30f.dp)
                        .background(Color.Blue)
                )
            }

            Box(
                Modifier
                    .padding(start = 15.dp)
                    .graphicsLayer(alpha = 0.5f)
                    .size(15f.dp, 15f.dp)
                    .background(Color.Red)
            ) {
                Box(
                    Modifier
                        .graphicsLayer(alpha = 0.5f)
                        .size(10f.dp, 10f.dp)
                        .background(Color.Blue)
                )
            }

            Box(
                Modifier
                    .graphicsLayer(
                        alpha = 0f
                    )
                    .size(10f.dp, 10f.dp)
                    .background(Color.Blue)
            )
        }
        screenshotRule.snap(window.surface)
    }

    @Test
    fun elevation() {
        val window = TestComposeWindow(width = 40, height = 40)
        window.setContent {
            Box(
                Modifier
                    .graphicsLayer(shadowElevation = 5f)
                    .size(20f.dp, 20f.dp)
            )
            Box(
                Modifier
                    .graphicsLayer(translationX = 20f, shadowElevation = 5f)
                    .size(20f.dp, 20f.dp)
            ) {
                Box(
                    Modifier
                        .size(20f.dp, 20f.dp)
                        .background(Color.Blue)
                )
            }
            Box(
                Modifier
                    .graphicsLayer(translationY = 20f, alpha = 0.8f, shadowElevation = 5f)
                    .size(20f.dp, 20f.dp)
            ) {
                Box(
                    Modifier
                        .size(20f.dp, 20f.dp)
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
                    .size(20f.dp, 20f.dp)
            )
        }
        screenshotRule.snap(window.surface)
    }
}