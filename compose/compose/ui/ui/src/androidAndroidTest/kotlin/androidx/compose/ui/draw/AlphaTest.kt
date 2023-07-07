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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertColorsEqual
import androidx.compose.ui.assertRect
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.waitAndScreenShot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max

@MediumTest
@RunWith(AndroidJUnit4::class)
class AlphaTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch
    private val unlatch = Modifier.drawBehind { drawLatch.countDown() }

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawFullAlpha() {
        val color = Color.LightGray
        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 10,
                    modifier = Modifier.background(Color.White)
                        .alpha(1f)
                        .background(color)
                        .then(unlatch)
                ) {
                }
            }
        }

        takeScreenShot(10).apply {
            assertRect(color)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawZeroAlpha() {
        val color = Color.LightGray
        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 10,
                    modifier = Modifier.background(Color.White)
                        .alpha(0f)
                        .background(color)
                        .then(unlatch)
                ) {
                }
            }
        }

        takeScreenShot(10).apply {
            assertRect(Color.White)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawHalfAlpha() {
        val color = Color.Red
        rule.runOnUiThreadIR {
            activity.setContent {
                Row(Modifier.background(Color.White)) {
                    AtLeastSize(
                        size = 10,
                        modifier = Modifier.background(Color.White)
                            .alpha(0.5f)
                            .background(color)
                            .then(unlatch)
                    ) {
                    }
                    AtLeastSize(
                        size = 10,
                        modifier = Modifier.background(color.copy(alpha = 0.5f))
                    ) {
                    }
                }
            }
        }

        takeScreenShot(20, 10).apply {
            assertColorsEqual(color(5, 5), color(15, 5))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromHalfAlphaToFull() {
        val color = Color.Green
        val alpha = mutableStateOf(0.5f)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 10,
                    modifier = Modifier.background(Color.White)
                        .alpha(alpha.value)
                        .then(unlatch)
                        .background(color)
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThreadIR {
            alpha.value = 1f
        }

        takeScreenShot(10).apply {
            assertRect(color)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromZeroAlphaToFullWithNestedRepaintBoundaries() {
        val color = Color.Green
        var alpha by mutableStateOf(0f)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 10,
                    modifier = Modifier.background(Color.White)
                        .alpha(1f)
                        .alpha(alpha)
                        .then(unlatch)
                        .background(color)
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThreadIR {
            alpha = 1f
        }

        takeScreenShot(10).apply {
            assertRect(color)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun emitDrawWithAlphaLater() {
        val model = mutableStateOf(false)

        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    size = 10,
                    modifier = Modifier.background(Color.White)
                        .run {
                            if (model.value) alpha(0f).background(Color.Green) else this
                        }
                        .then(unlatch)
                ) {
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            model.value = true
        }

        takeScreenShot(10).apply {
            assertRect(Color.White)
        }
    }

    // waitAndScreenShot() requires API level 26
    @RequiresApi(Build.VERSION_CODES.O)
    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = rule.waitAndScreenShot()
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
        return bitmap
    }
}

@Composable
fun Row(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        var width = 0
        var height = 0
        placeables.forEach {
            width += it.width
            height = max(height, it.height)
        }
        layout(width, height) {
            var offset = 0
            placeables.forEach {
                it.placeRelative(offset, 0)
                offset += it.width
            }
        }
    }
}

fun Bitmap.color(x: Int, y: Int): Color = Color(getPixel(x, y))
