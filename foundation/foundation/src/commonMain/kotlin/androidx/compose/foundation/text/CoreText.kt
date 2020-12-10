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
@file:Suppress("DEPRECATION_ERROR")

package androidx.compose.foundation.text

import androidx.compose.foundation.text.selection.MultiWidgetSelectionDelegate
import androidx.compose.runtime.CommitScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.LongPressDragObserver
import androidx.compose.ui.gesture.longPressDragGestureFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasureBlock
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasureBlock
import androidx.compose.ui.layout.globalPosition
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.AmbientFontLoader
import androidx.compose.ui.selection.AmbientSelectionRegistrar
import androidx.compose.ui.selection.AmbientTextSelectionColors
import androidx.compose.ui.selection.Selectable
import androidx.compose.ui.selection.SelectionRegistrar
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextDelegate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.length
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.subSequence
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.annotation.VisibleForTesting
import androidx.compose.ui.util.fastForEach
import kotlin.math.floor
import kotlin.math.roundToInt

private typealias PlaceholderRange = AnnotatedString.Range<Placeholder>
private typealias InlineContentRange = AnnotatedString.Range<@Composable (String) -> Unit>

/**
 * CoreText is a low level element that displays text with multiple different styles. The text to
 * display is described using a [AnnotatedString]. Typically you will instead want to use
 * [androidx.compose.foundation.Text], which is a higher level Text element that contains semantics and
 * consumes style information from a theme.
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
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 */
@Composable
@InternalTextApi
@OptIn(ExperimentalTextApi::class)
fun CoreText(
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
    val selectionRegistrar = AmbientSelectionRegistrar.current
    val density = AmbientDensity.current
    val resourceLoader = AmbientFontLoader.current
    val selectionBackgroundColor = AmbientTextSelectionColors.current.backgroundColor

    val (placeholders, inlineComposables) = resolveInlineContent(text, inlineContent)

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
            )
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
    state.selectionPaint.color = selectionBackgroundColor

    val controller = remember { TextController(state) }
    controller.update(selectionRegistrar)

    Layout(
        content = if (inlineComposables.isEmpty()) {
            emptyContent()
        } else {
            { InlineChildren(text, inlineComposables) }
        },
        modifier = modifier
            .then(controller.modifiers)
            .then(
                if (selectionRegistrar != null) {
                    Modifier.longPressDragGestureFilter(
                        longPressDragObserver(
                            state = state,
                            selectionRegistrar = selectionRegistrar
                        )
                    )
                } else {
                    Modifier
                }
            ),
        minIntrinsicWidthMeasureBlock = controller.minIntrinsicWidth,
        minIntrinsicHeightMeasureBlock = controller.minIntrinsicHeight,
        maxIntrinsicWidthMeasureBlock = controller.maxIntrinsicWidth,
        maxIntrinsicHeightMeasureBlock = controller.maxIntrinsicHeight,
        measureBlock = controller.measure
    )

    onCommit(selectionRegistrar, callback = controller.commit)
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
            val placeables = children.map { it.measure(constrains) }
            layout(width = constrains.maxWidth, height = constrains.maxHeight) {
                placeables.fastForEach { it.placeRelative(0, 0) }
            }
        }
    }
}

@OptIn(
    InternalTextApi::class,
    ExperimentalTextApi::class
)
private class TextController(val state: TextState) {
    var selectionRegistrar: SelectionRegistrar? = null

    fun update(selectionRegistrar: SelectionRegistrar?) {
        this.selectionRegistrar = selectionRegistrar
    }

