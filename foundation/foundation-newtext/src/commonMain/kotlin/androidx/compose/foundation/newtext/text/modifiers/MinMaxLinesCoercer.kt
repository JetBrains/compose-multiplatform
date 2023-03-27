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

import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

internal class MinMaxLinesCoercer private constructor(
    val layoutDirection: LayoutDirection,
    val inputTextStyle: TextStyle,
    val density: Density,
    val fontFamilyResolver: FontFamily.Resolver
) {
    private val resolvedStyle = resolveDefaults(inputTextStyle, layoutDirection)
    private var lineHeightCache: Float = Float.NaN
    private var oneLineHeightCache: Float = Float.NaN

    companion object {
        // LRU cache of one since this tends to be used for similar styles
        // ... it may be useful to increase this cache if requested by some dev use case
        private var last: MinMaxLinesCoercer? = null

        /**
         * Returns a coercer (possibly cached) with these parameters
         */
        fun from(
            minMaxUtil: MinMaxLinesCoercer?,
            layoutDirection: LayoutDirection,
            paramStyle: TextStyle,
            density: Density,
            fontFamilyResolver: FontFamily.Resolver
        ): MinMaxLinesCoercer {
            minMaxUtil?.let {
                if (layoutDirection == it.layoutDirection &&
                    paramStyle == it.inputTextStyle &&
                    density.density == it.density.density &&
                    fontFamilyResolver === it.fontFamilyResolver) {
                    return it
                }
            }
            last?.let {
                if (layoutDirection == it.layoutDirection &&
                    paramStyle == it.inputTextStyle &&
                    density.density == it.density.density &&
                    fontFamilyResolver === it.fontFamilyResolver) {
                    return it
                }
            }
            return MinMaxLinesCoercer(
                layoutDirection,
                resolveDefaults(paramStyle, layoutDirection),
                density,
                fontFamilyResolver
            ).also {
                last = it
            }
        }
    }

    /**
     * Coerce inConstraints to have min and max lines applied.
     *
     * On first invocation this will cause (2) Paragraph measurements.
     */
    internal fun coerceMaxMinLines(
        inConstraints: Constraints,
        minLines: Int,
        maxLines: Int,
    ): Constraints {
        var oneLineHeight = oneLineHeightCache
        var lineHeight = lineHeightCache
        if (oneLineHeight.isNaN() || lineHeight.isNaN()) {
            oneLineHeight = Paragraph(
                text = EmptyTextReplacement,
                style = resolvedStyle,
                constraints = Constraints(),
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                maxLines = 1,
                ellipsis = false
            ).height

            val twoLineHeight = Paragraph(
                text = TwoLineTextReplacement,
                style = resolvedStyle,
                constraints = Constraints(),
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                maxLines = 2,
                ellipsis = false
            ).height

            lineHeight = twoLineHeight - oneLineHeight
            oneLineHeightCache = oneLineHeight
            lineHeightCache = lineHeight
        }
        val maxHeight = if (maxLines != Int.MAX_VALUE) {
            (oneLineHeight + (lineHeight * (maxLines - 1)))
                .roundToInt()
                .coerceAtLeast(0)
        } else {
            inConstraints.maxHeight
        }
        val minHeight = if (minLines != 1) {
            (oneLineHeight + (lineHeight * (minLines - 1)))
                .roundToInt()
                .coerceAtLeast(0)
                .coerceAtMost(maxHeight)
        } else {
            inConstraints.minHeight
        }
        return Constraints(
            minHeight = minHeight,
            maxHeight = maxHeight,
            minWidth = inConstraints.minWidth,
            maxWidth = inConstraints.maxWidth,
        )
    }
}

private const val DefaultWidthCharCount = 10 // min width for TextField is 10 chars long
private val EmptyTextReplacement = "H".repeat(DefaultWidthCharCount) // just a reference character.
private val TwoLineTextReplacement = EmptyTextReplacement + "\n" + EmptyTextReplacement

internal fun validateMinMaxLines(minLines: Int, maxLines: Int) {
    require(minLines > 0 && maxLines > 0) {
        "both minLines $minLines and maxLines $maxLines must be greater than zero"
    }
    require(minLines <= maxLines) {
        "minLines $minLines must be less than or equal to maxLines $maxLines"
    }
}