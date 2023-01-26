/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.newtext.text.modifiers

import androidx.compose.foundation.newtext.text.DefaultMinLines
import androidx.compose.foundation.newtext.text.ceilToIntPx
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain

internal class MultiParagraphLayoutCache(
    private var text: AnnotatedString,
    private var style: TextStyle,
    private var fontFamilyResolver: FontFamily.Resolver,
    private var overflow: TextOverflow = TextOverflow.Clip,
    private var softWrap: Boolean = true,
    private var maxLines: Int = Int.MAX_VALUE,
    private var minLines: Int = DefaultMinLines,
    private var placeholders: List<AnnotatedString.Range<Placeholder>>? = null,
) {
    private var minMaxLinesCoercer: MinMaxLinesCoercer? = null
    internal var density: Density? = null
        set(value) {
            val localField = field
            if (value == null || localField == null) {
                field = value
                return
            }

            if (localField.density != value.density || localField.fontScale != value.fontScale) {
                field = value
                // none of our results are correct if density changed
                markDirty()
            }
        }

    /*@VisibleForTesting*/
    // NOTE(text-perf-review): it seems like TextDelegate essentially guarantees that we use
    // MultiParagraph. Can we have a fast-path that uses just Paragraph in simpler cases (ie,
    // String)?
    private var paragraphIntrinsics: MultiParagraphIntrinsics? = null

    private var intrinsicsLayoutDirection: LayoutDirection? = null

    private var layoutCache: TextLayoutResult? = null
    private var cachedIntrinsicHeight: Pair<Int, Int>? = null

    private val nonNullIntrinsics: MultiParagraphIntrinsics
        get() = paragraphIntrinsics ?: throw IllegalStateException(
            "MeasureScope.measure() must be called first to query text intrinsics"
        )

    /**
     * The width for text if all soft wrap opportunities were taken.
     *
     * Valid only after [layoutWithConstraints] has been called.
     */
    val minIntrinsicWidth: Int get() = nonNullIntrinsics.minIntrinsicWidth.ceilToIntPx()

    /**
     * The width at which increasing the width of the text no lonfger decreases the height.
     *
     * Valid only after [layoutWithConstraints] has been called.
     */
    val maxIntrinsicWidth: Int get() = nonNullIntrinsics.maxIntrinsicWidth.ceilToIntPx()

    val layout: TextLayoutResult
        get() = layoutCache
            ?: throw IllegalStateException("You must call doLayoutInConstraints first")

    val layoutOrNull: TextLayoutResult?
        get() = layoutCache

    /**
     * Update layout constraints for this text
     *
     * @return true if constraints caused a text layout invalidation
     */
    fun layoutWithConstraints(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Boolean {
        if (!layoutCache.newLayoutWillBeDifferent(constraints, layoutDirection)) {
            return false
        }
        val finalConstraints = if (maxLines != Int.MAX_VALUE || minLines > 1) {
            val localMinMax = MinMaxLinesCoercer.from(
                minMaxLinesCoercer,
                layoutDirection,
                style,
                density!!,
                fontFamilyResolver
            ).also {
                minMaxLinesCoercer = it
            }
            localMinMax.coerceMaxMinLines(
                inConstraints = constraints,
                minLines = minLines,
                maxLines = maxLines
            )
        } else {
            constraints
        }
        val multiParagraph = layoutText(finalConstraints, layoutDirection)

        val size = finalConstraints.constrain(
            IntSize(
                multiParagraph.width.ceilToIntPx(),
                multiParagraph.height.ceilToIntPx()
            )
        )

        layoutCache = TextLayoutResult(
            TextLayoutInput(
                text,
                style,
                placeholders.orEmpty(),
                maxLines,
                softWrap,
                overflow,
                density!!,
                layoutDirection,
                fontFamilyResolver,
                finalConstraints
            ),
            multiParagraph,
            size
        )
        return true
    }

    fun intrinsicHeightAt(width: Int, layoutDirection: LayoutDirection): Int {
        cachedIntrinsicHeight?.let { (prevWidth, prevHeight) ->
            if (width == prevWidth) return prevHeight
        }
        val result = layoutText(
            Constraints(0, width, 0, Constraints.Infinity),
            layoutDirection
        ).height.ceilToIntPx()

        cachedIntrinsicHeight = width to result
        return result
    }

    fun update(
        text: AnnotatedString,
        style: TextStyle,
        fontFamilyResolver: FontFamily.Resolver,
        overflow: TextOverflow,
        softWrap: Boolean,
        maxLines: Int,
        minLines: Int,
        placeholders: List<AnnotatedString.Range<Placeholder>>?
    ) {
        this.text = text
        this.style = style
        this.fontFamilyResolver = fontFamilyResolver
        this.overflow = overflow
        this.softWrap = softWrap
        this.maxLines = maxLines
        this.minLines = minLines
        this.placeholders = placeholders
        markDirty()
    }

    private fun setLayoutDirection(layoutDirection: LayoutDirection) {
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
                density = density!!,
                fontFamilyResolver = fontFamilyResolver,
                placeholders = placeholders.orEmpty()
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
        setLayoutDirection(layoutDirection)

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
        val finalMaxLines = if (overwriteMaxLines) 1 else maxLines.coerceAtLeast(1)

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

    private fun TextLayoutResult?.newLayoutWillBeDifferent(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Boolean {
        // no layout yet
        if (this == null) return true

        // async typeface changes
        if (this.multiParagraph.intrinsics.hasStaleResolvedFonts) return true

        // layout direction changed
        if (layoutDirection != layoutInput.layoutDirection) return true

        // if we were passed identical constraints just skip more work
        if (constraints == layoutInput.constraints) return false

        // only be clever if we can predict line break behavior exactly, which is only possible with
        // simple geometry math for the greedy layout case
        if (style.lineBreak != LineBreak.Simple) {
            return true
        }

        // see if width would produce the same wraps (greedy wraps only)
        val canWrap = softWrap && maxLines > 1
        if (canWrap && size.width != multiParagraph.maxIntrinsicWidth.ceilToIntPx()) {
            // some soft wrapping happened, check to see if we're between the previous measure and
            // the next wrap
            val prevActualMaxWidth = maxWidth(layoutInput.constraints)
            val newMaxWidth = maxWidth(constraints)
            if (newMaxWidth > prevActualMaxWidth) {
                // we've grown the potential layout area, and may break longer lines
                return true
            }
            if (newMaxWidth <= size.width) {
                // it's possible to shrink this text (possible opt: check minIntrinsicWidth
                return true
            }
        }

        // check any constraint width changes for single line text
        if (!canWrap &&
            (constraints.maxWidth != layoutInput.constraints.maxWidth ||
                (constraints.minWidth != layoutInput.constraints.minWidth))) {
            // no soft wrap and width is different, always invalidate
            return true
        }

        // if we get here width won't change, height may be clipped
        if (constraints.maxHeight < multiParagraph.height) {
            // vertical clip changes
            return true
        }

        // breaks can't change, height can't change
        return false
    }

    private fun maxWidth(constraints: Constraints): Int {
        val minWidth = constraints.minWidth
        val widthMatters = softWrap || overflow == TextOverflow.Ellipsis
        val maxWidth = if (widthMatters && constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            Constraints.Infinity
        }
        return if (minWidth == maxWidth) {
            maxWidth
        } else {
            maxIntrinsicWidth.coerceIn(minWidth, maxWidth)
        }
    }

    private fun markDirty() {
        paragraphIntrinsics = null
        layoutCache = null
    }
}