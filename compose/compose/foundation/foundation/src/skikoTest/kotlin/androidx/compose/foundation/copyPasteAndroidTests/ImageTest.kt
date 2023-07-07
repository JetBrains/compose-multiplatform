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

import androidx.compose.foundation.Image
import androidx.compose.foundation.assertPixels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ImageTest {

    private val contentTag = "ImageTest"

    private val imageWidth = 100
    private val imageHeight = 100
    private val containerSize = imageWidth
    private val sceneSize = Size(containerSize.toFloat(), containerSize.toFloat())

    private val bgColor = Color.Blue
    private val pathColor = Color.Red

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
    fun testImage() = runSkikoComposeUiTest(sceneSize) {
        setContent {
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

        captureToImage().asSkiaBitmap().apply {
            val imageStartX = width / 2 - imageWidth / 2
            val imageStartY = height / 2 - imageHeight / 2
            assertEquals(bgColorArgb, getColor(imageStartX + 2, imageStartY))
            assertEquals(pathArgb, getColor(imageStartX, imageStartY + 1))
            assertEquals(
                pathArgb,
                getColor(
                    imageStartX + (imageWidth / 2) - 1,
                    imageStartY + (imageHeight / 2) + 1
                )
            )
            assertEquals(
                bgColorArgb,
                getColor(
                    imageStartX + (imageWidth / 2) - 2,
                    imageStartY + (imageHeight / 2) - 5
                )
            )
            assertEquals(pathArgb, getColor(imageStartX, imageStartY + imageHeight - 1))
        }
    }

    @Test
    fun testImageSubsection() = runSkikoComposeUiTest(sceneSize) {
        val subsectionWidth = imageWidth / 2
        val subsectionHeight = imageHeight / 2
        setContent {
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

        captureToImage().asSkiaBitmap().apply {
            val imageStartX = width / 2 - subsectionWidth / 2
            val imageStartY = height / 2 - subsectionHeight / 2
            assertEquals(bgColorArgb, getColor(imageStartX + 2, imageStartY))
            assertEquals(pathArgb, getColor(imageStartX, imageStartY + 1))
            assertEquals(
                pathArgb,
                getColor(
                    imageStartX + (subsectionWidth / 2) - 1,
                    imageStartY + (subsectionHeight / 2) + 1
                )
            )
            assertEquals(
                bgColorArgb,
                getColor(
                    imageStartX + (subsectionWidth / 2) - 2,
                    imageStartY + (subsectionHeight / 2) - 5
                )
            )
            assertEquals(pathArgb, getColor(imageStartX, imageStartY + subsectionHeight - 1))

            // Verify top left region outside the subsection has a white background
            assertEquals(boxBgArgb, getColor(imageStartX - 1, imageStartY - 1))
            assertEquals(boxBgArgb, getColor(imageStartX - 1, imageStartY))
            assertEquals(boxBgArgb, getColor(imageStartX, imageStartY - 1))

            // Verify top right region outside the subsection has a white background
            assertEquals(
                boxBgArgb,
                getColor(imageStartX + subsectionWidth - 1, imageStartY - 1)
            )
            assertEquals(
                boxBgArgb,
                getColor(imageStartX + subsectionWidth, imageStartY - 1)
            )
            assertEquals(
                boxBgArgb,
                getColor(imageStartX + subsectionWidth, imageStartY)
            )

            // Verify bottom left region outside the subsection has a white background
            assertEquals(
                boxBgArgb,
                getColor(imageStartX - 1, imageStartY + subsectionHeight - 1)
            )
            assertEquals(
                boxBgArgb,
                getColor(imageStartX - 1, imageStartY + subsectionHeight)
            )
            assertEquals(
                boxBgArgb,
                getColor(imageStartX, imageStartY + subsectionHeight)
            )

            // Verify bottom right region outside the subsection has a white background
            assertEquals(
                boxBgArgb,
                getColor(imageStartX + subsectionWidth - 1, imageStartY + subsectionHeight)
            )
            assertEquals(
                boxBgArgb,
                getColor(imageStartX + subsectionWidth, imageStartY + subsectionHeight)
            )
            assertEquals(
                boxBgArgb,
                getColor(imageStartX + subsectionWidth, imageStartY + subsectionHeight - 1)
            )
        }
    }

    @Test
    fun testImageFixedSizeIsStretched() = runSkikoComposeUiTest(Size(imageWidth * 2f, imageHeight * 2f)) {
        val imageComposableWidth = imageWidth * 2
        val imageComposableHeight = imageHeight * 2
        setContent {
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
        captureToImage().asSkiaBitmap().apply {
            val imageStartX = width / 2 - imageComposableWidth / 2
            val imageStartY = height / 2 - imageComposableHeight / 2
            assertEquals(bgColorArgb, getColor(imageStartX + 5, imageStartY))
            assertEquals(pathArgb, getColor(imageStartX, imageStartY + 5))
            assertEquals(
                pathArgb,
                getColor(
                    imageStartX + (imageComposableWidth / 2) - 5,
                    imageStartY + (imageComposableHeight / 2) + 5
                )
            )
            assertEquals(
                bgColorArgb,
                getColor(
                    imageStartX + (imageComposableWidth / 2),
                    imageStartY + (imageComposableHeight / 2) - 10
                )
            )
            assertEquals(
                pathArgb,
                getColor(
                    imageStartX,
                    imageStartY +
                        imageComposableHeight - 1
                )
            )
        }
    }

    @Test
    fun testImageScalesNonuniformly() = runSkikoComposeUiTest(sceneSize) {
        val imageComposableWidth = imageWidth * 3
        val imageComposableHeight = imageHeight * 7

        setContent {
            val density = LocalDensity.current
            val size = (containerSize * 2 / density.density).dp
            val imageBitmap = ImageBitmap(imageWidth, imageHeight)
            CanvasDrawScope().draw(
                density,
                LayoutDirection.Ltr,
                Canvas(imageBitmap),
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
                    bitmap = imageBitmap,
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

        captureToImage().assertPixels { Color.Blue }
    }

    @Test
    fun testImageFixedSizeAlignedBottomEnd() = runSkikoComposeUiTest(Size(containerSize * 2f, containerSize * 2f)) {
        val imageComposableWidth = imageWidth * 2
        val imageComposableHeight = imageHeight * 2
        setContent {
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
        captureToImage().asSkiaBitmap().apply {
            val composableEndX = width / 2 + imageComposableWidth / 2
            val composableEndY = height / 2 + imageComposableHeight / 2
            val imageStartX = composableEndX - imageWidth
            val imageStartY = composableEndY - imageHeight
            assertEquals(bgColorArgb, getColor(imageStartX + 2, imageStartY))
            assertEquals(pathArgb, getColor(imageStartX, imageStartY + 1))
            assertEquals(
                pathArgb,
                getColor(
                    imageStartX + (imageWidth / 2) - 1,
                    imageStartY + (imageHeight / 2) + 1
                )
            )
            assertEquals(
                bgColorArgb,
                getColor(
                    imageStartX + (imageWidth / 2) - 2,
                    imageStartY + (imageHeight / 2) - 5
                )
            )
            assertEquals(pathArgb, getColor(imageStartX, imageStartY + imageHeight - 1))
        }
    }

    @Test
    @Ignore // TODO: implement `painterResource` than copy the test from androidAndroidTest
    fun testVectorScaledCentered(): Unit = TODO()

    @Test
    fun testContentScaleCropRespectsMaxDimension() = runSkikoComposeUiTest(Size(containerSize.toFloat(), containerSize / 2f)) {
        val testTag = "testTag"
        setContent {
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

        captureToImage().asSkiaBitmap().apply {
            assertEquals(100, width)
            assertEquals(50, height)
            assertEquals(Color.Blue.toArgb(), getColor(24, height / 2))
            assertEquals(Color.Blue.toArgb(), getColor(75, height / 2))
            assertEquals(Color.Red.toArgb(), getColor(50, 0))
            assertEquals(Color.Red.toArgb(), getColor(50, height - 1))
        }
    }

    @Test
    @Ignore // TODO: implement `painterResource` and then copy the test from androidAndroidTet
    fun testPainterResourceWithImage(): Unit = TODO()

    @Test
    fun defaultSemanticsWhenContentDescriptionProvided() = runSkikoComposeUiTest {
        val testTag = "TestTag"
        setContent {
            Image(
                bitmap = ImageBitmap(100, 100),
                modifier = Modifier.testTag(testTag),
                contentDescription = "asdf"
            )
        }
        onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
            .assertContentDescriptionEquals("asdf")
    }

    @Test
    fun testImageWithNoIntrinsicSizePainterFillsMaxConstraints() = runSkikoComposeUiTest(Size(50f, 50f)) {
        val testTag = "testTag"
        setContent {
            val sizeDp = with(LocalDensity.current) { 50 / density }
            Box(modifier = Modifier.requiredSize(sizeDp.dp)) {
                Image(
                    painter = ColorPainter(Color.Red),
                    modifier = Modifier.testTag(testTag),
                    contentDescription = null
                )
            }
        }
        captureToImage().assertPixels { Color.Red }
    }

    @Test
    fun testImageZeroSizeDoesNotCrash() = runSkikoComposeUiTest {
        setContent {
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
    fun testImageFilterQualityNone() = runSkikoComposeUiTest(Size(20f, 20f)) {
        val sampleBitmap = ImageBitmap(2, 2)
        val canvas = Canvas(sampleBitmap)
        val samplePaint = Paint().apply {
            color = Color.White
        }

        canvas.drawRect(0f, 0f, 2f, 2f, samplePaint)

        samplePaint.color = Color.Red
        canvas.drawRect(0f, 0f, 1f, 1f, samplePaint)

        samplePaint.color = Color.Blue
        canvas.drawRect(1f, 1f, 2f, 2f, samplePaint)

        val testTag = "filterQualityTest"
        setContent {
            val size = 20 / LocalDensity.current.density
            Image(
                bitmap = sampleBitmap,
                contentDescription = "FilterQuality None test",
                modifier = Modifier.size(size.dp).testTag(testTag),
                filterQuality = FilterQuality.None
            )
        }

        captureToImage().apply {
            val pixelMap = toPixelMap()
            for (i in 0 until width / 2) {
                for (j in 0 until height / 2) {
                    assertEquals(Color.Red, pixelMap[i, j], "invalid color at $i, $j")
                }
            }

            for (i in width / 2 + 1 until width) {
                for (j in height / 2 + 1 until height) {
                    assertEquals(Color.Blue, pixelMap[i, j], "invalid color at $i, $j")
                }
            }
        }
    }
}
