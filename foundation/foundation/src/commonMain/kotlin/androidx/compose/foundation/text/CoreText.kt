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
@file:Suppress("DEPRECATION_ERROR", "DEPRECATION")

package androidx.compose.foundation.text

import androidx.compose.foundation.fastMapIndexedNotNull
import androidx.compose.foundation.text.selection.MouseSelectionObserver
import androidx.compose.foundation.text.selection.MultiWidgetSelectionDelegate
import androidx.compose.foundation.text.selection.Selectable
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.foundation.text.selection.hasSelection
import androidx.compose.foundation.text.selection.mouseSelectionDetector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.math.floor
import kotlin.math.roundToInt

private typealias PlaceholderRange = AnnotatedString.Range<Placeholder>
private typealias InlineContentRange = AnnotatedString.Range<@Composable (String) -> Unit>

@Composable
internal fun InlineChildren(
    text: AnnotatedString,
    inlineContents: List<InlineContentRange>
) {
    inlineContents.fastForEach { (content, start, end) ->
        Layout(
            content = { content(text.subSequence(start, end).text) }
        ) { children, constrains ->
            val placeables = children.fastMap { it.measure(constrains) }
            layout(width = constrains.maxWidth, height = constrains.maxHeight) {
                placeables.fastForEach { it.placeRelative(0, 0) }
            }
        }
    }
}

// NOTE(text-perf-review): consider merging this with TextDelegate?
@OptIn(InternalFoundationTextApi::class)
/*@VisibleForTesting*/
internal class TextController(val state: TextState) : RememberObserver {
    private var selectionRegistrar: SelectionRegistrar? = null
    lateinit var longPressDragObserver: TextDragObserver

    fun update(selectionRegistrar: SelectionRegistrar?) {
        this.selectionRegistrar = selectionRegistrar
        selectionModifiers = if (selectionRegistrar != null) {
            if (isInTouchMode) {
                longPressDragObserver = object : TextDragObserver {
                    /**
                     * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
                     * recalculated.
                     */
                    var lastPosition = Offset.Zero

                    /**
                     * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
                     * it will be zeroed out.
                     */
                    var dragTotalDistance = Offset.Zero

                    override fun onDown(point: Offset) {
                        // Not supported for long-press-drag.
                    }

                    override fun onUp() {
                        // Nothing to do.
                    }

                    override fun onStart(startPoint: Offset) {
                        state.layoutCoordinates?.let {
                            if (!it.isAttached) return

                            if (outOfBoundary(startPoint, startPoint)) {
                                selectionRegistrar.notifySelectionUpdateSelectAll(
                                    selectableId = state.selectableId
                                )
                            } else {
                                selectionRegistrar.notifySelectionUpdateStart(
                                    layoutCoordinates = it,
                                    startPosition = startPoint,
                                    adjustment = SelectionAdjustment.Word
                                )
                            }

                            lastPosition = startPoint
                        }
                        // selection never started
                        if (!selectionRegistrar.hasSelection(state.selectableId)) return
                        // Zero out the total distance that being dragged.
                        dragTotalDistance = Offset.Zero
                    }

                    override fun onDrag(delta: Offset) {
                        state.layoutCoordinates?.let {
                            if (!it.isAttached) return
                            // selection never started, did not consume any drag
                            if (!selectionRegistrar.hasSelection(state.selectableId)) return

                            dragTotalDistance += delta
                            val newPosition = lastPosition + dragTotalDistance

                            if (!outOfBoundary(lastPosition, newPosition)) {
                                // Notice that only the end position needs to be updated here.
                                // Start position is left unchanged. This is typically important when
                                // long-press is using SelectionAdjustment.WORD or
                                // SelectionAdjustment.PARAGRAPH that updates the start handle position from
                                // the dragBeginPosition.
                                val consumed = selectionRegistrar.notifySelectionUpdate(
                                    layoutCoordinates = it,
                                    previousPosition = lastPosition,
                                    newPosition = newPosition,
                                    isStartHandle = false,
                                    adjustment = SelectionAdjustment.CharacterWithWordAccelerate
                                )
                                if (consumed == true) {
                                    lastPosition = newPosition
                                    dragTotalDistance = Offset.Zero
                                }
                            }
                        }
                    }

                    override fun onStop() {
                        if (selectionRegistrar.hasSelection(state.selectableId)) {
                            selectionRegistrar.notifySelectionUpdateEnd()
                        }
                    }

                    override fun onCancel() {
                        if (selectionRegistrar.hasSelection(state.selectableId)) {
                            selectionRegistrar.notifySelectionUpdateEnd()
                        }
                    }
                }
                Modifier.pointerInput(longPressDragObserver) {
                    detectDragGesturesAfterLongPressWithObserver(
                        longPressDragObserver
                    )
                }
            } else {
                val mouseSelectionObserver = object : MouseSelectionObserver {
                    var lastPosition = Offset.Zero

                    override fun onExtend(downPosition: Offset): Boolean {
                        state.layoutCoordinates?.let { layoutCoordinates ->
                            if (!layoutCoordinates.isAttached) return false
                            selectionRegistrar.let {
                                val consumed = it.notifySelectionUpdate(
                                    layoutCoordinates = layoutCoordinates,
                                    newPosition = downPosition,
                                    previousPosition = lastPosition,
                                    isStartHandle = false,
                                    adjustment = SelectionAdjustment.None
                                )
                                if (consumed) {
                                    lastPosition = downPosition
                                }
                            }
                            return selectionRegistrar.hasSelection(state.selectableId)
                        }
                        return false
                    }

                    override fun onExtendDrag(dragPosition: Offset): Boolean {
                        state.layoutCoordinates?.let { layoutCoordinates ->
                            if (!layoutCoordinates.isAttached) return false
                            if (!selectionRegistrar.hasSelection(state.selectableId)) return false

                            val consumed = selectionRegistrar.notifySelectionUpdate(
                                layoutCoordinates = layoutCoordinates,
                                newPosition = dragPosition,
                                previousPosition = lastPosition,
                                isStartHandle = false,
                                adjustment = SelectionAdjustment.None
                            )

                            if (consumed) {
                                lastPosition = dragPosition
                            }
                        }
                        return true
                    }

                    override fun onStart(
                        downPosition: Offset,
                        adjustment: SelectionAdjustment
                    ): Boolean {
                        state.layoutCoordinates?.let {
                            if (!it.isAttached) return false

                            selectionRegistrar.notifySelectionUpdateStart(
                                layoutCoordinates = it,
                                startPosition = downPosition,
                                adjustment = adjustment
                            )

                            lastPosition = downPosition
                            return selectionRegistrar.hasSelection(state.selectableId)
                        }

                        return false
                    }

                    override fun onDrag(
                        dragPosition: Offset,
                        adjustment: SelectionAdjustment
                    ): Boolean {
                        state.layoutCoordinates?.let {
                            if (!it.isAttached) return false
                            if (!selectionRegistrar.hasSelection(state.selectableId)) return false

                            val consumed = selectionRegistrar.notifySelectionUpdate(
                                layoutCoordinates = it,
                                previousPosition = lastPosition,
                                newPosition = dragPosition,
                                isStartHandle = false,
                                adjustment = adjustment
                            )
                            if (consumed == true) {
                                lastPosition = dragPosition
                            }
                        }
                        return true
                    }
                }

                Modifier.composed {
                    val currentMouseSelectionObserver by rememberUpdatedState(mouseSelectionObserver)
                    pointerInput(Unit) {
                        mouseSelectionDetector(currentMouseSelectionObserver)
                    }
                }
            }.pointerHoverIcon(textPointerIcon)
        } else {
            Modifier
        }
    }

