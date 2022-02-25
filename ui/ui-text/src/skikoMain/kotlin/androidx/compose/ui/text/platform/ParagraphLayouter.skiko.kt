/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.text.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.paragraph.Paragraph

/**
 * The purpose of this class is to store already built paragraph and pass it between
 * different internal entities (from SkiaParagraphIntrinsics to SkiaParagraph).
 *
 * An alternative to passing and reusing existed paragraph is to build it again, but it is 2.5x
 * slower.
 *
 * LayoutedParagraph should has only one owner to avoid concurrent usage.
 *
 * Tests:
 *
 * val text = (1..100000).joinToString(" ")
 * reusedParagraph.layout(300f): 116.848500ms
 * builder.build().layout(300f): 288.302300ms
 *
 * text = (1..10000).joinToString(" ")
 * reusedParagraph.layout(300f): 10.004400ms
 * builder.build().layout(300f): 23.421500ms
 */
class ParagraphLayouter(
    val text: String,
    textDirection: ResolvedTextDirection,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
) {
    private val builder = ParagraphBuilder(
        fontFamilyResolver = fontFamilyResolver,
        text = text,
        textStyle = style,
        spanStyles = spanStyles,
        placeholders = placeholders,
        density = density,
        textDirection = textDirection
    )
    private var para = builder.build()

    private var width: Float = -1f

    val defaultHeight get() = builder.defaultHeight
    val defaultFont get() = builder.defaultFont
    val paragraphStyle get() = builder.paragraphStyle

    fun layoutParagraph(
        width: Float = this.width,
        maxLines: Int = builder.maxLines,
        ellipsis: String = builder.ellipsis,
        color: Color = builder.textStyle.color,
        shadow: Shadow? = builder.textStyle.shadow,
        textDecoration: TextDecoration? = builder.textStyle.textDecoration,
    ): Paragraph {
        val actualColor = color.takeOrElse { builder.textStyle.color }
        if (
            builder.maxLines != maxLines ||
            builder.ellipsis != ellipsis ||
            builder.textStyle.color != actualColor ||
            builder.textStyle.shadow != shadow ||
            builder.textStyle.textDecoration != textDecoration
        ) {
            this.width = width
            builder.maxLines = maxLines
            builder.ellipsis = ellipsis
            builder.textStyle = builder.textStyle.copy(
                color = actualColor,
                shadow = shadow,
                textDecoration = textDecoration
            )
            para = builder.build()
            para.layout(width)
        } else if (this.width != width) {
            this.width = width
            para.layout(width)
        }
        return para
    }
}