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

package androidx.compose.ui.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.compose.ui.test.captureToBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidCanvasTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testEnableDisableZ() {
        val activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        val drawLatch = CountDownLatch(1)
        var groupView: ViewGroup? = null

        activityTestRule.runOnUiThread {
            val group = EnableDisableZViewGroup(drawLatch, activity)
            groupView = group
            group.setBackgroundColor(Color.WHITE)
            group.layoutParams = ViewGroup.LayoutParams(12, 12)
            val child = View(activity)
            val childLayoutParams = FrameLayout.LayoutParams(10, 10)
            childLayoutParams.gravity = Gravity.TOP or Gravity.LEFT
            child.layoutParams = childLayoutParams
            child.elevation = 4f
            child.setBackgroundColor(Color.WHITE)
            group.addView(child)
            activity.setContentView(group)
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        // Not sure why this test is flaky, so this is just going to make sure that
        // the drawn content can get onto the screen before we capture the bitmap.
        activityTestRule.runOnUiThread { }
        val bitmap = groupView!!.captureToBitmap()
        assertEquals(Color.WHITE, bitmap.getPixel(0, 0))
        assertEquals(Color.WHITE, bitmap.getPixel(9, 9))
        assertNotEquals(Color.WHITE, bitmap.getPixel(10, 10))
    }

    @Test
    fun testScaleWithDefaultPivot() {
        val bg = androidx.compose.ui.graphics.Color.Red
        val fg = androidx.compose.ui.graphics.Color.Blue
        val width = 200
        val height = 200
        val imageAsset = ImageAsset(width, height)
        val canvas = Canvas(imageAsset)
        val paint = Paint().apply { this.color = bg }
        val rect = Rect(Offset.Zero, Size(width.toFloat(), height.toFloat()))
        with(canvas) {
            drawRect(rect, paint)
            withSave {
                scale(0.5f, 0.5f)
                paint.color = fg
                drawRect(rect, paint)
            }
        }

        val pixelMap = imageAsset.toPixelMap()
        assertEquals(fg, pixelMap[0, 0])
        assertEquals(fg, pixelMap[99, 0])
        assertEquals(fg, pixelMap[0, 99])
        assertEquals(fg, pixelMap[99, 99])

        assertEquals(bg, pixelMap[0, 100])
        assertEquals(bg, pixelMap[100, 0])
        assertEquals(bg, pixelMap[100, 100])
        assertEquals(bg, pixelMap[100, 99])
        assertEquals(bg, pixelMap[99, 100])
    }

    @Test
    fun testScaleWithCenterPivot() {
        val bg = androidx.compose.ui.graphics.Color.Red
        val fg = androidx.compose.ui.graphics.Color.Blue
        val width = 200
        val height = 200
        val imageAsset = ImageAsset(width, height)
        val canvas = Canvas(imageAsset)
        val paint = Paint().apply { this.color = bg }
        val rect = Rect(Offset(0.0f, 0.0f), Size(width.toFloat(), height.toFloat()))
        with(canvas) {
            drawRect(rect, paint)
            withSave {
                scale(0.5f, 0.5f, width / 2.0f, height / 2.0f)
                paint.color = fg
                drawRect(rect, paint)
            }
        }

        val pixelMap = imageAsset.toPixelMap()
        val left = width / 2 - 50
        val top = height / 2 - 50
        val right = width / 2 + 50 - 1
        val bottom = height / 2 + 50 - 1
        assertEquals(fg, pixelMap[left, top])
        assertEquals(fg, pixelMap[right, top])
        assertEquals(fg, pixelMap[left, bottom])
        assertEquals(fg, pixelMap[right, bottom])

        assertEquals(bg, pixelMap[left - 1, top - 1])
        assertEquals(bg, pixelMap[left - 1, top])
        assertEquals(bg, pixelMap[left, top - 1])

        assertEquals(bg, pixelMap[right + 1, top - 1])
        assertEquals(bg, pixelMap[right + 1, top])
        assertEquals(bg, pixelMap[right, top - 1])

        assertEquals(bg, pixelMap[left - 1, bottom + 1])
        assertEquals(bg, pixelMap[left - 1, bottom])
        assertEquals(bg, pixelMap[left, bottom + 1])

        assertEquals(bg, pixelMap[right + 1, bottom + 1])
        assertEquals(bg, pixelMap[right + 1, bottom])
        assertEquals(bg, pixelMap[right, bottom + 1])
    }

    @Test
    fun testScaleWithBottomRightPivot() {
        val bg = androidx.compose.ui.graphics.Color.Red
        val fg = androidx.compose.ui.graphics.Color.Blue
        val width = 200
        val height = 200
        val imageAsset = ImageAsset(width, height)
        val canvas = Canvas(imageAsset)
        val paint = Paint().apply { this.color = bg }
        val rect = Rect(Offset(0.0f, 0.0f), Size(width.toFloat(), height.toFloat()))
        with(canvas) {
            drawRect(rect, paint)
            withSave {
                scale(0.5f, 0.5f, width.toFloat(), height.toFloat())
                paint.color = fg
                drawRect(rect, paint)
            }
        }

        val pixelMap = imageAsset.toPixelMap()

        val left = width - 100
        val top = height - 100
        val right = width - 1
        val bottom = height - 1
        assertEquals(fg, pixelMap[left, top])
        assertEquals(fg, pixelMap[right, top])
        assertEquals(fg, pixelMap[left, bottom])
        assertEquals(fg, pixelMap[left, right])

        assertEquals(bg, pixelMap[left, top - 1])
        assertEquals(bg, pixelMap[left - 1, top])
        assertEquals(bg, pixelMap[left - 1, top - 1])

        assertEquals(bg, pixelMap[right, top - 1])
        assertEquals(bg, pixelMap[left - 1, bottom])
    }

    @Test
    fun testRotationCenterPivot() {
        val bg = androidx.compose.ui.graphics.Color.Red
        val fg = androidx.compose.ui.graphics.Color.Blue
        val width = 200
        val height = 200
        val imageAsset = ImageAsset(width, height)
        val canvas = Canvas(imageAsset)
        val paint = Paint().apply { this.color = bg }
        val rect = Rect(Offset(0.0f, 0.0f), Size(width.toFloat(), height.toFloat()))
        with(canvas) {
            drawRect(rect, paint)
            withSave {
                rotate(180.0f, 100.0f, 100.0f)
                paint.color = fg
                drawRect(
                    Rect(100.0f, 100.0f, 200.0f, 200.0f),
                    paint
                )
            }
        }

        val pixelMap = imageAsset.toPixelMap()
        assertEquals(fg, pixelMap[0, 0])
        assertEquals(fg, pixelMap[99, 0])
        assertEquals(fg, pixelMap[0, 99])
        assertEquals(fg, pixelMap[99, 99])

        assertEquals(bg, pixelMap[0, 100])
        assertEquals(bg, pixelMap[100, 0])
        assertEquals(bg, pixelMap[100, 100])
        assertEquals(bg, pixelMap[100, 99])
        assertEquals(bg, pixelMap[99, 100])
    }

    @Test
    fun testRotationDefaultPivot() {
        val bg = androidx.compose.ui.graphics.Color.Red
        val fg = androidx.compose.ui.graphics.Color.Blue
        val width = 200
        val height = 200
        val imageAsset = ImageAsset(width, height)
        val canvas = Canvas(imageAsset)
        val paint = Paint().apply { this.color = bg }
        val rect = Rect(Offset(0.0f, 0.0f), Size(width.toFloat(), height.toFloat()))
        with(canvas) {
            drawRect(rect, paint)
            withSave {
                rotate(-45.0f)
                paint.color = fg
                drawRect(
                    Rect(0.0f, 0.0f, 100.0f, 100.0f),
                    paint
                )
            }
        }

        val pixelMap = imageAsset.toPixelMap()
        assertEquals(fg, pixelMap[2, 0])
        assertEquals(fg, pixelMap[50, 49])
        assertEquals(fg, pixelMap[70, 0])
        assertEquals(fg, pixelMap[70, 68])

        assertEquals(bg, pixelMap[50, 51])
        assertEquals(bg, pixelMap[75, 76])
    }

    class EnableDisableZViewGroup @JvmOverloads constructor(
        val drawLatch: CountDownLatch,
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : FrameLayout(context, attrs, defStyleAttr) {
        override fun dispatchDraw(canvas: Canvas) {
            val androidCanvas = Canvas(canvas)
            androidCanvas.enableZ()
            for (i in 0 until childCount) {
                drawChild(androidCanvas.nativeCanvas, getChildAt(i), drawingTime)
            }
            androidCanvas.disableZ()
            drawLatch.countDown()
        }
    }
}