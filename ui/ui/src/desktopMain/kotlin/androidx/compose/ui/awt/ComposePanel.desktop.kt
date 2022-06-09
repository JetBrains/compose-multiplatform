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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.window.WindowExceptionHandler
import org.jetbrains.skiko.ClipComponent
import org.jetbrains.skiko.GraphicsApi
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.FocusTraversalPolicy
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities.isEventDispatchThread

/**
 * ComposePanel is a panel for building UI using Compose for Desktop.
 */
class ComposePanel : JLayeredPane() {
    init {
        check(isEventDispatchThread()) {
            "ComposePanel should be created inside AWT Event Dispatch Thread" +
                " (use SwingUtilities.invokeLater).\n" +
                "Creating from another thread isn't supported."
        }
        background = Color.white
        layout = null
        focusTraversalPolicy = object : FocusTraversalPolicy() {
            override fun getComponentAfter(aContainer: Container?, aComponent: Component?): Component {
                val ancestor = focusCycleRootAncestor
                val policy = ancestor.focusTraversalPolicy
                return policy.getComponentAfter(ancestor, this@ComposePanel)
            }

            override fun getComponentBefore(aContainer: Container?, aComponent: Component?): Component {
                val ancestor = focusCycleRootAncestor
                val policy = ancestor.focusTraversalPolicy
                return policy.getComponentBefore(ancestor, this@ComposePanel)
            }

            override fun getFirstComponent(aContainer: Container?) = null
            override fun getLastComponent(aContainer: Container?) = null
            override fun getDefaultComponent(aContainer: Container?) = null
        }
        isFocusCycleRoot = true
    }

    private val _focusListeners = mutableSetOf<FocusListener?>()
    private var _isFocusable = true
    private var _isRequestFocusEnabled = false
    private var layer: ComposeLayer? = null
    private val clipMap = mutableMapOf<Component, ClipComponent>()
    private var content: (@Composable () -> Unit)? = null

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        layer?.component?.setSize(width, height)
        super.setBounds(x, y, width, height)
    }

    override fun getPreferredSize(): Dimension? {
        return if (isPreferredSizeSet) super.getPreferredSize() else layer?.component?.preferredSize
    }

    /**
     * Sets Compose content of the ComposePanel.
     *
     * @param content Composable content of the ComposePanel.
     */
    fun setContent(content: @Composable () -> Unit) {
        // The window (or root container) may not be ready to render composable content, so we need
        // to keep the lambda describing composable content and set the content only when
        // everything is ready to avoid accidental crashes and memory leaks on all supported OS
        // types.
        this.content = content
        initContent()
    }

    /**
     * Handler to catch uncaught exceptions during rendering frames, handling events,
     * or processing background Compose operations. If null, then exceptions throw
     * further up the call stack.
     */
    @ExperimentalComposeUiApi
    var exceptionHandler: WindowExceptionHandler? = null
        set(value) {
            field = value
            layer?.exceptionHandler = value
        }

    private fun initContent() {
        if (layer != null && content != null) {
            layer!!.setContent {
                CompositionLocalProvider(
                    LocalLayerContainer provides this,
                    content = content!!
                )
            }
        }
    }

    override fun add(component: Component): Component {
        if (layer == null) {
            return component
        }
        val clipComponent = ClipComponent(component)
        clipMap.put(component, clipComponent)
        layer!!.component.clipComponents.add(clipComponent)
        return super.add(component, Integer.valueOf(0))
    }

    override fun remove(component: Component) {
        layer!!.component.clipComponents.remove(clipMap.get(component)!!)
        clipMap.remove(component)
        super.remove(component)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun addNotify() {
        super.addNotify()

        // After [super.addNotify] is called we can safely initialize the layer and composable
        // content.
        layer = ComposeLayer().apply {
            scene.releaseFocus()
            component.setSize(width, height)
            component.isFocusable = _isFocusable
            component.isRequestFocusEnabled = _isRequestFocusEnabled
            _focusListeners.forEach(component::addFocusListener)
            exceptionHandler = this@ComposePanel.exceptionHandler
            component.addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent) {
                    // The focus can be switched from the child component inside SwingPanel.
                    // In that case, SwingPanel will take care of it.
                    if (!isParentOf(e.oppositeComponent)) {
                        when (e.cause) {
                            FocusEvent.Cause.TRAVERSAL_FORWARD -> {
                                layer?.scene?.requestFocus()
                                layer?.scene?.moveFocus(FocusDirection.Next)
                            }
                            FocusEvent.Cause.TRAVERSAL_BACKWARD -> {
                                layer?.scene?.requestFocus()
                                layer?.scene?.moveFocus(FocusDirection.Previous)
                            }
                            FocusEvent.Cause.ACTIVATION -> Unit
                            else -> {
                                layer?.scene?.requestFocus()
                            }
                        }
                    }
                }

                override fun focusLost(e: FocusEvent) {
                    // We don't reset focus for Compose when the window loses focus
                    // Partially because we don't support restoring focus after clearing it
                    if (e.cause != FocusEvent.Cause.ACTIVATION) {
                        layer?.scene?.releaseFocus()
                    }
                }
            })
        }
        initContent()
        super.add(layer!!.component, Integer.valueOf(1))
    }

    override fun removeNotify() {
        if (layer != null) {
            layer!!.dispose()
            super.remove(layer!!.component)
        }

        super.removeNotify()
    }

    override fun addFocusListener(l: FocusListener?) {
        layer?.component?.addFocusListener(l)
        _focusListeners.add(l)
    }

    override fun removeFocusListener(l: FocusListener?) {
        layer?.component?.removeFocusListener(l)
        _focusListeners.remove(l)
    }

    override fun isFocusable() = _isFocusable

    override fun setFocusable(focusable: Boolean) {
        _isFocusable = focusable
        layer?.component?.isFocusable = focusable
    }

    override fun isRequestFocusEnabled(): Boolean = _isRequestFocusEnabled

    override fun setRequestFocusEnabled(requestFocusEnabled: Boolean) {
        _isRequestFocusEnabled = requestFocusEnabled
        layer?.component?.isRequestFocusEnabled = requestFocusEnabled
    }

    override fun hasFocus(): Boolean {
        return layer?.component?.hasFocus() ?: false
    }

    override fun isFocusOwner(): Boolean {
        return layer?.component?.isFocusOwner ?: false
    }

    override fun requestFocus() {
        layer?.component?.requestFocus()
    }

    override fun requestFocus(temporary: Boolean): Boolean {
        return layer?.component?.requestFocus(temporary) ?: false
    }

    override fun requestFocus(cause: FocusEvent.Cause?) {
        layer?.component?.requestFocus(cause)
    }

    override fun requestFocusInWindow(): Boolean {
        return layer?.component?.requestFocusInWindow() ?: false
    }

    override fun requestFocusInWindow(cause: FocusEvent.Cause?): Boolean {
        return layer?.component?.requestFocusInWindow(cause) ?: false
    }

    override fun setFocusTraversalKeysEnabled(focusTraversalKeysEnabled: Boolean) {
        // ignore, traversal keys should always be handled by ComposeLayer
    }

    override fun getFocusTraversalKeysEnabled(): Boolean {
        return false
    }

    /**
     * Returns low-level rendering API used for rendering in this ComposeWindow. API is
     * automatically selected based on operating system, graphical hardware and `SKIKO_RENDER_API`
     * environment variable.
     */
    val renderApi: GraphicsApi
        get() = if (layer != null) layer!!.component.renderApi else GraphicsApi.UNKNOWN
}
