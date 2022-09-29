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

package androidx.compose.ui.text.style

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.ceil

@OptIn(ExperimentalTextApi::class)
open class TextLineBreaker {
    private val defaultDensity = Density(1f)
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = createFontFamilyResolver(context)
    private val defaultHyphens = Hyphens.None
    private val defaultLineBreak = LineBreak.Simple

    private fun constructTextLayoutResult(
        text: String,
        textStyle: TextStyle,
        maxWidth: Int = Constraints.Infinity
    ): TextLayoutResult {
        val constraints = Constraints(maxWidth = maxWidth)

        val input = TextLayoutInput(
            text = AnnotatedString(text),
            style = textStyle,
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Visible,
            density = defaultDensity,
            layoutDirection = LayoutDirection.Ltr,
            fontFamilyResolver = fontFamilyResolver,
            constraints = constraints
        )

        val paragraph = MultiParagraph(
            annotatedString = input.text,
            style = input.style,
            constraints = input.constraints,
            density = input.density,
            fontFamilyResolver = input.fontFamilyResolver
        )

        return TextLayoutResult(
            layoutInput = input,
            multiParagraph = paragraph,
            size = constraints.constrain(
                IntSize(
                    ceil(paragraph.width).toInt(),
                    ceil(paragraph.height).toInt()
                )
            )
        )
    }

    fun breakTextIntoLines(
        text: String,
        hyphens: Hyphens = defaultHyphens,
        lineBreak: LineBreak = defaultLineBreak,
        maxWidth: Int
    ): List<String> {
        val layoutResult = constructTextLayoutResult(
            text = text,
            textStyle = TextStyle(hyphens = hyphens, lineBreak = lineBreak),
            maxWidth = maxWidth
        )

        return (0 until layoutResult.lineCount).map { lineIndex ->
            text.substring(
                layoutResult.getLineStart(lineIndex),
                layoutResult.getLineEnd(lineIndex)
            )
        }
    }
}