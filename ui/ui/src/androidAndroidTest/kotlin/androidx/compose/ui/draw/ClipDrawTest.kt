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

package androidx.compose.ui.draw

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Padding
import androidx.compose.ui.assertColorsEqual
import androidx.compose.ui.assertRect
import androidx.compose.ui.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.padding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.waitAndScreenShot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class ClipDrawTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    private val rectShape = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
            Outline.Rectangle(size.toRect())
    }
    private val triangleShape = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
            Outline.Generic(
                Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
            )
    }
    private val invertedTriangleShape = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
            Outline.Generic(
                Path().apply {
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    lineTo(0f, 0f)
                    close()
                }
            )
    }

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleRectClip() {
        rule.runOnUiThreadIR {
            activity.setContent {
                Padding(size = 10, modifier = Modifier.fillColor(Color.Green)) {
                    AtLeastSize(
                        size = 10,
                        modifier = Modifier.clip(rectShape).fillColor(Color.Cyan)
                    ) {
                    }
                }
            }
        }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 10)
            assertRect(Color.Green, holeSize = 10)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleClipToBounds() {
        rule.runOnUiThreadIR {
            activity.setContent {
                Padding(size = 10, modifier = Modifier.fillColor(Color.Green)) {
                    AtLeastSize(
                        size = 10,
                        modifier = Modifier.clipToBounds().fillColor(Color.Cyan)
                    ) {
                    }
                }
            }
        }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 10)
            assertRect(Color.Green, holeSize = 10)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleRectClipWithModifiers() {
        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 10,
                    modifier = Modifier.fillColor(Color.Green)
                        .padding(10)
                        .clip(rectShape)
                        .fillColor(Color.Cyan)
                ) {}
            }
        }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 10)
            assertRect(Color.Green, holeSize = 10)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun roundedUniformRectClip() {
        val shape = object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ) = Outline.Rounded(RoundRect(size.toRect(), CornerRadius(12f)))
        }
        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 30,
                    modifier = Modifier.fillColor(Color.Green)
                        .clip(shape)
                        .fillColor(Color.Cyan)
                ) {}
            }
        }

        takeScreenShot(30).apply {
            // check corners
            assertColor(Color.Green, 2, 2)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 27, 2)
            // check inner rect
            assertRect(Color.Cyan, size = 18)
            // check centers of all sides
            assertColor(Color.Cyan, 0, 14)
            assertColor(Color.Cyan, 29, 14)
            assertColor(Color.Cyan, 14, 0)
            assertColor(Color.Cyan, 14, 29)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun roundedRectWithDiffCornersClip() {
        val shape = object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ) = Outline.Rounded(
                RoundRect(
                    size.toRect(),
                    CornerRadius.Zero,
                    CornerRadius(12f),
                    CornerRadius(12f),
                    CornerRadius(12f)
                )
            )
        }
        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 30,
                    modifier = Modifier.fillColor(Color.Green)
                        .clip(shape)
                        .fillColor(Color.Cyan)
                ) {}
            }
        }

        takeScreenShot(30).apply {
            // check corners
            assertColor(Color.Cyan, 2, 2)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 27, 2)
            // check inner rect
            assertRect(Color.Cyan, size = 18)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun triangleClip() {
        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 30,
                    modifier = Modifier.fillColor(Color.Green)
                        .clip(triangleShape)
                        .fillColor(Color.Cyan)
                ) {}
            }
        }

        takeScreenShot(30).apply {
            assertTriangle(Color.Cyan, Color.Green)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun concaveClip() {
        // 30 pixels rect with a rect hole of 10 pixels in the middle
        val concaveShape = object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ) = Outline.Generic(
                Path().apply {
                    op(
                        Path().apply { addRect(Rect(0f, 0f, 30f, 30f)) },
                        Path().apply { addRect(Rect(10f, 10f, 20f, 20f)) },
                        PathOperation.Difference
                    )
                }
            )
        }
        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 30,
                    modifier = Modifier.fillColor(Color.Green)
                        .clip(concaveShape)
                        .fillColor(Color.Cyan)
                ) {}
            }
        }

        takeScreenShot(30).apply {
            assertRect(color = Color.Green, size = 10)
            assertRect(color = Color.Cyan, size = 30, holeSize = 10)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromRectToRounded() {
        val model = mutableStateOf<Shape>(rectShape)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 30,
                    modifier = Modifier.fillColor(Color.Green)
                        .clip(model.value)
                        .fillColor(Color.Cyan)
                ) {}
            }
        }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 30)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            model.value = object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ) = Outline.Rounded(RoundRect(size.toRect(), CornerRadius(12f)))
            }
        }

        takeScreenShot(30).apply {
            assertColor(Color.Green, 2, 2)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 2, 27)
            assertColor(Color.Green, 27, 2)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromRectToPath() {
        val model = mutableStateOf<Shape>(rectShape)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 30,
                    modifier = Modifier.fillColor(Color.Green)
                        .clip(model.value)
                        .fillColor(Color.Cyan)
                ) {}
            }
        }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 30)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR { model.value = triangleShape }

        takeScreenShot(30).apply {
            assertTriangle(Color.Cyan, Color.Green)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromPathToRect() {
        val model = mutableStateOf<Shape>(triangleShape)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 30,
                    modifier = Modifier.fillColor(Color.Green)
                        .clip(model.value)
                        .fillColor(Color.Cyan)
                ) {}
            }
        }

        takeScreenShot(30).apply {
            assertTriangle(Color.Cyan, Color.Green)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR { model.value = rectShape }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 30)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchBetweenDifferentPaths() {
        val model = mutableStateOf<Shape>(triangleShape)
        // to be replaced with a DrawModifier wrapped into remember, so the recomposition
        // is not causing invalidation as the DrawModifier didn't change
        val drawCallback: DrawScope.() -> Unit = {
            drawRect(
                Color.Cyan,
                topLeft = Offset(-100f, -100f),
                size = Size(size.width + 200f, size.height + 200f)
            )
        }

        val clip = Modifier.graphicsLayer {
            shape = model.value
            clip = true
            drawLatch.countDown()
        }

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 30,
                    modifier = Modifier.background(Color.Green)
                        .then(clip)
                        .drawBehind(drawCallback)
                ) {
                }
            }
        }

        takeScreenShot(30).apply {
            assertTriangle(Color.Cyan, Color.Green)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR { model.value = invertedTriangleShape }

        takeScreenShot(30).apply {
            assertInvertedTriangle(Color.Cyan, Color.Green)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun emitClipLater() {
        val model = mutableStateOf(false)

        rule.runOnUiThreadIR {
            activity.setContent {
                Padding(size = 10, modifier = Modifier.fillColor(Color.Green)) {
                    val modifier = if (model.value) {
                        Modifier.clip(rectShape).fillColor(Color.Cyan)
                    } else {
                        Modifier
                    }
                    AtLeastSize(size = 10, modifier = modifier) {
                    }
                }
            }
        }
        Assert.assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            model.value = true
        }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 10)
            assertRect(Color.Green, holeSize = 10)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchLayoutDirection() {
        val direction = mutableStateOf(LayoutDirection.Ltr)
        val shape = object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ) = if (layoutDirection == LayoutDirection.Ltr) {
                rectShape.createOutline(size, layoutDirection, density)
            } else {
                triangleShape.createOutline(size, layoutDirection, density)
            }
        }

        rule.runOnUiThreadIR {
            activity.setContent {
                CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                    AtLeastSize(
                        size = 30,
                        modifier = Modifier.fillColor(Color.Green)
                            .clip(shape)
                            .fillColor(Color.Cyan)
                    ) {}
                }
            }
        }

        takeScreenShot(30).apply {
            assertRect(Color.Cyan, size = 30)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThread { direction.value = LayoutDirection.Rtl }

        takeScreenShot(30).apply {
            assertTriangle(Color.Cyan, Color.Green)
        }
    }

    private fun Modifier.fillColor(color: Color): Modifier {
        return drawBehind {
            drawRect(
                color,
                topLeft = Offset(-100f, -100f),
                size = Size(size.width + 200f, size.height + 200f)
            )
            drawLatch.countDown()
        }
    }

    // waitAndScreenShot() requires API level 26
    @RequiresApi(Build.VERSION_CODES.O)
    private fun takeScreenShot(size: Int): Bitmap {
        Assert.assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = rule.waitAndScreenShot()
        Assert.assertEquals(size, bitmap.width)
        Assert.assertEquals(size, bitmap.height)
        return bitmap
    }
}

