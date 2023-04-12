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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.assertPixelColor
import androidx.compose.foundation.assertShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CanvasTest {

    private val boxWidth = 100
    private val boxHeight = 100
    private val containerSize = boxWidth

    @Test
    fun testCanvas() = runSkikoComposeUiTest(Size(containerSize * 2f, containerSize * 2f)) {
        val strokeWidth = 5.0f
        setContent {
            val density = LocalDensity.current.density
            val containerSize = (containerSize * 2 / density).dp
            val minWidth = (boxWidth / density).dp
            val minHeight = (boxHeight / density).dp
            Box(
                modifier = Modifier.size(containerSize)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                Canvas(modifier = Modifier.size(minWidth, minHeight)) {
                    drawLine(
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                        color = Color.Red,
                        strokeWidth = strokeWidth
                    )
                }
            }
        }

        val paintBoxColor = Color.Red.toArgb()
        val containerBgColor = Color.White.toArgb()
        val strokeOffset = (strokeWidth / 2).toInt() + 3
        captureToImage().asSkiaBitmap().apply {
            val imageStartX = width / 2 - boxWidth / 2
            val imageStartY = height / 2 - boxHeight / 2

            // Top left
            assertEquals(paintBoxColor, getColor(imageStartX, imageStartY))

            // Top Left, to the left of the line
            assertEquals(
                containerBgColor,
                getColor(imageStartX - strokeOffset, imageStartY)
            )

            // Top Left, to the right of the line
            assertEquals(
                containerBgColor,
                getColor(imageStartX + strokeOffset, imageStartY)
            )

            // Bottom right
            assertEquals(
                paintBoxColor,
                getColor(
                    imageStartX + boxWidth - 1,
                    imageStartY + boxHeight - 1
                )
            )

            // Bottom right to the right of the line
            assertEquals(
                containerBgColor,
                getColor(
                    imageStartX + boxWidth + strokeOffset,
                    imageStartY + boxHeight
                )
            )

            // Bottom right to the left of the line
            assertEquals(
                containerBgColor,
                getColor(
                    imageStartX + boxWidth - strokeOffset,
                    imageStartY + boxHeight
                )
            )

            // Middle
            assertEquals(
                paintBoxColor,
                getColor(
                    imageStartX + boxWidth / 2,
                    imageStartY + boxHeight / 2
                )
            )

            // Middle to the left of the line
            assertEquals(
                containerBgColor,
                getColor(
                    imageStartX + boxWidth / 2 - strokeOffset,
                    imageStartY + boxHeight / 2
                )
            )

            // Middle to the right of the line
            assertEquals(
                containerBgColor,
                getColor(
                    imageStartX + boxWidth / 2 + strokeOffset,
                    imageStartY + boxHeight / 2
                )
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun canvas_contentDescription() = runSkikoComposeUiTest {
        val testTag = "canvas"
        val contentDescription = "cd"
        setContent {
            Canvas(modifier = Modifier.testTag(testTag), contentDescription = contentDescription) {
                drawRect(Color.Black)
            }
        }

        onNodeWithTag(testTag).assertContentDescriptionEquals(contentDescription)
    }

    @Test
    fun canvas_noSize_emptyCanvas() = runSkikoComposeUiTest {
        setContentForSizeAssertions {
            Canvas(modifier = Modifier) {
                drawRect(Color.Black)
            }
        }
            .assertHeightIsEqualTo(0.dp)
            .assertWidthIsEqualTo(0.dp)
    }

    @Test
    fun canvas_exactSizes() = runSkikoComposeUiTest {
        setContentForSizeAssertions {
            Canvas(Modifier.size(100.dp)) {
                drawRect(Color.Red)
            }
        }
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        captureToImage().apply {
            val pixelMap = toPixelMap(width = 100, height = 100)
            repeat(100) { x ->
                repeat(100) { y ->
                    pixelMap.assertPixelColor(Color.Red, x, y)
                }
            }
        }
    }

    @Test
    fun canvas_exactSizes_drawCircle() = runSkikoComposeUiTest(Size(100f, 100f)) {
        setContentForSizeAssertions {
            Canvas(Modifier.size(100.dp)) {
                drawRect(Color.Red)
                drawCircle(
                    Color.Blue,
                    radius = 10.0f
                )
            }
        }
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        captureToImage().assertShape(
            density = density,
            backgroundColor = Color.Red,
            shapeColor = Color.Blue,
            shape = CircleShape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeOverlapPixelCount = 2.0f
        )
    }
}
