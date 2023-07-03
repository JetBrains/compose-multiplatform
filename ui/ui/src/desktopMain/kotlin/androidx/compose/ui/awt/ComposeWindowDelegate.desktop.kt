/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.LocalWindow
import androidx.compose.ui.window.UndecoratedWindowResizer
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import java.awt.*
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import javax.accessibility.Accessible
import javax.swing.JLayeredPane
import org.jetbrains.skiko.*

internal class ComposeWindowDelegate(
    private val window: Window,
    private val isUndecorated: () -> Boolean,
    private val skiaLayerAnalytics: SkiaLayerAnalytics
) {
    private var isDisposed = false

    // AWT can leak JFrame in some cases
    // (see https://github.com/JetBrains/compose-jb/issues/1688),
    // so we nullify layer on dispose, to prevent keeping
    // big objects in memory (like the whole LayoutNode tree of the window)
    private var _bridge: WindowComposeBridge? = WindowComposeBridge(skiaLayerAnalytics)
    private val bridge
        get() = requireNotNull(_bridge) {
            "ComposeLayer is disposed"
        }
    internal val windowAccessible: Accessible
        get() = bridge.sceneAccessible
    val undecoratedWindowResizer = UndecoratedWindowResizer(window)

    private val _pane = object : JLayeredPane() {
        override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
            bridge.component.setSize(width, height)
            super.setBounds(x, y, width, height)
        }

        override fun add(component: Component): Component {
            val clipComponent = ClipComponent(component)
            clipMap[component] = clipComponent
            bridge.clipComponents.add(clipComponent)
            return add(component, Integer.valueOf(0))
        }

        override fun remove(component: Component) {
            bridge.clipComponents.remove(clipMap[component]!!)
            clipMap.remove(component)
            super.remove(component)
        }

        override fun addNotify() {
            super.addNotify()
            bridge.component.requestFocus()
        }

        override fun getPreferredSize() =
            if (isPreferredSizeSet) super.getPreferredSize() else bridge.component.preferredSize

        init {
            layout = null
            super.add(bridge.component, 1)
        }

        fun dispose() {
            super.remove(bridge.component)
        }
    }

    val pane get() = _pane

    private val clipMap = mutableMapOf<Component, ClipComponent>()

    init {
        pane.focusTraversalPolicy = object : FocusTraversalPolicy() {
            override fun getComponentAfter(aContainer: Container?, aComponent: Component?) = null
            override fun getComponentBefore(aContainer: Container?, aComponent: Component?) = null
            override fun getFirstComponent(aContainer: Container?) = null
            override fun getLastComponent(aContainer: Container?) = null
            override fun getDefaultComponent(aContainer: Container?) = null
        }
        pane.isFocusCycleRoot = true
        setContent {}
    }

    fun add(component: Component): Component {
        return _pane.add(component)
    }

    fun remove(component: Component) {
        _pane.remove(component)
    }

    var fullscreen: Boolean
        get() = bridge.component.fullscreen
        set(value) {
            bridge.component.fullscreen = value
        }

    var compositionLocalContext: CompositionLocalContext?
        get() = bridge.compositionLocalContext
        set(value) {
            bridge.compositionLocalContext = value
        }

    fun setContent(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
        content: @Composable () -> Unit
    ) {
        bridge.setContent(
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent,
        ) {
            CompositionLocalProvider(
                LocalWindow provides window,
                LocalLayerContainer provides _pane
            ) {
                WindowContentLayout(content)
            }
        }
    }

    @Composable
    private fun WindowContentLayout(
        content: @Composable () -> Unit
    ){
        Layout(
            {
                content()
                undecoratedWindowResizer.Content(
                    modifier = Modifier.layoutId("UndecoratedWindowResizer")
                )
            },
            measurePolicy = { measurables, constraints ->
                val resizerMeasurable = measurables.lastOrNull()?.let {
                    if (it.layoutId == "UndecoratedWindowResizer") it else null
                }
                val resizerPlaceable = resizerMeasurable?.let {
                    val density = bridge.component.density.density
                    val resizerWidth = (window.width * density).toInt()
                    val resizerHeight = (window.height * density).toInt()
                    it.measure(
                        Constraints(
                            minWidth = resizerWidth,
                            minHeight = resizerHeight,
                            maxWidth = resizerWidth,
                            maxHeight = resizerHeight
                        )
                    )
                }

                val contentPlaceables = buildList(measurables.size){
                    measurables.fastForEach {
                        if (it != resizerMeasurable)
                            add(it.measure(constraints))
                    }
                }

                val contentWidth = contentPlaceables.maxOfOrNull { it.measuredWidth } ?: 0
                val contentHeight = contentPlaceables.maxOfOrNull { it.measuredHeight } ?: 0
                layout(contentWidth, contentHeight) {
                    contentPlaceables.fastForEach { placeable ->
                        placeable.place(0, 0)
                    }
                    resizerPlaceable?.place(0, 0)
                }
            }
        )
    }

    fun dispose() {
        if (!isDisposed) {
            bridge.dispose()
            _pane.dispose()
            _bridge = null
            isDisposed = true
        }
    }

    fun onRenderApiChanged(action: () -> Unit) {
        bridge.component.onStateChanged(SkiaLayer.PropertyKind.Renderer) {
            action()
        }
    }

    @ExperimentalComposeUiApi
    var exceptionHandler: WindowExceptionHandler?
        get() = bridge.exceptionHandler
        set(value) {
            bridge.exceptionHandler = value
        }

    val windowHandle: Long
        get() = bridge.component.windowHandle

    val renderApi: GraphicsApi
        get() = bridge.renderApi

    var isTransparent: Boolean
        get() = bridge.component.transparency
        set(value) {
            if (value != bridge.component.transparency) {
                check(isUndecorated()) { "Transparent window should be undecorated!" }
                check(!window.isDisplayable) {
                    "Cannot change transparency if window is already displayable."
                }
                bridge.component.transparency = value
                if (value) {
                    if (hostOs != OS.Windows) {
                        window.background = Color(0, 0, 0, 0)
                    }
                } else {
                    window.background = null
                }
            }
        }

    fun addMouseListener(listener: MouseListener) {
        bridge.component.addMouseListener(listener)
    }

    fun removeMouseListener(listener: MouseListener) {
        bridge.component.removeMouseListener(listener)
    }

    fun addMouseMotionListener(listener: MouseMotionListener) {
        bridge.component.addMouseMotionListener(listener)
    }

    fun removeMouseMotionListener(listener: MouseMotionListener) {
        bridge.component.removeMouseMotionListener(listener)
    }

    fun addMouseWheelListener(listener: MouseWheelListener) {
        bridge.component.addMouseWheelListener(listener)
    }

    fun removeMouseWheelListener(listener: MouseWheelListener) {
        bridge.component.removeMouseWheelListener(listener)
    }
}
