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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.focus.*
import androidx.compose.ui.focus.FocusDirection.Companion.In
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Out
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.InputModeManagerImpl
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.key.Key.Companion.Back
import androidx.compose.ui.input.key.Key.Companion.DirectionCenter
import androidx.compose.ui.input.key.Key.Companion.Tab
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.RootMeasurePolicy
import androidx.compose.ui.modifier.ModifierLocalManager
import androidx.compose.ui.node.*
import androidx.compose.ui.semantics.SemanticsModifierCore
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.PlatformTextInputPluginRegistry
import androidx.compose.ui.text.input.PlatformTextInputPluginRegistryImpl
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.platform.FontLoader
import androidx.compose.ui.unit.*

private typealias Command = () -> Unit

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTextApi::class,
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

    // TODO(https://github.com/JetBrains/compose-multiplatform/issues/2944)
    //  Check if ComposePanel/SwingPanel focus interop work correctly with new features of
    //  the focus system (it works with the old features like moveFocus/clearFocus)
    override val focusOwner: FocusOwner = FocusOwnerImpl(
        parent = parentFocusManager
    ) {
        registerOnEndApplyChangesListener(it)
    }.apply {
        layoutDirection = platform.layoutDirection
    }

    override val inputModeManager: InputModeManager
        get() = platform.inputModeManager

    override val modifierLocalManager: ModifierLocalManager = ModifierLocalManager(this)

    // TODO(b/177931787) : Consider creating a KeyInputManager like we have for FocusManager so
    //  that this common logic can be used by all owners.
    private val keyInputModifier = Modifier.onKeyEvent {
        val focusDirection = getFocusDirection(it)
        if (focusDirection == null || it.type != KeyDown) return@onKeyEvent false

        inputModeManager.requestInputMode(Keyboard)
        // Consume the key event if we moved focus.
        focusOwner.moveFocus(focusDirection)
    }

    var constraints: Constraints = Constraints()

    override val root = LayoutNode().also {
        it.layoutDirection = platform.layoutDirection
        it.measurePolicy = RootMeasurePolicy
        it.modifier = semanticsModifier
            .then(focusOwner.modifier)
            .then(keyInputModifier)
            .onPreviewKeyEvent(onPreviewKeyEvent)
            .onKeyEvent(onKeyEvent)
    }

    override val rootForTest = this

    override val snapshotObserver = OwnerSnapshotObserver { command ->
        dispatchSnapshotChanges?.invoke(command)
    }
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)
    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)

    private val endApplyChangesListeners = mutableVectorOf<(() -> Unit)?>()

    override val textInputService = TextInputService(platform.textInputService)

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    @OptIn(InternalTextApi::class)
    override val platformTextInputPluginRegistry: PlatformTextInputPluginRegistry
        get() = PlatformTextInputPluginRegistryImpl { factory, platformTextInput ->
            TODO("See https://issuetracker.google.com/267235947")
        }

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

    internal val accessibilityController = platform.accessibilityController(semanticsOwner)

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

    // TODO: [1.4 Update] check that it works properly, since after merging 1.4 Modifier doesn't have dispatch method
    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean = focusOwner.dispatchKeyEvent(keyEvent)

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
                    pointerPositionUpdater.needSendMove()
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
        pointerPositionUpdater.needSendMove()
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

    /**
     * If pointerPosition is inside UIKitView, then Compose skip touches. And touches goes to UIKit.
     */
    fun hitInteropView(pointerPosition: Offset, isTouchEvent: Boolean): Boolean {
        val result = HitTestResult<PointerInputModifierNode>()
        pointerInputEventProcessor.root.hitTest(pointerPosition, result, isTouchEvent)
        val last: PointerInputModifierNode? = result.lastOrNull()
        return (last as? BackwardsCompatNode)?.element is InteropViewCatchPointerModifier
    }

    override fun onEndApplyChanges() {
        clearInvalidObservations()

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
        pointerIconService.current = PointerIcon.Default
    }

    override val pointerIconService = object : PointerIconService {
        override var current: PointerIcon = PointerIcon.Default

        override fun requestUpdate() {
            pointerPositionUpdater.needSendMove()
        }
    }
}
