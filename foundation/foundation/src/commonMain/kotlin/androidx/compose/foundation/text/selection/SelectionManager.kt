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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.fastFold
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.selection.Selection.AnchorInfo
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntSize
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.coroutineScope

/**
 * A bridge class between user interaction to the text composables for text selection.
 */
internal class SelectionManager(private val selectionRegistrar: SelectionRegistrarImpl) {

    private val _selection: MutableState<Selection?> = mutableStateOf(null)

    /**
     * The current selection.
     */
    var selection: Selection?
        get() = _selection.value
        set(value) {
            _selection.value = value
            if (value != null) {
                updateHandleOffsets()
            }
        }

    /**
     * Is touch mode active
     */
    var touchMode: Boolean = true

    /**
     * The manager will invoke this every time it comes to the conclusion that the selection should
     * change. The expectation is that this callback will end up causing `setSelection` to get
     * called. This is what makes this a "controlled component".
     */
    var onSelectionChange: (Selection?) -> Unit = {}

    /**
     * [HapticFeedback] handle to perform haptic feedback.
     */
    var hapticFeedBack: HapticFeedback? = null

    /**
     * [ClipboardManager] to perform clipboard features.
     */
    var clipboardManager: ClipboardManager? = null

    /**
     * [TextToolbar] to show floating toolbar(post-M) or primary toolbar(pre-M).
     */
    var textToolbar: TextToolbar? = null

    /**
     * Focus requester used to request focus when selection becomes active.
     */
    var focusRequester: FocusRequester = FocusRequester()

    /**
     * Return true if the corresponding SelectionContainer is focused.
     */
    var hasFocus: Boolean by mutableStateOf(false)

