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
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.In
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Out
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusManagerImpl
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.DesktopCanvas
import androidx.compose.ui.input.key.Key.Companion.Back
import androidx.compose.ui.input.key.Key.Companion.DirectionCenter
import androidx.compose.ui.input.key.Key.Companion.DirectionDown
import androidx.compose.ui.input.key.Key.Companion.DirectionLeft
import androidx.compose.ui.input.key.Key.Companion.DirectionRight
import androidx.compose.ui.input.key.Key.Companion.DirectionUp
import androidx.compose.ui.input.key.Key.Companion.Tab
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyInputModifier
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollEventFilter
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventProcessor
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerMoveEventFilter
import androidx.compose.ui.input.pointer.PositionCalculator
import androidx.compose.ui.input.pointer.ProcessResult
import androidx.compose.ui.input.pointer.TestPointerInputEventData
import androidx.compose.ui.layout.RootMeasurePolicy
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
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
import androidx.compose.ui.unit.LayoutDirection

private typealias Command = () -> Unit

@OptIn(
    ExperimentalComposeUiApi::class,
    InternalCoreApi::class
)
internal class DesktopOwner(
    val container: DesktopOwners,
    density: Density = Density(1f, 1f)
) : Owner, RootForTest, DesktopRootForTest, PositionCalculator {
    internal var size by mutableStateOf(IntSize(0, 0))

    override var density by mutableStateOf(density)

    // TODO(demin): support RTL
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr

    private val semanticsModifier = SemanticsModifierCore(
        id = SemanticsModifierCore.generateSemanticsId(),
        mergeDescendants = false,
        clearAndSetSemantics = false,
        properties = {}
    )

    private val _focusManager: FocusManagerImpl = FocusManagerImpl()
    override val focusManager: FocusManager
        get() = _focusManager

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

    override val root = LayoutNode().also {
        it.measurePolicy = RootMeasurePolicy
        it.modifier = semanticsModifier
            .then(_focusManager.modifier)
            .then(keyInputModifier)
    }

    override val rootForTest = this

    override val snapshotObserver = OwnerSnapshotObserver { command ->
        onDispatchCommand?.invoke(command)
    }
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)
    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)

    init {
        container.register(this)
        snapshotObserver.startObserving()
        root.attach(this)
        _focusManager.takeFocus()
    }

    fun dispose() {
        snapshotObserver.stopObserving()
        container.unregister(this)
        // we don't need to call root.detach() because root will be garbage collected
    }

    override val textInputService = TextInputService(container.platformInputService)

    override val fontLoader = FontLoader()

    override val hapticFeedBack = DesktopHapticFeedback()

    override val clipboardManager = DesktopClipboardManager()

    override val accessibilityManager = DesktopAccessibilityManager()

    override val textToolbar = DesktopTextToolbar()

    override val semanticsOwner: SemanticsOwner = SemanticsOwner(root)

    override val autofillTree = AutofillTree()

    override val autofill: Autofill? get() = null

    override val viewConfiguration: ViewConfiguration = DesktopViewConfiguration(density)

    val keyboard: Keyboard?
        get() = container.keyboard

    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean {
        when {
            keyEvent.nativeKeyEvent.id == java.awt.event.KeyEvent.KEY_TYPED ->
                container.platformInputService.charKeyPressed = true
            keyEvent.type == KeyEventType.KeyDown ->
                container.platformInputService.charKeyPressed = false
        }

        return keyInputModifier.processKeyInput(keyEvent) ||
            keyboard?.processKeyInput(keyEvent) ?: false
    }

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

    fun render(canvas: org.jetbrains.skija.Canvas, width: Int, height: Int) {
        needsLayout = false
        setSize(width, height)
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

    override fun measureAndLayout() {
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
    ) = SkijaLayer(
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
            DirectionRight -> Right
            DirectionLeft -> Left
            DirectionUp -> Up
            DirectionDown -> Down
            DirectionCenter -> In
            Back -> Out
            else -> null
        }
    }

    override fun requestRectangleOnScreen(rect: Rect) {
        // TODO: Scroll the owner to bring the specified rectangle into view.
    }

    override fun calculatePositionInWindow(localPosition: Offset): Offset = localPosition

    override fun calculateLocalPosition(positionInWindow: Offset): Offset = positionInWindow

    override fun localToScreen(localPosition: Offset): Offset = localPosition

    override fun screenToLocal(positionOnScreen: Offset): Offset = positionOnScreen

    fun setSize(width: Int, height: Int) {
        val constraints = Constraints(0, width, 0, height)
        this.size = IntSize(width, height)
        measureAndLayoutDelegate.updateRootConstraints(constraints)
    }

    fun draw(canvas: org.jetbrains.skija.Canvas) {
        root.draw(DesktopCanvas(canvas))
    }

    internal fun processPointerInput(event: PointerInputEvent): ProcessResult {
        measureAndLayout()
        return pointerInputEventProcessor.process(event, this)
    }

    override fun processPointerInput(nanoTime: Long, pointers: List<TestPointerInputEventData>) {
        processPointerInput(
            PointerInputEvent(
                nanoTime,
                pointers.map { it.toPointerInputEventData() }
            )
        )
    }

    // TODO(demin): This is likely temporary. After PointerInputEvent can handle mouse events
    //  (scroll in particular), we can replace it with processPointerInput. see b/166105940
    internal fun onMouseScroll(position: Offset, event: MouseScrollEvent) {
        measureAndLayout()

        val inputFilters = mutableListOf<PointerInputFilter>()
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

    private var oldMoveFilters = listOf<PointerMoveEventFilter>()
    private var newMoveFilters = mutableListOf<PointerInputFilter>()

    internal fun onPointerMove(position: Offset) {
        // TODO: do we actually need that?
        measureAndLayout()

        root.hitTest(position, newMoveFilters)
        // Optimize fastpath, where no pointer move event listeners are there.
        if (newMoveFilters.isEmpty() && oldMoveFilters.isEmpty()) return

        // For elements in `newMoveFilters` we call on `onMoveHandler`.
        // For elements in `oldMoveFilters` but not in `newMoveFilters` we call `onExitHandler`.
        // For elements not in `oldMoveFilters` but in `newMoveFilters` we call `onEnterHandler`.

        var onMoveConsumed = false
        var onEnterConsumed = false
        var onExitConsumed = false

        for (
            filter in newMoveFilters
                .asReversed()
                .asSequence()
                .filterIsInstance<PointerMoveEventFilter>()
        ) {
            if (!onMoveConsumed) {
                val relative = position - filter.layoutCoordinates!!.boundsInWindow().topLeft
                onMoveConsumed = filter.onMoveHandler(relative)
            }
            if (!onEnterConsumed && !oldMoveFilters.contains(filter))
                onEnterConsumed = filter.onEnterHandler()
        }

        // TODO: is this quadratic algorithm (by number of matching filters) a problem?
        //  Unlikely we'll have significant number of filters.
        for (filter in oldMoveFilters.asReversed()) {
            if (!onExitConsumed && !newMoveFilters.contains(filter))
                onExitConsumed = filter.onExitHandler()
        }

        oldMoveFilters = newMoveFilters.filterIsInstance<PointerMoveEventFilter>()
        newMoveFilters = mutableListOf()
    }

    internal fun onPointerEnter(position: Offset) {
        var onEnterConsumed = false
        // TODO: do we actually need that?
        measureAndLayout()
        root.hitTest(position, newMoveFilters)
        for (
            filter in newMoveFilters
                .asReversed()
                .asSequence()
                .filterIsInstance<PointerMoveEventFilter>()
        ) {
            if (!onEnterConsumed) {
                onEnterConsumed = filter.onEnterHandler()
            }
        }
        oldMoveFilters = newMoveFilters.filterIsInstance<PointerMoveEventFilter>()
        newMoveFilters = mutableListOf()
    }

    internal fun onPointerExit() {
        var onExitConsumed = false
        for (filter in oldMoveFilters.asReversed()) {
            if (!onExitConsumed) {
                onExitConsumed = filter.onExitHandler()
            }
        }
        oldMoveFilters = listOf()
        newMoveFilters = mutableListOf()
    }
}
