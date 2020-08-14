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

import androidx.compose.foundation.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.TransformOrigin
import androidx.compose.ui.drawLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.ui.desktop.test.DesktopScreenshotTestRule
import androidx.ui.desktop.test.TestSkiaWindow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DrawLayerTest {
    @get:Rule
    val screenshotRule = DesktopScreenshotTestRule("ui/ui-desktop/core")

    @Test
    fun scale() {
        val window = TestSkiaWindow(width = 40, height = 40)
        window.setContent {
            Box(
                Modifier
                    .drawLayer(
                        scaleX = 2f,
                        scaleY = 0.5f,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )
                    .size(10f.dp, 10f.dp),
                backgroundColor = Color.Red
            )
            Box(
                Modifier
                    .drawLayer(
                        translationX = 10f,
                        translationY = 20f,
                        scaleX = 2f,
                        scaleY = 0.5f
                    )
                    .size(10f.dp, 10f.dp),
                backgroundColor = Color.Blue
            )
        }
        screenshotRule.snap(window.surface)
    }

    @Test
    fun rotationZ() {
        val window = TestSkiaWindow(width = 40, height = 40)
        window.setContent {
            Box(
                Modifier
                    .drawLayer(
                        translationX = 10f,
                        rotationZ = 90f,
                        scaleX = 2f,
                        scaleY = 0.5f,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )
                    .size(10f.dp, 10f.dp),
                backgroundColor = Color.Red
            )
            Box(
                Modifier
                    .drawLayer(
                        translationX = 10f,
                        translationY = 20f,
                        rotationZ = 45f
                    )
                    .size(10f.dp, 10f.dp),
                backgroundColor = Color.Blue
            )
        }
        screenshotRule.snap(window.surface)
    }

    @Test
    fun clip() {
        val window = TestSkiaWindow(width = 40, height = 40)
        window.setContent {
            Box(
                Modifier
                    .drawLayer(
                        translationX = 10f,
                        translationY = 10f,
                        transformOrigin = TransformOrigin(0f, 0f),
                        clip = false
                    )
                    .size(10f.dp, 10f.dp),
                backgroundColor = Color.Red
            ) {
                Box(
                    Modifier
                        .drawLayer(
                            transformOrigin = TransformOrigin(0f, 0f),
                            clip = false
                        )
                        .size(20f.dp, 2f.dp),
                    backgroundColor = Color.Blue
                )
            }

            Box(
                Modifier
                    .drawLayer(
                        translationX = 10f,
                        translationY = 30f,
                        transformOrigin = TransformOrigin(0f, 0f),
                        clip = true
                    )
                    .size(10f.dp, 10f.dp),
                backgroundColor = Color.Red
            ) {
                Box(
                    Modifier
                        .drawLayer(
                            transformOrigin = TransformOrigin(0f, 0f),
                            clip = false
                        )
                        .size(20f.dp, 2f.dp),
                    backgroundColor = Color.Blue
                )
            }

            Box(
                Modifier
                    .drawLayer(
                        translationX = 30f,
                        translationY = 10f,
                        transformOrigin = TransformOrigin(0f, 0f),
                        clip = true,
                        shape = RoundedCornerShape(5.dp)
                    )
                    .size(10f.dp, 10f.dp),
                backgroundColor = Color.Red
            ) {
                Box(
                    Modifier
                        .drawLayer(
                            transformOrigin = TransformOrigin(0f, 0f),
                            clip = false
                        )
                        .size(20f.dp, 2f.dp),
                    backgroundColor = Color.Blue
                )
            }
        }
        screenshotRule.snap(window.surface)
    }
}