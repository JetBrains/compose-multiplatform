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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.test.R
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class ImageTest {

    val contentTag = "ImageTest"

    val imageWidth = 100
    val imageHeight = 100
    val containerSize = imageWidth

    val bgColor = Color.Blue
    val pathColor = Color.Red

    @get:Rule
    val rule = createComposeRule()

    private fun createImageBitmap(): ImageBitmap {
        val image = ImageBitmap(imageWidth, imageHeight)
        val path = Path().apply {
            lineTo(imageWidth.toFloat(), imageHeight.toFloat())
            lineTo(0.0f, imageHeight.toFloat())
            close()
        }
        val paint = Paint()
        Canvas(image).apply {
            paint.color = bgColor
            drawRect(
                Rect(Offset.Zero, Size(imageWidth.toFloat(), imageHeight.toFloat())),
                paint
            )

            paint.color = pathColor
            drawPath(path, paint)
        }
        return image
    }

    @Test
    fun testImage() {
        rule.setContent {
            val size = (containerSize / LocalDensity.current.density).dp
            Box(
                Modifier.size(size)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                Image(
                    modifier = Modifier.testTag(contentTag),
                    contentDescription = null,
                    bitmap = createImageBitmap()
                )
            }
        }

        val bgColorArgb = bgColor.toArgb()
        val pathArgb = pathColor.toArgb()

        rule.onNodeWithTag(contentTag).captureToImage().asAndroidBitmap().apply {
            val imageStartX = width / 2 - imageWidth / 2
            val imageStartY = height / 2 - imageHeight / 2
            Assert.assertEquals(bgColorArgb, getPixel(imageStartX + 2, imageStartY))
            Assert.assertEquals(pathArgb, getPixel(imageStartX, imageStartY + 1))
            Assert.assertEquals(
                pathArgb,
                getPixel(
                    imageStartX + (imageWidth / 2) - 1,
                    imageStartY + (imageHeight / 2) + 1
                )
            )
            Assert.assertEquals(
                bgColorArgb,
                getPixel(
                    imageStartX + (imageWidth / 2) - 2,
                    imageStartY + (imageHeight / 2) - 5
                )
            )
            Assert.assertEquals(pathArgb, getPixel(imageStartX, imageStartY + imageHeight - 1))
        }
    }

    @Test
    fun testImageSubsection() {
        val subsectionWidth = imageWidth / 2
        val subsectionHeight = imageHeight / 2
        rule.setContent {
            val size = (containerSize / LocalDensity.current.density).dp
            Box(
                Modifier.size(size)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                Image(
                    BitmapPainter(
                        createImageBitmap(),
                        IntOffset(
                            imageWidth / 2 - subsectionWidth / 2,
                            imageHeight / 2 - subsectionHeight / 2
                        ),
                        IntSize(subsectionWidth, subsectionHeight)
                    ),
                    null
                )
            }
        }

        val boxBgArgb = Color.White.toArgb()
        val bgColorArgb = bgColor.toArgb()
        val pathArgb = pathColor.toArgb()

        rule.onRoot().captureToImage().asAndroidBitmap().apply {
            val imageStartX = width / 2 - subsectionWidth / 2
            val imageStartY = height / 2 - subsectionHeight / 2
            Assert.assertEquals(bgColorArgb, getPixel(imageStartX + 2, imageStartY))
            Assert.assertEquals(pathArgb, getPixel(imageStartX, imageStartY + 1))
            Assert.assertEquals(
                pathArgb,
                getPixel(
                    imageStartX + (subsectionWidth / 2) - 1,
                    imageStartY + (subsectionHeight / 2) + 1
                )
            )
            Assert.assertEquals(
                bgColorArgb,
                getPixel(
                    imageStartX + (subsectionWidth / 2) - 2,
                    imageStartY + (subsectionHeight / 2) - 5
                )
            )
            Assert.assertEquals(pathArgb, getPixel(imageStartX, imageStartY + subsectionHeight - 1))

            // Verify top left region outside the subsection has a white background
            Assert.assertEquals(boxBgArgb, getPixel(imageStartX - 1, imageStartY - 1))
            Assert.assertEquals(boxBgArgb, getPixel(imageStartX - 1, imageStartY))
            Assert.assertEquals(boxBgArgb, getPixel(imageStartX, imageStartY - 1))

            // Verify top right region outside the subsection has a white background
            Assert.assertEquals(
                boxBgArgb,
                getPixel(imageStartX + subsectionWidth - 1, imageStartY - 1)
            )
            Assert.assertEquals(
                boxBgArgb,
                getPixel(imageStartX + subsectionWidth, imageStartY - 1)
            )
            Assert.assertEquals(
                boxBgArgb,
                getPixel(imageStartX + subsectionWidth, imageStartY)
            )

            // Verify bottom left region outside the subsection has a white background
            Assert.assertEquals(
                boxBgArgb,
                getPixel(imageStartX - 1, imageStartY + subsectionHeight - 1)
            )
            Assert.assertEquals(
                boxBgArgb,
                getPixel(imageStartX - 1, imageStartY + subsectionHeight)
            )
            Assert.assertEquals(
                boxBgArgb,
                getPixel(imageStartX, imageStartY + subsectionHeight)
            )

            // Verify bottom right region outside the subsection has a white background
            Assert.assertEquals(
                boxBgArgb,
                getPixel(imageStartX + subsectionWidth - 1, imageStartY + subsectionHeight)
            )
            Assert.assertEquals(
                boxBgArgb,
                getPixel(imageStartX + subsectionWidth, imageStartY + subsectionHeight)
            )
            Assert.assertEquals(
                boxBgArgb,
                getPixel(imageStartX + subsectionWidth, imageStartY + subsectionHeight - 1)
            )
        }
    }

    @Test
    fun testImageFixedSizeIsStretched() {
        val imageComposableWidth = imageWidth * 2
        val imageComposableHeight = imageHeight * 2
        rule.setContent {
            val density = LocalDensity.current.density
            val size = (containerSize * 2 / density).dp
            Box(
                Modifier.size(size)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                // The resultant Image composable should be twice the size of the underlying
                // ImageBitmap that is to be drawn and will stretch the content to fit
                // the bounds
                Image(
                    bitmap = createImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .testTag(contentTag)
                        .size(
                            (imageComposableWidth / density).dp,
                            (imageComposableHeight / density).dp
                        )
                )
            }
        }

        val bgColorArgb = bgColor.toArgb()
        val pathArgb = pathColor.toArgb()
        rule.onNodeWithTag(contentTag).captureToImage().asAndroidBitmap().apply {
            val imageStartX = width / 2 - imageComposableWidth / 2
            val imageStartY = height / 2 - imageComposableHeight / 2
            Assert.assertEquals(bgColorArgb, getPixel(imageStartX + 5, imageStartY))
            Assert.assertEquals(pathArgb, getPixel(imageStartX, imageStartY + 5))
            Assert.assertEquals(
                pathArgb,
                getPixel(
                    imageStartX + (imageComposableWidth / 2) - 5,
                    imageStartY + (imageComposableHeight / 2) + 5
                )
            )
            Assert.assertEquals(
                bgColorArgb,
                getPixel(
                    imageStartX + (imageComposableWidth / 2),
                    imageStartY + (imageComposableHeight / 2) - 10
                )
            )
            Assert.assertEquals(
                pathArgb,
                getPixel(
                    imageStartX,
                    imageStartY +
                        imageComposableHeight - 1
                )
            )
        }
    }

    @Test
    @LargeTest
    fun testImageScalesNonuniformly() {
        val imageComposableWidth = imageWidth * 3
        val imageComposableHeight = imageHeight * 7

        rule.setContent {
            val density = LocalDensity.current
            val size = (containerSize * 2 / density.density).dp
            val ImageBitmap = ImageBitmap(imageWidth, imageHeight)
            CanvasDrawScope().draw(
                density,
                LayoutDirection.Ltr,
                Canvas(ImageBitmap),
                Size(imageWidth.toFloat(), imageHeight.toFloat())
            ) {
                drawRect(color = Color.Blue)
            }
            Box(
                Modifier.size(size)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                Image(
                    bitmap = ImageBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .testTag(contentTag)
                        .size(
                            (imageComposableWidth / density.density).dp,
                            (imageComposableHeight / density.density).dp
                        ),
                    // Scale the image non-uniformly within the bounds of the composable
                    contentScale = ContentScale.FillBounds,
                    alignment = Alignment.BottomEnd
                )
            }
        }

        rule.onNodeWithTag(contentTag).captureToImage().assertPixels { Color.Blue }
    }

    @Test
    fun testImageFixedSizeAlignedBottomEnd() {
        val imageComposableWidth = imageWidth * 2
        val imageComposableHeight = imageHeight * 2
        rule.setContent {
            val density = LocalDensity.current.density
            val size = (containerSize * 2 / density).dp
            Box(
                Modifier.size(size)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                // The resultant Image composable should be twice the size of the underlying
                // ImageBitmap that is to be drawn in the bottom end section of the composable
                Image(
                    bitmap = createImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .testTag(contentTag)
                        .size(
                            (imageComposableWidth / density).dp,
                            (imageComposableHeight / density).dp
                        ),
                    // Intentionally do not scale up the contents of the ImageBitmap
                    contentScale = ContentScale.Inside,
                    alignment = Alignment.BottomEnd
                )
            }
        }

        val bgColorArgb = bgColor.toArgb()
        val pathArgb = pathColor.toArgb()
        rule.onNodeWithTag(contentTag).captureToImage().asAndroidBitmap().apply {
            val composableEndX = width / 2 + imageComposableWidth / 2
            val composableEndY = height / 2 + imageComposableHeight / 2
            val imageStartX = composableEndX - imageWidth
            val imageStartY = composableEndY - imageHeight
            Assert.assertEquals(bgColorArgb, getPixel(imageStartX + 2, imageStartY))
            Assert.assertEquals(pathArgb, getPixel(imageStartX, imageStartY + 1))
            Assert.assertEquals(
                pathArgb,
                getPixel(
                    imageStartX + (imageWidth / 2) - 1,
                    imageStartY + (imageHeight / 2) + 1
                )
            )
            Assert.assertEquals(
                bgColorArgb,
                getPixel(
                    imageStartX + (imageWidth / 2) - 2,
                    imageStartY + (imageHeight / 2) - 5
                )
            )
            Assert.assertEquals(pathArgb, getPixel(imageStartX, imageStartY + imageHeight - 1))
        }
    }

    @Test
    fun testVectorScaledCentered() {
        val boxWidth = 240
        val boxHeight = 240

        // used to wait until vector resource is loaded asynchronously
        var vectorDrawn = false
        rule.setContent {
            val density = LocalDensity.current.density
            val size = (boxWidth * 2 / density).dp
            val minWidth = (boxWidth / density).dp
            val minHeight = (boxHeight / density).dp
            Box(
                Modifier.size(size)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                Image(
                    painterResource(R.drawable.ic_vector_asset_test),
                    null,
                    modifier = Modifier.sizeIn(
                        minWidth = minWidth,
                        minHeight = minHeight
                    )
                        .drawBehind { vectorDrawn = true }
                )
            }
        }

        rule.waitUntil { vectorDrawn }

        val imageColor = Color.Red.toArgb()
        val containerBgColor = Color.White.toArgb()
        rule.onRoot().captureToImage().asAndroidBitmap().apply {
            val imageStartX = width / 2 - boxWidth / 2
            val imageStartY = height / 2 - boxHeight / 2
            Assert.assertEquals(containerBgColor, getPixel(imageStartX - 1, imageStartY - 1))
            Assert.assertEquals(
                containerBgColor,
                getPixel(
                    imageStartX + boxWidth + 1,
                    imageStartY - 1
                )
            )
            Assert.assertEquals(
                containerBgColor,
                getPixel(
                    imageStartX + boxWidth + 1,
                    imageStartY + boxHeight + 1
                )
            )
            Assert.assertEquals(
                containerBgColor,
                getPixel(
                    imageStartX - 1,
                    imageStartY +
                        boxHeight + 1
                )
            )

            Assert.assertEquals(imageColor, getPixel(imageStartX, imageStartY + 15))
            Assert.assertEquals(
                containerBgColor,
                getPixel(
                    imageStartX + boxWidth - 2,
                    imageStartY - 1
                )
            )
            Assert.assertEquals(
                imageColor,
                getPixel(
                    imageStartX + boxWidth - 10,
                    imageStartY + boxHeight - 2
                )
            )
            Assert.assertEquals(
                imageColor,
                getPixel(
                    imageStartX,
                    imageStartY +
                        boxHeight - 2
                )
            )
        }
    }

    @Test
    fun testContentScaleCropRespectsMaxDimension() {
        val testTag = "testTag"
        rule.setContent {
            val asset = with(ImageBitmap(100, 100)) {
                with(Canvas(this)) {
                    val paint = Paint().apply { this.color = Color.Blue }
                    drawRect(0f, 0f, 100f, 100f, paint)
                    drawRect(
                        25f, 25f, 75f, 75f,
                        paint.apply { this.color = Color.Red }
                    )
                }
                this
            }
            val heightDp = asset.height / LocalDensity.current.density
            Image(
                asset,
                null,
                modifier = Modifier
                    .testTag(testTag)
                    .background(Color.Green)
                    .heightIn(max = (heightDp / 2f).dp),
                contentScale = ContentScale.Crop
            )
        }

        rule.onNodeWithTag(testTag).captureToImage().asAndroidBitmap().apply {
            Assert.assertEquals(100, width)
            Assert.assertEquals(50, height)
            Assert.assertEquals(Color.Blue.toArgb(), getPixel(24, height / 2))
            Assert.assertEquals(Color.Blue.toArgb(), getPixel(75, height / 2))
            Assert.assertEquals(Color.Red.toArgb(), getPixel(50, 0))
            Assert.assertEquals(Color.Red.toArgb(), getPixel(50, height - 1))
        }
    }

    @Test
    @LargeTest
    fun testPainterResourceWithImage() {
        val testTag = "testTag"
        var imageColor = Color(0.023529412f, 0.0f, 1.0f, 1.0f) // ic_image_test color

        rule.setContent {
            val painterId = remember {
                mutableStateOf(R.drawable.ic_vector_square_asset_test)
            }
            Image(
                painterResource(painterId.value),
                null,
                modifier = Modifier.testTag(testTag).clickable {
                    if (painterId.value == R.drawable.ic_vector_square_asset_test) {
                        painterId.value = R.drawable.ic_image_test
                    } else {
                        painterId.value = R.drawable.ic_vector_square_asset_test
                    }
                },
                contentScale = ContentScale.FillBounds
            )
        }

        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Red }

        rule.onNodeWithTag(testTag).performClick()

        rule.waitForIdle()

        rule.onNodeWithTag(testTag).captureToImage().assertPixels { imageColor }
    }

    @Test
    fun defaultSemanticsWhenContentDescriptionProvided() {
        val testTag = "TestTag"
        rule.setContent {
            Image(
                bitmap = ImageBitmap(100, 100),
                modifier = Modifier.testTag(testTag),
                contentDescription = "asdf"
            )
        }
        rule.onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
            .assertContentDescriptionEquals("asdf")
    }

    @Test
    fun testImageWithNoIntrinsicSizePainterFillsMaxConstraints() {
        val testTag = "testTag"
        rule.setContent {
            val sizeDp = with(LocalDensity.current) { 50 / density }
            Box(modifier = Modifier.requiredSize(sizeDp.dp)) {
                Image(
                    painter = ColorPainter(Color.Red),
                    modifier = Modifier.testTag(testTag),
                    contentDescription = null
                )
            }
        }
        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Red }
    }

    @Test
    fun testImageZeroSizeDoesNotCrash() {
        rule.setContent {
            // Intentionally force a size of zero to ensure we do not crash
            Box(modifier = Modifier.requiredSize(0.dp)) {
                Image(
                    painter = ColorPainter(Color.Red),
                    contentDescription = null
                )
            }
        }
    }

    @Test
    fun testImageFilterQualityNone() {
        val sampleBitmap = ImageBitmap(2, 2)
        val canvas = androidx.compose.ui.graphics.Canvas(sampleBitmap)
        val samplePaint = Paint().apply {
            color = Color.White
        }

        canvas.drawRect(0f, 0f, 2f, 2f, samplePaint)

        samplePaint.color = Color.Red
        canvas.drawRect(0f, 0f, 1f, 1f, samplePaint)

        samplePaint.color = Color.Blue
        canvas.drawRect(1f, 1f, 2f, 2f, samplePaint)

        val testTag = "filterQualityTest"
        rule.setContent {
            val size = 20 / LocalDensity.current.density
            Image(
                bitmap = sampleBitmap,
                contentDescription = "FilterQuality None test",
                modifier = Modifier.size(size.dp).testTag(testTag),
                filterQuality = FilterQuality.None
            )
        }

        rule.onNodeWithTag(testTag).captureToImage().apply {
            val pixelMap = toPixelMap()
            for (i in 0 until width / 2) {
                for (j in 0 until height / 2) {
                    assertEquals("invalid color at $i, $j", Color.Red, pixelMap[i, j])
                }
            }

            for (i in width / 2 + 1 until width) {
                for (j in height / 2 + 1 until height) {
                    assertEquals("invalid color at $i, $j", Color.Blue, pixelMap[i, j])
                }
            }
        }
    }
}