    /**
     * Sets the [TextDelegate] in the [state]. If the text of the new delegate is different from
     * the text of the current delegate, the [semanticsModifier] will be recreated. Note that
     * changing the semantics modifier does not invalidate the composition, so callers of
     * [setTextDelegate] are required to call [modifiers] again if they wish to use the updated
     * semantics modifier.
     */
    fun setTextDelegate(textDelegate: TextDelegate) {
        if (state.textDelegate === textDelegate) {
            return
        }
        state.textDelegate = textDelegate
        semanticsModifier = createSemanticsModifierFor(state.textDelegate.text)
    }

    val measurePolicy = object : MeasurePolicy {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            // NOTE(text-perf-review): current implementation of layout means that layoutResult
            // will _never_ be the same instance. We should try and fast path case where
            // everything is the same and return same instance in that case.
            val prevLayout = state.layoutResult
            val layoutResult = state.textDelegate.layout(
                constraints,
                layoutDirection,
                prevLayout
            )
            if (prevLayout != layoutResult) {
                state.onTextLayout(layoutResult)

                prevLayout?.let { prevLayoutResult ->
                    // If the input text of this CoreText has changed, notify the SelectionContainer.
                    if (prevLayoutResult.layoutInput.text != layoutResult.layoutInput.text) {
                        selectionRegistrar?.notifySelectableChange(state.selectableId)
                    }
                }
            }
            state.layoutResult = layoutResult

            check(measurables.size >= layoutResult.placeholderRects.size)
            val placeables = layoutResult.placeholderRects.fastMapIndexedNotNull { index, rect ->
                // PlaceholderRect will be null if it's ellipsized. In that case, the corresponding
                // inline children won't be measured or placed.
                rect?.let {
                    Pair(
                        measurables[index].measure(
                            Constraints(
                                maxWidth = floor(it.width).toInt(),
                                maxHeight = floor(it.height).toInt()
                            )
                        ),
                        IntOffset(it.left.roundToInt(), it.top.roundToInt())
                    )
                }
            }
            return layout(
                layoutResult.size.width,
                layoutResult.size.height,
                // Provide values for the alignment lines defined by text - the first
                // and last baselines of the text. These can be used by parent layouts
                // to position this text or align this and other texts by baseline.
                //
                // Note: we use round to make Int but any rounding doesn't work well here since
                // the layout system works with integer pixels but baseline can be in a middle of
                // the pixel. So any rounding doesn't offer the pixel perfect baseline. We use
                // round just because the Android framework is doing float-to-int conversion with
                // round.
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/jni/android/graphics/Paint.cpp;l=635?q=Paint.cpp
                // NOTE(text-perf-review): layoutResult should ideally just cache this map. It is
                // being recreated every layout right now,
                mapOf(
                    FirstBaseline to layoutResult.firstBaseline.roundToInt(),
                    LastBaseline to layoutResult.lastBaseline.roundToInt()
                )
            ) {
                placeables.fastForEach { (placeable, position) ->
                    placeable.place(position)
                }
            }
        }

        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ): Int {
            state.textDelegate.layoutIntrinsics(layoutDirection)
            return state.textDelegate.minIntrinsicWidth
        }

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ): Int {
            return state.textDelegate
                .layout(Constraints(0, width, 0, Constraints.Infinity), layoutDirection)
                .size.height
        }

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ): Int {
            state.textDelegate.layoutIntrinsics(layoutDirection)
            return state.textDelegate.maxIntrinsicWidth
        }

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ): Int {
            return state.textDelegate
                .layout(Constraints(0, width, 0, Constraints.Infinity), layoutDirection)
                .size.height
        }
    }

    private fun outOfBoundary(start: Offset, end: Offset): Boolean {
        state.layoutResult?.let {
            val lastOffset = it.layoutInput.text.text.length
            val rawStartOffset = it.getOffsetForPosition(start)
            val rawEndOffset = it.getOffsetForPosition(end)

            return rawStartOffset >= lastOffset - 1 && rawEndOffset >= lastOffset - 1 ||
                rawStartOffset < 0 && rawEndOffset < 0
        }
        return false
    }

    /**
     * Draw the given selection on the canvas.
     */
    @Stable
    @OptIn(InternalFoundationTextApi::class)
    private fun Modifier.drawTextAndSelectionBehind(): Modifier =
        this.graphicsLayer().drawBehind {
            state.layoutResult?.let {
                state.drawScopeInvalidation
                val selection = selectionRegistrar?.subselections?.get(state.selectableId)

                if (selection != null) {
                    val start = if (!selection.handlesCrossed) {
                        selection.start.offset
                    } else {
                        selection.end.offset
                    }
                    val end = if (!selection.handlesCrossed) {
                        selection.end.offset
                    } else {
                        selection.start.offset
                    }

                    if (start != end) {
                        val selectionPath = it.multiParagraph.getPathForRange(start, end)
                        drawPath(selectionPath, state.selectionBackgroundColor)
                    }
                }
                drawIntoCanvas { canvas ->
                    TextDelegate.paint(canvas, it)
                }
            }
        }

    private val coreModifiers = Modifier.drawTextAndSelectionBehind().onGloballyPositioned {
        // Get the layout coordinates of the text composable. This is for hit test of
        // cross-composable selection.
        state.layoutCoordinates = it
        if (selectionRegistrar.hasSelection(state.selectableId)) {
            val newGlobalPosition = it.positionInWindow()
            if (newGlobalPosition != state.previousGlobalPosition) {
                selectionRegistrar?.notifyPositionChange(state.selectableId)
            }
            state.previousGlobalPosition = newGlobalPosition
        }
    }

    /*@VisibleForTesting*/
    internal var semanticsModifier = createSemanticsModifierFor(state.textDelegate.text)
        private set

    @Suppress("ModifierFactoryExtensionFunction") // not intended for chaining
    private fun createSemanticsModifierFor(text: AnnotatedString): Modifier {
        return Modifier.semantics {
            this.text = text
            getTextLayoutResult {
                if (state.layoutResult != null) {
                    it.add(state.layoutResult!!)
                    true
                } else {
                    false
                }
            }
        }
    }

    private var selectionModifiers: Modifier = Modifier

    val modifiers: Modifier
        get() = coreModifiers
            .then(semanticsModifier)
            .then(selectionModifiers)

    override fun onRemembered() {
        selectionRegistrar?.let { selectionRegistrar ->
            state.selectable = selectionRegistrar.subscribe(
                MultiWidgetSelectionDelegate(
                    selectableId = state.selectableId,
                    coordinatesCallback = { state.layoutCoordinates },
                    layoutResultCallback = { state.layoutResult }
                )
            )
        }
    }

    override fun onForgotten() {
        state.selectable?.let { selectionRegistrar?.unsubscribe(it) }
    }

    override fun onAbandoned() {
        state.selectable?.let { selectionRegistrar?.unsubscribe(it) }
    }
}

