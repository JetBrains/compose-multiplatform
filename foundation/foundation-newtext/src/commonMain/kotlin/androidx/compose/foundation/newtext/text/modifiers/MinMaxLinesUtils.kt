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

package androidx.compose.foundation.newtext.text.modifiers

import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

internal fun Constraints.coerceMaxMinLines(
    layoutDirection: LayoutDirection,
    minLines: Int,
    maxLines: Int,
    paramStyle: TextStyle,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
): Constraints {
    val style = resolveDefaults(paramStyle, layoutDirection)
    val oneLine = Paragraph(
        text = EmptyTextReplacement,
        style = style,
        constraints = Constraints(),
        density = density,
        fontFamilyResolver = fontFamilyResolver,
        maxLines = 1,
        ellipsis = false
    ).height

    val twoLines = Paragraph(
        text = TwoLineTextReplacement,
        style = style,
        constraints = Constraints(),
        density = density,
        fontFamilyResolver = fontFamilyResolver,
        maxLines = 2,
        ellipsis = false
    ).height

    val lineHeight = twoLines - oneLine
    val maxHeight = if (maxLines != Int.MAX_VALUE) {
        (oneLine + (lineHeight * (maxLines - 1)))
            .roundToInt()
            .coerceAtLeast(0)
    } else {
        this.maxHeight
    }
    val minHeight = if (minLines != 1) {
        (oneLine + (lineHeight * (minLines - 1)))
            .roundToInt()
            .coerceAtLeast(0)
            .coerceAtMost(maxHeight)
    } else {
        this.minHeight
    }
    return Constraints(
        minHeight = minHeight,
        maxHeight = maxHeight,
        minWidth = minWidth,
        maxWidth = maxWidth,
    )
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