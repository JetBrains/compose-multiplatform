/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.graphics.vector

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalImageVectorCache
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.ImageVectorCache
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.R
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class VectorTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorTint() {
        rule.setContent {
            VectorTint()
        }

        takeScreenShot(200).apply {
            assertEquals(getPixel(100, 100), Color.Cyan.toArgb())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorIntrinsicTint() {
        rule.setContent {
            val background = Modifier.paint(
                createTestVectorPainter(200, Color.Magenta),
                alignment = Alignment.Center
            )
            AtLeastSize(size = 200, modifier = background) {
            }
        }
        takeScreenShot(200).apply {
            assertEquals(getPixel(100, 100), Color.Magenta.toArgb())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorIntrinsicTintFirstFrame() {
        rule.setContent {
            val vector = createTestVectorPainter(200, Color.Magenta)

            val bitmap = remember {
                val bitmap = ImageBitmap(200, 200)
                val canvas = Canvas(bitmap)
                val bitmapSize = Size(200f, 200f)
                CanvasDrawScope().draw(
                    Density(1f),
                    LayoutDirection.Ltr,
                    canvas,
                    bitmapSize
                ) {
                    with(vector) {
                        draw(bitmapSize)
                    }
                }
                bitmap
            }

            val background = Modifier.paint(BitmapPainter(bitmap))

            AtLeastSize(size = 200, modifier = background) {
            }
        }
        takeScreenShot(200).apply {
            assertEquals(getPixel(100, 100), Color.Magenta.toArgb())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorAlignment() {
        rule.setContent {
            VectorTint(minimumSize = 450, alignment = Alignment.BottomEnd)
        }

        takeScreenShot(450).apply {
            assertEquals(getPixel(430, 430), Color.Cyan.toArgb())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorInvalidation() {
        val testCase = VectorInvalidationTestCase()
        rule.setContent {
            testCase.TestVector()
        }

        rule.waitUntil { testCase.measured }
        val size = testCase.vectorSize
        takeScreenShot(size).apply {
            assertEquals(Color.Blue.toArgb(), getPixel(5, size - 5))
            assertEquals(Color.White.toArgb(), getPixel(size - 5, 5))
        }

        testCase.measured = false
        rule.runOnUiThread {
            testCase.toggle()
        }

        rule.waitUntil { testCase.measured }

        takeScreenShot(size).apply {
            assertEquals(Color.White.toArgb(), getPixel(5, size - 5))
            assertEquals(Color.Red.toArgb(), getPixel(size - 5, 5))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorClipPath() {
        rule.setContent {
            VectorClip()
        }

        takeScreenShot(200).apply {
            assertEquals(getPixel(100, 50), Color.Cyan.toArgb())
            assertEquals(getPixel(100, 150), Color.Black.toArgb())
        }
    }

    @Test
    fun testVectorZeroSizeDoesNotCrash() {
        // Make sure that if we are given the size of zero we should not crash and instead
        // act as a no-op
        rule.setContent {
            Box(modifier = Modifier.size(0.dp).paint(createTestVectorPainter()))
        }
    }

    @Test
    fun testVectorZeroWidthDoesNotCrash() {
        rule.setContent {
            Box(
                modifier = Modifier.width(0.dp).height(100.dp).paint
                (createTestVectorPainter())
            )
        }
    }

    @Test
    fun testVectorZeroHeightDoesNotCrash() {
        rule.setContent {
            Box(
                modifier = Modifier.width(50.dp).height(0.dp).paint(
                    createTestVectorPainter()
                )
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorTrimPath() {
        rule.setContent {
            VectorTrim()
        }

        takeScreenShot(200).apply {
            assertEquals(Color.Yellow.toArgb(), getPixel(25, 100))
            assertEquals(Color.Blue.toArgb(), getPixel(100, 100))
            assertEquals(Color.Yellow.toArgb(), getPixel(175, 100))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testImageVectorChangeOnStateChange() {
        val defaultWidth = 48.dp
        val defaultHeight = 48.dp
        val viewportWidth = 24f
        val viewportHeight = 24f

        val icon1 = ImageVector.Builder(
            defaultWidth = defaultWidth,
            defaultHeight = defaultHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight
        )
            .addPath(
                fill = SolidColor(Color.Black),
                pathData = PathData {
                    lineTo(viewportWidth, 0f)
                    lineTo(viewportWidth, viewportHeight)
                    lineTo(0f, 0f)
                    close()
                }
            ).build()

        val icon2 = ImageVector.Builder(
            defaultWidth = defaultWidth,
            defaultHeight = defaultHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight
        )
            .addPath(
                fill = SolidColor(Color.Black),
                pathData = PathData {
                    lineTo(0f, viewportHeight)
                    lineTo(viewportWidth, viewportHeight)
                    lineTo(0f, 0f)
                    close()
                }
            ).build()

        val testTag = "iconClick"
        rule.setContent {
            val clickState = remember { mutableStateOf(false) }
            Image(
                imageVector = if (clickState.value) icon1 else icon2,
                contentDescription = null,
                modifier = Modifier
                    .testTag(testTag)
                    .size(icon1.defaultWidth, icon1.defaultHeight)
                    .background(Color.Red)
                    .clickable { clickState.value = !clickState.value },
                alignment = Alignment.TopStart,
                contentScale = ContentScale.FillHeight
            )
        }

        rule.onNodeWithTag(testTag).apply {
            captureToImage().asAndroidBitmap().apply {
                assertEquals(Color.Red.toArgb(), getPixel(width - 2, 0))
                assertEquals(Color.Red.toArgb(), getPixel(2, 0))
                assertEquals(Color.Red.toArgb(), getPixel(width - 1, height - 2))

                assertEquals(Color.Black.toArgb(), getPixel(0, 2))
                assertEquals(Color.Black.toArgb(), getPixel(0, height - 1))
                assertEquals(Color.Black.toArgb(), getPixel(width - 2, height - 1))
            }
            performClick()
        }

        rule.waitForIdle()

        rule.onNodeWithTag(testTag).captureToImage().asAndroidBitmap().apply {
            assertEquals(Color.Black.toArgb(), getPixel(width - 2, 0))
            assertEquals(Color.Black.toArgb(), getPixel(2, 0))
            assertEquals(Color.Black.toArgb(), getPixel(width - 1, height - 2))

            assertEquals(Color.Red.toArgb(), getPixel(0, 2))
            assertEquals(Color.Red.toArgb(), getPixel(0, height - 2))
            assertEquals(Color.Red.toArgb(), getPixel(width - 2, height - 1))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorScaleNonUniformly() {
        val defaultWidth = 24.dp
        val defaultHeight = 24.dp
        val testTag = "testTag"
        rule.setContent {
            val vectorPainter = rememberVectorPainter(
                defaultWidth = defaultWidth,
                defaultHeight = defaultHeight,
                autoMirror = false
            ) { viewportWidth, viewportHeight ->
                Path(
                    fill = SolidColor(Color.Blue),
                    pathData = PathData {
                        lineTo(viewportWidth, 0f)
                        lineTo(viewportWidth, viewportHeight)
                        lineTo(0f, viewportHeight)
                        close()
                    }
                )
            }
            Image(
                painter = vectorPainter,
                contentDescription = null,
                modifier = Modifier
                    .testTag(testTag)
                    .size(defaultWidth * 7, defaultHeight * 3)
                    .background(Color.Red),
                contentScale = ContentScale.FillBounds
            )
        }

        rule.onNodeWithTag(testTag).captureToImage().assertPixels { Color.Blue }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorChangeSize() {
        val size = mutableStateOf(200)
        val color = mutableStateOf(Color.Magenta)

        rule.setContent {
            val background = Modifier.background(Color.Red).paint(
                createTestVectorPainter(size.value, color.value),
                alignment = Alignment.TopStart
            )
            AtLeastSize(size = 400, modifier = background) {
            }
        }

        takeScreenShot(400).apply {
            assertEquals(getPixel(100, 100), Color.Magenta.toArgb())
            assertEquals(getPixel(300, 300), Color.Red.toArgb())
        }

        size.value = 400
        color.value = Color.Cyan

        takeScreenShot(400).apply {
            assertEquals(getPixel(100, 100), Color.Cyan.toArgb())
            assertEquals(getPixel(300, 300), Color.Cyan.toArgb())
        }

        size.value = 50
        color.value = Color.Yellow

        takeScreenShot(400).apply {
            assertEquals(getPixel(10, 10), Color.Yellow.toArgb())
            assertEquals(getPixel(100, 100), Color.Red.toArgb())
            assertEquals(getPixel(300, 300), Color.Red.toArgb())
        }
    }

    @Test
    fun testImageVectorCacheHit() {
        var vectorInCache = false
        rule.setContent {
            val theme = LocalContext.current.theme
            val imageVectorCache = LocalImageVectorCache.current
            imageVectorCache.clear()
            Image(
                painterResource(R.drawable.ic_triangle),
                contentDescription = null
            )

            vectorInCache =
                imageVectorCache[ImageVectorCache.Key(theme, R.drawable.ic_triangle)] != null
        }

        assertTrue(vectorInCache)
    }

    @Test
    fun testImageVectorCacheCleared() {
        var vectorInCache = false
        var application: Application? = null
        var theme: Resources.Theme? = null
        var vectorCache: ImageVectorCache? = null
        rule.setContent {
            application = LocalContext.current.applicationContext as Application
            theme = LocalContext.current.theme
            val imageVectorCache = LocalImageVectorCache.current
            imageVectorCache.clear()
            Image(
                painterResource(R.drawable.ic_triangle),
                contentDescription = null
            )

            vectorInCache =
                imageVectorCache[ImageVectorCache.Key(theme!!, R.drawable.ic_triangle)] != null

            vectorCache = imageVectorCache
        }

        application?.onTrimMemory(0)

        val cacheCleared = vectorCache?.let {
            it[ImageVectorCache.Key(theme!!, R.drawable.ic_triangle)] == null
        } ?: false

        assertTrue("Vector was not inserted in cache after initial creation", vectorInCache)
        assertTrue("Cache was not cleared after trim memory call", cacheCleared)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testImageVectorConfigChange() {
        val tag = "testTag"
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val latch = CountDownLatch(1)

        rule.activity.application.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(p0: Configuration) {
                latch.countDown()
            }

            override fun onLowMemory() {
                // NO-OP
            }

            override fun onTrimMemory(p0: Int) {
                // NO-OP
            }
        })

        try {
            latch.await(1500, TimeUnit.MILLISECONDS)
            rule.setContent {
                Image(
                    painterResource(R.drawable.ic_triangle_config),
                    contentDescription = null,
                    modifier = Modifier.testTag(tag)
                )
            }
            rule.onNodeWithTag(tag).captureToImage().apply {
                assertEquals(Color.Blue, toPixelMap()[width - 5, 5])
            }
        } catch (e: InterruptedException) {
            fail("Unable to verify vector asset in landscape orientation")
        } finally {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorMirror() {
        val tag = "mirroredVector"
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Image(
                    painter = VectorMirror(20),
                    contentDescription = null,
                    modifier = Modifier.testTag(tag)
                )
            }
        }
        rule.onNodeWithTag(tag).captureToImage().toPixelMap().apply {
            assertEquals(Color.Blue, this[2, 2])
            assertEquals(Color.Blue, this[2, height - 3])
            assertEquals(Color.Blue, this[width / 2 - 3, 2])
            assertEquals(Color.Blue, this[width / 2 - 3, height - 3])

            assertEquals(Color.Red, this[width - 3, 2])
            assertEquals(Color.Red, this[width - 3, height - 3])
            assertEquals(Color.Red, this[width / 2 + 3, 2])
            assertEquals(Color.Red, this[width / 2 + 3, height - 3])
        }
    }

    @Composable
    private fun VectorTint(
        size: Int = 200,
        minimumSize: Int = size,
        alignment: Alignment = Alignment.Center
    ) {
        val background = Modifier.paint(
            createTestVectorPainter(size),
            colorFilter = ColorFilter.tint(Color.Cyan),
            alignment = alignment
        )
        AtLeastSize(size = minimumSize, modifier = background) {
        }
    }

    @Composable
    private fun createTestVectorPainter(
        size: Int = 200,
        tintColor: Color = Color.Unspecified
    ): VectorPainter {
        val sizePx = size.toFloat()
        val sizeDp = (size / LocalDensity.current.density).dp
        return rememberVectorPainter(
            defaultWidth = sizeDp,
            defaultHeight = sizeDp,
            autoMirror = false,
            content = { _, _ ->
                Path(
                    pathData = PathData {
                        lineTo(sizePx, 0.0f)
                        lineTo(sizePx, sizePx)
                        lineTo(0.0f, sizePx)
                        close()
                    },
                    fill = SolidColor(Color.Black)
                )
            },
            tintColor = tintColor
        )
    }

    @Composable
    private fun VectorClip(
        size: Int = 200,
        minimumSize: Int = size,
        alignment: Alignment = Alignment.Center
    ) {
        val sizePx = size.toFloat()
        val sizeDp = (size / LocalDensity.current.density).dp
        val background = Modifier.paint(
            rememberVectorPainter(
                defaultWidth = sizeDp,
                defaultHeight = sizeDp,
                autoMirror = false
            ) { _, _ ->
                Path(
                    // Cyan background.
                    pathData = PathData {
                        lineTo(sizePx, 0.0f)
                        lineTo(sizePx, sizePx)
                        lineTo(0.0f, sizePx)
                        close()
                    },
                    fill = SolidColor(Color.Cyan)
                )
                Group(
                    // Only show the top half...
                    clipPathData = PathData {
                        lineTo(sizePx, 0.0f)
                        lineTo(sizePx, sizePx / 2)
                        lineTo(0.0f, sizePx / 2)
                        close()
                    },
                    // And rotate it, resulting in the bottom half being black.
                    pivotX = sizePx / 2,
                    pivotY = sizePx / 2,
                    rotation = 180f
                ) {
                    Path(
                        pathData = PathData {
                            lineTo(sizePx, 0.0f)
                            lineTo(sizePx, sizePx)
                            lineTo(0.0f, sizePx)
                            close()
                        },
                        fill = SolidColor(Color.Black)
                    )
                }
            },
            alignment = alignment
        )
        AtLeastSize(size = minimumSize, modifier = background) {
        }
    }

    @Composable
    private fun VectorTrim(
        size: Int = 200,
        minimumSize: Int = size,
        alignment: Alignment = Alignment.Center
    ) {
        val sizePx = size.toFloat()
        val sizeDp = (size / LocalDensity.current.density).dp
        val background = Modifier.paint(
            rememberVectorPainter(
                defaultWidth = sizeDp,
                defaultHeight = sizeDp,
                autoMirror = false
            ) { _, _ ->
                Path(
                    pathData = PathData {
                        lineTo(sizePx, 0.0f)
                        lineTo(sizePx, sizePx)
                        lineTo(0.0f, sizePx)
                        close()
                    },
                    fill = SolidColor(Color.Blue)
                )
                // A thick stroke
                Path(
                    pathData = PathData {
                        moveTo(0.0f, sizePx / 2)
                        lineTo(sizePx, sizePx / 2)
                    },
                    stroke = SolidColor(Color.Yellow),
                    strokeLineWidth = sizePx / 2,
                    trimPathStart = 0.25f,
                    trimPathEnd = 0.75f,
                    trimPathOffset = 0.5f
                )
            },
            alignment = alignment
        )
        AtLeastSize(size = minimumSize, modifier = background) {
        }
    }

    @Composable
    private fun VectorMirror(size: Int): VectorPainter {
        val sizePx = size.toFloat()
        val sizeDp = (size / LocalDensity.current.density).dp
        return rememberVectorPainter(
                defaultWidth = sizeDp,
                defaultHeight = sizeDp,
                autoMirror = true
            ) { _, _ ->
                Path(
                    pathData = PathData {
                        lineTo(sizePx / 2, 0f)
                        lineTo(sizePx / 2, sizePx)
                        lineTo(0f, sizePx)
                        close()
                    },
                    fill = SolidColor(Color.Red)
                )

                Path(
                    pathData = PathData {
                        moveTo(sizePx / 2, 0f)
                        lineTo(sizePx, 0f)
                        lineTo(sizePx, sizePx)
                        lineTo(sizePx / 2, sizePx)
                        close()
                    },
                    fill = SolidColor(Color.Blue)
                )
            }
    }

    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        Assert.assertEquals(width, bitmap.width)
        Assert.assertEquals(height, bitmap.height)
        return bitmap
    }
}
