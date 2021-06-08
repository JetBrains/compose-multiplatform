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

package androidx.compose.ui.test.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.TestContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.O)
internal fun captureRegionToImage(
    testContext: TestContext,
    captureRectInWindow: Rect,
    view: View,
    window: Window? = null
): ImageBitmap {
    fun Context.getActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> this.baseContext.getActivity()
            else -> null
        }
    }

    val windowToCapture = window ?: (view.context.getActivity()!!.window)
    val handler = Handler(Looper.getMainLooper())

    // first we wait for the drawing to happen
    var drawDone = false
    val decorView = windowToCapture.decorView
    handler.post {
        if (Build.VERSION.SDK_INT >= 29 && decorView.isHardwareAccelerated) {
            FrameCommitCallbackHelper.registerFrameCommitCallback(decorView.viewTreeObserver) {
                drawDone = true
            }
        } else {
            decorView.viewTreeObserver.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {
                var handled = false
                override fun onDraw() {
                    if (!handled) {
                        handled = true
                        handler.post {
                            drawDone = true
                            decorView.viewTreeObserver.removeOnDrawListener(this)
                        }
                    }
                }
            })
        }
        decorView.invalidate()
    }

    @OptIn(InternalTestApi::class)
    testContext.testOwner.mainClock.waitUntil(timeoutMillis = 2_000) { drawDone }

    // and then request the pixel copy of the drawn buffer
    val destBitmap = Bitmap.createBitmap(
        captureRectInWindow.width(),
        captureRectInWindow.height(),
        Bitmap.Config.ARGB_8888
    )

    val latch = CountDownLatch(1)
    var copyResult = 0
    val onCopyFinished = PixelCopy.OnPixelCopyFinishedListener { result ->
        copyResult = result
        latch.countDown()
    }
    PixelCopyHelper.request(
        windowToCapture,
        captureRectInWindow,
        destBitmap,
        onCopyFinished,
        handler
    )

    if (!latch.await(1, TimeUnit.SECONDS)) {
        throw AssertionError("Failed waiting for PixelCopy!")
    }
    if (copyResult != PixelCopy.SUCCESS) {
        throw AssertionError("PixelCopy failed!")
    }
    return destBitmap.asImageBitmap()
}

// Unfortunately this is a copy paste from AndroidComposeTestRule. At this moment it is a bit
// tricky to share this method. We can expose it on TestOwner in theory.
private fun MainTestClock.waitUntil(timeoutMillis: Long, condition: () -> Boolean) {
    val startTime = System.nanoTime()
    while (!condition()) {
        if (autoAdvance) {
            advanceTimeByFrame()
        }
        // Let Android run measure, draw and in general any other async operations.
        Thread.sleep(10)
        if (System.nanoTime() - startTime > timeoutMillis * 1_000_000) {
            throw ComposeTimeoutException(
                "Condition still not satisfied after $timeoutMillis ms"
            )
        }
    }
}

@RequiresApi(29)
private object FrameCommitCallbackHelper {
    @DoNotInline
    fun registerFrameCommitCallback(viewTreeObserver: ViewTreeObserver, runnable: Runnable) {
        viewTreeObserver.registerFrameCommitCallback(runnable)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private object PixelCopyHelper {
    @DoNotInline
    fun request(
        source: Window,
        srcRect: Rect?,
        dest: Bitmap,
        listener: PixelCopy.OnPixelCopyFinishedListener,
        listenerThread: Handler
    ) {
        PixelCopy.request(source, srcRect, dest, listener, listenerThread)
    }
}