    /**
     * Modifier for selection container.
     */
    val modifier
        get() = Modifier
            .onClearSelectionRequested { onRelease() }
            .onGloballyPositioned { containerLayoutCoordinates = it }
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (!focusState.isFocused && hasFocus) {
                    onRelease()
                }
                hasFocus = focusState.isFocused
            }
            .focusable()
            .onKeyEvent {
                if (isCopyKeyEvent(it)) {
                    copy()
                    true
                } else {
                    false
                }
            }
            .then(if (shouldShowMagnifier) Modifier.selectionMagnifier(this) else Modifier)

    private var previousPosition: Offset? = null

    /**
     * Layout Coordinates of the selection container.
     */
    var containerLayoutCoordinates: LayoutCoordinates? = null
        set(value) {
            field = value
            if (hasFocus && selection != null) {
                val positionInWindow = value?.positionInWindow()
                if (previousPosition != positionInWindow) {
                    previousPosition = positionInWindow
                    updateHandleOffsets()
                    updateSelectionToolbarPosition()
                }
            }
        }

    /**
     * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
     * recalculated.
     */
    internal var dragBeginPosition by mutableStateOf(Offset.Zero)
        private set

    /**
     * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
     * it will be zeroed out.
     */
    internal var dragTotalDistance by mutableStateOf(Offset.Zero)
        private set

    /**
     * The calculated position of the start handle in the [SelectionContainer] coordinates. It
     * is null when handle shouldn't be displayed.
     * It is a [State] so reading it during the composition will cause recomposition every time
     * the position has been changed.
     */
    var startHandlePosition: Offset? by mutableStateOf(null)
        private set

    /**
     * The calculated position of the end handle in the [SelectionContainer] coordinates. It
     * is null when handle shouldn't be displayed.
     * It is a [State] so reading it during the composition will cause recomposition every time
     * the position has been changed.
     */
    var endHandlePosition: Offset? by mutableStateOf(null)
        private set

    /**
     * The handle that is currently being dragged, or null when no handle is being dragged. To get
     * the position of the last drag event, use [currentDragPosition].
     */
    var draggingHandle: Handle? by mutableStateOf(null)
        private set

    /**
     * When a handle is being dragged (i.e. [draggingHandle] is non-null), this is the last position
     * of the actual drag event. It is not clamped to handle positions. Null when not being dragged.
     */
    var currentDragPosition: Offset? by mutableStateOf(null)
        private set

    private val shouldShowMagnifier get() = draggingHandle != null

    init {
        selectionRegistrar.onPositionChangeCallback = { selectableId ->
            if (
                selectableId == selection?.start?.selectableId ||
                selectableId == selection?.end?.selectableId
            ) {
                updateHandleOffsets()
                updateSelectionToolbarPosition()
            }
        }

        selectionRegistrar.onSelectionUpdateStartCallback =
            { layoutCoordinates, position, selectionMode ->
                val positionInContainer = convertToContainerCoordinates(
                    layoutCoordinates,
                    position
                )

                if (positionInContainer != null) {
                    startSelection(
                        position = positionInContainer,
                        isStartHandle = false,
                        adjustment = selectionMode
                    )

                    focusRequester.requestFocus()
                    hideSelectionToolbar()
                }
            }

        selectionRegistrar.onSelectionUpdateSelectAll =
            { selectableId ->
                val (newSelection, newSubselection) = selectAll(
                    selectableId = selectableId,
                    previousSelection = selection,
                )
                if (newSelection != selection) {
                    selectionRegistrar.subselections = newSubselection
                    onSelectionChange(newSelection)
                }

                focusRequester.requestFocus()
                hideSelectionToolbar()
            }

        selectionRegistrar.onSelectionUpdateCallback =
            { layoutCoordinates, newPosition, previousPosition, isStartHandle, selectionMode ->
                val newPositionInContainer =
                    convertToContainerCoordinates(layoutCoordinates, newPosition)
                val previousPositionInContainer =
                    convertToContainerCoordinates(layoutCoordinates, previousPosition)

                updateSelection(
                    newPosition = newPositionInContainer,
                    previousPosition = previousPositionInContainer,
                    isStartHandle = isStartHandle,
                    adjustment = selectionMode
                )
            }

        selectionRegistrar.onSelectionUpdateEndCallback = {
            showSelectionToolbar()
            // This property is set by updateSelection while dragging, so we need to clear it after
            // the original selection drag.
            draggingHandle = null
            currentDragPosition = null
        }

        selectionRegistrar.onSelectableChangeCallback = { selectableKey ->
            if (selectableKey in selectionRegistrar.subselections) {
                // clear the selection range of each Selectable.
                onRelease()
                selection = null
            }
        }

        selectionRegistrar.afterSelectableUnsubscribe = { selectableKey ->
            if (
                selectableKey == selection?.start?.selectableId ||
                selectableKey == selection?.end?.selectableId
            ) {
                // The selectable that contains a selection handle just unsubscribed.
                // Hide selection handles for now
                startHandlePosition = null
                endHandlePosition = null
            }
        }
    }

    /**
     * Returns the [Selectable] responsible for managing the given [AnchorInfo], or null if the
     * anchor is not from a currently-registered [Selectable].
     */
    internal fun getAnchorSelectable(anchor: AnchorInfo): Selectable? {
        return selectionRegistrar.selectableMap[anchor.selectableId]
    }

    private fun updateHandleOffsets() {
        val selection = selection
        val containerCoordinates = containerLayoutCoordinates
        val startSelectable = selection?.start?.let(::getAnchorSelectable)
        val endSelectable = selection?.end?.let(::getAnchorSelectable)
        val startLayoutCoordinates = startSelectable?.getLayoutCoordinates()
        val endLayoutCoordinates = endSelectable?.getLayoutCoordinates()
        if (
            selection == null ||
            containerCoordinates == null ||
            !containerCoordinates.isAttached ||
            startLayoutCoordinates == null ||
            endLayoutCoordinates == null
        ) {
            this.startHandlePosition = null
            this.endHandlePosition = null
            return
        }

        val startHandlePosition = containerCoordinates.localPositionOf(
            startLayoutCoordinates,
            startSelectable.getHandlePosition(
                selection = selection,
                isStartHandle = true
            )
        )
        val endHandlePosition = containerCoordinates.localPositionOf(
            endLayoutCoordinates,
            endSelectable.getHandlePosition(
                selection = selection,
                isStartHandle = false
            )
        )

        val visibleBounds = containerCoordinates.visibleBounds()
        this.startHandlePosition =
            if (visibleBounds.containsInclusive(startHandlePosition)) startHandlePosition else null
        this.endHandlePosition =
            if (visibleBounds.containsInclusive(endHandlePosition)) endHandlePosition else null
    }

    /**
     * Returns non-nullable [containerLayoutCoordinates].
     */
    internal fun requireContainerCoordinates(): LayoutCoordinates {
        val coordinates = containerLayoutCoordinates
        require(coordinates != null)
        require(coordinates.isAttached)
        return coordinates
    }

    internal fun selectAll(
        selectableId: Long,
        previousSelection: Selection?
    ): Pair<Selection?, Map<Long, Selection>> {
        val subselections = mutableMapOf<Long, Selection>()
        val newSelection = selectionRegistrar.sort(requireContainerCoordinates())
            .fastFold(null) { mergedSelection: Selection?, selectable: Selectable ->
                val selection = if (selectable.selectableId == selectableId)
                    selectable.getSelectAllSelection() else null
                selection?.let { subselections[selectable.selectableId] = it }
                merge(mergedSelection, selection)
            }
        if (newSelection != previousSelection) {
            hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        return Pair(newSelection, subselections)
    }

    internal fun getSelectedText(): AnnotatedString? {
        val selectables = selectionRegistrar.sort(requireContainerCoordinates())
        var selectedText: AnnotatedString? = null

        selection?.let {
            for (i in selectables.indices) {
                val selectable = selectables[i]
                // Continue if the current selectable is before the selection starts.
                if (selectable.selectableId != it.start.selectableId &&
                    selectable.selectableId != it.end.selectableId &&
                    selectedText == null
                ) continue

                val currentSelectedText = getCurrentSelectedText(
                    selectable = selectable,
                    selection = it
                )
                selectedText = selectedText?.plus(currentSelectedText) ?: currentSelectedText

                // Break if the current selectable is the last selected selectable.
                if (selectable.selectableId == it.end.selectableId && !it.handlesCrossed ||
                    selectable.selectableId == it.start.selectableId && it.handlesCrossed
                ) break
            }
        }
        return selectedText
    }

    internal fun copy() {
        val selectedText = getSelectedText()
        selectedText?.let { clipboardManager?.setText(it) }
    }

    /**
     * This function get the selected region as a Rectangle region, and pass it to [TextToolbar]
     * to make the FloatingToolbar show up in the proper place. In addition, this function passes
     * the copy method as a callback when "copy" is clicked.
     */
    internal fun showSelectionToolbar() {
        if (hasFocus) {
            selection?.let {
                textToolbar?.showMenu(
                    getContentRect(),
                    onCopyRequested = {
                        copy()
                        onRelease()
                    }
                )
            }
        }
    }

    internal fun hideSelectionToolbar() {
        if (hasFocus && textToolbar?.status == TextToolbarStatus.Shown) {
            textToolbar?.hide()
        }
    }

    private fun updateSelectionToolbarPosition() {
        if (hasFocus && textToolbar?.status == TextToolbarStatus.Shown) {
            showSelectionToolbar()
        }
    }

    /**
     * Calculate selected region as [Rect]. The top is the top of the first selected
     * line, and the bottom is the bottom of the last selected line. The left is the leftmost
     * handle's horizontal coordinates, and the right is the rightmost handle's coordinates.
     */
    private fun getContentRect(): Rect {
        val selection = selection ?: return Rect.Zero
        val startSelectable = getAnchorSelectable(selection.start)
        val endSelectable = getAnchorSelectable(selection.end)
        val startLayoutCoordinates = startSelectable?.getLayoutCoordinates() ?: return Rect.Zero
        val endLayoutCoordinates = endSelectable?.getLayoutCoordinates() ?: return Rect.Zero

        val localLayoutCoordinates = containerLayoutCoordinates
        if (localLayoutCoordinates != null && localLayoutCoordinates.isAttached) {
            var startOffset = localLayoutCoordinates.localPositionOf(
                startLayoutCoordinates,
                startSelectable.getHandlePosition(
                    selection = selection,
                    isStartHandle = true
                )
            )
            var endOffset = localLayoutCoordinates.localPositionOf(
                endLayoutCoordinates,
                endSelectable.getHandlePosition(
                    selection = selection,
                    isStartHandle = false
                )
            )

            startOffset = localLayoutCoordinates.localToRoot(startOffset)
            endOffset = localLayoutCoordinates.localToRoot(endOffset)

            val left = min(startOffset.x, endOffset.x)
            val right = max(startOffset.x, endOffset.x)

            var startTop = localLayoutCoordinates.localPositionOf(
                startLayoutCoordinates,
                Offset(
                    0f,
                    startSelectable.getBoundingBox(selection.start.offset).top
                )
            )

            var endTop = localLayoutCoordinates.localPositionOf(
                endLayoutCoordinates,
                Offset(
                    0.0f,
                    endSelectable.getBoundingBox(selection.end.offset).top
                )
            )

            startTop = localLayoutCoordinates.localToRoot(startTop)
            endTop = localLayoutCoordinates.localToRoot(endTop)

            val top = min(startTop.y, endTop.y)
            val bottom = max(startOffset.y, endOffset.y) + (HandleHeight.value * 4.0).toFloat()

            return Rect(
                left,
                top,
                right,
                bottom
            )
        }
        return Rect.Zero
    }

    // This is for PressGestureDetector to cancel the selection.
    fun onRelease() {
        selectionRegistrar.subselections = emptyMap()
        hideSelectionToolbar()
        if (selection != null) {
            onSelectionChange(null)
            hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun handleDragObserver(isStartHandle: Boolean): TextDragObserver = object : TextDragObserver {
        override fun onDown(point: Offset) {
            val selection = selection ?: return
            val anchor = if (isStartHandle) selection.start else selection.end
            val selectable = getAnchorSelectable(anchor) ?: return
            // The LayoutCoordinates of the composable where the drag gesture should begin. This
            // is used to convert the position of the beginning of the drag gesture from the
            // composable coordinates to selection container coordinates.
            val beginLayoutCoordinates = selectable.getLayoutCoordinates() ?: return

            // The position of the character where the drag gesture should begin. This is in
            // the composable coordinates.
            val beginCoordinates = getAdjustedCoordinates(
                selectable.getHandlePosition(
                    selection = selection, isStartHandle = isStartHandle
                )
            )

            // Convert the position where drag gesture begins from composable coordinates to
            // selection container coordinates.
            currentDragPosition = requireContainerCoordinates().localPositionOf(
                beginLayoutCoordinates,
                beginCoordinates
            )
            draggingHandle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd
        }

        override fun onUp() {
            draggingHandle = null
            currentDragPosition = null
        }

        override fun onStart(startPoint: Offset) {
            hideSelectionToolbar()
            val selection = selection!!
            val startSelectable =
                selectionRegistrar.selectableMap[selection.start.selectableId]
            val endSelectable =
                selectionRegistrar.selectableMap[selection.end.selectableId]
            // The LayoutCoordinates of the composable where the drag gesture should begin. This
            // is used to convert the position of the beginning of the drag gesture from the
            // composable coordinates to selection container coordinates.
            val beginLayoutCoordinates = if (isStartHandle) {
                startSelectable?.getLayoutCoordinates()!!
            } else {
                endSelectable?.getLayoutCoordinates()!!
            }

            // The position of the character where the drag gesture should begin. This is in
            // the composable coordinates.
            val beginCoordinates = getAdjustedCoordinates(
                if (isStartHandle) {
                    startSelectable!!.getHandlePosition(
                        selection = selection, isStartHandle = true
                    )
                } else {
                    endSelectable!!.getHandlePosition(
                        selection = selection, isStartHandle = false
                    )
                }
            )

            // Convert the position where drag gesture begins from composable coordinates to
            // selection container coordinates.
            dragBeginPosition = requireContainerCoordinates().localPositionOf(
                beginLayoutCoordinates,
                beginCoordinates
            )

            // Zero out the total distance that being dragged.
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(delta: Offset) {
            dragTotalDistance += delta
            val endPosition = dragBeginPosition + dragTotalDistance
            val consumed = updateSelection(
                newPosition = endPosition,
                previousPosition = dragBeginPosition,
                isStartHandle = isStartHandle,
                adjustment = SelectionAdjustment.CharacterWithWordAccelerate
            )
            if (consumed) {
                dragBeginPosition = endPosition
                dragTotalDistance = Offset.Zero
            }
        }

        override fun onStop() {
            showSelectionToolbar()
            draggingHandle = null
            currentDragPosition = null
        }

        override fun onCancel() {
            showSelectionToolbar()
            draggingHandle = null
            currentDragPosition = null
        }
    }

    /**
     * Detect tap without consuming the up event.
     */
    private suspend fun PointerInputScope.detectNonConsumingTap(onTap: (Offset) -> Unit) {
        forEachGesture {
            coroutineScope {
                awaitPointerEventScope {
                    waitForUpOrCancellation()?.let {
                        onTap(it.position)
                    }
                }
            }
        }
    }

    private fun Modifier.onClearSelectionRequested(block: () -> Unit): Modifier {
        return if (hasFocus) pointerInput(Unit) { detectNonConsumingTap { block() } } else this
    }

    private fun convertToContainerCoordinates(
        layoutCoordinates: LayoutCoordinates,
        offset: Offset
    ): Offset? {
        val coordinates = containerLayoutCoordinates
        if (coordinates == null || !coordinates.isAttached) return null
        return requireContainerCoordinates().localPositionOf(layoutCoordinates, offset)
    }

    /**
     * Cancel the previous selection and start a new selection at the given [position].
     * It's used for long-press, double-click, triple-click and so on to start selection.
     *
     * @param position initial position of the selection. Both start and end handle is considered
     * at this position.
     * @param isStartHandle whether it's considered as the start handle moving. This parameter
     * will influence the [SelectionAdjustment]'s behavior. For example,
     * [SelectionAdjustment.Character] only adjust the moving handle.
     * @param adjustment the selection adjustment.
     */
    private fun startSelection(
        position: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment
    ) {
        updateSelection(
            startHandlePosition = position,
            endHandlePosition = position,
            previousHandlePosition = null,
            isStartHandle = isStartHandle,
            adjustment = adjustment
        )
    }

    /**
     * Updates the selection after one of the selection handle moved.
     *
     * @param newPosition the new position of the moving selection handle.
     * @param previousPosition the previous position of the moving selection handle.
     * @param isStartHandle whether the moving selection handle is the start handle.
     * @param adjustment the [SelectionAdjustment] used to adjust the raw selection range and
     * produce the final selection range.
     *
     * @return a boolean representing whether the movement is consumed.
     *
     * @see SelectionAdjustment
     */
    internal fun updateSelection(
        newPosition: Offset?,
        previousPosition: Offset?,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
    ): Boolean {
        if (newPosition == null) return false
        val otherHandlePosition = selection?.let { selection ->
            val otherSelectableId = if (isStartHandle) {
                selection.end.selectableId
            } else {
                selection.start.selectableId
            }
            val otherSelectable =
                selectionRegistrar.selectableMap[otherSelectableId] ?: return@let null
            convertToContainerCoordinates(
                otherSelectable.getLayoutCoordinates()!!,
                getAdjustedCoordinates(
                    otherSelectable.getHandlePosition(selection, !isStartHandle)
                )
            )
        } ?: return false

        val startHandlePosition = if (isStartHandle) newPosition else otherHandlePosition
        val endHandlePosition = if (isStartHandle) otherHandlePosition else newPosition

        return updateSelection(
            startHandlePosition = startHandlePosition,
            endHandlePosition = endHandlePosition,
            previousHandlePosition = previousPosition,
            isStartHandle = isStartHandle,
            adjustment = adjustment
        )
    }

    /**
     * Updates the selection after one of the selection handle moved.
     *
     * To make sure that [SelectionAdjustment] works correctly, it's expected that only one
     * selection handle is updated each time. The only exception is that when a new selection is
     * started. In this case, [previousHandlePosition] is always null.
     *
     * @param startHandlePosition the position of the start selection handle.
     * @param endHandlePosition the position of the end selection handle.
     * @param previousHandlePosition the position of the moving handle before the update.
     * @param isStartHandle whether the moving selection handle is the start handle.
     * @param adjustment the [SelectionAdjustment] used to adjust the raw selection range and
     * produce the final selection range.
     *
     * @return a boolean representing whether the movement is consumed. It's useful for the case
     * where a selection handle is updating consecutively. When the return value is true, it's
     * expected that the caller will update the [startHandlePosition] to be the given
     * [endHandlePosition] in following calls.
     *
     * @see SelectionAdjustment
     */
    internal fun updateSelection(
        startHandlePosition: Offset,
        endHandlePosition: Offset,
        previousHandlePosition: Offset?,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
    ): Boolean {
        draggingHandle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd
        currentDragPosition = if (isStartHandle) startHandlePosition else endHandlePosition
        val newSubselections = mutableMapOf<Long, Selection>()
        var moveConsumed = false
        val newSelection = selectionRegistrar.sort(requireContainerCoordinates())
            .fastFold(null) { mergedSelection: Selection?, selectable: Selectable ->
                val previousSubselection =
                    selectionRegistrar.subselections[selectable.selectableId]
                val (selection, consumed) = selectable.updateSelection(
                    startHandlePosition = startHandlePosition,
                    endHandlePosition = endHandlePosition,
                    previousHandlePosition = previousHandlePosition,
                    isStartHandle = isStartHandle,
                    containerLayoutCoordinates = requireContainerCoordinates(),
                    adjustment = adjustment,
                    previousSelection = previousSubselection,
                )

                moveConsumed = moveConsumed || consumed
                selection?.let { newSubselections[selectable.selectableId] = it }
                merge(mergedSelection, selection)
            }
        if (newSelection != selection) {
            hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            selectionRegistrar.subselections = newSubselections
            onSelectionChange(newSelection)
        }
        return moveConsumed
    }

    fun contextMenuOpenAdjustment(position: Offset) {
        val isEmptySelection = selection?.toTextRange()?.collapsed ?: true
        // TODO(b/209483184) the logic should be more complex here, it should check that current
        // selection doesn't include click position
        if (isEmptySelection) {
            startSelection(
                position = position,
                isStartHandle = true,
                adjustment = SelectionAdjustment.Word
            )
        }
    }
}

internal fun merge(lhs: Selection?, rhs: Selection?): Selection? {
    return lhs?.merge(rhs) ?: rhs
}

internal expect fun isCopyKeyEvent(keyEvent: KeyEvent): Boolean

internal expect fun Modifier.selectionMagnifier(manager: SelectionManager): Modifier

internal fun calculateSelectionMagnifierCenterAndroid(
    manager: SelectionManager,
    magnifierSize: IntSize
): Offset {
    fun getMagnifierCenter(anchor: AnchorInfo, isStartHandle: Boolean): Offset {
        val selectable = manager.getAnchorSelectable(anchor) ?: return Offset.Unspecified
        val containerCoordinates = manager.containerLayoutCoordinates ?: return Offset.Unspecified
        val selectableCoordinates = selectable.getLayoutCoordinates() ?: return Offset.Unspecified
        // The end offset is exclusive.
        val offset = if (isStartHandle) anchor.offset else anchor.offset - 1

        // The horizontal position doesn't snap to cursor positions but should directly track the
        // actual drag.
        val localDragPosition = selectableCoordinates.localPositionOf(
            containerCoordinates,
            manager.currentDragPosition!!
        )
        val dragX = localDragPosition.x
        // But it is constrained by the horizontal bounds of the current line.
        val centerX = selectable.getRangeOfLineContaining(offset).let { line ->
            val lineMin = selectable.getBoundingBox(line.min)
            // line.end is exclusive, but we want the bounding box of the actual last character in
            // the line.
            val lineMax = selectable.getBoundingBox((line.max - 1).coerceAtLeast(line.min))
            val minX = minOf(lineMin.left, lineMax.left)
            val maxX = maxOf(lineMin.right, lineMax.right)
            dragX.coerceIn(minX, maxX)
        }

        // Hide the magnifier when dragged too far (outside the horizontal bounds of how big the
        // magnifier actually is). See
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/Editor.java;l=5228-5231;drc=2fdb6bd709be078b72f011334362456bb758922c
        if ((dragX - centerX).absoluteValue > magnifierSize.width / 2) {
            return Offset.Unspecified
        }

        // Let the selectable determine the vertical position of the magnifier, since it should be
        // clamped to the center of text lines.
        val anchorBounds = selectable.getBoundingBox(offset)
        val centerY = anchorBounds.center.y

        return containerCoordinates.localPositionOf(
            sourceCoordinates = selectableCoordinates,
            relativeToSource = Offset(centerX, centerY)
        )
    }

    val selection = manager.selection ?: return Offset.Unspecified
    return when (manager.draggingHandle) {
        null -> return Offset.Unspecified
        Handle.SelectionStart -> getMagnifierCenter(selection.start, isStartHandle = true)
        Handle.SelectionEnd -> getMagnifierCenter(selection.end, isStartHandle = false)
        Handle.Cursor -> error("SelectionContainer does not support cursor")
    }
}

internal fun getCurrentSelectedText(
    selectable: Selectable,
    selection: Selection
): AnnotatedString {
    val currentText = selectable.getText()

    return if (
        selectable.selectableId != selection.start.selectableId &&
        selectable.selectableId != selection.end.selectableId
    ) {
        // Select the full text content if the current selectable is between the
        // start and the end selectables.
        currentText
    } else if (
        selectable.selectableId == selection.start.selectableId &&
        selectable.selectableId == selection.end.selectableId
    ) {
        // Select partial text content if the current selectable is the start and
        // the end selectable.
        if (selection.handlesCrossed) {
            currentText.subSequence(selection.end.offset, selection.start.offset)
        } else {
            currentText.subSequence(selection.start.offset, selection.end.offset)
        }
    } else if (selectable.selectableId == selection.start.selectableId) {
        // Select partial text content if the current selectable is the start
        // selectable.
        if (selection.handlesCrossed) {
            currentText.subSequence(0, selection.start.offset)
        } else {
            currentText.subSequence(selection.start.offset, currentText.length)
        }
    } else {
        // Selectable partial text content if the current selectable is the end
        // selectable.
        if (selection.handlesCrossed) {
            currentText.subSequence(selection.end.offset, currentText.length)
        } else {
            currentText.subSequence(0, selection.end.offset)
        }
    }
}

/** Returns the boundary of the visible area in this [LayoutCoordinates]. */
internal fun LayoutCoordinates.visibleBounds(): Rect {
    // globalBounds is the global boundaries of this LayoutCoordinates after it's clipped by
    // parents. We can think it as the global visible bounds of this Layout. Here globalBounds
    // is convert to local, which is the boundary of the visible area within the LayoutCoordinates.
    val boundsInWindow = boundsInWindow()
    return Rect(
        windowToLocal(boundsInWindow.topLeft),
        windowToLocal(boundsInWindow.bottomRight)
    )
}

internal fun Rect.containsInclusive(offset: Offset): Boolean =
    offset.x in left..right && offset.y in top..bottom
