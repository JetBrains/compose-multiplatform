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

package androidx.compose.foundation

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertShape
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onRoot
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class CanvasTest {

    val contentTag = "CanvasTest"
    val boxWidth = 100
    val boxHeight = 100
    val containerSize = boxWidth

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testCanvas() {
        val strokeWidth = 5.0f
        rule.setContent {
            val density = DensityAmbient.current.density
            val containerSize = (containerSize * 2 / density).dp
            val minWidth = (boxWidth / density).dp
            val minHeight = (boxHeight / density).dp
            Box(
                modifier = Modifier.preferredSize(containerSize)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                Canvas(modifier = Modifier.preferredSize(minWidth, minHeight)) {
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
        rule.onRoot().captureToBitmap().apply {
            val imageStartX = width / 2 - boxWidth / 2
            val imageStartY = height / 2 - boxHeight / 2

            // Top left
            Assert.assertEquals(paintBoxColor, getPixel(imageStartX, imageStartY))

            // Top Left, to the left of the line
            Assert.assertEquals(
                containerBgColor,
                getPixel(imageStartX - strokeOffset, imageStartY)
            )

            // Top Left, to the right of the line
            Assert.assertEquals(
                containerBgColor,
                getPixel(imageStartX + strokeOffset, imageStartY)
            )

            // Bottom right
            Assert.assertEquals(
                paintBoxColor,
                getPixel(
                    imageStartX + boxWidth - 1,
                    imageStartY + boxHeight - 1
                )
            )

            // Bottom right to the right of the line
            Assert.assertEquals(
                containerBgColor,
                getPixel(
                    imageStartX + boxWidth + strokeOffset,
                    imageStartY + boxHeight
                )
            )

            // Bottom right to the left of the line
            Assert.assertEquals(
                containerBgColor,
                getPixel(
                    imageStartX + boxWidth - strokeOffset,
                    imageStartY + boxHeight
                )
            )

            // Middle
            Assert.assertEquals(
                paintBoxColor,
                getPixel(
                    imageStartX + boxWidth / 2,
                    imageStartY + boxHeight / 2
                )
            )

            // Middle to the left of the line
            Assert.assertEquals(
                containerBgColor,
                getPixel(
                    imageStartX + boxWidth / 2 - strokeOffset,
                    imageStartY + boxHeight / 2
                )
            )

            // Middle to the right of the line
            Assert.assertEquals(
                containerBgColor,
                getPixel(
                    imageStartX + boxWidth / 2 + strokeOffset,
                    imageStartY + boxHeight / 2
                )
            )
        }
    }

    @Test
    fun canvas_noSize_emptyCanvas() {
        rule.setContentForSizeAssertions {
            Canvas(modifier = Modifier) {
                drawRect(Color.Black)
            }
        }
            .assertHeightIsEqualTo(0.dp)
            .assertWidthIsEqualTo(0.dp)
    }

    @Test
    @LargeTest
    fun canvas_exactSizes() {
        rule.setContentForSizeAssertions {
            Canvas(Modifier.preferredSize(100.dp)) {
                drawRect(Color.Red)
            }
        }
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
            .captureToBitmap()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.Red,
                shapeColor = Color.Red,
                shape = RectangleShape
            )
    }

    @Test
    @LargeTest
    fun canvas_exactSizes_drawCircle() {
        rule.setContentForSizeAssertions {
            Canvas(Modifier.preferredSize(100.dp)) {
                drawRect(Color.Red)
                drawCircle(
                    Color.Blue,
                    radius = 10.0f
                )
            }
        }
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
            .captureToBitmap()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.Red,
                shapeColor = Color.Blue,
                shape = CircleShape,
                shapeSizeX = 20.0f,
                shapeSizeY = 20.0f,
                shapeOverlapPixelCount = 2.0f
            )
    }
}