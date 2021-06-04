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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.text.TextDragObserver
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
import kotlinx.coroutines.coroutineScope
import kotlin.math.max
import kotlin.math.min

/**
 * A bridge class between user interaction to the text composables for text selection.
 */
internal class SelectionManager(private val selectionRegistrar: SelectionRegistrarImpl) {
    /**
     * The current selection.
     */
    var selection: Selection? = null
        set(value) {
            field = value
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
    val modifier get() = Modifier
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
    private var dragBeginPosition = Offset.Zero

    /**
     * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
     * it will be zeroed out.
     */
    private var dragTotalDistance = Offset.Zero

    /**
     * The calculated position of the start handle in the [SelectionContainer] coordinates. It
     * is null when handle shouldn't be displayed.
     * It is a [State] so reading it during the composition will cause recomposition every time
     * the position has been changed.
     */
    var startHandlePosition by mutableStateOf<Offset?>(
        null,
        policy = structuralEqualityPolicy()
    )
        private set

    /**
     * The calculated position of the end handle in the [SelectionContainer] coordinates. It
     * is null when handle shouldn't be displayed.
     * It is a [State] so reading it during the composition will cause recomposition every time
     * the position has been changed.
     */
    var endHandlePosition by mutableStateOf<Offset?>(
        null,
        policy = structuralEqualityPolicy()
    )
        private set

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
            { layoutCoordinates, startPosition, selectionMode ->
                val startPositionInContainer = convertToContainerCoordinates(
                    layoutCoordinates,
                    startPosition
                )

                updateSelection(
                    startPosition = startPositionInContainer,
                    endPosition = startPositionInContainer,
                    isStartHandle = true,
                    adjustment = selectionMode
                )

                focusRequester.requestFocus()
                hideSelectionToolbar()
            }

        selectionRegistrar.onSelectionUpdateSelectAll =
            { selectableId ->
                val (newSelection, newSubselection) = mergeSelections(
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
            { layoutCoordinates, startPosition, endPosition, selectionMode ->
                val startPositionOrCurrent = if (startPosition == null) {
                    currentSelectionStartPosition()
                } else {
                    convertToContainerCoordinates(layoutCoordinates, startPosition)
                }

                updateSelection(
                    startPosition = startPositionOrCurrent,
                    endPosition = convertToContainerCoordinates(layoutCoordinates, endPosition),
                    isStartHandle = false,
                    adjustment = selectionMode
                )
            }

        selectionRegistrar.onSelectionUpdateEndCallback = {
            showSelectionToolbar()
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

    private fun currentSelectionStartPosition(): Offset? {
        return selection?.let { selection ->
            val startSelectable =
                selectionRegistrar.selectableMap[selection.start.selectableId]

            requireContainerCoordinates().localPositionOf(
                startSelectable?.getLayoutCoordinates()!!,
                getAdjustedCoordinates(
                    startSelectable.getHandlePosition(
                        selection = selection,
                        isStartHandle = true
                    )
                )
            )
        }
    }

    private fun updateHandleOffsets() {
        val selection = selection
        val containerCoordinates = containerLayoutCoordinates
        val startSelectable = selection?.start?.selectableId?.let {
            selectionRegistrar.selectableMap[it]
        }
        val endSelectable = selection?.end?.selectableId?.let {
            selectionRegistrar.selectableMap[it]
        }
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

    /**
     * Iterates over the handlers, gets the selection for each Composable, and merges all the
     * returned [Selection]s.
     *
     * @param startPosition [Offset] for the start of the selection
     * @param endPosition [Offset] for the end of the selection
     * @param previousSelection previous selection
     *
     * @return a [Pair] of a [Selection] object which is constructed by combining all
     * composables that are selected and a [Map] from selectable key to [Selection]s on the
     * [Selectable] corresponding to the that key.
     */
    // This function is internal for testing purposes.
    internal fun mergeSelections(
        startPosition: Offset,
        endPosition: Offset,
        adjustment: SelectionAdjustment = SelectionAdjustment.NONE,
        previousSelection: Selection? = null,
        isStartHandle: Boolean = true
    ): Pair<Selection?, Map<Long, Selection>> {
        val subselections = mutableMapOf<Long, Selection>()
        val newSelection = selectionRegistrar.sort(requireContainerCoordinates())
            .fastFold(null) { mergedSelection: Selection?, selectable: Selectable ->
                val selection = selectable.getSelection(
                    startPosition = startPosition,
                    endPosition = endPosition,
                    containerLayoutCoordinates = requireContainerCoordinates(),
                    previousSelection = previousSelection,
                    isStartHandle = isStartHandle,
                    adjustment = adjustment
                )
                selection?.let { subselections[selectable.selectableId] = it }
                merge(mergedSelection, selection)
            }
        if (previousSelection != newSelection) hapticFeedBack?.performHapticFeedback(
            HapticFeedbackType.TextHandleMove
        )
        return Pair(newSelection, subselections)
    }

    internal fun mergeSelections(
        previousSelection: Selection? = null,
        selectableId: Long
    ): Pair<Selection?, Map<Long, Selection>> {
        val subselections = mutableMapOf<Long, Selection>()
        val newSelection = selectionRegistrar.sort(requireContainerCoordinates())
            .fastFold(null) { mergedSelection: Selection?, selectable: Selectable ->
                val selection = if (selectable.selectableId == selectableId)
                    selectable.getSelectAllSelection() else null
                selection?.let { subselections[selectable.selectableId] = it }
                merge(mergedSelection, selection)
            }
        if (previousSelection != newSelection) hapticFeedBack?.performHapticFeedback(
            HapticFeedbackType.TextHandleMove
        )
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
        val startSelectable = selectionRegistrar.selectableMap[selection.start.selectableId]
        val endSelectable = selectionRegistrar.selectableMap[selection.start.selectableId]
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
            val bottom = max(startOffset.y, endOffset.y) + (HANDLE_HEIGHT.value * 4.0).toFloat()

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

    fun handleDragObserver(isStartHandle: Boolean): TextDragObserver {
        return object : TextDragObserver {
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
                val selection = selection!!
                dragTotalDistance += delta
                val startSelectable =
                    selectionRegistrar.selectableMap[selection.start.selectableId]
                val endSelectable =
                    selectionRegistrar.selectableMap[selection.end.selectableId]
                val currentStart = if (isStartHandle) {
                    dragBeginPosition + dragTotalDistance
                } else {
                    requireContainerCoordinates().localPositionOf(
                        startSelectable?.getLayoutCoordinates()!!,
                        getAdjustedCoordinates(
                            startSelectable.getHandlePosition(
                                selection = selection,
                                isStartHandle = true
                            )
                        )
                    )
                }

                val currentEnd = if (isStartHandle) {
                    requireContainerCoordinates().localPositionOf(
                        endSelectable?.getLayoutCoordinates()!!,
                        getAdjustedCoordinates(
                            endSelectable.getHandlePosition(
                                selection = selection,
                                isStartHandle = false
                            )
                        )
                    )
                } else {
                    dragBeginPosition + dragTotalDistance
                }
                updateSelection(
                    startPosition = currentStart,
                    endPosition = currentEnd,
                    isStartHandle = isStartHandle
                )
                return
            }

            override fun onStop() {
                showSelectionToolbar()
            }

            override fun onCancel() {
                showSelectionToolbar()
            }
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

    private fun updateSelection(
        startPosition: Offset?,
        endPosition: Offset?,
        adjustment: SelectionAdjustment = SelectionAdjustment.NONE,
        isStartHandle: Boolean = true
    ) {
        if (startPosition == null || endPosition == null) return
        val (newSelection, newSubselection) = mergeSelections(
            startPosition = startPosition,
            endPosition = endPosition,
            adjustment = adjustment,
            isStartHandle = isStartHandle,
            previousSelection = selection,
        )
        if (newSelection != selection) {
            selectionRegistrar.subselections = newSubselection
            onSelectionChange(newSelection)
        }
    }
}

internal fun merge(lhs: Selection?, rhs: Selection?): Selection? {
    return lhs?.merge(rhs) ?: rhs
}

internal expect fun isCopyKeyEvent(keyEvent: KeyEvent): Boolean

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
