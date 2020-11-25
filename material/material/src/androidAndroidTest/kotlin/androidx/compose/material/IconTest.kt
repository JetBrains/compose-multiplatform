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
package androidx.compose.material

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.ImagePainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class IconTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun vector_materialIconSize_dimensions() {
        val width = 24.dp
        val height = 24.dp
        val vector = Icons.Filled.Menu
        rule
            .setMaterialContentForSizeAssertions {
                Icon(vector)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun vector_customIconSize_dimensions() {
        val width = 35.dp
        val height = 83.dp
        val vector = ImageVector.Builder(
            defaultWidth = width, defaultHeight = height,
            viewportWidth = width.value, viewportHeight = height.value
        ).build()
        rule
            .setMaterialContentForSizeAssertions {
                Icon(vector)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun image_noIntrinsicSize_dimensions() {
        val width = 24.dp
        val height = 24.dp
        rule
            .setMaterialContentForSizeAssertions {
                val image = with(AmbientDensity.current) {
                    ImageBitmap(width.toIntPx(), height.toIntPx())
                }

                Icon(image)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun image_withIntrinsicSize_dimensions() {
        val width = 35.dp
        val height = 83.dp

        rule
            .setMaterialContentForSizeAssertions {
                val image = with(AmbientDensity.current) {
                    ImageBitmap(width.toIntPx(), height.toIntPx())
                }

                Icon(image)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun painter_noIntrinsicSize_dimensions() {
        val width = 24.dp
        val height = 24.dp
        val painter = ColorPainter(Color.Red)
        rule
            .setMaterialContentForSizeAssertions {
                Icon(painter)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun painter_withIntrinsicSize_dimensions() {
        val width = 35.dp
        val height = 83.dp

        rule
            .setMaterialContentForSizeAssertions {
                val image = with(AmbientDensity.current) {
                    ImageBitmap(width.toIntPx(), height.toIntPx())
                }

                val imagePainter = ImagePainter(image)
                Icon(imagePainter)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun iconUnspecifiedTintColorIgnored() {
        val width = 35.dp
        val height = 83.dp
        val testTag = "testTag"
        rule.setMaterialContentForSizeAssertions {
            val image: ImageBitmap
            with(AmbientDensity.current) {
                image = createBitmapWithColor(
                    this,
                    width.toIntPx(),
                    height.toIntPx(),
                    Color.Red
                )
            }
            Icon(image, modifier = Modifier.testTag(testTag), tint = Color.Unspecified)
        }

        // With no color provided for a tint, the icon should render the original pixels
        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Red }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun iconSpecifiedTintColorApplied() {
        val width = 35.dp
        val height = 83.dp
        val testTag = "testTag"
        rule.setMaterialContentForSizeAssertions {
            val image: ImageBitmap
            with(AmbientDensity.current) {
                image = createBitmapWithColor(
                    this,
                    width.toIntPx(),
                    height.toIntPx(),
                    Color.Red
                )
            }
            Icon(image, modifier = Modifier.testTag(testTag), tint = Color.Blue)
        }

        // With a tint color provided, all pixels should be blue
        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Blue }
    }

    private fun createBitmapWithColor(
        density: Density,
        width: Int,
        height: Int,
        color: Color
    ): ImageBitmap {
        val size = Size(width.toFloat(), height.toFloat())
        val image = ImageBitmap(width, height)
        CanvasDrawScope().draw(
            density,
            LayoutDirection.Ltr,
            Canvas(image),
            size
        ) {
            drawRect(color)
        }
        return image
    }
}
