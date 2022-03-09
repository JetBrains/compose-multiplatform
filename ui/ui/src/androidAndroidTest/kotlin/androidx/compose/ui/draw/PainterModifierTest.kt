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

package androidx.compose.ui.draw

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.AlignTopLeft
import androidx.compose.ui.Alignment
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Padding
import androidx.compose.ui.assertColorsEqual
import androidx.compose.ui.background
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
class PainterModifierTest {

    val containerWidth = 100.0f
    private val containerHeight = 100.0f

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterModifierColorFilter() {
        rule.setContent {
            TestPainter(colorFilter = ColorFilter.tint(Color.Cyan, BlendMode.SrcIn))
        }

        rule.obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Cyan.toArgb(), getPixel(50, 50))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterModifierAlpha() {
        rule.setContent {
            TestPainter(alpha = 0.5f)
        }

        rule.obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            val expected = Color(
                alpha = 0.5f,
                red = Color.Red.red,
                green = Color.Red.green,
                blue = Color.Red.blue
            ).compositeOver(Color.White)

            val result = Color(getPixel(50, 50))
            assertColorsEqual(expected, result)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterModifierRtl() {
        rule.setContent {
            TestPainter(rtl = true)
        }

        rule.obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Blue.toArgb(), getPixel(50, 50))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterAspectRatioMaintainedInSmallerParent() {
        val containerSizePx = containerWidth.roundToInt() * 3
        rule.setContent {
            FixedSize(size = containerSizePx, modifier = Modifier.background(Color.White)) {
                // Verify that the contents are scaled down appropriately even though
                // the Painter's intrinsic width and height is twice that of the component
                // it is to be drawn into
                Padding(containerWidth.roundToInt()) {
                    AtLeastSize(
                        size = containerWidth.roundToInt(),
                        modifier = Modifier.paint(
                            TestPainter(
                                containerWidth * 2,
                                containerHeight * 2
                            ),
                            alignment = Alignment.Center,
                            contentScale = ContentScale.Inside
                        )
                    ) {
                    }
                }
            }
        }

        rule.obtainScreenshotBitmap(
            containerSizePx,
            containerSizePx
        ).apply {
            assertEquals(
                Color.White.toArgb(),
                getPixel(
                    containerWidth.roundToInt() - 1,
                    containerHeight.roundToInt() - 1
                )
            )
            assertEquals(
                Color.Red.toArgb(),
                getPixel(
                    containerWidth.roundToInt() + 1,
                    containerWidth.roundToInt() + 1
                )
            )
            assertEquals(
                Color.Red.toArgb(),
                getPixel(
                    containerWidth.roundToInt() * 2 - 1,
                    containerWidth.roundToInt() * 2 - 1
                )
            )
            assertEquals(
                Color.White.toArgb(),
                getPixel(
                    containerWidth.roundToInt() * 2 + 1,
                    containerHeight.roundToInt() * 2 + 1
                )
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterAlignedBottomRightIfSmallerThanParent() {
        val containerSizePx = containerWidth.roundToInt() * 2
        rule.setContent {
            AtLeastSize(
                size = containerWidth.roundToInt() * 2,
                modifier = Modifier.background(Color.White).paint(
                    TestPainter(
                        containerWidth,
                        containerHeight
                    ),
                    alignment = Alignment.BottomEnd,
                    contentScale = ContentScale.Inside
                )
            ) {
                // Intentionally empty
            }
        }

        val bottom = containerSizePx - 1
        val right = containerSizePx - 1
        val innerBoxTop = containerSizePx - containerWidth.roundToInt()
        val innerBoxLeft = containerSizePx - containerWidth.roundToInt()
        rule.obtainScreenshotBitmap(
            containerSizePx,
            containerSizePx
        ).apply {
            assertEquals(Color.Red.toArgb(), getPixel(right, bottom))
            assertEquals(Color.Red.toArgb(), getPixel(innerBoxLeft, bottom))
            assertEquals(Color.Red.toArgb(), getPixel(innerBoxLeft, innerBoxTop + 1))
            assertEquals(Color.Red.toArgb(), getPixel(right, innerBoxTop + 1))

            assertEquals(Color.White.toArgb(), getPixel(innerBoxLeft - 1, bottom))
            assertEquals(Color.White.toArgb(), getPixel(innerBoxLeft - 1, innerBoxTop - 1))
            assertEquals(Color.White.toArgb(), getPixel(right, innerBoxTop - 1))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterModifierIntrinsicSize() {
        rule.setContent {
            NoMinSizeContainer {
                NoIntrinsicSizeContainer(
                    Modifier.paint(TestPainter(containerWidth, containerHeight))
                ) {
                    // Intentionally empty
                }
            }
        }

        rule.obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Red.toArgb(), getPixel(0, 0))
            assertEquals(Color.Red.toArgb(), getPixel(containerWidth.roundToInt() - 1, 0))
            assertEquals(
                Color.Red.toArgb(),
                getPixel(
                    containerWidth.roundToInt() - 1,
                    containerHeight.roundToInt() - 1
                )
            )
            assertEquals(Color.Red.toArgb(), getPixel(0, containerHeight.roundToInt() - 1))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterIntrinsicSizeDoesNotExceedMax() {
        val containerSize = containerWidth.roundToInt() / 2
        rule.setContent {
            NoIntrinsicSizeContainer(
                Modifier
                    .background(Color.White)
                    .then(FixedSizeModifier(containerWidth.roundToInt()))
            ) {
                NoIntrinsicSizeContainer(
                    AlignTopLeft.then(
                        FixedSizeModifier(containerSize).paint(
                            TestPainter(
                                containerWidth,
                                containerHeight
                            ),
                            alignment = Alignment.TopStart
                        )
                    )
                ) {
                    // Intentionally empty
                }
            }
        }

        rule.obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Red.toArgb(), getPixel(0, 0))
            assertEquals(Color.Red.toArgb(), getPixel(containerWidth.roundToInt() / 2 - 1, 0))
            assertEquals(
                Color.White.toArgb(),
                getPixel(
                    containerWidth.roundToInt() - 1,
                    containerHeight.roundToInt() - 1
                )
            )
            assertEquals(Color.Red.toArgb(), getPixel(0, containerHeight.roundToInt() / 2 - 1))

            assertEquals(Color.White.toArgb(), getPixel(containerWidth.roundToInt() / 2 + 1, 0))
            assertEquals(
                Color.White.toArgb(),
                getPixel(
                    containerWidth.roundToInt() / 2 + 1,
                    containerHeight.roundToInt() / 2 + 1
                )
            )
        }
    }

    @Test
    fun testUnboundedPainterDoesNotCrash() {
        rule.setContent {
            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    // Lazy column has unbounded height so ensure that the constraints
                    // provided to Painters without an intrinsic size are with a finite
                    // range (i.e. don't crash)
                    Image(
                        painter = ColorPainter(Color.Black),
                        contentDescription = ""
                    )
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterNotSizedToIntrinsics() {
        val containerSize = containerWidth.roundToInt() / 2
        rule.setContent {
            NoIntrinsicSizeContainer(
                Modifier.background(Color.White).then(FixedSizeModifier(containerSize))
            ) {
                NoIntrinsicSizeContainer(
                    FixedSizeModifier(containerSize).paint(
                        TestPainter(
                            containerWidth,
                            containerHeight
                        ),
                        sizeToIntrinsics = false, alignment = Alignment.TopStart
                    )
                ) {
                    // Intentionally empty
                }
            }
        }

        rule.obtainScreenshotBitmap(
            containerSize,
            containerSize
        ).apply {
            assertEquals(Color.Red.toArgb(), getPixel(0, 0))
            assertEquals(Color.Red.toArgb(), getPixel(containerSize - 1, 0))
            assertEquals(
                Color.Red.toArgb(),
                getPixel(
                    containerSize - 1,
                    containerSize - 1
                )
            )
            assertEquals(Color.Red.toArgb(), getPixel(0, containerSize - 1))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterIntrinsicWidthRespected() {
        rule.setContent {
            NoMinSizeContainer {
                NoIntrinsicSizeContainer(
                    Modifier
                        .width(IntrinsicSize.Min)
                        .paint(TestPainter(containerWidth, containerHeight))
                ) {
                    // Intentionally empty
                }
            }
        }

        rule.obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Red.toArgb(), getPixel(0, 0))
            assertEquals(Color.Red.toArgb(), getPixel(containerWidth.roundToInt() - 1, 0))
            assertEquals(
                Color.Red.toArgb(),
                getPixel(
                    containerWidth.roundToInt() - 1,
                    containerHeight.roundToInt() - 1
                )
            )
            assertEquals(Color.Red.toArgb(), getPixel(0, containerHeight.roundToInt() - 1))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterIntrinsicHeightRespected() {
        rule.setContent {
            NoMinSizeContainer {
                NoIntrinsicSizeContainer(
                    Modifier
                        .height(IntrinsicSize.Min)
                        .paint(TestPainter(containerWidth, containerHeight))
                ) {
                    // Intentionally empty
                }
            }
        }

        rule.obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Red.toArgb(), getPixel(0, 0))
            assertEquals(Color.Red.toArgb(), getPixel(containerWidth.roundToInt() - 1, 0))
            assertEquals(
                Color.Red.toArgb(),
                getPixel(
                    containerWidth.roundToInt() - 1,
                    containerHeight.roundToInt() - 1
                )
            )
            assertEquals(Color.Red.toArgb(), getPixel(0, containerHeight.roundToInt() - 1))
        }
    }

    @Test
    fun testPainterFixedHeightScalesDownWidth() {
        val composableHeightPx = 100f
        val composableMaxWidthPx = 300f
        val painterWidth = 400f
        val painterHeight = 200f

        val density = rule.density.density
        // The resultant composable should match the height provided in the height modifier
        // however, despite the width being a maximum of 300 pixels, the composable
        // should be 200 pixels wide as the painter is scaled down to ensure the height constraint
        // is satisfied. Because the Painter is twice as tall as the composable, the composable
        // width should be half that of the painter
        testPainterScaleMatchesSize(
            Modifier.requiredHeight(((composableHeightPx) / density).dp)
                .requiredWidthIn(0.dp, (composableMaxWidthPx / density).dp),
            ContentScale.Inside,
            Size(painterWidth, painterHeight),
            painterWidth / 2,
            composableHeightPx
        )
    }

    @Test
    fun testPainterFixedWidthScalesDownHeight() {
        val composableWidthPx = 100f
        val composableMaxHeightPx = 300f
        val painterWidth = 400f
        val painterHeight = 200f

        val density = rule.density.density
        // The resultant composable should match the height provided in the height modifier
        // however, despite the width being a maximum of 300 pixels, the resultant composable
        // should be 200 pixels wide as the painter is scaled down to ensure the height constraint
        // is satisfied. Because the Painter is twice as tall as the composable, the composable
        // width should be half that of the painter
        testPainterScaleMatchesSize(
            Modifier.requiredWidth(((composableWidthPx) / density).dp)
                .requiredHeightIn(0.dp, (composableMaxHeightPx / density).dp),
            ContentScale.Inside,
            Size(painterWidth, painterHeight),
            composableWidthPx,
            painterHeight / 4
        )
    }

    @Test
    fun testPainterFixedDimensionUnchanged(): Unit = with(rule.density) {
        val painterWidth = 1000f
        val painterHeight = 375f
        val composableWidth = 250f
        val composableHeight = 400f
        // Because the constraints are tight here, do not attempt to resize the composable
        // based on the intrinsic dimensions of the Painter
        testPainterScaleMatchesSize(
            Modifier.requiredWidth(composableWidth.toDp())
                .requiredHeight(composableHeight.toDp()),
            ContentScale.Fit,
            Size(painterWidth, painterHeight),
            composableWidth,
            composableHeight
        )
    }

    @Test
    fun testPainterComposableHeightScaledUpWithFixedWidth() {
        val composableWidthPx = 200f
        val painterWidth = 100f
        val painterHeight = 200f
        // A Painter with ContentScale.FillWidth will scale its content to ensure that the
        // composable width fills its width constraint. This also scales the height by the
        // same scale factor. Because the intrinsic width is twice that of the width constraint,
        // the height should be double that of the intrinsic height of the painter
        testPainterScaleMatchesSize(
            Modifier.requiredWidth((composableWidthPx / rule.density.density).dp)
                .wrapContentHeight(),
            ContentScale.FillWidth,
            Size(painterWidth, painterHeight),
            composableWidthPx,
            painterHeight * 2
        )
    }

    @Test
    fun testPainterWidthScaledDownWithSmallerHeight() {
        val composableWidthPx = 200f
        val painterWidth = 100f
        val painterHeight = 200f
        // A Painter with ContentScale.Inside should scale its content down to fit within the
        // constraints of the composable
        // In this case a fixed width that is larger than the painter with undefined height
        // should have the composable width match that of its input and the height match
        // that of the painter
        testPainterScaleMatchesSize(
            Modifier.requiredWidth((composableWidthPx / rule.density.density).dp)
                .wrapContentHeight(),
            ContentScale.Inside,
            Size(painterWidth, painterHeight),
            composableWidthPx,
            painterHeight
        )
    }

    private fun testPainterScaleMatchesSize(
        modifier: Modifier,
        contentScale: ContentScale,
        painterSize: Size,
        composableWidthPx: Float,
        composableHeightPx: Float
    ) = with(rule.density) {
        val composableWidth = composableWidthPx.toDp()
        val composableHeight = composableHeightPx.toDp()
        rule.setContent {
            // Because the painter is told to fit inside the constraints, the width should
            // match that of the provided fixed width and the height should match that of the
            // composable as no scaling is being done
            val painter = object : Painter() {
                override val intrinsicSize: Size
                    get() = painterSize

                override fun DrawScope.onDraw() { /* no-op */
                }
            }
            Box(
                modifier =
                    Modifier.then(modifier)
                        .paint(painter, contentScale = contentScale)
            )
        }

        rule.onRoot()
            .assertWidthIsEqualTo(composableWidth)
            .assertHeightIsEqualTo(composableHeight)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testBitmapPainterScalesContent(): Unit = with(rule.density) {
        // BitmapPainter should handle scaling its content image up to fill the
        // corresponding content bounds. Because the composable is twice the
        // height of the image and we are providing ContentScale.FillHeight
        // the BitmapPainter should draw the image with twice its original
        // height and width centered within the bounds of the composable
        val boxWidth = 300
        val boxHeight = 200
        val srcImage = ImageBitmap(50, 100)
        val canvas = Canvas(srcImage)
        val paint = Paint().apply { this.color = Color.Red }
        canvas.drawRect(0f, 0f, 200f, 100f, paint)

        val testTag = "testTag"

        rule.setContent {
            Box(
                modifier = Modifier
                    .testTag(testTag)
                    .background(color = Color.Gray)
                    .requiredWidth(boxWidth.toDp())
                    .requiredHeight(boxHeight.toDp())
                    .paint(BitmapPainter(srcImage), contentScale = ContentScale.FillHeight)
            )
        }

        rule.obtainScreenshotBitmap(
            boxWidth,
            boxHeight
        ).apply {
            assertEquals(width, boxWidth)
            assertEquals(height, boxHeight)
            assertEquals(Color.Gray.toArgb(), getPixel(boxWidth / 2 - srcImage.width - 5, 0))
            assertEquals(
                Color.Gray.toArgb(),
                getPixel(boxWidth / 2 + srcImage.width + 5, boxHeight - 1)
            )
            assertEquals(Color.Red.toArgb(), getPixel(boxWidth / 2 - srcImage.width + 5, 0))
            assertEquals(
                Color.Red.toArgb(),
                getPixel(boxWidth / 2 + srcImage.width - 5, boxHeight - 1)
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testBitmapPainterScalesNonUniformly() {
        // The composable dimensions are larger than the ImageBitmap. By not passing in
        // a ContentScale parameter to the painter, the ImageBitmap should be stretched
        // non-uniformly to fully occupy the bounds of the composable
        val boxWidth = 60
        val boxHeight = 40
        val srcImage = ImageBitmap(10, 20)
        val canvas = Canvas(srcImage)
        val paint = Paint().apply { this.color = Color.Red }
        canvas.drawRect(0f, 0f, 40f, 20f, paint)

        val testTag = "testTag"

        rule.setContent {
            Box(
                modifier = Modifier
                    .testTag(testTag)
                    .background(color = Color.Gray)
                    .requiredWidth((boxWidth / LocalDensity.current.density).dp)
                    .requiredHeight((boxHeight / LocalDensity.current.density).dp)
                    .paint(BitmapPainter(srcImage), contentScale = ContentScale.FillBounds)
            )
        }

        rule.obtainScreenshotBitmap(boxWidth, boxHeight).asImageBitmap().assertPixels { Color.Red }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testVectorPainterScalesContent(): Unit = with(rule.density) {
        // VectorPainter should handle scaling its content vector up to fill the
        // corresponding content bounds. Because the composable is twice the
        // height of the vector and we are providing ContentScale.FillHeight
        // the VectorPainter should draw the vector with twice its original
        // height and width centered within the bounds of the composable
        val boxWidth = 300
        val boxHeight = 200

        val vectorWidth = 50
        val vectorHeight = 100
        rule.setContent {
            val vectorWidthDp = vectorWidth.toDp()
            val vectorHeightDp = vectorHeight.toDp()
            Box(
                modifier = Modifier.background(color = Color.Gray)
                    .requiredWidth(boxWidth.toDp())
                    .requiredHeight(boxHeight.toDp())
                    .paint(
                        rememberVectorPainter(
                            defaultWidth = vectorWidthDp,
                            defaultHeight = vectorHeightDp,
                            autoMirror = false,
                            content = { viewportWidth, viewportHeight ->
                                Path(
                                    fill = SolidColor(Color.Red),
                                    pathData = PathData {
                                        horizontalLineToRelative(viewportWidth)
                                        verticalLineToRelative(viewportHeight)
                                        horizontalLineToRelative(-viewportWidth)
                                        close()
                                    }
                                )
                            }
                        ),
                        contentScale = ContentScale.FillHeight
                    )
            )
        }

        rule.obtainScreenshotBitmap(
            boxWidth,
            boxHeight
        ).apply {
            assertEquals(width, boxWidth)
            assertEquals(height, boxHeight)
            assertEquals(Color.Gray.toArgb(), getPixel(boxWidth / 2 - vectorWidth - 5, 0))
            assertEquals(
                Color.Gray.toArgb(),
                getPixel(boxWidth / 2 + vectorWidth + 5, boxHeight - 1)
            )
            assertEquals(Color.Red.toArgb(), getPixel(boxWidth / 2 - vectorWidth + 5, 0))
            assertEquals(
                Color.Red.toArgb(),
                getPixel(boxWidth / 2 + vectorWidth - 5, boxHeight - 1)
            )
        }
    }

    @Test
    @SmallTest
    fun testInspectable() {
        val painter = TestPainter(10f, 20f)
        val modifier = Modifier.paint(painter) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("paint")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("painter", painter),
            ValueElement("sizeToIntrinsics", true),
            ValueElement("alignment", Alignment.Center),
            ValueElement("contentScale", ContentScale.Inside),
            ValueElement("alpha", DefaultAlpha),
            ValueElement("colorFilter", null)
        )
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testBackgroundPainterChain() {
        val painter = TestPainter(100f, 100f).apply {
            color = Color.Red
        }

        val painter2 = TestPainter(100f, 100f).apply {
            color = Color.Blue.copy(alpha = 0.5f)
        }

        val tag = "testTag"
        var sizePx = 0f
        val size = 2.dp
        rule.setContent {
            with(LocalDensity.current) { sizePx = size.toPx() }
            Box(
                modifier = Modifier.testTag(tag)
                    .size(size)
                    .paint(painter)
                    .paint(painter2)
            )
        }

        rule.onNodeWithTag(tag).captureToImage().apply {
            assertEquals(sizePx.roundToInt(), width)
            assertEquals(sizePx.roundToInt(), height)
            assertPixels {
                // Verify that the last painter in the chain covers all the pixels rendered by
                // the painter before it
                Color.Blue.copy(alpha = 0.5f).compositeOver(Color.Red)
            }
        }
    }

    @Composable
    private fun TestPainter(
        alpha: Float = DefaultAlpha,
        colorFilter: ColorFilter? = null,
        rtl: Boolean = false
    ) {
        val p = TestPainter(containerWidth, containerHeight)
        val layoutDirection = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            AtLeastSize(
                modifier = Modifier.background(Color.White)
                    .paint(p, alpha = alpha, colorFilter = colorFilter),
                size = containerWidth.roundToInt()
            ) {
                // Intentionally empty
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ComposeTestRule.obtainScreenshotBitmap(width: Int, height: Int = width): Bitmap {
    val bitmap = onRoot().captureToImage()
    assertEquals(width, bitmap.width)
    assertEquals(height, bitmap.height)
    return bitmap.asAndroidBitmap()
}

private class TestPainter(
    val width: Float,
    val height: Float
) : Painter() {

    var color = Color.Red

    override val intrinsicSize: Size
        get() = Size(width, height)

    override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean {
        color = if (layoutDirection == LayoutDirection.Rtl) Color.Blue else Color.Red
        return true
    }

    override fun DrawScope.onDraw() {
        drawRect(color = color)
    }
}

/**
 * Container composable that relaxes the minimum width and height constraints
 * before giving them to their child
 */
@Composable
fun NoMinSizeContainer(content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val loosenedConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loosenedConstraints) }
        val maxPlaceableWidth = placeables.maxByOrNull { it.width }?.width ?: 0
        val maxPlaceableHeight = placeables.maxByOrNull { it.height }?.width ?: 0
        val width = max(maxPlaceableWidth, loosenedConstraints.minWidth)
        val height = max(maxPlaceableHeight, loosenedConstraints.minHeight)
        layout(width, height) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}

/**
 * Composable that is sized purely by the constraints given by its modifiers
 */
@Composable
fun NoIntrinsicSizeContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content, modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val width = max(
            placeables.maxByOrNull { it.width }?.width ?: 0,
            constraints
                .minWidth
        )
        val height = max(
            placeables.maxByOrNull { it.height }?.height ?: 0,
            constraints
                .minHeight
        )
        layout(width, height) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}

class FixedSizeModifier(val width: Int, val height: Int = width) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(
            Constraints(
                minWidth = width,
                minHeight = height,
                maxWidth = width,
                maxHeight = height
            )
        )
        return layout(width, height) {
            placeable.place(0, 0)
        }
    }
}