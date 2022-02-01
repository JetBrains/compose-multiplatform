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
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.DesktopPlatform
import androidx.compose.ui.platform.AccessibilityControllerImpl
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoView
import java.awt.Cursor
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.FocusEvent
import java.awt.event.InputEvent
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.MouseWheelEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.im.InputMethodRequests
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
import javax.swing.SwingUtilities
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType

internal class ComposeLayer {
    private var isDisposed = false

    private val _component = ComponentImpl()
    val component: SkiaLayer get() = _component

    @OptIn(ExperimentalComposeUiApi::class)
    private val coroutineExceptionHandler = object :
        AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
        override fun handleException(context: CoroutineContext, exception: Throwable) {
            exceptionHandler?.onException(exception) ?: throw exception
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    var exceptionHandler: WindowExceptionHandler? = null

    @OptIn(ExperimentalComposeUiApi::class)
    private fun catchExceptions(body: () -> Unit) {
        try {
            body()
        } catch (e: Throwable) {
            exceptionHandler?.onException(e) ?: throw e
        }
    }

    internal val scene = ComposeScene(
        Dispatchers.Swing + coroutineExceptionHandler,
        _component,
        Density(1f),
        _component::needRedraw,
        createSyntheticNativeMoveEvent = _component::createSyntheticMouseEvent,
    )

    private val density get() = _component.density.density

    /**
     * Keyboard modifiers state might be changed when window is not focused, so window doesn't
     * receive any key events.
     * This flag is set when window focus changes. Then we can rely on it when handling the
     * first movementEvent to get the actual keyboard modifiers state from it.
     * After window gains focus, the first motionEvent.metaState (after focus gained) is used
     * to update windowInfo.keyboardModifiers.
     *
     * TODO: needs to be set `true` when focus changes:
     * (Window focus change is implemented in JB fork, but not upstreamed yet).
     */
    private var keyboardModifiersRequireUpdate = false

    private val a11yDisabled by lazy {
        System.getProperty("compose.accessibility.enable") == "false" ||
        System.getenv("COMPOSE_DISABLE_ACCESSIBILITY") != null
    }

    fun makeAccessible(component: Component) = object : Accessible {
        override fun getAccessibleContext(): AccessibleContext? {
            if (a11yDisabled) return null
            val controller =
                scene.mainOwner?.accessibilityController as? AccessibilityControllerImpl
            val accessible = controller?.rootAccessible
            accessible?.getAccessibleContext()?.accessibleParent = component.parent as Accessible
            return accessible?.getAccessibleContext()
        }
    }

    private inner class ComponentImpl :
        SkiaLayer(externalAccessibleFactory = ::makeAccessible), Accessible, PlatformComponent {
        var currentInputMethodRequests: InputMethodRequests? = null

        private var window: Window? = null
        private var windowListener = object : WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent) = refreshWindowFocus()
            override fun windowLostFocus(e: WindowEvent) = refreshWindowFocus()
        }

        override fun addNotify() {
            super.addNotify()
            resetDensity()
            initContent()
            updateSceneSize()
            window = SwingUtilities.getWindowAncestor(this)
            window?.addWindowFocusListener(windowListener)
            refreshWindowFocus()
        }

        override fun removeNotify() {
            window?.removeWindowFocusListener(windowListener)
            window = null
            refreshWindowFocus()
            super.removeNotify()
        }

        override fun paint(g: Graphics) {
            resetDensity()
            super.paint(g)
        }

        override fun getInputMethodRequests() = currentInputMethodRequests
        private var _desiredCursor: Cursor? = null
        override var desiredCursor: Cursor
            get() = _desiredCursor ?: super.getCursor()
            set(value) { _desiredCursor = value }

        override fun commitCursor() {
            super.setCursor(_desiredCursor ?: Cursor(Cursor.DEFAULT_CURSOR))
            _desiredCursor = null
        }

        override fun enableInput(inputMethodRequests: InputMethodRequests) {
            currentInputMethodRequests = inputMethodRequests
            enableInputMethods(true)
            val focusGainedEvent = FocusEvent(this.canvas, FocusEvent.FOCUS_GAINED)
            inputContext.dispatchEvent(focusGainedEvent)
        }

