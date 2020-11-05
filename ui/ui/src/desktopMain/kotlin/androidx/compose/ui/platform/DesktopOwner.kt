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

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.DrawLayerModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.drawLayer
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusManagerImpl
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.DesktopCanvas
import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifier
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollEventFilter
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventProcessor
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerMoveEventFilter
import androidx.compose.ui.layout.RootMeasureBlocks
import androidx.compose.ui.layout.globalBounds
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.MeasureAndLayoutDelegate
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerScope
import androidx.compose.ui.semantics.SemanticsModifierCore
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.platform.FontLoader
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

@OptIn(
    ExperimentalFocus::class,
    ExperimentalKeyInput::class,
    ExperimentalLayoutNodeApi::class,
    ExperimentalComposeApi::class,
    InternalCoreApi::class
)
class DesktopOwner(
    val container: DesktopOwners,
    density: Density = Density(1f, 1f)
) : Owner {
    private var size: IntSize = IntSize(0, 0)

    override var density by mutableStateOf(density)

    // TODO(demin): support RTL
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr

    private val semanticsModifier = SemanticsModifierCore(
        id = SemanticsModifierCore.generateSemanticsId(),
        mergeAllDescendants = false,
        properties = {}
    )

    private val _focusManager: FocusManagerImpl = FocusManagerImpl()
    override val focusManager: FocusManager
        get() = _focusManager

    private val keyInputModifier = KeyInputModifier(null, null)

    override val root = LayoutNode().also {
        it.measureBlocks = RootMeasureBlocks
        it.modifier = Modifier.drawLayer()
            .then(semanticsModifier)
            .then(_focusManager.modifier)
            .then(keyInputModifier)
    }

    private val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)
    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)

    init {
        container.register(this)
        snapshotObserver.enableStateUpdatesObserving(true)
        root.attach(this)
        _focusManager.takeFocus()
    }

    fun dispose() {
        snapshotObserver.enableStateUpdatesObserving(false)
        container.unregister(this)
        // we don't need to call root.detach() because root will be garbage collected
    }

    override val textInputService = TextInputService(container.platformInputService)

    override val fontLoader = FontLoader()

    override val hapticFeedBack = DesktopHapticFeedback()

    override val clipboardManager = DesktopClipboardManager()

    internal val selectionManager = SelectionManagerTracker()

    override val textToolbar = DesktopTextToolbar()

    override val semanticsOwner: SemanticsOwner = SemanticsOwner(root)

    override val autofillTree = AutofillTree()

    override val autofill: Autofill? get() = null

    override val viewConfiguration: ViewConfiguration = DesktopViewConfiguration(density)

    val keyboard: Keyboard?
        get() = container.keyboard

    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean {
        return keyInputModifier.processKeyInput(keyEvent) ||
            keyboard?.processKeyInput(keyEvent) ?: false
    }

    override var showLayoutBounds = false

    override fun pauseModelReadObserveration(block: () -> Unit) =
        snapshotObserver.pauseObservingReads(block)

    override fun requestFocus() = true

    override fun onAttach(node: LayoutNode) = Unit

    override fun onDetach(node: LayoutNode) {
        measureAndLayoutDelegate.onNodeDetached(node)
        snapshotObserver.clear(node)
    }

    override val measureIteration: Long get() = measureAndLayoutDelegate.measureIteration

    override fun measureAndLayout() {
        if (measureAndLayoutDelegate.measureAndLayout()) {
            container.invalidate()
        }
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
    }

    override fun onRequestMeasure(layoutNode: LayoutNode) {
        if (measureAndLayoutDelegate.requestRemeasure(layoutNode)) {
            container.invalidate()
        }
    }

    override fun onRequestRelayout(layoutNode: LayoutNode) {
        if (measureAndLayoutDelegate.requestRelayout(layoutNode)) {
            container.invalidate()
        }
    }

    override val hasPendingMeasureOrLayout
        get() = measureAndLayoutDelegate.hasPendingMeasureOrLayout

    // Don't inline these variables into snapshotObserver.observeReads,
    // because observeReads requires that onChanged should always be the same instance.
    // Otherwise there will be a memory leak and FPS drop (see b/163905871)
    private val onCommitAffectingLayout = ::onRequestRelayout
    private val onCommitAffectingMeasure = ::onRequestMeasure
    private val onCommitAffectingLayer = OwnedLayer::invalidate

    override fun observeLayoutModelReads(node: LayoutNode, block: () -> Unit) {
        snapshotObserver.observeReads(node, onCommitAffectingLayout, block)
    }

    override fun observeMeasureModelReads(node: LayoutNode, block: () -> Unit) {
        snapshotObserver.observeReads(node, onCommitAffectingMeasure, block)
    }

    override fun <T : OwnerScope> observeReads(
        target: T,
        onChanged: (T) -> Unit,
        block: () -> Unit
    ) {
        snapshotObserver.observeReads(target, onChanged, block)
    }

    private fun observeDrawModelReads(layer: SkijaLayer, block: () -> Unit) {
        snapshotObserver.observeReads(layer, onCommitAffectingLayer, block)
    }

    override fun createLayer(
        drawLayerModifier: DrawLayerModifier,
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit
    ) = SkijaLayer(
        this,
        drawLayerModifier,
        invalidateParentLayer = {
            invalidateParentLayer()
            container.invalidate()
        }
    ) { canvas ->
        observeDrawModelReads(this) {
            drawBlock(canvas)
        }
    }

    override fun onSemanticsChange() = Unit

    override fun calculatePosition() = IntOffset.Zero

    fun setSize(width: Int, height: Int) {
        val constraints = Constraints(0, width, 0, height)
        this.size = IntSize(width, height)
        measureAndLayoutDelegate.updateRootConstraints(constraints)
    }

    fun draw(canvas: org.jetbrains.skija.Canvas) {
        root.draw(DesktopCanvas(canvas))
    }

    fun processPointerInput(event: PointerInputEvent) {
        measureAndLayout()
        pointerInputEventProcessor.process(event)
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
                val relative = position - filter.layoutCoordinates!!.globalBounds.topLeft
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
}