    val modifiers = Modifier.graphicsLayer().drawBehind {
        state.layoutResult?.let { layoutResult ->
            drawIntoCanvas { canvas ->
                state.selectionRange?.let {
                    TextDelegate.paintBackground(
                        it.min,
                        it.max,
                        state.selectionPaint,
                        canvas,
                        layoutResult
                    )
                }
                TextDelegate.paint(canvas, layoutResult)
            }
        }
    }.onGloballyPositioned {
        // Get the layout coordinates of the text composable. This is for hit test of
        // cross-composable selection.
        state.layoutCoordinates = it
        selectionRegistrar?.let { selectionRegistrar ->
            if (state.selectionRange != null) {
                val newGlobalPosition = it.globalPosition
                if (newGlobalPosition != state.previousGlobalPosition) {
                    selectionRegistrar.notifyPositionChange()
                }
                state.previousGlobalPosition = newGlobalPosition
            }
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

    val minIntrinsicWidth: IntrinsicMeasureBlock = { _, _ ->
        state.textDelegate.layoutIntrinsics(layoutDirection)
        state.textDelegate.minIntrinsicWidth
    }

    val minIntrinsicHeight: IntrinsicMeasureBlock = { _, width ->
        // given the width constraint, determine the min height
        state.textDelegate
            .layout(Constraints(0, width, 0, Constraints.Infinity), layoutDirection)
            .size.height
    }

    val maxIntrinsicWidth: IntrinsicMeasureBlock = { _, _ ->
        state.textDelegate.layoutIntrinsics(layoutDirection)
        state.textDelegate.maxIntrinsicWidth
    }

    val maxIntrinsicHeight: IntrinsicMeasureBlock = { _, width ->
        state.textDelegate
            .layout(Constraints(0, width, 0, Constraints.Infinity), layoutDirection)
            .size.height
    }

    val measure: MeasureBlock = { measurables, constraints ->
        val layoutResult = state.textDelegate.layout(
            constraints,
            layoutDirection,
            state.layoutResult
        )
        if (state.layoutResult != layoutResult) {
            state.onTextLayout(layoutResult)
        }
        state.layoutResult = layoutResult

        check(measurables.size >= layoutResult.placeholderRects.size)
        val placeables = layoutResult.placeholderRects.mapIndexedNotNull { index, rect ->
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

        layout(
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

    val commit: CommitScope.() -> Unit = {
        // if no SelectionContainer is added as parent selectionRegistrar will be null
        val id: Selectable? =
            selectionRegistrar?.let { selectionRegistrar ->
                selectionRegistrar.subscribe(
                    MultiWidgetSelectionDelegate(
                        selectionRangeUpdate = { state.selectionRange = it },
                        coordinatesCallback = { state.layoutCoordinates },
                        layoutResultCallback = { state.layoutResult }
                    )
                )
            }

        onDispose {
            // unregister only if any id was provided by SelectionRegistrar
            id?.let { selectionRegistrar?.unsubscribe(id) }
        }
    }
}

@OptIn(InternalTextApi::class)
@VisibleForTesting
internal class TextState(
    var textDelegate: TextDelegate
) {
    var onTextLayout: (TextLayoutResult) -> Unit = {}

    /**
     * The current selection range, used by selection.
     * This should be a state as every time we update the value during the selection we
     * need to redraw it. state observation during onDraw callback will make it work.
     */
    var selectionRange by mutableStateOf<TextRange?>(null, structuralEqualityPolicy())
    /** The last layout coordinates for the Text's layout, used by selection */
    var layoutCoordinates: LayoutCoordinates? = null
    /** The latest TextLayoutResult calculated in the measure block */
    var layoutResult: TextLayoutResult? = null
    /** The global position calculated during the last notifyPosition callback */
    var previousGlobalPosition: Offset = Offset.Zero
    /** The paint used to draw highlight background for selected text. */
    val selectionPaint: Paint = Paint()
}

/**
 * Returns the [TextDelegate] passed as a [current] param if the input didn't change
 * otherwise creates a new [TextDelegate].
 */
@OptIn(InternalTextApi::class)
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

@OptIn(
    InternalTextApi::class,
    ExperimentalTextApi::class
)
@VisibleForTesting
internal fun longPressDragObserver(
    state: TextState,
    selectionRegistrar: SelectionRegistrar?
): LongPressDragObserver {
    /**
     * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
     * recalculated.
     */
    var dragBeginPosition = Offset.Zero

    /**
     * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
     * it will be zeroed out.
     */
    var dragTotalDistance = Offset.Zero
    return object : LongPressDragObserver {
        override fun onLongPress(pxPosition: Offset) {
            state.layoutCoordinates?.let {
                if (!it.isAttached) return

                selectionRegistrar?.notifySelectionUpdateStart(
                    layoutCoordinates = it,
                    startPosition = pxPosition
                )

                dragBeginPosition = pxPosition
            }
        }

        override fun onDragStart() {
            // selection never started
            if (state.selectionRange == null) return
            // Zero out the total distance that being dragged.
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(dragDistance: Offset): Offset {
            state.layoutCoordinates?.let {
                if (!it.isAttached) return Offset.Zero
                // selection never started, did not consume any drag
                if (state.selectionRange == null) return Offset.Zero

                dragTotalDistance += dragDistance

                selectionRegistrar?.notifySelectionUpdate(
                    layoutCoordinates = it,
                    startPosition = dragBeginPosition,
                    endPosition = dragBeginPosition + dragTotalDistance
                )
            }
            return dragDistance
        }

        override fun onStop(velocity: Offset) {
            selectionRegistrar?.notifySelectionUpdateEnd()
        }

        override fun onCancel() {
            selectionRegistrar?.notifySelectionUpdateEnd()
        }
    }
}
