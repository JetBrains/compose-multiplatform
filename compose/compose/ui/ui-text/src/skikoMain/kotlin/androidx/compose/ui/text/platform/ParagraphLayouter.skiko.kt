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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import kotlin.math.abs
import org.jetbrains.skia.Paint
import org.jetbrains.skia.paragraph.Paragraph

/**
 * The purpose of this class is to store already built paragraph and pass it between
 * different internal entities (from SkiaParagraphIntrinsics to SkiaParagraph).
 *
 * An alternative to passing and reusing existed paragraph is to build it again, but it is 2.5x
 * slower.
 *
 * LayoutedParagraph should have only one owner to avoid concurrent usage.
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
internal class ParagraphLayouter(
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
    private var paragraphCache: Paragraph? = null
    private var width: Float = Float.NaN

    val defaultFont get() = builder.defaultFont
    val paragraphStyle get() = builder.paragraphStyle

    fun setParagraphStyle(
        maxLines: Int,
        ellipsis: String
    ) {
        if (builder.maxLines != maxLines ||
            builder.ellipsis != ellipsis
        ) {
            builder.maxLines = maxLines
            builder.ellipsis = ellipsis
            paragraphCache = null
        }
    }

    fun setTextStyle(
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?
    ) {
        val actualColor = color.takeOrElse { builder.textStyle.color }
        if (builder.textStyle.color != actualColor ||
            builder.textStyle.shadow != shadow ||
            builder.textStyle.textDecoration != textDecoration
        ) {
            builder.textStyle = builder.textStyle.copy(
                color = actualColor,
                shadow = shadow,
                textDecoration = textDecoration
            )
            paragraphCache = null
        }
    }

    @ExperimentalTextApi
    fun setTextStyle(
        brush: Brush?,
        brushSize: Size,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?
    ) {
        val actualSize = builder.brushSize
        if (builder.textStyle.brush != brush ||
            actualSize.isUnspecified ||
            !actualSize.width.sameValueAs(brushSize.width) ||
            !actualSize.height.sameValueAs(brushSize.height) ||
            !builder.textStyle.alpha.sameValueAs(alpha) ||
            builder.textStyle.shadow != shadow ||
            builder.textStyle.textDecoration != textDecoration
        ) {
            builder.textStyle = builder.textStyle.copy(
                brush = brush,
                alpha = alpha,
                shadow = shadow,
                textDecoration = textDecoration
            )
            builder.brushSize = brushSize
            paragraphCache = null
        }
    }

    fun setDrawStyle(drawStyle: DrawStyle?) {
        if (builder.drawStyle != drawStyle) {
            builder.drawStyle = drawStyle
            paragraphCache = null
        }
    }

    fun setBlendMode(blendMode: BlendMode) {
        if (builder.blendMode != blendMode) {
            builder.blendMode = blendMode
            paragraphCache = null
        }
    }

    fun layoutParagraph(width: Float): Paragraph {
        val paragraph = paragraphCache
        return if (paragraph != null) {
            if (!this.width.sameValueAs(width)) {
                this.width = width
                paragraph.layout(width)
            }
            paragraph
        } else {
            builder.build().apply {
                paragraphCache = this
                layout(width)
            }
        }
    }
}

private fun Float.sameValueAs(other: Float) : Boolean {
    return abs(this - other) < 0.00001f
}