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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.DefaultCursorThickness
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.HandleState
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.TextFieldState
import androidx.compose.foundation.text.UndoManager
import androidx.compose.foundation.text.detectDownAndDragGesturesWithObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.input.getTextAfterSelection
import androidx.compose.ui.text.input.getTextBeforeSelection
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * A bridge class between user interaction to the text field selection.
 */
internal class TextFieldSelectionManager(
    val undoManager: UndoManager? = null
) {

    /**
     * The current [OffsetMapping] for text field.
     */
    internal var offsetMapping: OffsetMapping = OffsetMapping.Identity

    /**
     * Called when the input service updates the values in [TextFieldValue].
     */
    internal var onValueChange: (TextFieldValue) -> Unit = {}

    /**
     * The current [TextFieldState].
     */
    internal var state: TextFieldState? = null

    /**
     * The current [TextFieldValue].
     */
    internal var value: TextFieldValue by mutableStateOf(TextFieldValue())

    /**
     * Visual transformation of the text field's text. Used to check if certain toolbar options
     * are permitted. For example, 'cut' will not be available is it is password transformation.
     */
    internal var visualTransformation: VisualTransformation = VisualTransformation.None

    /**
     * [ClipboardManager] to perform clipboard features.
     */
    internal var clipboardManager: ClipboardManager? = null

    /**
     * [TextToolbar] to show floating toolbar(post-M) or primary toolbar(pre-M).
     */
    var textToolbar: TextToolbar? = null

    /**
     * [HapticFeedback] handle to perform haptic feedback.
     */
    var hapticFeedBack: HapticFeedback? = null

    /**
     * [FocusRequester] used to request focus for the TextField.
     */
    var focusRequester: FocusRequester? = null

    /**
     * Defines if paste and cut toolbar menu actions should be shown
     */
    var editable by mutableStateOf(true)

    /**
     * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
     * recalculated.
     */
    private var dragBeginPosition = Offset.Zero

    /**
     * The beginning offset of the drag gesture translated into position in text. Every time a
     * new drag gesture starts, it wil be recalculated.
     * Unlike [dragBeginPosition] that is relative to the decoration box,
     * [dragBeginOffsetInText] represents index in text. Essentially, it is equal to
     * `layoutResult.getOffsetForPosition(dragBeginPosition)`.
     */
    private var dragBeginOffsetInText: Int? = null

    /**
     * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
     * it will be zeroed out.
     */
    private var dragTotalDistance = Offset.Zero

    /**
     * A flag to check if a selection or cursor handle is being dragged, and which handle is being
     * dragged.
     * If this value is non-null, then onPress will not select any text.
     * This value will be set to non-null when either handle is being dragged, and be reset to null
     * when the dragging is stopped.
     */
    var draggingHandle: Handle? by mutableStateOf(null)
        private set

    var currentDragPosition: Offset? by mutableStateOf(null)
        private set

    /**
     * The old [TextFieldValue] before entering the selection mode on long press. Used to exit
     * the selection mode.
     */
    private var oldValue: TextFieldValue = TextFieldValue()

    /**
     * [TextDragObserver] for long press and drag to select in TextField.
     */
    internal val touchSelectionObserver = object : TextDragObserver {
        override fun onDown(point: Offset) {
            // Not supported for long-press-drag.
        }

        override fun onUp() {
            // Nothing to do.
        }

        override fun onStart(startPoint: Offset) {
            if (draggingHandle != null) return
            // While selecting by long-press-dragging, the "end" of the selection is always the one
            // being controlled by the drag.
            draggingHandle = Handle.SelectionEnd

            // ensuring that current action mode (selection toolbar) is invalidated
            hideSelectionToolbar()

            // Long Press at the blank area, the cursor should show up at the end of the line.
            if (state?.layoutResult?.isPositionOnText(startPoint) != true) {
                state?.layoutResult?.let { layoutResult ->
                    val offset = offsetMapping.transformedToOriginal(
                        layoutResult.getLineEnd(
                            layoutResult.getLineForVerticalPosition(startPoint.y)
                        )
                    )
                    hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    val newValue = createTextFieldValue(
                        annotatedString = value.annotatedString,
                        selection = TextRange(offset, offset)
                    )
                    enterSelectionMode()
                    onValueChange(newValue)
                    return
                }
            }

            // selection never started
            if (value.text.isEmpty()) return
            enterSelectionMode()
            state?.layoutResult?.let { layoutResult ->
                val offset = layoutResult.getOffsetForPosition(startPoint)
                updateSelection(
                    value = value,
                    transformedStartOffset = offset,
                    transformedEndOffset = offset,
                    isStartHandle = false,
                    adjustment = SelectionAdjustment.Word
                )
                dragBeginOffsetInText = offset
            }
            dragBeginPosition = startPoint
            currentDragPosition = dragBeginPosition
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(delta: Offset) {
            // selection never started, did not consume any drag
            if (value.text.isEmpty()) return

            dragTotalDistance += delta
            state?.layoutResult?.let { layoutResult ->
                currentDragPosition = dragBeginPosition + dragTotalDistance
                val startOffset = dragBeginOffsetInText ?: layoutResult.getOffsetForPosition(
                    position = dragBeginPosition,
                    coerceInVisibleBounds = false
                )
                val endOffset = layoutResult.getOffsetForPosition(
                    position = currentDragPosition!!,
                    coerceInVisibleBounds = false
                )
                updateSelection(
                    value = value,
                    transformedStartOffset = startOffset,
                    transformedEndOffset = endOffset,
                    isStartHandle = false,
                    adjustment = SelectionAdjustment.Word
                )
            }
            state?.showFloatingToolbar = false
        }

        override fun onStop() {
            draggingHandle = null
            currentDragPosition = null
            state?.showFloatingToolbar = true
            if (textToolbar?.status == TextToolbarStatus.Hidden) showSelectionToolbar()
            dragBeginOffsetInText = null
        }

        override fun onCancel() {}
    }

    internal val mouseSelectionObserver = object : MouseSelectionObserver {
        override fun onExtend(downPosition: Offset): Boolean {
            state?.layoutResult?.let { layoutResult ->
                val startOffset = offsetMapping.originalToTransformed(value.selection.start)
                val clickOffset = layoutResult.getOffsetForPosition(downPosition)
                updateSelection(
                    value = value,
                    transformedStartOffset = startOffset,
                    transformedEndOffset = clickOffset,
                    isStartHandle = false,
                    adjustment = SelectionAdjustment.None
                )
                return true
            }
            return false
        }

        override fun onExtendDrag(dragPosition: Offset): Boolean {
            if (value.text.isEmpty()) return false

            state?.layoutResult?.let { layoutResult ->
                val startOffset = offsetMapping.originalToTransformed(value.selection.start)
                val dragOffset =
                    layoutResult.getOffsetForPosition(
                        position = dragPosition,
                        coerceInVisibleBounds = false
                    )

                updateSelection(
                    value = value,
                    transformedStartOffset = startOffset,
                    transformedEndOffset = dragOffset,
                    isStartHandle = false,
                    adjustment = SelectionAdjustment.None
                )
                return true
            }
            return false
        }

        override fun onStart(
            downPosition: Offset,
            adjustment: SelectionAdjustment
        ): Boolean {
            focusRequester?.requestFocus()

            dragBeginPosition = downPosition

            state?.layoutResult?.let { layoutResult ->
                dragBeginOffsetInText = layoutResult.getOffsetForPosition(downPosition)
                val clickOffset = layoutResult.getOffsetForPosition(dragBeginPosition)
                updateSelection(
                    value = value,
                    transformedStartOffset = clickOffset,
                    transformedEndOffset = clickOffset,
                    isStartHandle = false,
                    adjustment = adjustment
                )
                return true
            }
            return false
        }

        override fun onDrag(dragPosition: Offset, adjustment: SelectionAdjustment): Boolean {
            if (value.text.isEmpty()) return false

            state?.layoutResult?.let { layoutResult ->
                val dragOffset =
                    layoutResult.getOffsetForPosition(
                        position = dragPosition,
                        coerceInVisibleBounds = false
                    )

                updateSelection(
                    value = value,
                    transformedStartOffset = dragBeginOffsetInText!!,
                    transformedEndOffset = dragOffset,
                    isStartHandle = false,
                    adjustment = adjustment
                )
                return true
            }
            return false
        }
    }

    /**
     * [TextDragObserver] for dragging the selection handles to change the selection in TextField.
     */
    internal fun handleDragObserver(isStartHandle: Boolean): TextDragObserver =
        object : TextDragObserver {
            override fun onDown(point: Offset) {
                draggingHandle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd
                currentDragPosition = getAdjustedCoordinates(getHandlePosition(isStartHandle))
            }

            override fun onUp() {
                draggingHandle = null
                currentDragPosition = null
            }

            override fun onStart(startPoint: Offset) {
                // The position of the character where the drag gesture should begin. This is in
                // the composable coordinates.
                dragBeginPosition = getAdjustedCoordinates(getHandlePosition(isStartHandle))
                currentDragPosition = dragBeginPosition
                // Zero out the total distance that being dragged.
                dragTotalDistance = Offset.Zero
                draggingHandle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd
                state?.showFloatingToolbar = false
            }

            override fun onDrag(delta: Offset) {
                dragTotalDistance += delta

                state?.layoutResult?.value?.let { layoutResult ->
                    currentDragPosition = dragBeginPosition + dragTotalDistance
                    val startOffset = if (isStartHandle)
                        layoutResult.getOffsetForPosition(currentDragPosition!!)
                    else
                        offsetMapping.originalToTransformed(value.selection.start)

                    val endOffset = if (isStartHandle)
                        offsetMapping.originalToTransformed(value.selection.end)
                    else
                        layoutResult.getOffsetForPosition(currentDragPosition!!)

                    updateSelection(
                        value = value,
                        transformedStartOffset = startOffset,
                        transformedEndOffset = endOffset,
                        isStartHandle = isStartHandle,
                        adjustment = SelectionAdjustment.Character
                    )
                }
                state?.showFloatingToolbar = false
            }

            override fun onStop() {
                draggingHandle = null
                currentDragPosition = null
                state?.showFloatingToolbar = true
                if (textToolbar?.status == TextToolbarStatus.Hidden) showSelectionToolbar()
            }

            override fun onCancel() {}
        }

    /**
     * [TextDragObserver] for dragging the cursor to change the selection in TextField.
     */
    internal fun cursorDragObserver(): TextDragObserver = object : TextDragObserver {
        override fun onDown(point: Offset) {
            draggingHandle = Handle.Cursor
            currentDragPosition = getAdjustedCoordinates(getHandlePosition(true))
        }

        override fun onUp() {
            draggingHandle = null
            currentDragPosition = null
        }

        override fun onStart(startPoint: Offset) {
            // The position of the character where the drag gesture should begin. This is in
            // the composable coordinates.
            dragBeginPosition = getAdjustedCoordinates(getHandlePosition(true))
            currentDragPosition = dragBeginPosition
            // Zero out the total distance that being dragged.
            dragTotalDistance = Offset.Zero
            draggingHandle = Handle.Cursor
        }

        override fun onDrag(delta: Offset) {
            dragTotalDistance += delta

            state?.layoutResult?.value?.let { layoutResult ->
                currentDragPosition = dragBeginPosition + dragTotalDistance
                val offset = layoutResult.getOffsetForPosition(currentDragPosition!!)

                val newSelection = TextRange(offset, offset)

                // Nothing changed, skip onValueChange hand hapticFeedback.
                if (newSelection == value.selection) return

                hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(
                    createTextFieldValue(
                        annotatedString = value.annotatedString,
                        selection = newSelection
                    )
                )
            }
        }

        override fun onStop() {
            draggingHandle = null
            currentDragPosition = null
        }

        override fun onCancel() {}
    }

    /**
     * The method to record the required state values on entering the selection mode.
     *
     * Is triggered on long press or accessibility action.
     */
    internal fun enterSelectionMode() {
        if (state?.hasFocus == false) {
            focusRequester?.requestFocus()
        }
        oldValue = value
        state?.showFloatingToolbar = true
        setHandleState(HandleState.Selection)
    }

    /**
     * The method to record the corresponding state values on exiting the selection mode.
     *
     * Is triggered on accessibility action.
     */
    internal fun exitSelectionMode() {
        state?.showFloatingToolbar = false
        setHandleState(HandleState.None)
    }

    internal fun deselect(position: Offset? = null) {
        if (!value.selection.collapsed) {
            // if selection was not collapsed, set a default cursor location, otherwise
            // don't change the location of the cursor.
            val layoutResult = state?.layoutResult
            val newCursorOffset = if (position != null && layoutResult != null) {
                offsetMapping.transformedToOriginal(
                    layoutResult.getOffsetForPosition(position)
                )
            } else {
                value.selection.max
            }
            val newValue = value.copy(selection = TextRange(newCursorOffset))
            onValueChange(newValue)
        }

        // If a new cursor position is given and the text is not empty, enter the
        // HandleState.Cursor state.
        val selectionMode = if (position != null && value.text.isNotEmpty()) {
            HandleState.Cursor
        } else {
            HandleState.None
        }
        setHandleState(selectionMode)
        hideSelectionToolbar()
    }

    /**
     * The method for copying text.
     *
     * If there is no selection, return.
     * Put the selected text into the [ClipboardManager], and cancel the selection, if
     * [cancelSelection] is true.
     * The text in the text field should be unchanged.
     * If [cancelSelection] is true, the new cursor offset should be at the end of the previous
     * selected text.
     */
    internal fun copy(cancelSelection: Boolean = true) {
        if (value.selection.collapsed) return

        // TODO(b/171947959) check if original or transformed should be copied
        clipboardManager?.setText(value.getSelectedText())

        if (!cancelSelection) return

        val newCursorOffset = value.selection.max
        val newValue = createTextFieldValue(
            annotatedString = value.annotatedString,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        setHandleState(HandleState.None)
    }

    /**
     * The method for pasting text.
     *
     * Get the text from [ClipboardManager]. If it's null, return.
     * The new text should be the text before the selected text, plus the text from the
     * [ClipboardManager], and plus the text after the selected text.
     * Then the selection should collapse, and the new cursor offset should be the end of the
     * newly added text.
     */
    internal fun paste() {
        val text = clipboardManager?.getText() ?: return

        val newText = value.getTextBeforeSelection(value.text.length) +
            text +
            value.getTextAfterSelection(value.text.length)
        val newCursorOffset = value.selection.min + text.length

        val newValue = createTextFieldValue(
            annotatedString = newText,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        setHandleState(HandleState.None)
        undoManager?.forceNextSnapshot()
    }

    /**
     * The method for cutting text.
     *
     * If there is no selection, return.
     * Put the selected text into the [ClipboardManager].
     * The new text should be the text before the selection plus the text after the selection.
     * And the new cursor offset should be between the text before the selection, and the text
     * after the selection.
     */
    internal fun cut() {
        if (value.selection.collapsed) return

        // TODO(b/171947959) check if original or transformed should be cut
        clipboardManager?.setText(value.getSelectedText())

        val newText = value.getTextBeforeSelection(value.text.length) +
            value.getTextAfterSelection(value.text.length)
        val newCursorOffset = value.selection.min

        val newValue = createTextFieldValue(
            annotatedString = newText,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        setHandleState(HandleState.None)
        undoManager?.forceNextSnapshot()
    }

    /*@VisibleForTesting*/
    internal fun selectAll() {
        val newValue = createTextFieldValue(
            annotatedString = value.annotatedString,
            selection = TextRange(0, value.text.length)
        )
        onValueChange(newValue)
        oldValue = oldValue.copy(selection = newValue.selection)
        state?.showFloatingToolbar = true
    }

    internal fun getHandlePosition(isStartHandle: Boolean): Offset {
        val offset = if (isStartHandle) value.selection.start else value.selection.end
        return getSelectionHandleCoordinates(
            textLayoutResult = state?.layoutResult!!.value,
            offset = offsetMapping.originalToTransformed(offset),
            isStart = isStartHandle,
            areHandlesCrossed = value.selection.reversed
        )
    }

    internal fun getCursorPosition(density: Density): Offset {
        val offset = offsetMapping.originalToTransformed(value.selection.start)
        val layoutResult = state?.layoutResult!!.value
        val cursorRect = layoutResult.getCursorRect(
            offset.coerceIn(0, layoutResult.layoutInput.text.length)
        )
        val x = with(density) {
            cursorRect.left + DefaultCursorThickness.toPx() / 2
        }
        return Offset(x, cursorRect.bottom)
    }

    /**
     * This function get the selected region as a Rectangle region, and pass it to [TextToolbar]
     * to make the FloatingToolbar show up in the proper place. In addition, this function passes
     * the copy, paste and cut method as callbacks when "copy", "cut" or "paste" is clicked.
     */
    internal fun showSelectionToolbar() {
        val isPassword = visualTransformation is PasswordVisualTransformation
        val copy: (() -> Unit)? = if (!value.selection.collapsed && !isPassword) {
            {
                copy()
                hideSelectionToolbar()
            }
        } else null

        val cut: (() -> Unit)? = if (!value.selection.collapsed && editable && !isPassword) {
            {
                cut()
                hideSelectionToolbar()
            }
        } else null

        val paste: (() -> Unit)? = if (editable && clipboardManager?.getText() != null) {
            {
                paste()
                hideSelectionToolbar()
            }
        } else null

        val selectAll: (() -> Unit)? = if (value.selection.length != value.text.length) {
            {
                selectAll()
            }
        } else null

        textToolbar?.showMenu(
            rect = getContentRect(),
            onCopyRequested = copy,
            onPasteRequested = paste,
            onCutRequested = cut,
            onSelectAllRequested = selectAll
        )
    }

    internal fun hideSelectionToolbar() {
        if (textToolbar?.status == TextToolbarStatus.Shown) {
            textToolbar?.hide()
        }
    }

    fun contextMenuOpenAdjustment(position: Offset) {
        state?.layoutResult?.let { layoutResult ->
            val offset = layoutResult.getOffsetForPosition(position)
            if (!value.selection.contains(offset)) {
                updateSelection(
                    value = value,
                    transformedStartOffset = offset,
                    transformedEndOffset = offset,
                    isStartHandle = false,
                    adjustment = SelectionAdjustment.Word
                )
            }
        }
    }

    /**
     * Check if the text in the text field changed.
     * When the content in the text field is modified, this method returns true.
     */
    internal fun isTextChanged(): Boolean {
        return oldValue.text != value.text
    }

    /**
     * Calculate selected region as [Rect]. The top is the top of the first selected
     * line, and the bottom is the bottom of the last selected line. The left is the leftmost
     * handle's horizontal coordinates, and the right is the rightmost handle's coordinates.
     */
    @OptIn(InternalFoundationTextApi::class)
    private fun getContentRect(): Rect {
        state?.let {
            val startOffset =
                state?.layoutCoordinates?.localToRoot(getHandlePosition(true)) ?: Offset.Zero
            val endOffset =
                state?.layoutCoordinates?.localToRoot(getHandlePosition(false)) ?: Offset.Zero
            val startTop =
                state?.layoutCoordinates?.localToRoot(
                    Offset(
                        0f,
                        it.layoutResult?.value?.getCursorRect(
                            value.selection.start.coerceIn(
                                0,
                                max(0, value.text.length - 1)
                            )
                        )?.top ?: 0f
                    )
                )?.y ?: 0f
            val endTop =
                state?.layoutCoordinates?.localToRoot(
                    Offset(
                        0f,
                        it.layoutResult?.value?.getCursorRect(
                            value.selection.end.coerceIn(
                                0,
                                max(0, value.text.length - 1)
                            )
                        )?.top ?: 0f
                    )
                )?.y ?: 0f

            val left = min(startOffset.x, endOffset.x)
            val right = max(startOffset.x, endOffset.x)
            val top = min(startTop, endTop)
            val bottom = max(startOffset.y, endOffset.y) +
                25.dp.value * it.textDelegate.density.density

            return Rect(left, top, right, bottom)
        }

        return Rect.Zero
    }

    private fun updateSelection(
        value: TextFieldValue,
        transformedStartOffset: Int,
        transformedEndOffset: Int,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment
    ) {
        val transformedSelection = TextRange(
            offsetMapping.originalToTransformed(value.selection.start),
            offsetMapping.originalToTransformed(value.selection.end)
        )

        val newTransformedSelection = getTextFieldSelection(
            textLayoutResult = state?.layoutResult?.value,
            rawStartOffset = transformedStartOffset,
            rawEndOffset = transformedEndOffset,
            previousSelection = if (transformedSelection.collapsed) null else transformedSelection,
            isStartHandle = isStartHandle,
            adjustment = adjustment
        )

        val originalSelection = TextRange(
            start = offsetMapping.transformedToOriginal(newTransformedSelection.start),
            end = offsetMapping.transformedToOriginal(newTransformedSelection.end)
        )

        if (originalSelection == value.selection) return

        hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        val newValue = createTextFieldValue(
            annotatedString = value.annotatedString,
            selection = originalSelection
        )
        onValueChange(newValue)

        // showSelectionHandleStart/End might be set to false when scrolled out of the view.
        // When the selection is updated, they must also be updated so that handles will be shown
        // or hidden correctly.
        state?.showSelectionHandleStart = isSelectionHandleInVisibleBound(true)
        state?.showSelectionHandleEnd = isSelectionHandleInVisibleBound(false)
    }

    private fun setHandleState(handleState: HandleState) {
        state?.let { it.handleState = handleState }
    }

    private fun createTextFieldValue(
        annotatedString: AnnotatedString,
        selection: TextRange
    ): TextFieldValue {
        return TextFieldValue(
            annotatedString = annotatedString,
            selection = selection
        )
    }
}

@Composable
internal fun TextFieldSelectionHandle(
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    manager: TextFieldSelectionManager
) {
    val observer = remember(isStartHandle, manager) {
        manager.handleDragObserver(isStartHandle)
    }
    val position = manager.getHandlePosition(isStartHandle)

    SelectionHandle(
        position = position,
        isStartHandle = isStartHandle,
        direction = direction,
        handlesCrossed = manager.value.selection.reversed,
        modifier = Modifier.pointerInput(observer) {
            detectDownAndDragGesturesWithObserver(observer)
        },
        content = null
    )
}

/**
 * Whether the selection handle is in the visible bound of the TextField.
 */
internal fun TextFieldSelectionManager.isSelectionHandleInVisibleBound(
    isStartHandle: Boolean
): Boolean = state?.layoutCoordinates?.visibleBounds()?.containsInclusive(
    getHandlePosition(isStartHandle)
) ?: false

// TODO(b/180075467) it should be part of PointerEvent API in one way or another
internal expect val PointerEvent.isShiftPressed: Boolean

/**
 * Optionally shows a magnifier widget, if the current platform supports it, for the current state
 * of a [TextFieldSelectionManager]. Should check [TextFieldSelectionManager.draggingHandle] to see
 * which handle is being dragged and then calculate the magnifier position for that handle.
 *
 * Actual implementations should as much as possible actually live in this common source set, _not_
 * the platform-specific source sets. The actual implementations of this function should then just
 * delegate to those functions.
 */
internal expect fun Modifier.textFieldMagnifier(manager: TextFieldSelectionManager): Modifier

internal fun calculateSelectionMagnifierCenterAndroid(
    manager: TextFieldSelectionManager,
    magnifierSize: IntSize
): Offset {
    // Never show the magnifier in an empty text field.
    if (manager.value.text.isEmpty()) return Offset.Unspecified
    val rawTextOffset = when (manager.draggingHandle) {
        null -> return Offset.Unspecified
        Handle.Cursor,
        Handle.SelectionStart -> manager.value.selection.start
        Handle.SelectionEnd -> manager.value.selection.end
    }
    val textOffset = manager.offsetMapping.originalToTransformed(rawTextOffset)
        .coerceIn(manager.value.text.indices)
    val layoutResult = manager.state?.layoutResult?.value ?: return Offset.Unspecified
    // Center vertically on the current line.
    // If the text hasn't been laid out yet, don't show the modifier.
    val offsetCenter = layoutResult.getBoundingBox(textOffset).center

    val containerCoordinates = manager.state?.layoutCoordinates ?: return Offset.Unspecified
    val fieldCoordinates =
        manager.state?.layoutResult?.innerTextFieldCoordinates ?: return Offset.Unspecified
    val localDragPosition = manager.currentDragPosition?.let {
        fieldCoordinates.localPositionOf(containerCoordinates, it)
    } ?: return Offset.Unspecified
    val dragX = localDragPosition.x
    val line = layoutResult.getLineForOffset(textOffset)
    val lineStartOffset = layoutResult.getLineStart(line)
    val lineEndOffset = layoutResult.getLineEnd(line, visibleEnd = true)
    val areHandlesCrossed = manager.value.selection.start > manager.value.selection.end
    val lineStart = layoutResult.getHorizontalPosition(
        lineStartOffset,
        isStart = true,
        areHandlesCrossed = areHandlesCrossed
    )
    val lineEnd = layoutResult.getHorizontalPosition(
        lineEndOffset,
        isStart = false,
        areHandlesCrossed = areHandlesCrossed
    )
    val lineMin = minOf(lineStart, lineEnd)
    val lineMax = maxOf(lineStart, lineEnd)
    val centerX = dragX.coerceIn(lineMin, lineMax)

    // Hide the magnifier when dragged too far (outside the horizontal bounds of how big the
    // magnifier actually is). See
    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/Editor.java;l=5228-5231;drc=2fdb6bd709be078b72f011334362456bb758922c
    if ((dragX - centerX).absoluteValue > magnifierSize.width / 2) {
        return Offset.Unspecified
    }

    return containerCoordinates.localPositionOf(
        fieldCoordinates,
        Offset(centerX, offsetCenter.y)
    )
}