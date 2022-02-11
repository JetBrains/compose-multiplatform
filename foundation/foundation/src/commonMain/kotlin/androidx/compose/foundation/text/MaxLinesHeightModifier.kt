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

/**
 * Constraint the height of the text field so that it vertically occupies no more than [maxLines]
 * number of lines.
 */
internal fun Modifier.maxLinesHeight(
    /*@IntRange(from = 1)*/
    maxLines: Int,
    textStyle: TextStyle
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "maxLinesHeight"
        properties["maxLines"] = maxLines
        properties["textStyle"] = textStyle
    }
) {
    require(maxLines > 0) {
        "maxLines must be greater than 0"
    }
    if (maxLines == Int.MAX_VALUE) return@composed Modifier

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
    val precomputedMaxLinesHeight = firstLineHeight + lineHeight * (maxLines - 1)

    Modifier.heightIn(
        max = with(density) { precomputedMaxLinesHeight.toDp() }
    )
}