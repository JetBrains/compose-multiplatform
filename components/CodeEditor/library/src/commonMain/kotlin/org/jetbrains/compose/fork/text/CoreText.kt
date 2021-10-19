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

package org.jetbrains.compose.fork.text

import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import org.jetbrains.compose.fork.fastForEach
import org.jetbrains.compose.fork.fastMap
import org.jetbrains.compose.fork.fastMapIndexedNotNull
import org.jetbrains.compose.fork.text.selection.LocalSelectionRegistrar
import org.jetbrains.compose.fork.text.selection.MouseSelectionObserver
import org.jetbrains.compose.fork.text.selection.MultiWidgetSelectionDelegate
import org.jetbrains.compose.fork.text.selection.Selectable
import org.jetbrains.compose.fork.text.selection.SelectionAdjustment
import org.jetbrains.compose.fork.text.selection.SelectionRegistrar
import org.jetbrains.compose.fork.text.selection.hasSelection
import org.jetbrains.compose.fork.text.selection.mouseSelectionDetector
import kotlin.math.floor
import kotlin.math.roundToInt

private typealias PlaceholderRange = AnnotatedString.Range<Placeholder>
private typealias InlineContentRange = AnnotatedString.Range<@Composable (String) -> Unit>

/**
 * CoreText is a low level element that displays text with multiple different styles. The text to
 * display is described using a [AnnotatedString]. Typically you will instead want to use
 * [androidx.compose.material.Text], which is a higher level Text element that contains
 * semantics and consumes style information from a theme.
 *
 * @param text AnnotatedString encoding a styled text.
 * @param modifier Modifier to apply to this layout node.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and [TextAlign] may have unexpected effects.
 * @param overflow How visual overflow should be handled.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
 * @param inlineContent A map store composables that replaces certain ranges of the text. It's
 * used to insert composables into text layout. Check [InlineTextContent] for more information.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw selection around the text.
 */
