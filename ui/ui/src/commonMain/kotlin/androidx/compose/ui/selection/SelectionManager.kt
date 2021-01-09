/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.selection

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.length
import androidx.compose.ui.text.subSequence
import kotlin.math.max
import kotlin.math.min

/**
 * A bridge class between user interaction to the text composables for text selection.
 */
@OptIn(
    InternalTextApi::class,
    ExperimentalTextApi::class
)
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
     * Layout Coordinates of the selection container.
     */
    var containerLayoutCoordinates: LayoutCoordinates? = null
        set(value) {
            field = value
            updateHandleOffsets()
            updateSelectionToolbarPosition()
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
        selectionRegistrar.onPositionChangeCallback = {
            updateHandleOffsets()
            updateSelectionToolbarPosition()
        }

        selectionRegistrar.onSelectionUpdateStartCallback = { layoutCoordinates, startPosition ->
            updateSelection(
                startPosition = convertToContainerCoordinates(layoutCoordinates, startPosition),
                endPosition = convertToContainerCoordinates(layoutCoordinates, startPosition),
                isStartHandle = true,
                longPress = true
            )
            hideSelectionToolbar()
        }

        selectionRegistrar.onSelectionUpdateCallback =
            { layoutCoordinates, startPosition, endPosition ->
                updateSelection(
                    startPosition = convertToContainerCoordinates(layoutCoordinates, startPosition),
                    endPosition = convertToContainerCoordinates(layoutCoordinates, endPosition),
                    isStartHandle = false,
                    longPress = true
                )
            }

        selectionRegistrar.onSelectionUpdateEndCallback = {
            showSelectionToolbar()
        }

        selectionRegistrar.onSelectableChangeCallback = { selectable ->
            if (selectable in selectionRegistrar.selectables) {
                // clear the selection range of each Selectable.
                onRelease()
                selection = null
            }
        }
    }

    private fun updateHandleOffsets() {
        val selection = selection
        val containerCoordinates = containerLayoutCoordinates
        val startLayoutCoordinates = selection?.start?.selectable?.getLayoutCoordinates()
        val endLayoutCoordinates = selection?.end?.selectable?.getLayoutCoordinates()

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
            selection.start.selectable.getHandlePosition(
                selection = selection,
                isStartHandle = true
            )
        )
        val endHandlePosition = containerCoordinates.localPositionOf(
            endLayoutCoordinates,
            selection.end.selectable.getHandlePosition(
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
     * @param longPress the selection is a result of long press
     * @param previousSelection previous selection
     *
     * @return [Selection] object which is constructed by combining all Composables that are
     * selected.
     */
    // This function is internal for testing purposes.
    internal fun mergeSelections(
        startPosition: Offset,
        endPosition: Offset,
        longPress: Boolean = false,
        previousSelection: Selection? = null,
        isStartHandle: Boolean = true
    ): Selection? {

        val newSelection = selectionRegistrar.sort(requireContainerCoordinates())
            .fold(null) { mergedSelection: Selection?,
                handler: Selectable ->
                merge(
                    mergedSelection,
                    handler.getSelection(
                        startPosition = startPosition,
                        endPosition = endPosition,
                        containerLayoutCoordinates = requireContainerCoordinates(),
                        longPress = longPress,
                        previousSelection = previousSelection,
                        isStartHandle = isStartHandle
                    )
                )
            }
        if (previousSelection != newSelection) hapticFeedBack?.performHapticFeedback(
            HapticFeedbackType.TextHandleMove
        )
        return newSelection
    }

    internal fun getSelectedText(): AnnotatedString? {
        val selectables = selectionRegistrar.sort(requireContainerCoordinates())
        var selectedText: AnnotatedString? = null

        selection?.let {
            for (handler in selectables) {
                // Continue if the current selectable is before the selection starts.
                if (handler != it.start.selectable && handler != it.end.selectable &&
                    selectedText == null
                ) continue

                val currentSelectedText = getCurrentSelectedText(
                    selectable = handler,
                    selection = it
                )
                selectedText = selectedText?.plus(currentSelectedText) ?: currentSelectedText

                // Break if the current selectable is the last selected selectable.
                if (handler == it.end.selectable && !it.handlesCrossed ||
                    handler == it.start.selectable && it.handlesCrossed
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

    internal fun hideSelectionToolbar() {
        if (textToolbar?.status == TextToolbarStatus.Shown) {
            textToolbar?.hide()
        }
    }

    private fun updateSelectionToolbarPosition() {
        if (textToolbar?.status == TextToolbarStatus.Shown) {
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
        val startLayoutCoordinates =
            selection.start.selectable.getLayoutCoordinates() ?: return Rect.Zero
        val endLayoutCoordinates =
            selection.end.selectable.getLayoutCoordinates() ?: return Rect.Zero

        val localLayoutCoordinates = containerLayoutCoordinates
        if (localLayoutCoordinates != null && localLayoutCoordinates.isAttached) {
            var startOffset = localLayoutCoordinates.localPositionOf(
                startLayoutCoordinates,
                selection.start.selectable.getHandlePosition(
                    selection = selection,
                    isStartHandle = true
                )
            )
            var endOffset = localLayoutCoordinates.localPositionOf(
                endLayoutCoordinates,
                selection.end.selectable.getHandlePosition(
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
                    selection.start.selectable.getBoundingBox(selection.start.offset).top
                )
            )

            var endTop = localLayoutCoordinates.localPositionOf(
                endLayoutCoordinates,
                Offset(
                    0.0f,
                    selection.end.selectable.getBoundingBox(selection.end.offset).top
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
        // Call mergeSelections with an out of boundary input to inform all text widgets to
        // cancel their individual selection.
        mergeSelections(
            startPosition = Offset(-1f, -1f),
            endPosition = Offset(-1f, -1f),
            previousSelection = selection
        )
        hideSelectionToolbar()
        if (selection != null) onSelectionChange(null)
    }

    fun handleDragObserver(isStartHandle: Boolean): DragObserver {
        return object : DragObserver {
            override fun onStart(downPosition: Offset) {
                hideSelectionToolbar()
                val selection = selection!!
                // The LayoutCoordinates of the composable where the drag gesture should begin. This
                // is used to convert the position of the beginning of the drag gesture from the
                // composable coordinates to selection container coordinates.
                val beginLayoutCoordinates = if (isStartHandle) {
                    selection.start.selectable.getLayoutCoordinates()!!
                } else {
                    selection.end.selectable.getLayoutCoordinates()!!
                }

                // The position of the character where the drag gesture should begin. This is in
                // the composable coordinates.
                val beginCoordinates = getAdjustedCoordinates(
                    if (isStartHandle) {
                        selection.start.selectable.getHandlePosition(
                            selection = selection, isStartHandle = true
                        )
                    } else {
                        selection.end.selectable.getHandlePosition(
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

            override fun onDrag(dragDistance: Offset): Offset {
                val selection = selection!!
                dragTotalDistance += dragDistance

                val currentStart = if (isStartHandle) {
                    dragBeginPosition + dragTotalDistance
                } else {
                    requireContainerCoordinates().localPositionOf(
                        selection.start.selectable.getLayoutCoordinates()!!,
                        getAdjustedCoordinates(
                            selection.start.selectable.getHandlePosition(
                                selection = selection,
                                isStartHandle = true
                            )
                        )
                    )
                }

                val currentEnd = if (isStartHandle) {
                    requireContainerCoordinates().localPositionOf(
                        selection.end.selectable.getLayoutCoordinates()!!,
                        getAdjustedCoordinates(
                            selection.end.selectable.getHandlePosition(
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
                return dragDistance
            }

            override fun onStop(velocity: Offset) {
                showSelectionToolbar()
            }

            override fun onCancel() {
                showSelectionToolbar()
            }
        }
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
        longPress: Boolean = false,
        isStartHandle: Boolean = true
    ) {
        if (startPosition == null || endPosition == null) return
        val newSelection = mergeSelections(
            startPosition = startPosition,
            endPosition = endPosition,
            longPress = longPress,
            isStartHandle = isStartHandle,
            previousSelection = selection
        )
        if (newSelection != selection) onSelectionChange(newSelection)
    }
}

internal fun merge(lhs: Selection?, rhs: Selection?): Selection? {
    return lhs?.merge(rhs) ?: rhs
}

@OptIn(ExperimentalTextApi::class)
internal fun getCurrentSelectedText(
    selectable: Selectable,
    selection: Selection
): AnnotatedString {
    val currentText = selectable.getText()

    return if (
        selectable != selection.start.selectable &&
        selectable != selection.end.selectable
    ) {
        // Select the full text content if the current selectable is between the
        // start and the end selectables.
        currentText
    } else if (
        selectable == selection.start.selectable &&
        selectable == selection.end.selectable
    ) {
        // Select partial text content if the current selectable is the start and
        // the end selectable.
        if (selection.handlesCrossed) {
            currentText.subSequence(selection.end.offset, selection.start.offset)
        } else {
            currentText.subSequence(selection.start.offset, selection.end.offset)
        }
    } else if (selectable == selection.start.selectable) {
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
private fun LayoutCoordinates.visibleBounds(): Rect {
    // globalBounds is the global boundaries of this LayoutCoordinates after it's clipped by
    // parents. We can think it as the global visible bounds of this Layout. Here globalBounds
    // is convert to local, which is the boundary of the visible area within the LayoutCoordinates.
    val boundsInWindow = boundsInWindow()
    return Rect(
        windowToLocal(boundsInWindow.topLeft),
        windowToLocal(boundsInWindow.bottomRight)
    )
}

private fun Rect.containsInclusive(offset: Offset): Boolean =
    offset.x in left..right && offset.y in top..bottom
