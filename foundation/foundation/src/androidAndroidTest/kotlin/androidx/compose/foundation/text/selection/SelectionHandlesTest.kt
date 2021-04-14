/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.TestActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class SelectionHandlesTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule(TestActivity::class.java)
    private lateinit var activity: TestActivity

    private val HANDLE_COLOR = Color(0xFF4286F4)
    // Due to the rendering effect of captured bitmap from activity, if we want the pixels from the
    // corners, we need a little bit offset from the edges of the bitmap.
    private val OFFSET_FROM_EDGE = 5

    private val selectionLtrHandleDirection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = 0
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = 0
        ),
        handlesCrossed = false
    )
    private val selectionRtlHandleDirection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = 0
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = 0
        ),
        handlesCrossed = true
    )

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun StartSelectionHandle_left_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                DefaultSelectionHandle(
                    modifier = Modifier,
                    isStartHandle = true,
                    directions = Pair(
                        selectionLtrHandleDirection.start.direction,
                        selectionLtrHandleDirection.end.direction
                    ),
                    handlesCrossed = selectionLtrHandleDirection.handlesCrossed
                )
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isNotEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun StartSelectionHandle_right_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                DefaultSelectionHandle(
                    modifier = Modifier,
                    isStartHandle = true,
                    directions = Pair(
                        selectionRtlHandleDirection.start.direction,
                        selectionRtlHandleDirection.end.direction
                    ),
                    handlesCrossed = selectionRtlHandleDirection.handlesCrossed
                )
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isNotEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun EndSelectionHandle_right_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                DefaultSelectionHandle(
                    modifier = Modifier,
                    isStartHandle = false,
                    directions = Pair(
                        selectionLtrHandleDirection.start.direction,
                        selectionLtrHandleDirection.end.direction
                    ),
                    handlesCrossed = selectionLtrHandleDirection.handlesCrossed
                )
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isNotEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun EndSelectionHandle_left_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                DefaultSelectionHandle(
                    modifier = Modifier,
                    isStartHandle = false,
                    directions = Pair(
                        selectionRtlHandleDirection.start.direction,
                        selectionRtlHandleDirection.end.direction
                    ),
                    handlesCrossed = selectionRtlHandleDirection.handlesCrossed
                )
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isNotEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    @SmallTest
    fun isHandleLtrDirection_ltr_handles_not_cross_return_true() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Ltr, areHandlesCrossed = false)
        ).isTrue()
    }

    @Test
    @SmallTest
    fun isHandleLtrDirection_ltr_handles_cross_return_false() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Ltr, areHandlesCrossed = true)
        ).isFalse()
    }

    @Test
    @SmallTest
    fun isHandleLtrDirection_rtl_handles_not_cross_return_false() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Rtl, areHandlesCrossed = false)
        ).isFalse()
    }

    @Test
    @SmallTest
    fun isHandleLtrDirection_rtl_handles_cross_return_true() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Rtl, areHandlesCrossed = true)
        ).isTrue()
    }
}

@Suppress("DEPRECATION")
// We only need this because IR compiler doesn't like converting lambdas to Runnables
private fun androidx.test.rule.ActivityTestRule<*>.runOnUiThreadIR(block: () -> Unit) {
    val runnable = Runnable { block() }
    runOnUiThread(runnable)
}

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<*>.findAndroidComposeView(): ViewGroup {
    val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
    return findAndroidComposeView(contentViewGroup)!!
}

fun findAndroidComposeView(parent: ViewGroup): ViewGroup? {
    for (index in 0 until parent.childCount) {
        val child = parent.getChildAt(index)
        if (child is ViewGroup) {
            if (child is ComposeView)
                return child
            else {
                val composeView = findAndroidComposeView(child)
                if (composeView != null) {
                    return composeView
                }
            }
        }
    }
    return null
}

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.O)
fun androidx.test.rule.ActivityTestRule<*>.waitAndScreenShot(
    forceInvalidate: Boolean = true
): Bitmap = waitAndScreenShot(findAndroidComposeView(), forceInvalidate)

class DrawCounterListener(private val view: View) :
    ViewTreeObserver.OnPreDrawListener {
    val latch = CountDownLatch(5)

    override fun onPreDraw(): Boolean {
        latch.countDown()
        if (latch.count > 0) {
            view.postInvalidate()
        } else {
            view.viewTreeObserver.removeOnPreDrawListener(this)
        }
        return true
    }
}

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.O)
fun androidx.test.rule.ActivityTestRule<*>.waitAndScreenShot(
    view: View,
    forceInvalidate: Boolean = true
): Bitmap {
    val flushListener = DrawCounterListener(view)
    val offset = intArrayOf(0, 0)
    var handler: Handler? = null
    runOnUiThread {
        view.getLocationInWindow(offset)
        if (forceInvalidate) {
            view.viewTreeObserver.addOnPreDrawListener(flushListener)
            view.invalidate()
        }
        handler = Handler(Looper.getMainLooper())
    }

    if (forceInvalidate) {
        assertTrue("Drawing latch timed out", flushListener.latch.await(1, TimeUnit.SECONDS))
    }
    val width = view.width
    val height = view.height

    val dest =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val srcRect = android.graphics.Rect(0, 0, width, height)
    srcRect.offset(offset[0], offset[1])
    val latch = CountDownLatch(1)
    var copyResult = 0
    val onCopyFinished = PixelCopy.OnPixelCopyFinishedListener { result ->
        copyResult = result
        latch.countDown()
    }
    PixelCopy.request(activity.window, srcRect, dest, onCopyFinished, handler!!)
    assertTrue("Pixel copy latch timed out", latch.await(1, TimeUnit.SECONDS))
    assertEquals(PixelCopy.SUCCESS, copyResult)
    return dest
}