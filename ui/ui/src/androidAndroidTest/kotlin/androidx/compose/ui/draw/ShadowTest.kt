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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.first
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.waitAndScreenShot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class ShadowTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    private val rectShape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ) = Outline.Rectangle(size.toRect())
    }

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun tearDown() {
        isDebugInspectorInfoEnabled = false
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shadowDrawn() {
        rule.runOnUiThreadIR {
            activity.setContent {
                ShadowContainer()
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(12).apply {
            hasShadow()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shadowDrawnInsideRenderNode() {
        rule.runOnUiThreadIR {
            activity.setContent {
                ShadowContainer(modifier = Modifier.graphicsLayer())
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(12).apply {
            hasShadow()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromShadowToNoShadow() {
        val elevation = mutableStateOf(0.dp)

        rule.runOnUiThreadIR {
            activity.setContent {
                ShadowContainer(elevation = elevation)
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThreadIR {
            elevation.value = 0.dp
        }

        takeScreenShot(12).apply {
            assertEquals(color(5, 11), Color.White)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromNoShadowToShadowWithNestedRepaintBoundaries() {
        val elevation = mutableStateOf(0.dp)

        rule.runOnUiThreadIR {
            activity.setContent {
                ShadowContainer(modifier = Modifier.graphicsLayer(clip = true), elevation)
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThreadIR {
            elevation.value = 12.dp
        }

        takeScreenShot(12).apply {
            hasShadow()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun opacityAppliedForTheShadow() {
        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 12, modifier = Modifier.background(Color.White)) {
                    val elevation = with(LocalDensity.current) { 4.dp.toPx() }
                    AtLeastSize(
                        size = 10,
                        modifier = Modifier.graphicsLayer(
                            shadowElevation = elevation,
                            shape = rectShape,
                            alpha = 0.5f
                        )
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(12).apply {
            val shadowColor = color(width / 2, height - 1)
            // assert the shadow is still visible
            assertNotEquals(shadowColor, Color.White)
            // but the shadow is not as dark as it would be without opacity.
            // Full opacity depends on the device, but is around 0.8 luminance.
            // At 50%, the luminance is over 0.9
            assertTrue(shadowColor.luminance() > 0.9f)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun colorsAppliedForTheShadow() {
        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 12, modifier = Modifier.background(Color.White)) {
                    val elevation = with(LocalDensity.current) { 4.dp.toPx() }
                    AtLeastSize(
                        size = 10,
                        modifier = Modifier.graphicsLayer(
                            shadowElevation = elevation,
                            shape = rectShape,
                            ambientShadowColor = Color(0xFFFF00FF),
                            spotShadowColor = Color(0xFFFF00FF),
                        )
                    ) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        takeScreenShot(12).apply {
            val shadowColor = color(width / 2, height - 1)
            // assert the shadow is still visible
            assertNotEquals(shadowColor, Color.White)
            // The shadow should have a magenta hue
            assertTrue(shadowColor.red > shadowColor.green)
            assertTrue(shadowColor.blue > shadowColor.green)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun emitShadowLater() {
        val model = mutableStateOf(false)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 12, modifier = Modifier.background(Color.White)) {
                    val shadow = if (model.value) {
                        Modifier.shadow(8.dp, rectShape)
                    } else {
                        Modifier
                    }
                    AtLeastSize(size = 10, modifier = shadow) {
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            model.value = true
        }

        takeScreenShot(12).apply {
            hasShadow()
        }
    }

    @Test
    fun testInspectorValue() {
        rule.runOnUiThreadIR {
            val modifier = Modifier.shadow(4.0.dp).first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("shadow")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.asIterable()).containsExactly(
                ValueElement("elevation", 4.0.dp),
                ValueElement("shape", RectangleShape),
                ValueElement("clip", true),
                ValueElement("ambientColor", DefaultShadowColor),
                ValueElement("spotColor", DefaultShadowColor)
            )
        }
    }

    @Test
    fun elevationWithinModifier() {
        val elevation = mutableStateOf(0f)
        val color = mutableStateOf(Color.Blue)
        val underColor = mutableStateOf(Color.Transparent)
        val modifier = Modifier.graphicsLayer()
            .background(underColor)
            .drawLatchModifier()
            .graphicsLayer {
                shadowElevation = elevation.value
            }
            .background(color)

        rule.runOnUiThread {
            activity.setContent {
                androidx.compose.ui.FixedSize(30, modifier)
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)

        rule.runOnUiThread {
            color.value = Color.Red
        }

        Assert.assertFalse(drawLatch.await(200, TimeUnit.MILLISECONDS))

        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            elevation.value = 1f
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)

        rule.runOnUiThread {
            elevation.value = 2f // elevation was already 1, so it doesn't need to enableZ again
        }
        Assert.assertFalse(drawLatch.await(200, TimeUnit.MILLISECONDS))

        rule.runOnUiThread {
            elevation.value = 0f // going to 0 doesn't trigger invalidation
        }
        Assert.assertFalse(drawLatch.await(200, TimeUnit.MILLISECONDS))

        rule.runOnUiThread {
            elevation.value = 1f // going to 1 won't invalidate because it was last drawn with Z
        }
        Assert.assertFalse(drawLatch.await(200, TimeUnit.MILLISECONDS))

        rule.runOnUiThread {
            elevation.value = 0f
            underColor.value = Color.Black
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)

        rule.runOnUiThread {
            elevation.value = 1f
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
    }

    @Composable
    private fun ShadowContainer(
        modifier: Modifier = Modifier,
        elevation: State<Dp> = mutableStateOf(8.dp)
    ) {
        AtLeastSize(size = 12, modifier = modifier.background(Color.White)) {
            AtLeastSize(
                size = 10,
                modifier = Modifier.shadow(elevation = elevation.value, shape = rectShape)
            ) {
            }
        }
    }

    private fun Bitmap.hasShadow() {
        assertNotEquals(color(width / 2, height - 1), Color.White)
    }

    private fun Modifier.background(color: Color): Modifier = drawBehind {
        drawRect(color)
        drawLatch.countDown()
    }

    fun Modifier.drawLatchModifier() = drawBehind { drawLatch.countDown() }

    private fun Modifier.background(
        color: State<Color>
    ) = drawBehind {
        if (color.value != Color.Transparent) {
            drawRect(color.value)
        }
    }

    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        val bitmap = rule.waitAndScreenShot()
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
        return bitmap
    }
}
