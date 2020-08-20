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

import androidx.compose.runtime.dispatch.DesktopUiDispatcher
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.platform.DesktopOwners
import org.jetbrains.skija.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.MouseWheelEvent
import javax.swing.JFrame

class ComposeWindow : JFrame {
    companion object {
        init {
            initCompose()
        }
    }

    val parent: AppFrame
    private val layer: SkiaLayer = SkiaLayer()

    var owners: DesktopOwners? = null
        set(value) {
            field = value
            layer.renderer = value?.let(::OwnersRenderer)
        }

    constructor(parent: AppFrame) : super() {
        this.parent = parent
        contentPane.add(layer)

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                layer.reinit()
            }
        })
        initCanvas()
    }

    fun updateLayer() {
        if (!isVisible) {
            return
        }
        layer.updateLayer()
    }

    fun redrawLayer() {
        if (!isVisible) {
            return
        }
        layer.redrawLayer()
    }

    private fun initCanvas() {
        layer.addInputMethodListener(object : InputMethodListener {
            override fun caretPositionChanged(p0: InputMethodEvent?) {
                TODO("Implement input method caret change")
            }

            override fun inputMethodTextChanged(
                event: InputMethodEvent
            ) = DesktopUiDispatcher.Dispatcher.lockCallbacks {
                owners?.onInputMethodTextChanged(event)
            }
        })

        layer.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) = Unit

            override fun mousePressed(
                event: MouseEvent
            ) = DesktopUiDispatcher.Dispatcher.lockCallbacks {
                owners?.onMousePressed(event.x, event.y)
            }

            override fun mouseReleased(
                event: MouseEvent
            ) = DesktopUiDispatcher.Dispatcher.lockCallbacks {
                owners?.onMouseReleased(event.x, event.y)
            }
        })
        layer.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(
                event: MouseEvent
            ) = DesktopUiDispatcher.Dispatcher.lockCallbacks {
                owners?.onMouseDragged(event.x, event.y)
            }
        })
        layer.addMouseWheelListener { event ->
            DesktopUiDispatcher.Dispatcher.lockCallbacks {
                owners?.onMouseScroll(event.x, event.y, event.toComposeEvent())
            }
        }
        layer.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(
                event: KeyEvent
            ) = DesktopUiDispatcher.Dispatcher.lockCallbacks {
                owners?.onKeyPressed(event.keyCode, event.keyChar)
            }

            override fun keyReleased(
                event: KeyEvent
            ) = DesktopUiDispatcher.Dispatcher.lockCallbacks {
                owners?.onKeyReleased(event.keyCode, event.keyChar)
            }

            override fun keyTyped(
                event: KeyEvent
            ) = DesktopUiDispatcher.Dispatcher.lockCallbacks {
                owners?.onKeyTyped(event.keyChar)
            }
        })
    }

    fun disposeCanvas() {
        layer.disposeLayer()
        layer.updateLayer()
        layer.renderer!!.onDispose()
    }

    override fun setVisible(value: Boolean) {
        super.setVisible(value)
        updateLayer()
    }
}

private class OwnersRenderer(private val owners: DesktopOwners) : SkiaRenderer {
    override fun onDispose() = Unit
    override fun onInit() = Unit
    override fun onReshape(width: Int, height: Int) = Unit

    override fun onRender(canvas: Canvas, width: Int, height: Int) {
        Thread.currentThread().contextClassLoader = ClassLoader.getSystemClassLoader()
        DesktopUiDispatcher.Dispatcher.lockCallbacks {
            owners.onRender(canvas, width, height)
        }
    }
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

// Simple FPS tracker for debug purposes
internal class FPSTracker {
    private var t0 = 0L
    private val times = DoubleArray(155)
    private var timesIdx = 0

    fun track() {
        val t1 = System.nanoTime()
        times[timesIdx] = (t1 - t0) / 1000000.0
        t0 = t1
        timesIdx = (timesIdx + 1) % times.size
        println("FPS: ${1000 / times.takeWhile { it > 0 }.average()}")
    }
}
