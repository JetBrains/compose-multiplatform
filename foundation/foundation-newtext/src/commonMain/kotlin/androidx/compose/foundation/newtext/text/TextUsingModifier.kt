/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.newtext.text

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.modifierElementOf
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.math.floor
import kotlin.math.roundToInt

@ExperimentalTextApi
@Composable
fun TextUsingModifier(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent>? = null,
) = TextUsingModifier(
    AnnotatedString(text),
    modifier,
    style,
    onTextLayout,
    overflow,
    softWrap,
    maxLines,
    inlineContent
)

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalTextApi
@Composable
fun TextUsingModifier(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent>? = null,
) {
    val fontFamilyResolver = LocalFontFamilyResolver.current

    val (placeholders, inlineComposables) = resolveInlineContent(text, inlineContent)
    val measuredPlaceholderPositions = remember {
        mutableStateOf<List<Rect?>?>(null)
    }
    Layout(
        content = if (inlineComposables.isEmpty()) {
            {}
        } else {
            { InlineChildren(text, inlineComposables) }
        },
        modifier = modifier.textLayoutModifier(
            text = text,
            style = style,
            onTextLayout = onTextLayout,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            fontFamilyResolver = fontFamilyResolver,
            placeholders = placeholders,
            onPlaceholderLayout = { measuredPlaceholderPositions.value = it }
        ),
        measurePolicy = TextMeasurePolicy { measuredPlaceholderPositions.value }
    )
}

private class TextMeasurePolicy(
    private val placements: () -> List<Rect?>?
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val toPlace = placements()?.fastMapIndexedNotNull { index, rect ->
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
            constraints.maxWidth,
            constraints.maxHeight,
        ) {
            toPlace?.fastForEach { (placeable, position) ->
                placeable.place(position)
            }
        }
    }
}

@ExperimentalComposeUiApi
private fun Modifier.textLayoutModifier(
    text: AnnotatedString,
    style: TextStyle,
    onTextLayout: (TextLayoutResult) -> Unit,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<AnnotatedString.Range<Placeholder>>?,
    onPlaceholderLayout: ((List<Rect?>) -> Unit)?
): Modifier {
    val params = TextParams(
        text,
        style,
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        fontFamilyResolver,
        placeholders,
        onPlaceholderLayout,
    )
    return this then modifierElementOf(
        params,
        create = { TextLayoutModifier(params) },
        update = { it.update(params) },
        definitions = {},
    )
}

internal data class TextParams(
    val text: AnnotatedString,
    val style: TextStyle,
    val onTextLayout: ((TextLayoutResult) -> Unit)?,
    val overflow: TextOverflow,
    val softWrap: Boolean,
    val maxLines: Int,
    val fontFamilyResolver: FontFamily.Resolver,
    val placeholders: List<AnnotatedString.Range<Placeholder>>?,
    val onPlaceholderLayout: ((List<Rect?>) -> Unit)?,
)

