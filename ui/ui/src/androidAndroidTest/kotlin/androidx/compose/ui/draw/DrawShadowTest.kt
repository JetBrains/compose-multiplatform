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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.drawBehind
import androidx.compose.ui.drawLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.waitAndScreenShot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
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
class DrawShadowTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    private val rectShape = object : Shape {
        override fun createOutline(size: Size, density: Density): Outline =
            Outline.Rectangle(size.toRect())
    }

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
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
                ShadowContainer(modifier = Modifier.drawLayer())
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
                ShadowContainer(elevation)
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
                ShadowContainer(elevation, modifier = Modifier.drawLayer(clip = true))
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
                AtLeastSize(size = 12, modifier = background(Color.White)) {
                    AtLeastSize(
                        size = 10,
                        modifier = Modifier.drawShadow(4.dp, rectShape, opacity = 0.5f)
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun emitShadowLater() {
        val model = mutableStateOf(false)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 12, modifier = background(Color.White)) {
                    val shadow = if (model.value) {
                        Modifier.drawShadow(8.dp, rectShape)
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

    @Composable
    private fun ShadowContainer(
        elevation: State<Dp> = mutableStateOf(8.dp),
        modifier: Modifier = Modifier
    ) {
        AtLeastSize(size = 12, modifier = modifier.then(background(Color.White))) {
            AtLeastSize(
                size = 10,
                modifier = Modifier.drawShadow(elevation = elevation.value, shape = rectShape)
            ) {
            }
        }
    }

    private fun Bitmap.hasShadow() {
        assertNotEquals(color(width / 2, height - 1), Color.White)
    }

    private fun background(color: Color) = Modifier.drawBehind {
        drawRect(color)
        drawLatch.countDown()
    }

    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        val bitmap = rule.waitAndScreenShot()
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
        return bitmap
    }
}
