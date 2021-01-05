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

package androidx.compose.foundation.text

import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.gestures.rememberScrollableController
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.savedinstancestate.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.AmbientLayoutDirection
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min
import kotlin.math.roundToInt

internal fun Modifier.textFieldScroll(
    orientation: Orientation,
    scrollerPosition: TextFieldScrollerPosition,
    textFieldValue: TextFieldValue,
    visualTransformation: VisualTransformation,
    interactionState: InteractionState,
    textLayoutResult: Ref<TextLayoutResult?>,
    enabled: Boolean = true
) = composed(
    factory = {
        // do not reverse direction only in case of RTL in horizontal orientation
        val rtl = AmbientLayoutDirection.current == LayoutDirection.Rtl
        val reverseDirection = orientation == Orientation.Vertical || !rtl
        val controller =
            rememberScrollableController(interactionState = interactionState) { delta ->
                val newOffset = scrollerPosition.offset + delta
                val consumedDelta = when {
                    newOffset > scrollerPosition.maximum ->
                        scrollerPosition.maximum - scrollerPosition.offset
                    newOffset < 0f -> -scrollerPosition.offset
                    else -> delta
                }
                scrollerPosition.offset += consumedDelta
                consumedDelta
            }
        val scroll = Modifier.scrollable(
            orientation = orientation,
            canScroll = { scrollerPosition.maximum != 0f },
            reverseDirection = reverseDirection,
            controller = controller,
            enabled = enabled
        )

        val cursorOffset = scrollerPosition.getOffsetToFollow(textFieldValue.selection)
        scrollerPosition.previousSelection = textFieldValue.selection

        val transformedText = visualTransformation.filter(
            AnnotatedString(textFieldValue.text)
        )
        val layout = when (orientation) {
            Orientation.Vertical ->
                VerticalScrollLayoutModifier(
                    scrollerPosition,
                    cursorOffset,
                    transformedText,
                    textLayoutResult
                )
            Orientation.Horizontal ->
                HorizontalScrollLayoutModifier(
                    scrollerPosition,
                    cursorOffset,
                    transformedText,
                    textLayoutResult
                )
        }
        this.then(scroll).clipToBounds().then(layout)
    },
    inspectorInfo = debugInspectorInfo {
        name = "textFieldScroll"
        properties["orientation"] = orientation
        properties["scrollerPosition"] = scrollerPosition
        properties["enabled"] = enabled
        properties["textFieldValue"] = textFieldValue
        properties["visualTransformation"] = visualTransformation
        properties["interactionState"] = interactionState
        properties["textLayoutResult"] = textLayoutResult
    }
)

private data class VerticalScrollLayoutModifier(
    val scrollerPosition: TextFieldScrollerPosition,
    val cursorOffset: Int,
    val transformedText: TransformedText,
    val textLayoutResult: Ref<TextLayoutResult?>
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val childConstraints = constraints.copy(maxHeight = Constraints.Infinity)
        val placeable = measurable.measure(childConstraints)
        val height = min(placeable.height, constraints.maxHeight)

        return layout(placeable.width, height) {
            val cursorRect = getCursorRectInScroller(
                cursorOffset = cursorOffset,
                transformedText = transformedText,
                textLayoutResult = textLayoutResult.value,
                rtl = false,
                textFieldWidth = placeable.width
            )

            scrollerPosition.update(
                orientation = Orientation.Vertical,
                cursorRect = cursorRect,
                containerSize = height,
                textFieldSize = placeable.height
            )

            val offset = -scrollerPosition.offset
            placeable.placeRelative(0, offset.roundToInt())
        }
    }
}