@Composable
@OptIn(InternalFoundationTextApi::class, ExperimentalComposeUiApi::class)
internal fun CoreText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle,
    softWrap: Boolean,
    overflow: TextOverflow,
    maxLines: Int,
    inlineContent: Map<String, InlineTextContent>,
    onTextLayout: (TextLayoutResult) -> Unit
) {
    require(maxLines > 0) { "maxLines should be greater than 0" }

    // selection registrar, if no SelectionContainer is added ambient value will be null
    val selectionRegistrar = LocalSelectionRegistrar.current
    val density = LocalDensity.current
    val resourceLoader = LocalFontLoader.current
    val selectionBackgroundColor = LocalTextSelectionColors.current.backgroundColor

    val (placeholders, inlineComposables) = resolveInlineContent(text, inlineContent)

    // The ID used to identify this CoreText. If this CoreText is removed from the composition
    // tree and then added back, this ID should stay the same.
    // Notice that we need to update selectable ID when the input text or selectionRegistrar has
    // been updated.
    // When text is updated, the selection on this CoreText becomes invalid. It can be treated
    // as a brand new CoreText.
    // When SelectionRegistrar is updated, CoreText have to request a new ID to avoid ID collision.

    // NOTE(text-perf-review): potential bug. selectableId is regenerated here whenever text
    // changes, but it is only saved in the initial creation of TextState.
    val selectableId =
        rememberSaveable(text, selectionRegistrar, saver = selectionIdSaver(selectionRegistrar)) {
            selectionRegistrar?.nextSelectableId() ?: SelectionRegistrar.InvalidSelectableId
        }

    // NOTE(text-perf-review): We might want to consider consolidating a lot of what is going on
    // here. We might end up saving a bit of cost here. There are 3 things that we remember and
    // store separately but could instead just be a single object:
    // selectableId, TextState, TextController.
    val state = remember {
        TextState(
            TextDelegate(
                text = text,
                style = style,
                density = density,
                softWrap = softWrap,
                resourceLoader = resourceLoader,
                overflow = overflow,
                maxLines = maxLines,
                placeholders = placeholders
            ),
            selectableId
        )
    }
    state.textDelegate = updateTextDelegate(
        current = state.textDelegate,
        text = text,
        style = style,
        density = density,
        softWrap = softWrap,
        resourceLoader = resourceLoader,
        overflow = overflow,
        maxLines = maxLines,
        placeholders = placeholders
    )
    state.onTextLayout = onTextLayout
    state.selectionBackgroundColor = selectionBackgroundColor

    val controller = remember { TextController(state) }
    controller.update(selectionRegistrar)

    // NOTE(text-perf-review): if we split this function into separate String/AnnotatedString
    // versions, we wouldn't have any inline composables, and text would always be child-less. We
    // might then want to consider using `ReusableComposeNode` directly instead of `Layout`,
    // which should be a bit faster (layout isn't buying us anything here). Additionally, we
    // might want to see if the "non skippable updateable" version of ComposeNode is advantageous
    // to us here, since this is already a restartable composable and it is a "leaf" node, we don't
    // need the modifiers to materialize inside of a tight scope.
    Layout(
        content = if (inlineComposables.isEmpty()) {
            {}
        } else {
            { InlineChildren(text, inlineComposables) }
        },
        modifier = modifier
            .then(controller.modifiers)
            // NOTE(text-perf-review): consider moving this to controller.modifiers. It only uses
            // the controller already, and by having the modifier be instance-equal across
            // recompositions you would gain a lot.
            .then(
                if (selectionRegistrar != null) {
                    if (isInTouchMode) {
                        Modifier.pointerInput(controller.longPressDragObserver) {
                            detectDragGesturesAfterLongPressWithObserver(
                                controller.longPressDragObserver
                            )
                        }
                    } else {
                        Modifier.pointerInput(controller.mouseSelectionObserver) {
                            mouseSelectionDetector(
                                controller.mouseSelectionObserver
                            )
                        }.pointerTextIcon()
                    }
                } else {
                    Modifier
                }
            ),
        measurePolicy = controller.measurePolicy
    )

    // NOTE(text-perf-review): consider making TextController or TextState a remember observer
    // and just implementing onDispose on it, which might save us a little bit here.
    DisposableEffect(selectionRegistrar, effect = controller.commit)
}

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
internal class TextController(val state: TextState) {
    var selectionRegistrar: SelectionRegistrar? = null

    fun update(selectionRegistrar: SelectionRegistrar?) {
        this.selectionRegistrar = selectionRegistrar
    }

    val modifiers = Modifier.drawTextAndSelectionBehind().onGloballyPositioned {
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
    }.semantics {
        getTextLayoutResult {
            if (state.layoutResult != null) {
                it.add(state.layoutResult!!)
                true
            } else {
                false
            }
        }
    }

