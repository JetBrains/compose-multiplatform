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

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.DesktopComponent
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import java.awt.Dimension
import java.awt.Graphics
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
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent

internal class ComposeLayer {
    private var isDisposed = false

    // TODO(demin): probably we need to get rid of asynchronous events. it was added because of
    //  slow lazy scroll. But events become unpredictable, and we can't consume them.
    //  Alternative solution to a slow scroll - merge multiple scroll events into a single one.
    private val events = AWTDebounceEventQueue()

    private val _component = ComponentImpl()
    val component: SkiaLayer get() = _component

    private val scene = ComposeScene(
        Dispatchers.Swing,
        _component,
        Density(1f),
        _component::needRedraw
    )

    private val density get() = _component.density.density

    private inner class ComponentImpl : SkiaLayer(), DesktopComponent {
        var currentInputMethodRequests: InputMethodRequests? = null

        override fun addNotify() {
            super.addNotify()
            resetDensity()
            initContent()
        }

        override fun paint(g: Graphics) {
            resetDensity()
            super.paint(g)
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

        override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
            this@ComposeLayer.scene.constraints = Constraints(
                maxWidth = (width * density.density).toInt().coerceAtLeast(0),
                maxHeight = (height * density.density).toInt().coerceAtLeast(0)
            )
            super.setBounds(x, y, width, height)
        }

        override fun doLayout() {
            super.doLayout()
            preferredSize = Dimension(
                (this@ComposeLayer.scene.contentSize.width / density.density).toInt(),
                (this@ComposeLayer.scene.contentSize.height / density.density).toInt()
            )
        }

        override val locationOnScreen: Point
            @Suppress("ACCIDENTAL_OVERRIDE") // KT-47743
            get() = super.getLocationOnScreen()

        override var density: Density = Density(1f)

        private fun resetDensity() {
            density = (this as SkiaLayer).density
            this@ComposeLayer.scene.density = density
        }
    }

    init {
        _component.renderer = object : SkiaRenderer {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                try {
                    scene.render(canvas, nanoTime)
                } catch (e: Throwable) {
                    if (System.getProperty("compose.desktop.render.ignore.errors") == null) {
                        throw e
                    }
                }
            }
        }

        _component.addInputMethodListener(object : InputMethodListener {
            override fun caretPositionChanged(event: InputMethodEvent?) {
                if (event != null) {
                    scene.onInputMethodEvent(event)
                }
            }

            override fun inputMethodTextChanged(event: InputMethodEvent) = events.post {
                scene.onInputMethodEvent(event)
            }
        })

        _component.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) = Unit

            override fun mousePressed(event: MouseEvent) = events.post {
                scene.onMouseEvent(density, event)
            }

            override fun mouseReleased(event: MouseEvent) = events.post {
                scene.onMouseEvent(density, event)
            }

            override fun mouseEntered(event: MouseEvent) = events.post {
                scene.onMouseEvent(density, event)
            }

            override fun mouseExited(event: MouseEvent) = events.post {
                scene.onMouseEvent(density, event)
            }
        })
        _component.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) = events.post {
                scene.onMouseEvent(density, event)
            }

            override fun mouseMoved(event: MouseEvent) = events.post {
                scene.onMouseEvent(density, event)
            }
        })
        _component.addMouseWheelListener { event ->
            events.post {
                scene.onMouseWheelEvent(density, event)
            }
        }
        _component.focusTraversalKeysEnabled = false
        _component.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                scene.sendKeyEvent(event)
            }

            override fun keyReleased(event: KeyEvent) {
                scene.sendKeyEvent(event)
            }

            override fun keyTyped(event: KeyEvent) {
                scene.sendKeyEvent(event)
            }
        })
    }

    fun dispose() {
        check(!isDisposed)
        scene.dispose()
        events.cancel()
        _component.dispose()
        _initContent = null
        isDisposed = true
    }

    fun setContent(
        onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        onKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        content: @Composable () -> Unit
    ) {
        // If we call it before attaching, everything probably will be fine,
        // but the first composition will be useless, as we set density=1
        // (we don't know the real density if we have unattached component)
        _initContent = {
            scene.setContent(
                onPreviewKeyEvent = onPreviewKeyEvent,
                onKeyEvent = onKeyEvent,
                content = content
            )
        }
        initContent()
    }

    private var _initContent: (() -> Unit)? = null

    private fun initContent() {
        if (_component.isDisplayable) {
            _initContent?.invoke()
            _initContent = null
        }
    }
}

@Suppress("ControlFlowWithEmptyBody")
@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeScene.onMouseEvent(
    density: Float,
    event: MouseEvent
) {
    val eventType = when (event.id) {
        MouseEvent.MOUSE_PRESSED -> PointerEventType.Press
        MouseEvent.MOUSE_RELEASED -> PointerEventType.Release
        MouseEvent.MOUSE_DRAGGED -> PointerEventType.Move
        MouseEvent.MOUSE_MOVED -> PointerEventType.Move
        MouseEvent.MOUSE_ENTERED -> PointerEventType.Enter
        MouseEvent.MOUSE_EXITED -> PointerEventType.Exit
        else -> PointerEventType.Unknown
    }
    sendPointerEvent(
        eventType = eventType,
        position = Offset(event.x.toFloat(), event.y.toFloat()) * density,
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        mouseEvent = event
    )
}

@Suppress("ControlFlowWithEmptyBody")
@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeScene.onMouseWheelEvent(
    density: Float,
    event: MouseWheelEvent
) = with(event) {
    sendPointerScrollEvent(
        position = Offset(event.x.toFloat(), event.y.toFloat()) * density,
        delta = if (scrollType == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
            MouseScrollUnit.Page((scrollAmount * preciseWheelRotation).toFloat())
        } else {
            MouseScrollUnit.Line((scrollAmount * preciseWheelRotation).toFloat())
        },
        // There are no other way to detect horizontal scrolling in AWT
        orientation = if (isShiftDown) {
            MouseScrollOrientation.Horizontal
        } else {
            MouseScrollOrientation.Vertical
        },
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        mouseEvent = event
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeScene.sendKeyEvent(event: KeyEvent) {
    sendKeyEvent(ComposeKeyEvent(event))
}