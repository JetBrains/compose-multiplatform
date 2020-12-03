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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionReference
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.platform.DesktopComponent
import androidx.compose.ui.platform.DesktopOwner
import androidx.compose.ui.platform.DesktopOwners
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skija.Canvas
import org.jetbrains.skiko.HardwareLayer
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import java.awt.Point
import java.awt.event.FocusEvent
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.MouseWheelEvent
import java.awt.im.InputMethodRequests

internal class ComposeLayer {
    private var isDisposed = false

    private val coroutineScope = CoroutineScope(Dispatchers.Swing)
    // TODO(demin): maybe pass CoroutineScope into AWTDebounceEventQueue and get rid of [cancel]
    //  method?
    private val events = AWTDebounceEventQueue()

    internal val wrapped = Wrapped()
    internal val owners: DesktopOwners = DesktopOwners(
        coroutineScope,
        wrapped,
        wrapped::needRedraw
    )

    private var owner: DesktopOwner? = null
    private var composition: Composition? = null

    private var content: (@Composable () -> Unit)? = null
    private var parentComposition: CompositionReference? = null

    private lateinit var density: Density

    inner class Wrapped : SkiaLayer(), DesktopComponent {
        var currentInputMethodRequests: InputMethodRequests? = null

        var isInit = false
            private set

        override fun init() {
            super.init()
            isInit = true
            resetDensity()
            initOwner()
        }

        override fun contentScaleChanged() {
            super.contentScaleChanged()
            resetDensity()
        }

        private fun resetDensity() {
            this@ComposeLayer.density = detectCurrentDensity()
            owner?.density = density
        }

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
            get() = this@ComposeLayer.density
    }

    val component: HardwareLayer
        get() = wrapped

    init {
        wrapped.renderer = object : SkiaRenderer {
            override suspend fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                try {
                    owners.onFrame(canvas, width, height, nanoTime)
                } catch (e: Throwable) {
                    if (System.getProperty("compose.desktop.render.ignore.errors") == null) {
                        throw e
                    }
                }
            }
        }
        initCanvas()
    }

    private fun initCanvas() {
        wrapped.addInputMethodListener(object : InputMethodListener {
            override fun caretPositionChanged(event: InputMethodEvent?) {
                if (event != null) {
                    owners.onInputMethodEvent(event)
                }
            }

            override fun inputMethodTextChanged(event: InputMethodEvent) = events.post {
                owners.onInputMethodEvent(event)
            }
        })

        wrapped.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) = Unit

            override fun mousePressed(event: MouseEvent) = events.post {
                owners.onMousePressed(
                    (event.x * density.density).toInt(),
                    (event.y * density.density).toInt(),
                    event
                )
            }

            override fun mouseReleased(event: MouseEvent) = events.post {
                owners.onMouseReleased(
                    (event.x * density.density).toInt(),
                    (event.y * density.density).toInt(),
                    event
                )
            }
        })
        wrapped.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) = events.post {
                owners.onMouseDragged(
                    (event.x * density.density).toInt(),
                    (event.y * density.density).toInt(),
                    event
                )
            }

            override fun mouseMoved(event: MouseEvent) = events.post {
                owners.onMouseMoved(
                    (event.x * density.density).toInt(),
                    (event.y * density.density).toInt()
                )
            }
        })
        wrapped.addMouseWheelListener { event ->
            events.post {
                owners.onMouseScroll(
                    (event.x * density.density).toInt(),
                    (event.y * density.density).toInt(),
                    event.toComposeEvent()
                )
            }
        }
        wrapped.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) = events.post {
                owners.onKeyPressed(event)
            }

            override fun keyReleased(event: KeyEvent) = events.post {
                owners.onKeyReleased(event)
            }

            override fun keyTyped(event: KeyEvent) = events.post {
                owners.onKeyTyped(event)
            }
        })
    }

    private fun MouseWheelEvent.toComposeEvent() = MouseScrollEvent(
        delta = if (scrollType == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
            MouseScrollUnit.Page((scrollAmount * preciseWheelRotation).toFloat())
        } else {
            MouseScrollUnit.Line((scrollAmount * preciseWheelRotation).toFloat())
        },

        // There are no other way to detect horizontal scrolling in AWT
        orientation = if (isShiftDown) {
            Orientation.Horizontal
        } else {
            Orientation.Vertical
        }
    )

    // TODO(demin): detect OS fontScale
    //  font size can be changed on Windows 10 in Settings - Ease of Access,
    //  on Ubuntu in Settings - Universal Access
    //  on macOS there is no such setting
    private fun detectCurrentDensity(): Density {
        return Density(wrapped.contentScale, 1f)
    }

    fun dispose() {
        check(!isDisposed)
        composition?.dispose()
        owner?.dispose()
        events.cancel()
        coroutineScope.cancel()
        wrapped.dispose()
        isDisposed = true
    }

    internal fun setContent(
        parentComposition: CompositionReference? = null,
        content: @Composable () -> Unit
    ) {
        check(!isDisposed)
        check(this.content == null) { "Cannot set content twice" }
        this.content = content
        this.parentComposition = parentComposition
        // We can't create DesktopOwner now, because we don't know density yet.
        // We will know density only after SkiaLayer will be visible.
        initOwner()
    }

    private fun initOwner() {
        check(!isDisposed)
        if (wrapped.isInit && owner == null && content != null) {
            owner = DesktopOwner(owners, density)
            composition = owner!!.setContent(parent = parentComposition, content = content!!)
        }
    }
}