@ExperimentalComposeUiApi
private class TextLayoutModifier(
    params: TextParams
) : Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode {
    private lateinit var lastTextDelegate: ParamsTextDelegate
    private var lastTextLayoutResult: TextLayoutResult? by mutableStateOf(null)
    private var textDelegateDirty = true

    private var params: TextParams = params
        set(value) {
            textDelegateDirty = true
            field = value
            // TODO: calculate if only draw changed
            invalidateLayout()
        }

    private fun invalidateDraw() {
    }

    private fun invalidateLayout() {
        forceRemeasure()
    }

    fun update(params: TextParams) {
        this.params = params
    }

    private fun getCurrentTextDelegate(density: Density): ParamsTextDelegate {
        return if (!textDelegateDirty) {
            lastTextDelegate
        } else {
            val textDelegate = ParamsTextDelegate(params, density)
            lastTextDelegate = textDelegate
            textDelegateDirty = false
            textDelegate
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val td = getCurrentTextDelegate(this)

        // TODO: Detect if this is only a constraints change and compare intrinsic and old/new
        //  constraints

        // Otherwise, we expect that all restarts lead to a new text layout
        val textLayoutResult = td.layout(constraints, layoutDirection)

        params.onTextLayout?.let { onTextLayout ->
            onTextLayout(textLayoutResult)
            // TODO: figure out how to expose this to selection here
        }

        // first share the placeholders
        params.onPlaceholderLayout?.invoke(textLayoutResult.placeholderRects)

        // then allow children to measure _inside_ our final box, with the above placeholders
        val placeable = measurable.measure(
            Constraints.fixed(
                textLayoutResult.size.width,
                textLayoutResult.size.height
            )
        )

        lastTextLayoutResult = textLayoutResult

        return layout(
            textLayoutResult.size.width,
            textLayoutResult.size.height,
            mapOf(
                FirstBaseline to textLayoutResult.firstBaseline.roundToInt(),
                LastBaseline to textLayoutResult.lastBaseline.roundToInt()
            )
        ) {
            placeable.place(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val td = getCurrentTextDelegate(this)
        return td.minIntrinsicWidth
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val td = getCurrentTextDelegate(this)
        return td
            .layout(Constraints(0, width, 0, Constraints.Infinity), layoutDirection)
            .size.height
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val td = getCurrentTextDelegate(this)
        return td.maxIntrinsicWidth
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val td = getCurrentTextDelegate(this)
        return td
            .layout(Constraints(0, width, 0, Constraints.Infinity), layoutDirection)
            .size.height
    }

    override fun ContentDrawScope.draw() {
        drawIntoCanvas { canvas ->
            lastTextLayoutResult?.let { textLayout ->
                ParamsTextDelegate.paint(canvas, textLayout)
            }
            drawContent()
        }
    }
}

private class ParamsTextDelegate(
    val params: TextParams,
    val density: Density,
    val placeholders: List<AnnotatedString.Range<Placeholder>> = emptyList()
) {
    /*@VisibleForTesting*/
    // NOTE(text-perf-review): it seems like TextDelegate essentially guarantees that we use
    // MultiParagraph. Can we have a fast-path that uses just Paragraph in simpler cases (ie,
    // String)?
    internal var paragraphIntrinsics: MultiParagraphIntrinsics? = null
    internal var intrinsicsLayoutDirection: LayoutDirection? = null

    private val nonNullIntrinsics: MultiParagraphIntrinsics
        get() = paragraphIntrinsics
        ?: throw IllegalStateException("layoutIntrinsics must be called first")

    /**
     * The width for text if all soft wrap opportunities were taken.
     *
     * Valid only after [layout] has been called.
     */
    val minIntrinsicWidth: Int get() = nonNullIntrinsics.minIntrinsicWidth.ceilToIntPx()

    /**
     * The width at which increasing the width of the text no longer decreases the height.
     *
     * Valid only after [layout] has been called.
     */
    val maxIntrinsicWidth: Int get() = nonNullIntrinsics.maxIntrinsicWidth.ceilToIntPx()

    fun layoutIntrinsics(layoutDirection: LayoutDirection) {
        val localIntrinsics = paragraphIntrinsics
        val intrinsics = if (
            localIntrinsics == null ||
            layoutDirection != intrinsicsLayoutDirection ||
            localIntrinsics.hasStaleResolvedFonts
        ) {
            intrinsicsLayoutDirection = layoutDirection
            MultiParagraphIntrinsics(
                annotatedString = params.text,
                style = resolveDefaults(params.style, layoutDirection),
                density = density,
                fontFamilyResolver = params.fontFamilyResolver,
                placeholders = placeholders
            )
        } else {
            localIntrinsics
        }

        paragraphIntrinsics = intrinsics
    }

    /**
     * Computes the visual position of the glyphs for painting the text.
     *
     * The text will layout with a width that's as close to its max intrinsic width as possible
     * while still being greater than or equal to `minWidth` and less than or equal to `maxWidth`.
     */
    private fun layoutText(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MultiParagraph {
        layoutIntrinsics(layoutDirection)

        val minWidth = constraints.minWidth
        val widthMatters = params.softWrap || params.overflow == TextOverflow.Ellipsis
        val maxWidth = if (widthMatters && constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            Constraints.Infinity
        }

        // This is a fallback behavior because native text layout doesn't support multiple
        // ellipsis in one text layout.
        // When softWrap is turned off and overflow is ellipsis, it's expected that each line
        // that exceeds maxWidth will be ellipsized.
        // For example,
        // input text:
        //     "AAAA\nAAAA"
        // maxWidth:
        //     3 * fontSize that only allow 3 characters to be displayed each line.
        // expected output:
        //     AA…
        //     AA…
        // Here we assume there won't be any '\n' character when softWrap is false. And make
        // maxLines 1 to implement the similar behavior.
        val overwriteMaxLines = !params.softWrap && params.overflow == TextOverflow.Ellipsis
        val finalMaxLines = if (overwriteMaxLines) 1 else params.maxLines.coerceAtLeast(1)

        // if minWidth == maxWidth the width is fixed.
        //    therefore we can pass that value to our paragraph and use it
        // if minWidth != maxWidth there is a range
        //    then we should check if the max intrinsic width is in this range to decide the
        //    width to be passed to Paragraph
        //        if max intrinsic width is between minWidth and maxWidth
        //           we can use it to layout
        //        else if max intrinsic width is greater than maxWidth, we can only use maxWidth
        //        else if max intrinsic width is less than minWidth, we should use minWidth
        val width = if (minWidth == maxWidth) {
            maxWidth
        } else {
            maxIntrinsicWidth.coerceIn(minWidth, maxWidth)
        }

        return MultiParagraph(
            intrinsics = nonNullIntrinsics,
            constraints = Constraints(maxWidth = width, maxHeight = constraints.maxHeight),
            // This is a fallback behavior for ellipsis. Native
            maxLines = finalMaxLines,
            ellipsis = params.overflow == TextOverflow.Ellipsis
        )
    }

    fun layout(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): TextLayoutResult {
        val multiParagraph = layoutText(
            constraints,
            layoutDirection
        )

        val size = constraints.constrain(
            IntSize(
                multiParagraph.width.ceilToIntPx(),
                multiParagraph.height.ceilToIntPx()
            )
        )

        // NOTE(text-perf-review): it feels odd to create the input + result at the same time. if
        // the allocation of these objects is 1:1 then it might make sense to just merge them?
        // Alternatively, we might be able to save some effortØBa here by having a common object for
        // the types that go into the result here that are less likely to change
        return TextLayoutResult(
            TextLayoutInput(
                params.text,
                params.style,
                placeholders,
                params.maxLines,
                params.softWrap,
                params.overflow,
                density,
                layoutDirection,
                params.fontFamilyResolver,
                constraints
            ),
            multiParagraph,
            size
        )
    }

    companion object {
        /**
         * Paints the text onto the given canvas.
         *
         * Valid only after [layout] has been called.
         *
         * If you cannot see the text being painted, check that your text color does not conflict
         * with the background on which you are drawing. The default text color is white (to
         * contrast with the default black background color), so if you are writing an
         * application with a white background, the text will not be visible by default.
         *
         * To set the text style, specify a [SpanStyle] when creating the [AnnotatedString] that
         * you pass to the [TextDelegate] constructor or to the [textLayoutModifier] property.
         */
        fun paint(canvas: Canvas, textLayoutResult: TextLayoutResult) {
            TextPainter.paint(canvas, textLayoutResult)
        }
    }
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

private typealias PlaceholderRange = AnnotatedString.Range<Placeholder>
private typealias InlineContentRange = AnnotatedString.Range<@Composable (String) -> Unit>
internal const val INLINE_CONTENT_TAG = "androidx.compose.foundation.newtext.text.inlineContent"

private val EmptyInlineContent: Pair<List<PlaceholderRange>, List<InlineContentRange>> =
    Pair(emptyList(), emptyList())

private fun resolveInlineContent(
    text: AnnotatedString,
    inlineContent: Map<String, InlineTextContent>?
): Pair<List<PlaceholderRange>, List<InlineContentRange>> {
    if (inlineContent.isNullOrEmpty()) {
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
