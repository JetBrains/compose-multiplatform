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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.SpanStyleRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import org.jetbrains.skija.paragraph.DecorationLineStyle as SkDecorationLineStyle
import org.jetbrains.skija.paragraph.Paragraph
import org.jetbrains.skija.paragraph.ParagraphBuilder
import org.jetbrains.skija.paragraph.ParagraphStyle
import kotlin.math.ceil
import org.jetbrains.skija.paragraph.TextStyle as SkTextStyle
import org.jetbrains.skija.paragraph.DecorationStyle as SkDecorationStyle
import org.jetbrains.skija.FontStyle as SkFontStyle
import org.jetbrains.skija.Paint as SkPaint

@Suppress("UNUSED_PARAMETER")
internal actual fun ActualParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
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
    spanStyles: List<SpanStyleRange>,
    @Suppress("UNUSED_PARAMETER") placeholders: List<AnnotatedString.Range<Placeholder>>,
    @Suppress("UNUSED_PARAMETER") density: Density,
    resourceLoader: Font.ResourceLoader
) : ParagraphIntrinsics {

    val fontLoader = resourceLoader as FontLoader
    val para: Paragraph
    init {
        para = buildParagraph(text, style, spanStyles)

        para.layout(Float.POSITIVE_INFINITY)
    }

    override val minIntrinsicWidth = ceil(para.getMinIntrinsicWidth())
    override val maxIntrinsicWidth = ceil(para.getMaxIntrinsicWidth())

    /**
     * SkParagraph styles model doesn't match Compose's one.
     * SkParagraph has only a stack-based push/pop styles interface that works great with Span
     * trees.
     * But in Compose we have a list of SpanStyles attached to arbitrary ranges, possibly
     * overlapped, where a position in the list denotes style's priority
     * We map Compose styles to SkParagraph styles by projecting every range start/end to single
     * positions line and maintaining a list of active styles while building a paragraph. This list
     * of active styles is being compiled into single SkParagraph's style for every chunk of text
    */
    private fun buildParagraph(
        text: String,
        textStyle: TextStyle,
        spanStyles: List<SpanStyleRange>
    ): Paragraph {
        val cuts = spansToCuts(spanStyles)

        var pos = 0
        val ps = textStyleToParagraphStyle(textStyle)
        val pb = ParagraphBuilder(ps, fontLoader.fonts)
        // TODO: for some reasons paragraph style doesn't apply to text. maybe it's Skia bug,
        // we need to investigate
        val currentStyles = mutableListOf(Pair(0, textStyle.toSpanStyle()))
        pb.pushStyle(textStylesToSkStyle(currentStyles)!!)

        for (cut in cuts) {
            pb.addText(text.subSequence(pos, cut.position).toString())
            pb.popStyle()

            when (cut.instruction) {
                StyleInstruction.ADD -> currentStyles.add(Pair(cut.priority, cut.style))
                StyleInstruction.REMOVE -> currentStyles.remove(Pair(cut.priority, cut.style))
            }

            textStylesToSkStyle(currentStyles)?.let { ts ->
                pb.pushStyle(ts)
            }
            pos = cut.position
        }

        pb.addText(text.subSequence(pos, text.length).toString())

        return pb.build()
    }

    private enum class StyleInstruction {
        ADD,
        REMOVE
    }

    private data class Cut(
        val position: Int,
        val priority: Int,
        val style: SpanStyle,
        val instruction: StyleInstruction
    )

    private fun spansToCuts(spans: List<SpanStyleRange>): List<Cut> {
        val positions = mutableMapOf<Int, Cut>()
        for ((i, span) in spans.withIndex()) {
            positions[span.start] = Cut(span.start, i, span.item, StyleInstruction.ADD)
            positions[span.end] = Cut(span.end, i, span.item, StyleInstruction.REMOVE)
        }

        val cuts = ArrayList<Cut>(positions.size)

        for (v in positions.toSortedMap().values) {
            cuts.add(v)
        }
        return cuts
    }

    private fun textStyleToParagraphStyle(style: TextStyle): ParagraphStyle {
        val pStyle = ParagraphStyle()
        val textStyle = SkTextStyle()
        applyStyles(style.toSpanStyle(), textStyle)
        pStyle.setTextStyle(textStyle)
        return pStyle
    }

    private fun applyStyles(from: SpanStyle, to: SkTextStyle) {
        if (from.color != Color.Unset) {
            to.setColor(from.color.toArgb())
        }
        from.fontFamily?.let {
            val fontFamilies = fontLoader.ensureRegistered(it)
            to.setFontFamilies(fontFamilies.toTypedArray())
        }
        from.fontStyle?.let {
            to.fontStyle = it.toSkFontStyle()
        }
        from.textDecoration?.let {
            to.decorationStyle = it.toSkDecorationStyle(from.color)
        }
        to.background = SkPaint().apply {
            color = from.background.toArgb()
        }
        from.fontWeight?.let {
            to.fontStyle = to.fontStyle.withWeight(it.weight)
        }

        // TODO: support [TextUnit.Em]
        if (from.fontSize != TextUnit.Inherit) {
            to.setFontSize(from.fontSize.value)
        }
    }

    private fun textStylesToSkStyle(styles: List<Pair<Int, SpanStyle>>): SkTextStyle? {
        if (styles.isEmpty()) {
            return null
        }
        val skStyle = SkTextStyle()
        for (s in styles.sortedBy { (priority, _) -> priority }.map { (_, v) -> v }) {
            applyStyles(s, skStyle)
        }
        return skStyle
    }
}

fun FontStyle.toSkFontStyle(): SkFontStyle {
    return when (this) {
        FontStyle.Normal -> SkFontStyle.NORMAL
        FontStyle.Italic -> SkFontStyle.ITALIC
    }
}

fun TextDecoration.toSkDecorationStyle(color: Color): SkDecorationStyle {
    val underline = contains(TextDecoration.Underline)
    val overline = false
    val lineThrough = contains(TextDecoration.LineThrough)
    val gaps = false
    val lineStyle = SkDecorationLineStyle.SOLID
    val thicknessMultiplier = 1f
    return SkDecorationStyle(
        underline,
        overline,
        lineThrough,
        gaps,
        color.toArgb(),
        lineStyle,
        thicknessMultiplier
    )
}