// NOTE(text-perf-review): consider merging with TextDelegate?
@OptIn(InternalFoundationTextApi::class)
/*@VisibleForTesting*/
internal class TextState(
    /** Should *NEVER* be set directly, only through [TextController.setTextDelegate] */
    var textDelegate: TextDelegate,
    /** The selectable Id assigned to the [selectable] */
    val selectableId: Long
) {
    var onTextLayout: (TextLayoutResult) -> Unit = {}

    /** The [Selectable] associated with this [BasicText]. */
    var selectable: Selectable? = null

    /** The last layout coordinates for the Text's layout, used by selection */
    var layoutCoordinates: LayoutCoordinates? = null

    /** The latest TextLayoutResult calculated in the measure block.*/
    var layoutResult: TextLayoutResult? = null
        set(value) {
            drawScopeInvalidation = Unit
            field = value
        }

    /** The global position calculated during the last notifyPosition callback */
    var previousGlobalPosition: Offset = Offset.Zero

    /** The background color of selection */
    var selectionBackgroundColor: Color = Color.Unspecified

    /** Read in draw scopes to invalidate when layoutResult  */
    var drawScopeInvalidation by mutableStateOf(Unit, neverEqualPolicy())
        private set
}

/**
 * Returns the [TextDelegate] passed as a [current] param if the input didn't change
 * otherwise creates a new [TextDelegate].
 */