fun Bitmap.assertTriangle(innerColor: Color, outerColor: Color) {
    Assert.assertEquals(width, height)
    val center = (width - 1) / 2
    val last = width - 1
    // check center
    assertColor(innerColor, center, center)
    // check top corners
    assertColor(outerColor, 4, 4)
    assertColor(outerColor, last - 4, 4)
    // check bottom corners
    assertColor(outerColor, 0, last - 4)
    assertColor(innerColor, 4, last - 4)
    assertColor(outerColor, last, last - 6)
    assertColor(innerColor, last - 6, last)
    // check top center
    assertColor(outerColor, center - 4, 0)
    assertColor(outerColor, center + 4, 0)
    assertColor(innerColor, center, 8)
}

fun Bitmap.assertInvertedTriangle(innerColor: Color, outerColor: Color) {
    Assert.assertEquals(width, height)
    val center = (width - 1) / 2
    val last = width - 1
    // check center
    assertColor(innerColor, center, center)
    // check top corners
    assertColor(outerColor, 0, 4)
    assertColor(innerColor, 4, 4)
    assertColor(outerColor, last, 4)
    assertColor(innerColor, last - 4, 0)
    // check bottom corners
    assertColor(outerColor, 4, last - 4)
    assertColor(outerColor, last - 4, last - 4)
    // check bottom center
    assertColor(outerColor, center - 4, last)
    assertColor(outerColor, center + 4, last)
    assertColor(innerColor, center, last - 6)
}

fun Bitmap.assertColor(expectedColor: Color, x: Int, y: Int) {
    val pixel = Color(getPixel(x, y))
    assertColorsEqual(expectedColor, pixel) {
        "Pixel [$x, $y] is expected to be $expectedColor," + " " + "but was $pixel"
    }
}
