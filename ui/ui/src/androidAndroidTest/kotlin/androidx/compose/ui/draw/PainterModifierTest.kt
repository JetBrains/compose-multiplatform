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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.ui.AlignTopLeft
import androidx.compose.ui.Alignment
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Layout
import androidx.compose.ui.LayoutModifier
import androidx.compose.ui.Measurable
import androidx.compose.ui.MeasureScope
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
import androidx.compose.ui.graphics.ImageAsset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.ImagePainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.test.ComposeTestRule
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onRoot
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
    val containerHeight = 100.0f

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
            testPainter(colorFilter = ColorFilter(Color.Cyan, BlendMode.SrcIn))
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
            testPainter(alpha = 0.5f)
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
            testPainter(rtl = true)
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
            Modifier.height(((composableHeightPx) / density).dp)
                .widthIn(0.dp, (composableMaxWidthPx / density).dp),
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
            Modifier.width(((composableWidthPx) / density).dp)
                .heightIn(0.dp, (composableMaxHeightPx / density).dp),
            ContentScale.Inside,
            Size(painterWidth, painterHeight),
            composableWidthPx,
            painterHeight / 4
        )
    }

    @Test
    fun testPainterFixedDimensionUnchanged() {
        val painterWidth = 1000f
        val painterHeight = 375f
        val density = rule.density.density
        val composableWidth = 500f
        val composableHeight = 800f
        // Because the constraints are tight here, do not attempt to resize the composable
        // based on the intrinsic dimensions of the Painter
        testPainterScaleMatchesSize(
            Modifier.width((composableWidth / density).dp).height((composableHeight / density).dp),
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
            Modifier.width((composableWidthPx / rule.density.density).dp).wrapContentHeight(),
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
            Modifier.width((composableWidthPx / rule.density.density).dp).wrapContentHeight(),
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
    ) {
        var composableWidth = 0f
        var composableHeight = 0f
        rule.setContent {
            composableWidth = composableWidthPx / DensityAmbient.current.density
            composableHeight = composableHeightPx / DensityAmbient.current.density
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
            .assertWidthIsEqualTo(composableWidth.dp)
            .assertHeightIsEqualTo(composableHeight.dp)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testImagePainterScalesContent() {
        // ImagePainter should handle scaling its content image up to fill the
        // corresponding content bounds. Because the composable is twice the
        // height of the image and we are providing ContentScale.FillHeight
        // the ImagePainter should draw the image with twice its original
        // height and width centered within the bounds of the composable
        val boxWidth = 600
        val boxHeight = 400
        val srcImage = ImageAsset(100, 200)
        val canvas = Canvas(srcImage)
        val paint = Paint().apply { this.color = Color.Red }
        canvas.drawRect(0f, 0f, 400f, 200f, paint)

        val testTag = "testTag"

        rule.setContent {
            Box(
                modifier = Modifier
                    .testTag(testTag)
                    .background(color = Color.Gray)
                    .width((boxWidth / DensityAmbient.current.density).dp)
                    .height((boxHeight / DensityAmbient.current.density).dp)
                    .paint(ImagePainter(srcImage), contentScale = ContentScale.FillHeight)
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
    fun testVectorPainterScalesContent() {
        // VectorPainter should handle scaling its content vector up to fill the
        // corresponding content bounds. Because the composable is twice the
        // height of the vector and we are providing ContentScale.FillHeight
        // the VectorPainter should draw the vector with twice its original
        // height and width centered within the bounds of the composable
        val boxWidth = 600
        val boxHeight = 400

        val vectorWidth = 100
        val vectorHeight = 200
        rule.setContent {
            val vectorWidthDp = (vectorWidth / DensityAmbient.current.density).dp
            val vectorHeightDp = (vectorHeight / DensityAmbient.current.density).dp
            Box(
                modifier = Modifier.background(color = Color.Gray)
                    .width((boxWidth / DensityAmbient.current.density).dp)
                    .height((boxHeight / DensityAmbient.current.density).dp)
                    .paint(
                        rememberVectorPainter(
                            defaultWidth = vectorWidthDp,
                            defaultHeight = vectorHeightDp,
                            children = { viewportWidth, viewportHeight ->
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

    @Composable
    private fun testPainter(
        alpha: Float = DefaultAlpha,
        colorFilter: ColorFilter? = null,
        rtl: Boolean = false
    ) {
        val p = TestPainter(containerWidth, containerHeight)
        val layoutDirection = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        Providers(LayoutDirectionAmbient provides layoutDirection) {
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

private fun ComposeTestRule.obtainScreenshotBitmap(width: Int, height: Int = width): Bitmap {
    val bitmap = onRoot().captureToBitmap()
    assertEquals(width, bitmap.width)
    assertEquals(height, bitmap.height)
    return bitmap
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
fun NoMinSizeContainer(children: @Composable () -> Unit) {
    Layout(children) { measurables, constraints ->
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
    children: @Composable () -> Unit
) {
    Layout(children, modifier) { measurables, constraints ->
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
    ): MeasureScope.MeasureResult {
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