        override fun disableInput() {
            currentInputMethodRequests = null
        }

        override fun doLayout() {
            super.doLayout()
            updateSceneSize()
        }

        private fun updateSceneSize() {
            this@ComposeLayer.scene.constraints = Constraints(
                maxWidth = (width * density.density).toInt().coerceAtLeast(0),
                maxHeight = (height * density.density).toInt().coerceAtLeast(0)
            )
        }

        override fun getPreferredSize(): Dimension {
            return if (isPreferredSizeSet) super.getPreferredSize() else Dimension(
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
            if (this@ComposeLayer.scene.density != density) {
                this@ComposeLayer.scene.density = density
                updateSceneSize()
            }
        }

        @Suppress("DEPRECATION")
        fun createSyntheticMouseEvent(sourceEvent: Any?, positionSourceEvent: Any?): Any {
            sourceEvent as MouseEvent
            positionSourceEvent as MouseEvent

            return SyntheticMouseEvent(
                sourceEvent.source as Component,
                MouseEvent.MOUSE_MOVED,
                sourceEvent.`when`,
                sourceEvent.modifiersEx,
                positionSourceEvent.x,
                positionSourceEvent.y
            )
        }

        private val _windowInfo = WindowInfoImpl()

        override val windowInfo = _windowInfo

        private fun refreshWindowFocus() {
            windowInfo.isWindowFocused = window?.isFocused ?: false
            keyboardModifiersRequireUpdate = true
        }

        fun setCurrentKeyboardModifiers(modifiers: PointerKeyboardModifiers) {
            _windowInfo.keyboardModifiers = modifiers
        }
    }

    init {
        _component.skikoView = object : SkikoView {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                catchExceptions {
                    scene.render(canvas, nanoTime)
                }
            }
        }

        _component.addInputMethodListener(object : InputMethodListener {
            override fun caretPositionChanged(event: InputMethodEvent?) {
                if (isDisposed) return
                if (event != null) {
                    catchExceptions {
                        scene.onInputMethodEvent(event)
                    }
                }
            }

            override fun inputMethodTextChanged(event: InputMethodEvent) {
                if (isDisposed) return
                catchExceptions {
                    scene.onInputMethodEvent(event)
                }
            }
        })

        _component.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) = Unit
            override fun mousePressed(event: MouseEvent) = onMouseEvent(event)
            override fun mouseReleased(event: MouseEvent) = onMouseEvent(event)
            override fun mouseEntered(event: MouseEvent) = onMouseEvent(event)
            override fun mouseExited(event: MouseEvent) = onMouseEvent(event)
        })
        _component.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) = onMouseEvent(event)
            override fun mouseMoved(event: MouseEvent) = onMouseEvent(event)
        })
        _component.addMouseWheelListener { event ->
            onMouseWheelEvent(event)
        }
        _component.focusTraversalKeysEnabled = false
        _component.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) = onKeyEvent(event)
            override fun keyReleased(event: KeyEvent) = onKeyEvent(event)
            override fun keyTyped(event: KeyEvent) = onKeyEvent(event)
        })
    }

    private fun onMouseEvent(event: MouseEvent) = catchExceptions {
        // AWT can send events after the window is disposed
        if (isDisposed) return@catchExceptions
        if (keyboardModifiersRequireUpdate) {
            keyboardModifiersRequireUpdate = false
            _component.setCurrentKeyboardModifiers(event.keyboardModifiers)
        }
        scene.onMouseEvent(density, event)
    }

    private fun onMouseWheelEvent(event: MouseWheelEvent) = catchExceptions {
        if (isDisposed) return@catchExceptions
        scene.onMouseWheelEvent(density, event)
    }

    private fun onKeyEvent(event: KeyEvent) = catchExceptions {
        if (isDisposed) return@catchExceptions
        _component.setCurrentKeyboardModifiers(event.toPointerKeyboardModifiers())
        if (scene.sendKeyEvent(ComposeKeyEvent(event))) {
            event.consume()
        }
    }

    private fun KeyEvent.toPointerKeyboardModifiers() = PointerKeyboardModifiers(
        isCtrlPressed = this.isControlDown,
        isMetaPressed = this.isMetaDown,
        isAltPressed = this.isAltDown,
        isShiftPressed = this.isShiftDown,
        isAltGraphPressed = this.isAltGraphDown,
        isSymPressed = false,
        isFunctionPressed = false,
        isCapsLockOn = getLockingKeyStateSafe(KeyEvent.VK_CAPS_LOCK),
        isScrollLockOn = getLockingKeyStateSafe(KeyEvent.VK_SCROLL_LOCK),
        isNumLockOn = getLockingKeyStateSafe(KeyEvent.VK_NUM_LOCK)
    )

    fun dispose() {
        check(!isDisposed)
        scene.close()
        _component.dispose()
        _initContent = null
        isDisposed = true
    }

    var compositionLocalContext: CompositionLocalContext? by scene::compositionLocalContext

    fun setContent(
        onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        onKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        content: @Composable () -> Unit
    ) {
        // If we call it before attaching, everything probably will be fine,
        // but the first composition will be useless, as we set density=1
        // (we don't know the real density if we have unattached component)
        _initContent = {
            catchExceptions {
                scene.setContent(
                    onPreviewKeyEvent = onPreviewKeyEvent,
                    onKeyEvent = onKeyEvent,
                    content = content
                )
            }
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
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event
    )
}