private data class HorizontalScrollLayoutModifier(
    val scrollerPosition: TextFieldScrollerPosition,
    val cursorOffset: Int,
    val transformedText: TransformedText,
    val textLayoutResult: Ref<TextLayoutResult?>
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val childConstraints = constraints.copy(maxWidth = Constraints.Infinity)
        val placeable = measurable.measure(childConstraints)
        val width = min(placeable.width, constraints.maxWidth)

        return layout(width, placeable.height) {
            val cursorRect = getCursorRectInScroller(
                cursorOffset = cursorOffset,
                transformedText = transformedText,
                textLayoutResult = textLayoutResult.value,
                rtl = layoutDirection == LayoutDirection.Rtl,
                textFieldWidth = placeable.width
            )

            scrollerPosition.update(
                orientation = Orientation.Horizontal,
                cursorRect = cursorRect,
                containerSize = width,
                textFieldSize = placeable.width
            )

            val offset = -scrollerPosition.offset
            placeable.placeRelative(offset.roundToInt(), 0)
        }
    }
}

private fun Density.getCursorRectInScroller(
    cursorOffset: Int,
    transformedText: TransformedText,
    textLayoutResult: TextLayoutResult?,
    rtl: Boolean,
    textFieldWidth: Int
): Rect {
    val cursorRect = textLayoutResult?.getCursorRect(
        transformedText.offsetMap.originalToTransformed(cursorOffset)
    ) ?: Rect.Zero
    val thickness = DefaultCursorThickness.toIntPx()

    val cursorLeft = if (rtl) {
        textFieldWidth - cursorRect.left - thickness
    } else {
        cursorRect.left
    }

    val cursorRight = if (rtl) {
        textFieldWidth - cursorRect.left
    } else {
        cursorRect.left + thickness
    }
    return cursorRect.copy(left = cursorLeft, right = cursorRight)
}

@Stable
internal class TextFieldScrollerPosition(initial: Float = 0f) {
    /**
     * Left or top offset. Takes values from 0 to [maximum].
     * Taken with the opposite sign defines the x or y position of the text field in the
     * horizontal or vertical scroller container correspondingly.
     */
    var offset by mutableStateOf(initial, structuralEqualityPolicy())

    /**
     * Maximum length by which the text field can be scrolled. Defined as a difference in
     * size between the scroller container and the text field.
     */
    var maximum by mutableStateOf(Float.POSITIVE_INFINITY, structuralEqualityPolicy())
        private set

    /**
     * Keeps the cursor position before a new symbol has been typed or the text field has been
     * dragged. We check it to understand if the [offset] needs to be updated.
     */
    private var previousCursorRect: Rect = Rect.Zero

    /**
     * Keeps the previous selection data in TextFieldValue in order to identify what has changed
     * in the new selection, and decide which selection offset (start, end) to follow.
     */
    var previousSelection: TextRange = TextRange.Zero

    fun update(
        orientation: Orientation,
        cursorRect: Rect,
        containerSize: Int,
        textFieldSize: Int
    ) {
        val difference = (textFieldSize - containerSize).toFloat()
        maximum = difference

        if (cursorRect.left != previousCursorRect.left ||
            cursorRect.top != previousCursorRect.top
        ) {
            val vertical = orientation == Orientation.Vertical
            val cursorStart = if (vertical) cursorRect.top else cursorRect.left
            val cursorEnd = if (vertical) cursorRect.bottom else cursorRect.right
            coerceOffset(cursorStart, cursorEnd, containerSize)
            previousCursorRect = cursorRect
        }
        offset = offset.coerceIn(0f, difference)
    }

    private fun coerceOffset(cursorStart: Float, cursorEnd: Float, containerSize: Int) {
        val startVisibleBound = offset
        val endVisibleBound = startVisibleBound + containerSize
        if (cursorStart < startVisibleBound) {
            offset -= startVisibleBound - cursorStart
        } else if (cursorEnd > endVisibleBound) {
            offset += cursorEnd - endVisibleBound
        }
    }

    fun getOffsetToFollow(selection: TextRange): Int {
        return when {
            selection.start != previousSelection.start -> selection.start
            selection.end != previousSelection.end -> selection.end
            else -> selection.min
        }
    }

    companion object {
        val Saver = Saver<TextFieldScrollerPosition, Float>(
            save = { it.offset },
            restore = { restored ->
                TextFieldScrollerPosition(
                    initial = restored
                )
            }
        )
    }
}