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

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.PointerPositionUpdater
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
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.InputModeManagerImpl
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.key.Key.Companion.Back
import androidx.compose.ui.input.key.Key.Companion.DirectionCenter
import androidx.compose.ui.input.key.Key.Companion.Tab
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyInputModifier
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventProcessor
import androidx.compose.ui.input.pointer.PositionCalculator
import androidx.compose.ui.input.pointer.ProcessResult
import androidx.compose.ui.input.pointer.TestPointerInputEventData
import androidx.compose.ui.layout.RootMeasurePolicy
import androidx.compose.ui.modifier.ModifierLocalManager
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
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.platform.FontLoader
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round

private typealias Command = () -> Unit

@OptIn(
    ExperimentalComposeUiApi::class,
    InternalCoreApi::class,
    InternalComposeUiApi::class
)
internal class SkiaBasedOwner(
    override val scene: ComposeScene,
    private val platform: Platform,
    parentFocusManager: FocusManager = EmptyFocusManager,
    private val pointerPositionUpdater: PointerPositionUpdater,
    initDensity: Density = Density(1f, 1f),
    bounds: IntRect = IntRect.Zero,
    val isFocusable: Boolean = true,
    val onDismissRequest: (() -> Unit)? = null,
    private val onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    private val onKeyEvent: (KeyEvent) -> Boolean = { false },
) : Owner, RootForTest, SkiaRootForTest, PositionCalculator {
    override val windowInfo: WindowInfo get() = platform.windowInfo

    fun isHovered(point: Offset): Boolean {
        val intOffset = IntOffset(point.x.toInt(), point.y.toInt())
        return bounds.contains(intOffset)
    }

    var bounds by mutableStateOf(bounds)

    override var density by mutableStateOf(initDensity)

    override val layoutDirection: LayoutDirection = platform.layoutDirection

    override val sharedDrawScope = LayoutNodeDrawScope()

    private val semanticsModifier = SemanticsModifierCore(
        mergeDescendants = false,
        clearAndSetSemantics = false,
        properties = {}
    )

    override val focusManager = FocusManagerImpl(
        parent = parentFocusManager
    ).apply {
        layoutDirection = platform.layoutDirection
    }

    // TODO: Set the input mode. For now we don't support touch mode, (always in Key mode).
    private val _inputModeManager = InputModeManagerImpl(
        initialInputMode = Keyboard,
        onRequestInputModeChange = {
            if (it == Touch || it == Keyboard) {
                setInputMode(it)
                true
            } else {
                false
            }
        }
    )

    private fun setInputMode(inputMode: InputMode) {
        _inputModeManager.inputMode = inputMode
    }

    override val inputModeManager: InputModeManager
        get() = _inputModeManager

    override val modifierLocalManager: ModifierLocalManager = ModifierLocalManager(this)

    // TODO(b/177931787) : Consider creating a KeyInputManager like we have for FocusManager so
    //  that this common logic can be used by all owners.
    private val keyInputModifier: KeyInputModifier = KeyInputModifier(
        onKeyEvent = {
            val focusDirection = getFocusDirection(it)
            if (focusDirection == null || it.type != KeyDown) return@KeyInputModifier false

            inputModeManager.requestInputMode(Keyboard)
            // Consume the key event if we moved focus.
            focusManager.moveFocus(focusDirection)
        },
        onPreviewKeyEvent = null
    )

    var constraints: Constraints = Constraints()

    override val root = LayoutNode().also {
        it.layoutDirection = platform.layoutDirection
        it.measurePolicy = RootMeasurePolicy
        it.modifier = semanticsModifier
            .then(focusManager.modifier)
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
        dispatchSnapshotChanges?.invoke(command)
    }
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)
    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)

    private val endApplyChangesListeners = mutableVectorOf<(() -> Unit)?>()

    override val textInputService = TextInputService(platform.textInputService)

    @Deprecated(
        "fontLoader is deprecated, use fontFamilyResolver",
        replaceWith = ReplaceWith("fontFamilyResolver")
    )
    override val fontLoader = FontLoader()

    override val fontFamilyResolver = createFontFamilyResolver()

    override val hapticFeedBack = DefaultHapticFeedback()

    override val clipboardManager = PlatformClipboardManager()

    override val accessibilityManager = DefaultAccessibilityManager()

    override val textToolbar = platform.textToolbar

    override val semanticsOwner: SemanticsOwner = SemanticsOwner(root)

    internal var accessibilityController = platform.accessibilityController(semanticsOwner)

    override val autofillTree = AutofillTree()

    override val autofill: Autofill? get() = null

    override val viewConfiguration = platform.viewConfiguration

    override val hasPendingMeasureOrLayout: Boolean
        get() = measureAndLayoutDelegate.hasPendingMeasureOrLayout

    init {
        snapshotObserver.startObserving()
        root.attach(this)
    }

    fun dispose() {
        snapshotObserver.stopObserving()
        // we don't need to call root.detach() because root will be garbage collected
    }

    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean = keyInputModifier.processKeyInput(keyEvent)

    override var showLayoutBounds = false

    override fun requestFocus() = platform.requestFocusForOwner()

    override fun onAttach(node: LayoutNode) = Unit

    override fun onDetach(node: LayoutNode) {
        measureAndLayoutDelegate.onNodeDetached(node)
        snapshotObserver.clear(node)
        needClearObservations = true
    }

    override val measureIteration: Long get() = measureAndLayoutDelegate.measureIteration

    var requestLayout: (() -> Unit)? = null
    var requestDraw: (() -> Unit)? = null
    var dispatchSnapshotChanges: ((Command) -> Unit)? = null

    private var needClearObservations = false

    fun clearInvalidObservations() {
        if (needClearObservations) {
            snapshotObserver.clearInvalidObservations()
            needClearObservations = false
        }
    }

    var contentSize = IntSize.Zero
        private set

    override fun measureAndLayout(sendPointerUpdate: Boolean) {
        measureAndLayoutDelegate.updateRootConstraints(constraints)
        if (
            measureAndLayoutDelegate.measureAndLayout {
                if (sendPointerUpdate) {
                    pointerPositionUpdater.needUpdate()
                }
            }
        ) {
            requestDraw?.invoke()
        }
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
        contentSize = computeContentSize()
    }

    override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
        measureAndLayoutDelegate.measureAndLayout(layoutNode, constraints)
        pointerPositionUpdater.needUpdate()
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
        contentSize = computeContentSize()
    }

    // Don't use mainOwner.root.width here, as it strictly coerced by [constraints]
    private fun computeContentSize() = IntSize(
        root.children.maxOfOrNull { it.outerCoordinator.measuredWidth } ?: 0,
        root.children.maxOfOrNull { it.outerCoordinator.measuredHeight } ?: 0,
    )

    override fun forceMeasureTheSubtree(layoutNode: LayoutNode) {
        measureAndLayoutDelegate.forceMeasureTheSubtree(layoutNode)
    }

    override fun onRequestMeasure(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean
    ) {
        if (affectsLookahead) {
            if (measureAndLayoutDelegate.requestLookaheadRemeasure(layoutNode, forceRequest)) {
                requestLayout?.invoke()
            }
        } else if (measureAndLayoutDelegate.requestRemeasure(layoutNode, forceRequest)) {
            requestLayout?.invoke()
        }
    }

    override fun onRequestRelayout(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean
    ) {
        if (affectsLookahead) {
            if (measureAndLayoutDelegate.requestLookaheadRelayout(layoutNode, forceRequest)) {
                requestLayout?.invoke()
            }
        } else if (measureAndLayoutDelegate.requestRelayout(layoutNode, forceRequest)) {
            requestLayout?.invoke()
        }
    }

    override fun requestOnPositionedCallback(layoutNode: LayoutNode) {
        measureAndLayoutDelegate.requestOnPositionedCallback(layoutNode)
        requestLayout?.invoke()
    }

    override fun createLayer(
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit
    ) = SkiaLayer(
        density,
        invalidateParentLayer = {
            invalidateParentLayer()
            requestDraw?.invoke()
        },
        drawBlock = drawBlock,
        onDestroy = { needClearObservations = true }
    )

    override fun onSemanticsChange() {
        accessibilityController.onSemanticsChange()
    }

    override fun onLayoutChange(layoutNode: LayoutNode) {
        accessibilityController.onLayoutChange(layoutNode)
    }

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

    internal fun processPointerInput(
        event: PointerInputEvent,
        isInBounds: Boolean = true
    ): ProcessResult {
        if (event.button != null) {
            inputModeManager.requestInputMode(Touch)
        }
        return pointerInputEventProcessor.process(
            event,
            this,
            isInBounds = isInBounds && event.pointers.all {
                bounds.contains(it.position.round())
            }
        ).also {
            if (it.dispatchedToAPointerInputModifier) {
                commitPointerIcon()
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun processPointerInput(timeMillis: Long, pointers: List<TestPointerInputEventData>) {
        // TODO(https://github.com/JetBrains/compose-jb/issues/1846)
        //  we should route test events through ComposeScene, not through SkiaBasedOwner
        measureAndLayout()
        val isPressed = pointers.any { it.down }
        processPointerInput(
            PointerInputEvent(
                PointerEventType.Unknown,
                timeMillis,
                pointers.map { it.toPointerInputEventData() },
                if (isPressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
                button = null
            )
        )
    }

    override fun onEndApplyChanges() {
        // Listeners can add more items to the list and we want to ensure that they
        // are executed after being added, so loop until the list is empty
        while (endApplyChangesListeners.isNotEmpty()) {
            val size = endApplyChangesListeners.size
            for (i in 0 until size) {
                val listener = endApplyChangesListeners[i]
                // null out the item so that if the listener is re-added then we execute it again.
                endApplyChangesListeners[i] = null
                listener?.invoke()
            }
            // Remove all the items that were visited. Removing items shifts all items after
            // to the front of the list, so removing in a chunk is cheaper than removing one-by-one
            endApplyChangesListeners.removeRange(0, size)
        }
    }

    override fun registerOnEndApplyChangesListener(listener: () -> Unit) {
        if (listener !in endApplyChangesListeners) {
            endApplyChangesListeners += listener
        }
    }

    override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        measureAndLayoutDelegate.registerOnLayoutCompletedListener(listener)
        requestLayout?.invoke()
    }

    private fun commitPointerIcon() {
        platform.setPointerIcon(pointerIconService.current)
        pointerIconService.current = PointerIconDefaults.Default
    }

    override val pointerIconService = object : PointerIconService {
        override var current: PointerIcon = PointerIconDefaults.Default

        override fun requestUpdate() {
            pointerPositionUpdater.needUpdate()
        }
    }
}
