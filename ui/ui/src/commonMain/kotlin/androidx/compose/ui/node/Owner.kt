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
package androidx.compose.ui.node

import androidx.compose.runtime.Applier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Owner implements the connection to the underlying view system. On Android, this connects
 * to Android [views][android.view.View] and all layout, draw, input, and accessibility is hooked
 * through them.
 */
internal interface Owner {

    /**
     * The root layout node in the component tree.
     */
    val root: LayoutNode

    /**
     * Draw scope reused for drawing speed up.
     */
    val sharedDrawScope: LayoutNodeDrawScope

    val rootForTest: RootForTest

    /**
     * Provide haptic feedback to the user. Use the Android version of haptic feedback.
     */
    val hapticFeedBack: HapticFeedback

    /**
     * Provide information about the current input mode, and a way to programmatically change the
     * input mode.
     */
    val inputModeManager: InputModeManager

    /**
     * Provide clipboard manager to the user. Use the Android version of clipboard manager.
     */
    val clipboardManager: ClipboardManager

    /**
     * Provide accessibility manager to the user. Use the Android version of accessibility manager.
     */
    val accessibilityManager: AccessibilityManager

    /**
     * Provide toolbar for text-related actions, such as copy, paste, cut etc.
     */
    val textToolbar: TextToolbar

    /**
     *  A data structure used to store autofill information. It is used by components that want to
     *  provide autofill semantics.
     *  TODO(ralu): Replace with SemanticsTree. This is a temporary hack until we have a semantics
     *  tree implemented.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalComposeUiApi
    @ExperimentalComposeUiApi
    val autofillTree: AutofillTree

    /**
     * The [Autofill] class can be used to perform autofill operations. It is used as a
     * CompositionLocal.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalComposeUiApi
    @ExperimentalComposeUiApi
    val autofill: Autofill?

    val density: Density

    val textInputService: TextInputService

    val pointerIconService: PointerIconService

    /**
     * Provide a focus manager that controls focus within Compose.
     */
    val focusManager: FocusManager

    /**
     * Provide information about the window that hosts this [Owner].
     */
    val windowInfo: WindowInfo

    @Deprecated(
        "fontLoader is deprecated, use fontFamilyResolver",
        replaceWith = ReplaceWith("fontFamilyResolver")
    )
    @Suppress("DEPRECATION")
    val fontLoader: Font.ResourceLoader

    val fontFamilyResolver: FontFamily.Resolver

    val layoutDirection: LayoutDirection

    /**
     * `true` when layout should draw debug bounds.
     */
    var showLayoutBounds: Boolean
        /** @suppress */
        @InternalCoreApi
        set

    /**
     * Called by [LayoutNode] to request the Owner a new measurement+layout. [forceRequest] defines
     * whether the node should bypass the logic that would reject measure requests, and therefore
     * force the measure request to be evaluated even when it's already pending measure.
     *
     * [affectsLookahead] specifies whether this measure request is for the lookahead pass.
     */
    fun onRequestMeasure(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean = false,
        forceRequest: Boolean = false
    )

    /**
     * Called by [LayoutNode] to request the Owner a new layout. [forceRequest] defines
     * whether the node should bypass the logic that would reject relayout requests, and therefore
     * force the relayout request to be evaluated even when it's already pending measure/layout.
     *
     * [affectsLookahead] specifies whether this relayout request is for the lookahead pass
     * pass.
     */
    fun onRequestRelayout(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean = false,
        forceRequest: Boolean = false
    )

    /**
     * Called when graphics layers have changed the position of children and the
     * OnGloballyPositionedModifiers must be called.
     */
    fun requestOnPositionedCallback(layoutNode: LayoutNode)

    /**
     * Called by [LayoutNode] when it is attached to the view system and now has an owner.
     * This is used by [Owner] to track which nodes are associated with it. It will only be
     * called when [node] is not already attached to an owner.
     */
    fun onAttach(node: LayoutNode)

    /**
     * Called by [LayoutNode] when it is detached from the view system, such as during
     * [LayoutNode.removeAt]. This will only be called for [node]s that are already
     * [LayoutNode.attach]ed.
     */
    fun onDetach(node: LayoutNode)

    /**
     * Returns the position relative to the containing window of the [localPosition],
     * the position relative to the [Owner]. If the [Owner] is rotated, scaled, or otherwise
     * transformed relative to the window, this will not be a simple translation.
     */
    fun calculatePositionInWindow(localPosition: Offset): Offset

    /**
     * Returns the position relative to the [Owner] of the [positionInWindow],
     * the position relative to the window. If the [Owner] is rotated, scaled, or otherwise
     * transformed relative to the window, this will not be a simple translation.
     */
    fun calculateLocalPosition(positionInWindow: Offset): Offset

    /**
     * Ask the system to provide focus to this owner.
     *
     * @return true if the system granted focus to this owner. False otherwise.
     */
    fun requestFocus(): Boolean

    /**
     * Iterates through all LayoutNodes that have requested layout and measures and lays them out.
     * If [sendPointerUpdate] is `true` then a simulated PointerEvent may be sent to update pointer
     * input handlers.
     */
    fun measureAndLayout(sendPointerUpdate: Boolean = true)

    /**
     * Measures and lays out only the passed [layoutNode]. It will be remeasured with the passed
     * [constraints].
     */
    fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints)

    /**
     * Makes sure the passed [layoutNode] and its subtree is remeasured and has the final sizes.
     */
    fun forceMeasureTheSubtree(layoutNode: LayoutNode)

    /**
     * Creates an [OwnedLayer] which will be drawing the passed [drawBlock].
     */
    fun createLayer(drawBlock: (Canvas) -> Unit, invalidateParentLayer: () -> Unit): OwnedLayer

    /**
     * The semantics have changed. This function will be called when a SemanticsNode is added to
     * or deleted from the Semantics tree. It will also be called when a SemanticsNode in the
     * Semantics tree has some property change.
     */
    fun onSemanticsChange()

    /**
     * The position and/or size of the [layoutNode] changed.
     */
    fun onLayoutChange(layoutNode: LayoutNode)

    /**
     * The [FocusDirection] represented by the specified keyEvent.
     */
    fun getFocusDirection(keyEvent: KeyEvent): FocusDirection?

    val measureIteration: Long

    /**
     * The [ViewConfiguration] to use in the application.
     */
    val viewConfiguration: ViewConfiguration

    /**
     * Performs snapshot observation for blocks like draw and layout which should be re-invoked
     * automatically when the snapshot value has been changed.
     */
    val snapshotObserver: OwnerSnapshotObserver

    /**
     * Registers a call to be made when the [Applier.onEndChanges] is called. [listener]
     * should be called in [onEndApplyChanges] and then removed after being called.
     */
    fun registerOnEndApplyChangesListener(listener: () -> Unit)

    /**
     * Called when [Applier.onEndChanges] executes. This must call all listeners registered
     * in [registerOnEndApplyChangesListener] and then remove them so that they are not
     * called again.
     */
    fun onEndApplyChanges()

    /**
     * [listener] will be notified after the current or next layout has finished.
     */
    fun registerOnLayoutCompletedListener(listener: OnLayoutCompletedListener)

    companion object {
        /**
         * Enables additional (and expensive to do in production) assertions. Useful to be set
         * to true during the tests covering our core logic.
         */
        var enableExtraAssertions: Boolean = false
    }

    interface OnLayoutCompletedListener {
        fun onLayoutComplete()
    }
}
