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

package androidx.compose.foundation.text

import androidx.compose.foundation.text.TextDelegate.Companion.paint
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain
import kotlin.math.ceil

/**
 * An object that paints text onto a [Canvas].
 *
 * To use a [TextDelegate], follow these steps:
 *
 * 1. Create an [AnnotatedString] and pass it to the [TextDelegate] constructor.
 *
 * 2. Call [layout] to prepare the paragraph.
 *
 * 3. Call [paint] as often as desired to paint the paragraph.
 *
 *  If the width of the area into which the text is being painted changes, return to step 2. If the
 *  text to be painted changes, return to step 1.
 *
 * @param text the text to paint.
 *
 * @param style The text style specified to render the text. Notice that you can also set text
 * style on the given [AnnotatedString], and the style set on [text] always has higher priority
 * than this setting. But if only one global text style is needed, passing it to [TextDelegate]
 * is always preferred.
 *
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it is truncated such that subsequent
 * lines are dropped.
 *
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and [TextAlign] may have unexpected effects.
 *
 * @param overflow How visual overflow should be handled. Specifically, the ellipsis is applied
 * to the last line before the line truncated by [maxLines], if [maxLines] is non-null and that
 * line overflows the width constraint.
 *
 * @param density The [Density] object that provides pixel density information of the device
 *
 * @param placeholders a list of [Placeholder]s that specify ranges of text where the original
 * text is replaced empty spaces. It's typically used to embed images into text.
 *
 * @suppress
 */
@InternalFoundationTextApi // Used by benchmarks
@Stable
class TextDelegate(
    val text: AnnotatedString,
    val style: TextStyle,
    val maxLines: Int = Int.MAX_VALUE,
    val softWrap: Boolean = true,
    val overflow: TextOverflow = TextOverflow.Clip,
    val density: Density,
    val fontFamilyResolver: FontFamily.Resolver,
    val placeholders: List<AnnotatedString.Range<Placeholder>> = emptyList()
) {
    /*@VisibleForTesting*/
    // NOTE(text-perf-review): it seems like TextDelegate essentially guarantees that we use
    // MultiParagraph. Can we have a fast-path that uses just Paragraph in simpler cases (ie,
    // String)?
    internal var paragraphIntrinsics: MultiParagraphIntrinsics? = null
    internal var intrinsicsLayoutDirection: LayoutDirection? = null

    private val nonNullIntrinsics: MultiParagraphIntrinsics get() = paragraphIntrinsics
        ?: throw IllegalStateException("layoutIntrinsics must be called first")

    /**
     * The width for text if all soft wrap opportunities were taken.
     *
     * Valid only after [layout] has been called.
     */
    val minIntrinsicWidth: Int get() = ceil(nonNullIntrinsics.minIntrinsicWidth).toInt()

    /**
     * The width at which increasing the width of the text no longer decreases the height.
     *
     * Valid only after [layout] has been called.
     */
    val maxIntrinsicWidth: Int get() = ceil(nonNullIntrinsics.maxIntrinsicWidth).toInt()

    init {
        check(maxLines > 0)
    }

    fun layoutIntrinsics(layoutDirection: LayoutDirection) {
        val localIntrinsics = paragraphIntrinsics
        val intrinsics = if (
            localIntrinsics == null ||
            layoutDirection != intrinsicsLayoutDirection ||
            localIntrinsics.hasStaleResolvedFonts
        ) {
            intrinsicsLayoutDirection = layoutDirection
            MultiParagraphIntrinsics(
                annotatedString = text,
                style = resolveDefaults(style, layoutDirection),
                density = density,
                fontFamilyResolver = fontFamilyResolver,
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
        val widthMatters = softWrap || overflow == TextOverflow.Ellipsis
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
        val overwriteMaxLines = !softWrap && overflow == TextOverflow.Ellipsis
        val finalMaxLines = if (overwriteMaxLines) 1 else maxLines

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
            ellipsis = overflow == TextOverflow.Ellipsis
        )
    }

    fun layout(
        constraints: Constraints,
        layoutDirection: LayoutDirection,
        prevResult: TextLayoutResult? = null
    ): TextLayoutResult {
        if (prevResult != null && prevResult.canReuse(
                text, style, placeholders, maxLines, softWrap, overflow, density, layoutDirection,
                fontFamilyResolver, constraints
            )
        ) {
            // NOTE(text-perf-review): seems like there's a nontrivial chance for us to be able
            // to just return prevResult here directly?
            return with(prevResult) {
                copy(
                    layoutInput = TextLayoutInput(
                        layoutInput.text,
                        style,
                        layoutInput.placeholders,
                        layoutInput.maxLines,
                        layoutInput.softWrap,
                        layoutInput.overflow,
                        layoutInput.density,
                        layoutInput.layoutDirection,
                        layoutInput.fontFamilyResolver,
                        constraints
                    ),
                    size = constraints.constrain(
                        IntSize(
                            ceil(multiParagraph.width).toInt(),
                            ceil(multiParagraph.height).toInt()
                        )
                    )
                )
            }
        }

        val multiParagraph = layoutText(
            constraints,
            layoutDirection
        )

        val size = constraints.constrain(
            IntSize(
                ceil(multiParagraph.width).toInt(),
                ceil(multiParagraph.height).toInt()
            )
        )

        // NOTE(text-perf-review): it feels odd to create the input + result at the same time. if
        // the allocation of these objects is 1:1 then it might make sense to just merge them?
        // Alternatively, we might be able to save some effort here by having a common object for
        // the types that go into the result here that are less likely to change
        return TextLayoutResult(
            TextLayoutInput(
                text,
                style,
                placeholders,
                maxLines,
                softWrap,
                overflow,
                density,
                layoutDirection,
                fontFamilyResolver,
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
         * you pass to the [TextDelegate] constructor or to the [text] property.
         */
        fun paint(canvas: Canvas, textLayoutResult: TextLayoutResult) {
            TextPainter.paint(canvas, textLayoutResult)
        }
    }
}