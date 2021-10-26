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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.In
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Out
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusManagerImpl
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.InputModeManagerImpl
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.key.Key.Companion.Back
import androidx.compose.ui.input.key.Key.Companion.DirectionCenter
import androidx.compose.ui.input.key.Key.Companion.Tab
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyInputModifier
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollEventFilter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventProcessor
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PositionCalculator
import androidx.compose.ui.input.pointer.ProcessResult
import androidx.compose.ui.input.pointer.TestPointerInputEventData
import androidx.compose.ui.layout.RootMeasurePolicy
import androidx.compose.ui.node.HitTestResult
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeDrawScope
import androidx.compose.ui.node.MeasureAndLayoutDelegate
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsModifierCore
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.platform.FontLoader
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection

private typealias Command = () -> Unit

@OptIn(
    ExperimentalComposeUiApi::class,
    InternalCoreApi::class,
    InternalComposeUiApi::class
)
internal class SkiaBasedOwner(
    private val platformInputService: PlatformInput,
    density: Density = Density(1f, 1f),
    val isPopup: Boolean = false,
    val isFocusable: Boolean = true,
    val onDismissRequest: (() -> Unit)? = null,
    private val onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    private val onKeyEvent: (KeyEvent) -> Boolean = { false },
) : Owner, RootForTest, SkiaRootForTest, PositionCalculator {

    internal fun isHovered(point: Offset): Boolean {
        val intOffset = IntOffset(point.x.toInt(), point.y.toInt())
        return bounds.contains(intOffset)
    }

    internal var bounds by mutableStateOf(IntRect.Zero)

    override var density by mutableStateOf(density)

    // TODO(demin): support RTL
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr

    override val sharedDrawScope = LayoutNodeDrawScope()

    private val semanticsModifier = SemanticsModifierCore(
        id = SemanticsModifierCore.generateSemanticsId(),
        mergeDescendants = false,
        clearAndSetSemantics = false,
        properties = {}
    )

    private val _focusManager: FocusManagerImpl = FocusManagerImpl().apply {
        // TODO(demin): support RTL [onRtlPropertiesChanged]
        layoutDirection = LayoutDirection.Ltr
    }
    override val focusManager: FocusManager
        get() = _focusManager

    // TODO: Set the input mode. For now we don't support touch mode, (always in Key mode).
    private val _inputModeManager = InputModeManagerImpl(
        initialInputMode = Keyboard,
        onRequestInputModeChange = {
            // TODO: Change the input mode programmatically. For now we just return true if the
            //  requested input mode is Keyboard mode.
            it == Keyboard
        }
    )
    override val inputModeManager: InputModeManager
        get() = _inputModeManager

    // TODO: set/clear _windowInfo.isWindowFocused when the window gains/loses focus.
    private val _windowInfo: WindowInfoImpl = WindowInfoImpl()
    override val windowInfo: WindowInfo
        get() = _windowInfo

    // TODO(b/177931787) : Consider creating a KeyInputManager like we have for FocusManager so
    //  that this common logic can be used by all owners.
    private val keyInputModifier: KeyInputModifier = KeyInputModifier(
        onKeyEvent = {
            val focusDirection = getFocusDirection(it)
            if (focusDirection == null || it.type != KeyDown) return@KeyInputModifier false

            // Consume the key event if we moved focus.
            focusManager.moveFocus(focusDirection)
        },
        onPreviewKeyEvent = null
    )

    var constraints: Constraints = Constraints()
        set(value) {
            field = value

            if (!isPopup) {
                this.bounds = IntRect(
                    IntOffset(bounds.left, bounds.top),
                    IntSize(constraints.maxWidth, constraints.maxHeight)
                )
            }
        }

    override val root = LayoutNode().also {
        it.measurePolicy = RootMeasurePolicy
        it.modifier = semanticsModifier
            .then(_focusManager.modifier)
            .then(keyInputModifier)
            .then(
                KeyInputModifier(
                    onKeyEvent = onKeyEvent,
                    onPreviewKeyEvent = onPreviewKeyEvent
                )
            )
    }

    override val rootForTest = this

    override val snapshotObserver = OwnerSnapshotObserver { command ->
        onDispatchCommand?.invoke(command)
    }
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)
    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)

    init {
        snapshotObserver.startObserving()
        root.attach(this)
        _focusManager.takeFocus()
    }

    fun dispose() {
        snapshotObserver.stopObserving()
        // we don't need to call root.detach() because root will be garbage collected
    }

    override val textInputService = TextInputService(platformInputService)

    override val fontLoader = FontLoader()

    override val hapticFeedBack = DefaultHapticFeedback()

    override val clipboardManager = PlatformClipboardManager()

    override val accessibilityManager = DefaultAccessibilityManager()

    override val textToolbar = DefaultTextToolbar()

    override val semanticsOwner: SemanticsOwner = SemanticsOwner(root)

    override val autofillTree = AutofillTree()

    override val autofill: Autofill? get() = null

    override val viewConfiguration: ViewConfiguration = DefaultViewConfiguration(density)

    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean =
        sendKeyEvent(platformInputService, keyInputModifier, keyEvent)

    override var showLayoutBounds = false

    override fun requestFocus() = true

    override fun onAttach(node: LayoutNode) = Unit

    override fun onDetach(node: LayoutNode) {
        measureAndLayoutDelegate.onNodeDetached(node)
        snapshotObserver.clear(node)
        needClearObservations = true
    }

    override val measureIteration: Long get() = measureAndLayoutDelegate.measureIteration

    private var needsLayout = true
    private var needsDraw = true

    val needsRender get() = needsLayout || needsDraw
    var onNeedsRender: (() -> Unit)? = null
    var onDispatchCommand: ((Command) -> Unit)? = null
    var containerCursor: PlatformComponentWithCursor? = null

    fun render(canvas: org.jetbrains.skia.Canvas) {
        needsLayout = false
        measureAndLayout()
        needsDraw = false
        draw(canvas)
        clearInvalidObservations()
    }

    private var needClearObservations = false

    private fun clearInvalidObservations() {
        if (needClearObservations) {
            snapshotObserver.clearInvalidObservations()
            needClearObservations = false
        }
    }

    private fun requestLayout() {
        needsLayout = true
        needsDraw = true
        onNeedsRender?.invoke()
    }

    private fun requestDraw() {
        needsDraw = true
        onNeedsRender?.invoke()
    }

    override fun measureAndLayout(sendPointerUpdate: Boolean) {
        measureAndLayoutDelegate.updateRootConstraints(constraints)
        if (measureAndLayoutDelegate.measureAndLayout()) {
            requestDraw()
        }
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
    }

    override fun onRequestMeasure(layoutNode: LayoutNode) {
        if (measureAndLayoutDelegate.requestRemeasure(layoutNode)) {
            requestLayout()
        }
    }

    override fun onRequestRelayout(layoutNode: LayoutNode) {
        if (measureAndLayoutDelegate.requestRelayout(layoutNode)) {
            requestLayout()
        }
    }

    override fun createLayer(
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit
    ) = SkiaLayer(
        density,
        invalidateParentLayer = {
            invalidateParentLayer()
            requestDraw()
        },
        drawBlock = drawBlock,
        onDestroy = { needClearObservations = true }
    )

    override fun onSemanticsChange() = Unit

    override fun onLayoutChange(layoutNode: LayoutNode) = Unit

    override fun getFocusDirection(keyEvent: KeyEvent): FocusDirection? {
        return when (keyEvent.key) {
            Tab -> if (keyEvent.isShiftPressed) Previous else Next
            DirectionCenter -> In
            Back -> Out
            else -> null
        }
    }

    override fun calculatePositionInWindow(localPosition: Offset): Offset = localPosition

    override fun calculateLocalPosition(positionInWindow: Offset): Offset = positionInWindow

    override fun localToScreen(localPosition: Offset): Offset = localPosition

    override fun screenToLocal(positionOnScreen: Offset): Offset = positionOnScreen

    fun draw(canvas: org.jetbrains.skia.Canvas) {
        root.draw(canvas.asComposeCanvas())
    }

    private var desiredPointerIcon: PointerIcon? = null

    internal fun processPointerInput(event: PointerInputEvent): ProcessResult {
        measureAndLayout()
        desiredPointerIcon = null
        return pointerInputEventProcessor.process(
            event,
            this,
            isInBounds = event.pointers.all {
                it.position.x in 0f..root.width.toFloat() &&
                    it.position.y in 0f..root.height.toFloat()
            }
        ).also {
            setPointerIcon(containerCursor, desiredPointerIcon)
        }
    }

    override fun processPointerInput(timeMillis: Long, pointers: List<TestPointerInputEventData>) {
        processPointerInput(
            PointerInputEvent(
                PointerEventType.Unknown,
                timeMillis,
                pointers.map { it.toPointerInputEventData() }
            )
        )
    }

    // TODO(demin): This is likely temporary. After PointerInputEvent can handle mouse events
    //  (scroll in particular), we can replace it with processPointerInput. see b/166105940
    internal fun onMouseScroll(position: Offset, event: MouseScrollEvent) {
        measureAndLayout()

        val inputFilters = HitTestResult<PointerInputFilter>()
        root.hitTest(position, inputFilters)

        for (
            filter in inputFilters
                .asReversed()
                .asSequence()
                .filterIsInstance<MouseScrollEventFilter>()
        ) {
            val isConsumed = filter.onMouseScroll(event)
            if (isConsumed) break
        }
    }

    override val pointerIconService: PointerIconService =
        object : PointerIconService {
            override var current: PointerIcon
                get() = desiredPointerIcon ?: PointerIconDefaults.Default
                set(value) { desiredPointerIcon = value }
        }
}

internal expect fun sendKeyEvent(
    platformInputService: PlatformInput,
    keyInputModifier: KeyInputModifier,
    keyEvent: KeyEvent
): Boolean

internal expect fun setPointerIcon(
    containerCursor: PlatformComponentWithCursor?,
    icon: PointerIcon?
)
