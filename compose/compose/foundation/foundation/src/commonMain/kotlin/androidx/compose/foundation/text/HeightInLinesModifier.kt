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

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.unit.Dp

/**
 * The default minimum height in terms of minimum number of visible lines.
 *
 * Should not be used in public API and samples unless it's public, too.
 */
internal const val DefaultMinLines = 1

/**
 * Constraint the height of the TextField or BasicText so that it vertically occupies at least
 * [minLines] number of lines and at most [maxLines] number of lines. BasicText should not use this
 * function for calculating maxLines constraints since MultiParagraph computation already handles
 * that.
 */
internal fun Modifier.heightInLines(
    textStyle: TextStyle,
    minLines: Int = DefaultMinLines,
    maxLines: Int = Int.MAX_VALUE
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "heightInLines"
        properties["minLines"] = minLines
        properties["maxLines"] = maxLines
        properties["textStyle"] = textStyle
    }
) {
    validateMinMaxLines(minLines, maxLines)
    if (minLines == DefaultMinLines && maxLines == Int.MAX_VALUE) return@composed Modifier

    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val layoutDirection = LocalLayoutDirection.current

    // Difference between the height of two lines paragraph and one line paragraph gives us
    // an approximation of height of one line
    val resolvedStyle = remember(textStyle, layoutDirection) {
        resolveDefaults(textStyle, layoutDirection)
    }
    val typeface by remember(fontFamilyResolver, resolvedStyle) {
        fontFamilyResolver.resolve(
            resolvedStyle.fontFamily,
            resolvedStyle.fontWeight ?: FontWeight.Normal,
            resolvedStyle.fontStyle ?: FontStyle.Normal,
            resolvedStyle.fontSynthesis ?: FontSynthesis.All
        )
    }

    val firstLineHeight = remember(
        density,
        fontFamilyResolver,
        textStyle,
        layoutDirection,
        typeface
    ) {
        computeSizeForDefaultText(
            style = resolvedStyle,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            text = EmptyTextReplacement,
            maxLines = 1
        ).height
    }

    val firstTwoLinesHeight = remember(
        density,
        fontFamilyResolver,
        textStyle,
        layoutDirection,
        typeface
    ) {
        val twoLines = EmptyTextReplacement + "\n" + EmptyTextReplacement
        computeSizeForDefaultText(
            style = resolvedStyle,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            text = twoLines,
            maxLines = 2
        ).height
    }
    val lineHeight = firstTwoLinesHeight - firstLineHeight
    val precomputedMinLinesHeight =
        if (minLines == DefaultMinLines) null else firstLineHeight + lineHeight * (minLines - 1)
    val precomputedMaxLinesHeight =
        if (maxLines == Int.MAX_VALUE) null else firstLineHeight + lineHeight * (maxLines - 1)

    with(density) {
        Modifier.heightIn(
            min = precomputedMinLinesHeight?.toDp() ?: Dp.Unspecified,
            max = precomputedMaxLinesHeight?.toDp() ?: Dp.Unspecified
        )
    }
}

internal fun validateMinMaxLines(minLines: Int, maxLines: Int) {
    require(minLines > 0 && maxLines > 0) {
        "both minLines $minLines and maxLines $maxLines must be greater than zero"
    }
    require(minLines <= maxLines) {
        "minLines $minLines must be less than or equal to maxLines $maxLines"
    }
}