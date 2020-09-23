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

package androidx.compose.desktop

import androidx.compose.ui.platform.DesktopComponent
import androidx.compose.ui.platform.FrameDispatcher
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Picture
import org.jetbrains.skija.PictureRecorder
import org.jetbrains.skija.Rect
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import java.awt.DisplayMode
import java.awt.event.FocusEvent
import java.awt.im.InputMethodRequests

internal class FrameSkiaLayer {
    var renderer: Renderer? = null

    private var isDisposed = false
    private var frameNanoTime = 0L
    private val frameDispatcher = FrameDispatcher(
        onFrame = { onFrame(it) },
        framesPerSecond = ::getFramesPerSecond
    )

    private val picture = MutableResource<Picture>()
    private val pictureRecorder = PictureRecorder()

    private suspend fun onFrame(nanoTime: Long) {
        this.frameNanoTime = nanoTime
        preparePicture(frameNanoTime)
        wrapped.redrawLayer()
    }

    inner class Wrapped : SkiaLayer(), DesktopComponent {
        var currentInputMethodRequests: InputMethodRequests? = null

        override fun getInputMethodRequests() = currentInputMethodRequests

        override fun enableInput(inputMethodRequests: InputMethodRequests) {
            currentInputMethodRequests = inputMethodRequests
            enableInputMethods(true)
            val focusGainedEvent = FocusEvent(this, FocusEvent.FOCUS_GAINED)
            inputContext.dispatchEvent(focusGainedEvent)
        }

        override fun disableInput() {
            currentInputMethodRequests = null
        }

        override fun locationOnScreen() = locationOnScreen
    }

    val wrapped = Wrapped()

    init {
        wrapped.renderer = object : SkiaRenderer {
            override fun onRender(canvas: Canvas, width: Int, height: Int) {
                try {
                    picture.useWithoutClosing {
                        it?.also(canvas::drawPicture)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace(System.err)
                    if (System.getProperty("compose.desktop.render.ignore.errors") == null) {
                        System.exit(1)
                    }
                }
            }

            override fun onDispose() = Unit
            override fun onInit() = Unit
            override fun onReshape(width: Int, height: Int) = Unit
        }
    }

    // We draw into picture, because SkiaLayer.draw can be called from the other thread,
    // but onRender should be called in AWT thread. Picture doesn't add any visible overhead on
    // CPU/RAM.
    private suspend fun preparePicture(frameTimeNanos: Long) {
        val bounds = Rect.makeWH(wrapped.width.toFloat(), wrapped.height.toFloat())
        val pictureCanvas = pictureRecorder.beginRecording(bounds)
        renderer?.onFrame(pictureCanvas, wrapped.width, wrapped.height, frameTimeNanos)
        picture.set(pictureRecorder.finishRecordingAsPicture())
    }

    fun reinit() {
        check(!isDisposed)
        wrapped.reinit()
    }

    private fun getFramesPerSecond(): Float {
        val refreshRate = wrapped.graphicsConfiguration.device.displayMode.refreshRate
        return if (refreshRate != DisplayMode.REFRESH_RATE_UNKNOWN) refreshRate.toFloat() else 60f
    }

    fun updateLayer() {
        check(!isDisposed)
        wrapped.updateLayer()
    }

    fun dispose() {
        check(!isDisposed)
        frameDispatcher.cancel()
        wrapped.disposeLayer()
        wrapped.updateLayer()
        picture.close()
        pictureRecorder.close()
        isDisposed = true
    }

    fun needRedrawLayer() {
        check(!isDisposed)
        frameDispatcher.scheduleFrame()
    }

    interface Renderer {
        suspend fun onFrame(canvas: Canvas, width: Int, height: Int, nanoTime: Long)
    }
}