    val measurePolicy = object : MeasurePolicy {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            // NOTE(text-perf-review): current implementation of layout means that layoutResult
            // will _never_ be the same instance. We should try and fast path case where
            // everything is the same and return same instance in that case.
            val layoutResult = state.textDelegate.layout(
                constraints,
                layoutDirection,
                state.layoutResult
            )
            if (state.layoutResult != layoutResult) {
                state.onTextLayout(layoutResult)

                state.layoutResult?.let { prevLayoutResult ->
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
                placeables.fastForEach { placeable ->
                    placeable.first.placeRelative(placeable.second)
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

    val commit: DisposableEffectScope.() -> DisposableEffectResult = {
        // if no SelectionContainer is added as parent selectionRegistrar will be null
        selectionRegistrar?.let { selectionRegistrar ->
            state.selectable = selectionRegistrar.subscribe(
                MultiWidgetSelectionDelegate(
                    selectableId = state.selectableId,
                    coordinatesCallback = { state.layoutCoordinates },
                    layoutResultCallback = { state.layoutResult }
                )
            )
        }
        onDispose {
            state.selectable?.let { selectionRegistrar?.unsubscribe(it) }
        }
    }

    val longPressDragObserver: TextDragObserver = object : TextDragObserver {
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

        override fun onStart(startPoint: Offset) {
            state.layoutCoordinates?.let {
                if (!it.isAttached) return

                if (outOfBoundary(startPoint, startPoint)) {
                    selectionRegistrar?.notifySelectionUpdateSelectAll(
                        selectableId = state.selectableId
                    )
                } else {
                    selectionRegistrar?.notifySelectionUpdateStart(
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
                    val consumed = selectionRegistrar?.notifySelectionUpdate(
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
                selectionRegistrar?.notifySelectionUpdateEnd()
            }
        }

        override fun onCancel() {
            if (selectionRegistrar.hasSelection(state.selectableId)) {
                selectionRegistrar?.notifySelectionUpdateEnd()
            }
        }
    }

    val mouseSelectionObserver = object : MouseSelectionObserver {
        var lastPosition = Offset.Zero

        override fun onExtend(downPosition: Offset): Boolean {
            state.layoutCoordinates?.let { layoutCoordinates ->
                if (!layoutCoordinates.isAttached) return false
                selectionRegistrar?.let {
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

                selectionRegistrar?.let {
                    val consumed = it.notifySelectionUpdate(
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
            }
            return true
        }

        override fun onStart(
            downPosition: Offset,
            adjustment: SelectionAdjustment
        ): Boolean {
            state.layoutCoordinates?.let {
                if (!it.isAttached) return false

                selectionRegistrar?.notifySelectionUpdateStart(
                    layoutCoordinates = it,
                    startPosition = downPosition,
                    adjustment = adjustment
                )

                lastPosition = downPosition
                return selectionRegistrar.hasSelection(state.selectableId)
            }

            return false
        }

        override fun onDrag(dragPosition: Offset, adjustment: SelectionAdjustment): Boolean {
            state.layoutCoordinates?.let {
                if (!it.isAttached) return false
                if (!selectionRegistrar.hasSelection(state.selectableId)) return false

                val consumed = selectionRegistrar?.notifySelectionUpdate(
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
}

// NOTE(text-perf-review): consider merging with TextDelegate?
@OptIn(InternalFoundationTextApi::class)
/*@VisibleForTesting*/
internal class TextState(
    var textDelegate: TextDelegate,
    /** The selectable Id assigned to the [selectable] */
    val selectableId: Long
) {
    var onTextLayout: (TextLayoutResult) -> Unit = {}

    /** The [Selectable] associated with this [CoreText]. */
    var selectable: Selectable? = null

    /** The last layout coordinates for the Text's layout, used by selection */
    var layoutCoordinates: LayoutCoordinates? = null

    /** The latest TextLayoutResult calculated in the measure block */
    var layoutResult: TextLayoutResult? = null

    /** The global position calculated during the last notifyPosition callback */
    var previousGlobalPosition: Offset = Offset.Zero

    /** The background color of selection */
    var selectionBackgroundColor: Color = Color.Unspecified
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
    resourceLoader: Font.ResourceLoader,
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
        current.placeholders != placeholders
    ) {
        TextDelegate(
            text = text,
            style = style,
            softWrap = softWrap,
            overflow = overflow,
            maxLines = maxLines,
            density = density,
            resourceLoader = resourceLoader,
            placeholders = placeholders
        )
    } else {
        current
    }
}

private val EmptyInlineContent: Pair<List<PlaceholderRange>, List<InlineContentRange>> =
    Pair(emptyList(), emptyList())

private fun resolveInlineContent(
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

/**
 * A custom saver that won't save if no selection is active.
 */
private fun selectionIdSaver(selectionRegistrar: SelectionRegistrar?) = Saver<Long, Long>(
    save = { if (selectionRegistrar.hasSelection(it)) it else null },
    restore = { it }
)
