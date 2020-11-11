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
import androidx.compose.ui.unit.Density
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Picture
import org.jetbrains.skija.PictureRecorder
import org.jetbrains.skija.Rect
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import java.awt.DisplayMode
import java.awt.Point
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

    var onDensityChanged: ((Density) -> Unit)? = null

    private var _density: Density? = null
    val density
        get() = _density ?: detectCurrentDensity().also {
            _density = it
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

        override val locationOnScreen: Point
            get() = super.getLocationOnScreen()

        override val density: Density
            get() = this@FrameSkiaLayer.density

        override fun scaleCanvas(dpi: Float) {}
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
        val bounds = Rect.makeWH(wrapped.width * density.density, wrapped.height * density.density)
        val pictureCanvas = pictureRecorder.beginRecording(bounds)
        renderer?.onFrame(
            pictureCanvas,
            (wrapped.width * density.density).toInt(),
            (wrapped.height * density.density).toInt(),
            frameTimeNanos
        )
        picture.set(pictureRecorder.finishRecordingAsPicture())
    }

    fun reinit() {
        val currentDensity = detectCurrentDensity()
        if (_density != currentDensity) {
            _density = currentDensity
            onDensityChanged?.invoke(density)
        }
        check(!isDisposed)
        wrapped.reinit()
    }

    // TODO(demin): detect OS fontScale
    //  font size can be changed on Windows 10 in Settings - Ease of Access,
    //  on Ubuntu in Settings - Universal Access
    //  on macOS there is no such setting
    private fun detectCurrentDensity(): Density {
        val density = wrapped.graphicsConfiguration.defaultTransform.scaleX.toFloat()
        return Density(density, 1f)
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