@Suppress("ControlFlowWithEmptyBody")
@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeScene.onMouseWheelEvent(
    density: Float,
    event: MouseWheelEvent
) {
    sendPointerEvent(
        eventType = PointerEventType.Scroll,
        position = Offset(event.x.toFloat(), event.y.toFloat()) * density,
        scrollDelta = if (event.isShiftDown) {
            Offset(event.preciseWheelRotation.toFloat(), 0f)
        } else {
            Offset(0f, event.preciseWheelRotation.toFloat())
        },
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private val MouseEvent.buttons get() = PointerButtons(
    isPrimaryPressed = (modifiersEx and MouseEvent.BUTTON1_DOWN_MASK) != 0 && !isMacOsCtrlClick,
    isSecondaryPressed = (modifiersEx and MouseEvent.BUTTON3_DOWN_MASK) != 0 || isMacOsCtrlClick,
    isTertiaryPressed = (modifiersEx and MouseEvent.BUTTON2_DOWN_MASK) != 0,
    isBackPressed = (modifiersEx and MouseEvent.getMaskForButton(4)) != 0,
    isForwardPressed = (modifiersEx and MouseEvent.getMaskForButton(5)) != 0,
)

@OptIn(ExperimentalComposeUiApi::class)
private val MouseEvent.keyboardModifiers get() = PointerKeyboardModifiers(
    isCtrlPressed = (modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0,
    isMetaPressed = (modifiersEx and InputEvent.META_DOWN_MASK) != 0,
    isAltPressed = (modifiersEx and InputEvent.ALT_DOWN_MASK) != 0,
    isShiftPressed = (modifiersEx and InputEvent.SHIFT_DOWN_MASK) != 0,
    isAltGraphPressed = (modifiersEx and InputEvent.ALT_GRAPH_DOWN_MASK) != 0,
    isSymPressed = false,
    isFunctionPressed = false,
    isCapsLockOn = getLockingKeyStateSafe(KeyEvent.VK_CAPS_LOCK),
    isScrollLockOn = getLockingKeyStateSafe(KeyEvent.VK_SCROLL_LOCK),
    isNumLockOn = getLockingKeyStateSafe(KeyEvent.VK_NUM_LOCK),
)

private fun getLockingKeyStateSafe(
    mask: Int
): Boolean = try {
    Toolkit.getDefaultToolkit().getLockingKeyState(mask)
} catch (_: Exception) {
    false
}

private val MouseEvent.isMacOsCtrlClick
    get() = (
            DesktopPlatform.Current == DesktopPlatform.MacOS &&
                    ((modifiersEx and InputEvent.BUTTON1_DOWN_MASK) != 0) &&
                    ((modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0)
            )

@Deprecated("Will be removed in Compose 1.3")
internal class SyntheticMouseEvent(
    source: Component,
    id: Int,
    `when`: Long,
    modifiers: Int,
    x: Int,
    y: Int
) : MouseEvent(source, id, `when`, modifiers, x, y, 0, false)