@OptIn(InternalFoundationTextApi::class)
internal fun updateTextDelegate(
    current: TextDelegate,
    text: AnnotatedString,
    style: TextStyle,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    placeholders: List<AnnotatedString.Range<Placeholder>>
): TextDelegate {
    // NOTE(text-perf-review): whenever we have remember intrinsic implemented, this might be a
    // lot slower than the equivalent `remember(a, b, c, ...) { ... }` call.
    return if (current.text != text ||
        current.style != style ||
        current.softWrap != softWrap ||
        current.overflow != overflow ||
        current.maxLines != maxLines ||
        current.density != density ||
        current.placeholders != placeholders ||
        current.fontFamilyResolver !== fontFamilyResolver
    ) {
        TextDelegate(
            text = text,
            style = style,
            softWrap = softWrap,
            overflow = overflow,
            maxLines = maxLines,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            placeholders = placeholders,
        )
    } else {
        current
    }
}

@OptIn(InternalFoundationTextApi::class)
internal fun updateTextDelegate(
    current: TextDelegate,
    text: String,
    style: TextStyle,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
): TextDelegate {
    // NOTE(text-perf-review): whenever we have remember intrinsic implemented, this might be a
    // lot slower than the equivalent `remember(a, b, c, ...) { ... }` call.
    return if (current.text.text != text ||
        current.style != style ||
        current.softWrap != softWrap ||
        current.overflow != overflow ||
        current.maxLines != maxLines ||
        current.density != density ||
        current.fontFamilyResolver !== fontFamilyResolver
    ) {
        TextDelegate(
            text = AnnotatedString(text),
            style = style,
            softWrap = softWrap,
            overflow = overflow,
            maxLines = maxLines,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
        )
    } else {
        current
    }
}

private val EmptyInlineContent: Pair<List<PlaceholderRange>, List<InlineContentRange>> =
    Pair(emptyList(), emptyList())

internal fun resolveInlineContent(
    text: AnnotatedString,
    inlineContent: Map<String, InlineTextContent>
): Pair<List<PlaceholderRange>, List<InlineContentRange>> {
    if (inlineContent.isEmpty()) {
        return EmptyInlineContent
    }
    val inlineContentAnnotations = text.getStringAnnotations(INLINE_CONTENT_TAG, 0, text.length)

    val placeholders = mutableListOf<AnnotatedString.Range<Placeholder>>()
    val inlineComposables = mutableListOf<AnnotatedString.Range<@Composable (String) -> Unit>>()
    inlineContentAnnotations.fastForEach { annotation ->
        inlineContent[annotation.item]?.let { inlineTextContent ->
            placeholders.add(
                AnnotatedString.Range(
                    inlineTextContent.placeholder,
                    annotation.start,
                    annotation.end
                )
            )
            inlineComposables.add(
                AnnotatedString.Range(
                    inlineTextContent.children,
                    annotation.start,
                    annotation.end
                )
            )
        }
    }
    return Pair(placeholders, inlineComposables)
}
