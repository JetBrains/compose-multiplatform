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

package androidx.compose.testutils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.PixelCopy.OnPixelCopyFinishedListener
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Captures the underlying view's surface into bitmap.
 *
 * This has currently several limitations. Currently we assume that the view is hosted in
 * Activity's window. Also if there is another window covering part of the component if won't occur
 * in the bitmap as this is taken from the component's window surface.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun View.captureToImage(): ImageBitmap {
    val locationInWindow = intArrayOf(0, 0)
    getLocationInWindow(locationInWindow)
    val x = locationInWindow[0]
    val y = locationInWindow[1]
    val boundsInWindow = Rect(x, y, x + width, y + height)

    fun Context.getActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> this.baseContext.getActivity()
            else -> null
        }
    }

    val windowToCapture = context.getActivity()!!.window
    val handler = Handler(Looper.getMainLooper())

    // first we wait for the drawing to happen
    val drawLatch = CountDownLatch(1)
    val decorView = windowToCapture.decorView
    handler.post {
        if (Build.VERSION.SDK_INT >= 29 && decorView.isHardwareAccelerated) {
            FrameCommitCallbackHelper.registerFrameCommitCallback(decorView.viewTreeObserver) {
                drawLatch.countDown()
            }
        } else {
            decorView.viewTreeObserver.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {
                var handled = false
                override fun onDraw() {
                    if (!handled) {
                        handled = true
                        handler.post {
                            drawLatch.countDown()
                            decorView.viewTreeObserver.removeOnDrawListener(this)
                        }
                    }
                }
            })
        }
        decorView.invalidate()
    }
    if (!drawLatch.await(2, TimeUnit.SECONDS)) {
        throw AssertionError("Failed waiting for DecorView redraw!")
    }

    // and then request the pixel copy of the drawn buffer
    val destBitmap = Bitmap.createBitmap(
        boundsInWindow.width(),
        boundsInWindow.height(),
        Bitmap.Config.ARGB_8888
    )

    val latch = CountDownLatch(1)
    var copyResult = 0
    val onCopyFinished = OnPixelCopyFinishedListener { result ->
        copyResult = result
        latch.countDown()
    }
    PixelCopyHelper.request(windowToCapture, boundsInWindow, destBitmap, onCopyFinished, handler)

    if (!latch.await(1, TimeUnit.SECONDS)) {
        throw AssertionError("Failed waiting for PixelCopy!")
    }
    if (copyResult != PixelCopy.SUCCESS) {
        throw AssertionError("PixelCopy failed!")
    }
    return destBitmap.asImageBitmap()
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
        listener: OnPixelCopyFinishedListener,
        listenerThread: Handler
    ) {
        PixelCopy.request(source, srcRect, dest, listener, listenerThread)
    }
}