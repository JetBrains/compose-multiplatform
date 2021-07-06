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
package androidx.compose.ui.text.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import org.jetbrains.skija.paragraph.Paragraph
import kotlin.math.ceil

internal actual fun ActualParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<Range<SpanStyle>>,
    placeholders: List<Range<Placeholder>>,
    density: Density,
    resourceLoader: Font.ResourceLoader
): ParagraphIntrinsics =
    DesktopParagraphIntrinsics(
        text,
        style,
        spanStyles,
        placeholders,
        density,
        resourceLoader
    )

internal class DesktopParagraphIntrinsics(
    val text: String,
    style: TextStyle,
    spanStyles: List<Range<SpanStyle>>,
    placeholders: List<Range<Placeholder>>,
    density: Density,
    resourceLoader: Font.ResourceLoader
) : ParagraphIntrinsics {

    private val fontLoader = resourceLoader as FontLoader
    val textDirection = resolveTextDirection(style.textDirection)
    val defaultHeight get() = builder.defaultHeight
    private val builder = ParagraphBuilder(
        fontLoader = fontLoader,
        text = text,
        textStyle = style,
        spanStyles = spanStyles,
        placeholders = placeholders,
        density = density,
        textDirection = textDirection
    )
    private var para = builder.build()
    private var width = Float.POSITIVE_INFINITY

    init {
        para.layout(width)
    }

    fun layoutParagraph(
        width: Float = this.width,
        maxLines: Int = builder.maxLines,
        ellipsis: String = builder.ellipsis,
        color: Color = builder.textStyle.color,
        shadow: Shadow? = builder.textStyle.shadow,
        textDecoration: TextDecoration? = builder.textStyle.textDecoration,
    ): Paragraph {
        if (
            builder.maxLines != maxLines ||
            builder.ellipsis != ellipsis ||
            (builder.textStyle.color != color && color.isSpecified) ||
            builder.textStyle.shadow != shadow ||
            builder.textStyle.textDecoration != textDecoration
        ) {
            this.width = width
            builder.maxLines = maxLines
            builder.ellipsis = ellipsis
            builder.textStyle = builder.textStyle.copy(
                color = color,
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

    override val minIntrinsicWidth = ceil(para.minIntrinsicWidth)
    override val maxIntrinsicWidth = ceil(para.maxIntrinsicWidth)

    private fun resolveTextDirection(direction: TextDirection?): ResolvedTextDirection {
        return when (direction) {
            TextDirection.Ltr -> ResolvedTextDirection.Ltr
            TextDirection.Rtl -> ResolvedTextDirection.Rtl
            TextDirection.Content -> contentBasedTextDirection() ?: ResolvedTextDirection.Ltr
            TextDirection.ContentOrLtr -> contentBasedTextDirection() ?: ResolvedTextDirection.Ltr
            TextDirection.ContentOrRtl -> contentBasedTextDirection() ?: ResolvedTextDirection.Rtl
            else -> ResolvedTextDirection.Ltr
        }
    }

    private fun contentBasedTextDirection(): ResolvedTextDirection? {
        for (char in text) {
            when (char.directionality) {
                CharDirectionality.LEFT_TO_RIGHT -> return ResolvedTextDirection.Ltr
                CharDirectionality.RIGHT_TO_LEFT -> return ResolvedTextDirection.Rtl
                else -> continue
            }
        }
        return null
    }
}
