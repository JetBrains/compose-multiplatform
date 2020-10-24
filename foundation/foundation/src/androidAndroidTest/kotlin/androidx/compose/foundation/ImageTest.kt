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
import androidx.compose.foundation.layout.preferredHeightIn
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredSizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.test.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageAsset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.ImagePainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.loadVectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.onRoot
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

    private fun createImageAsset(): ImageAsset {
        val image = ImageAsset(imageWidth, imageHeight)
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
            val size = (containerSize / DensityAmbient.current.density).dp
            Box(
                Modifier.preferredSize(size)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                Image(modifier = Modifier.testTag(contentTag), asset = createImageAsset())
            }
        }

        val bgColorArgb = bgColor.toArgb()
        val pathArgb = pathColor.toArgb()

        rule.onNodeWithTag(contentTag).captureToBitmap().apply {
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
            val size = (containerSize / DensityAmbient.current.density).dp
            Box(
                Modifier.preferredSize(size)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                Image(
                    ImagePainter(
                        createImageAsset(),
                        IntOffset(
                            imageWidth / 2 - subsectionWidth / 2,
                            imageHeight / 2 - subsectionHeight / 2
                        ),
                        IntSize(subsectionWidth, subsectionHeight)
                    )
                )
            }
        }

        val boxBgArgb = Color.White.toArgb()
        val bgColorArgb = bgColor.toArgb()
        val pathArgb = pathColor.toArgb()

        rule.onRoot().captureToBitmap().apply {
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
            val density = DensityAmbient.current.density
            val size = (containerSize * 2 / density).dp
            Box(
                Modifier.preferredSize(size)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                // The resultant Image composable should be twice the size of the underlying
                // ImageAsset that is to be drawn and will stretch the content to fit
                // the bounds
                Image(
                    asset = createImageAsset(),
                    modifier = Modifier
                        .testTag(contentTag)
                        .preferredSize(
                            (imageComposableWidth / density).dp,
                            (imageComposableHeight / density).dp
                        )
                )
            }
        }

        val bgColorArgb = bgColor.toArgb()
        val pathArgb = pathColor.toArgb()
        rule.onNodeWithTag(contentTag).captureToBitmap().apply {
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
    fun testImageFixedSizeAlignedBottomEnd() {
        val imageComposableWidth = imageWidth * 2
        val imageComposableHeight = imageHeight * 2
        rule.setContent {
            val density = DensityAmbient.current.density
            val size = (containerSize * 2 / density).dp
            Box(
                Modifier.preferredSize(size)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                // The resultant Image composable should be twice the size of the underlying
                // ImageAsset that is to be drawn in the bottom end section of the composable
                Image(
                    asset = createImageAsset(),
                    modifier = Modifier
                        .testTag(contentTag)
                        .preferredSize(
                            (imageComposableWidth / density).dp,
                            (imageComposableHeight / density).dp
                        ),
                    // Intentionally do not scale up the contents of the ImageAsset
                    contentScale = ContentScale.Inside,
                    alignment = Alignment.BottomEnd
                )
            }
        }

        val bgColorArgb = bgColor.toArgb()
        val pathArgb = pathColor.toArgb()
        rule.onNodeWithTag(contentTag).captureToBitmap().apply {
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

        // latch used to wait until vector resource is loaded asynchronously
        val vectorLatch = CountDownLatch(1)
        rule.setContent {
            val density = DensityAmbient.current.density
            val size = (boxWidth * 2 / density).dp
            val minWidth = (boxWidth / density).dp
            val minHeight = (boxHeight / density).dp
            Box(
                Modifier.preferredSize(size)
                    .background(color = Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                // This is an async call to parse the VectorDrawable xml asset into
                // a VectorAsset, update the latch once we receive this callback
                // and draw the Image composable
                loadVectorResource(R.drawable.ic_vector_asset_test).resource.resource?.let {
                    Image(
                        it,
                        modifier = Modifier.preferredSizeIn(
                            minWidth = minWidth,
                            minHeight = minHeight
                        )
                            .drawBehind { vectorLatch.countDown() }
                    )
                }
            }
        }

        Assert.assertTrue(vectorLatch.await(5, TimeUnit.SECONDS))

        val imageColor = Color.Red.toArgb()
        val containerBgColor = Color.White.toArgb()
        rule.onRoot().captureToBitmap().apply {
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
            val asset = with(ImageAsset(100, 100)) {
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
            val heightDp = asset.height / DensityAmbient.current.density
            Image(
                asset,
                modifier = Modifier
                    .testTag(testTag)
                    .background(Color.Green)
                    .preferredHeightIn(max = (heightDp / 2f).dp),
                contentScale = ContentScale.Crop
            )
        }

        rule.onNodeWithTag(testTag).captureToBitmap().apply {
            Assert.assertEquals(100, width)
            Assert.assertEquals(50, height)
            Assert.assertEquals(Color.Blue.toArgb(), getPixel(24, height / 2))
            Assert.assertEquals(Color.Blue.toArgb(), getPixel(75, height / 2))
            Assert.assertEquals(Color.Red.toArgb(), getPixel(50, 0))
            Assert.assertEquals(Color.Red.toArgb(), getPixel(50, height - 1))
        }
    }
}