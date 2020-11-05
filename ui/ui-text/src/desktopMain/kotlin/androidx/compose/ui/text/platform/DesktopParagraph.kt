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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DesktopPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.SpanStyleRange
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.jetbrains.skija.Paint
import org.jetbrains.skija.paragraph.BaselineMode
import org.jetbrains.skija.paragraph.LineMetrics
import org.jetbrains.skija.paragraph.ParagraphBuilder
import org.jetbrains.skija.paragraph.ParagraphStyle
import org.jetbrains.skija.paragraph.PlaceholderAlignment
import org.jetbrains.skija.paragraph.PlaceholderStyle
import org.jetbrains.skija.paragraph.RectHeightMode
import org.jetbrains.skija.paragraph.RectWidthMode
import org.jetbrains.skija.paragraph.TextBox
import java.lang.UnsupportedOperationException
import java.nio.charset.Charset
import kotlin.math.floor
import org.jetbrains.skija.Rect as SkRect
import org.jetbrains.skija.paragraph.Paragraph as SkParagraph
import org.jetbrains.skija.paragraph.TextStyle as SkTextStyle
import org.jetbrains.skija.FontStyle as SkFontStyle
import org.jetbrains.skija.paragraph.DecorationLineStyle as SkDecorationLineStyle
import org.jetbrains.skija.paragraph.DecorationStyle as SkDecorationStyle
import org.jetbrains.skija.paragraph.Shadow as SkShadow

private val DefaultFontSize = 16.sp

internal actual fun ActualParagraph(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    maxLines: Int,
    ellipsis: Boolean,
    width: Float,
    density: Density,
    resourceLoader: Font.ResourceLoader
): Paragraph = DesktopParagraph(
    DesktopParagraphIntrinsics(
        text,
        style,
        spanStyles,
        placeholders,
        density,
        resourceLoader
    ),
    maxLines,
    ellipsis,
    width
)

@Suppress("UNUSED_PARAMETER")
internal actual fun ActualParagraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    maxLines: Int,
    ellipsis: Boolean,
    width: Float
): Paragraph = DesktopParagraph(
    paragraphIntrinsics as DesktopParagraphIntrinsics,
    maxLines,
    ellipsis,
    width
)

internal class DesktopParagraph(
    intrinsics: ParagraphIntrinsics,
    val maxLines: Int,
    val ellipsis: Boolean,
    override val width: Float
) : Paragraph {

    val paragraphIntrinsics = intrinsics as DesktopParagraphIntrinsics

    /**
     * Paragraph isn't always immutable, it could be changed via [paint] method without
     * rerunning layout
     */
    val para: SkParagraph
        get() = paragraphIntrinsics.para

    init {
        if (resetMaxLinesIfNeeded()) {
            rebuildParagraph()
        }
        para.layout(width)
    }

    private val text: String
        get() = paragraphIntrinsics.text

    override val height: Float
        get() = para.getHeight()

    override val minIntrinsicWidth: Float
        get() = paragraphIntrinsics.minIntrinsicWidth

    override val maxIntrinsicWidth: Float
        get() = paragraphIntrinsics.maxIntrinsicWidth

    override val firstBaseline: Float
        get() = para.getLineMetrics().firstOrNull()?.run { baseline.toFloat() } ?: 0f

    override val lastBaseline: Float
        get() = para.getLineMetrics().lastOrNull()?.run { baseline.toFloat() } ?: 0f

    override val didExceedMaxLines: Boolean
        get() = para.didExceedMaxLines()

    override val lineCount: Int
        get() = para.lineNumber.toInt()

    override val placeholderRects: List<Rect?>
        get() =
            para.rectsForPlaceholders.map {
                it.rect.toComposeRect()
            }

    override fun getPathForRange(start: Int, end: Int): Path {
        val boxes = para.getRectsForRange(
            start,
            end,
            RectHeightMode.MAX,
            RectWidthMode.MAX
        )
        val path = DesktopPath()
        for (b in boxes) {
            path.internalPath.addRect(b.rect)
        }
        return path
    }

    private val cursorWidth = 2.0f
    override fun getCursorRect(offset: Int) =
        getBoxForwardByOffset(offset)?.let { box ->
            Rect(box.rect.left, box.rect.top, box.rect.left + cursorWidth, box.rect.bottom)
        } ?: getBoxBackwardByOffset(offset)?.let { box ->
            Rect(box.rect.right, box.rect.top, box.rect.right + cursorWidth, box.rect.bottom)
        } ?: Rect(0f, 0f, cursorWidth, para.height)

    override fun getLineLeft(lineIndex: Int): Float {
        println("Paragraph.getLineLeft $lineIndex")
        return 0.0f
    }

    override fun getLineRight(lineIndex: Int): Float {
        println("Paragraph.getLineRight $lineIndex")
        return 0.0f
    }

    override fun getLineTop(lineIndex: Int) =
        para.lineMetrics.getOrNull(lineIndex)?.let { line ->
            floor((line.baseline - line.ascent).toFloat())
        } ?: 0f

    override fun getLineBottom(lineIndex: Int) =
        para.lineMetrics.getOrNull(lineIndex)?.let { line ->
            floor((line.baseline + line.descent).toFloat())
        } ?: 0f

    private fun lineMetricsForOffset(offset: Int): LineMetrics? {
        // For some reasons SkParagraph Line metrics use (UTF-8) byte offsets for start and end
        // indexes
        val byteOffset = text.substring(0, offset).toByteArray(Charset.forName("UTF-8")).size
        val metrics = para.lineMetrics
        for (line in metrics) {
            if (byteOffset < line.endIndex) {
                return line
            }
        }
        return metrics.last()
    }

    override fun getLineHeight(lineIndex: Int) = para.lineMetrics[lineIndex].height.toFloat()

    override fun getLineWidth(lineIndex: Int) = para.lineMetrics[lineIndex].width.toFloat()

    override fun getLineStart(lineIndex: Int) = para.lineMetrics[lineIndex].startIndex.toInt()

    override fun getLineEnd(lineIndex: Int) = para.lineMetrics[lineIndex].endIndex.toInt()

    override fun getLineVisibleEnd(lineIndex: Int) =
        para.lineMetrics[lineIndex].endExcludingWhitespaces.toInt()

    override fun isLineEllipsized(lineIndex: Int) = false

    override fun getLineForOffset(offset: Int) =
        lineMetricsForOffset(offset)?.run { lineNumber.toInt() }
            ?: 0

    override fun getLineForVerticalPosition(vertical: Float): Int {
        println("Paragraph.getLineForVerticalPosition $vertical")
        return 0
    }

    override fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float {
        return if (usePrimaryDirection) {
            getHorizontalPositionForward(offset) ?: getHorizontalPositionBackward(offset) ?: 0f
        } else {
            getHorizontalPositionBackward(offset) ?: getHorizontalPositionForward(offset) ?: 0f
        }
    }

    private fun getBoxForwardByOffset(offset: Int): TextBox? {
        var to = offset + 1
        while (to <= text.length) {
            val box = para.getRectsForRange(
                offset, to,
                RectHeightMode.STRUT, RectWidthMode.TIGHT
            ).firstOrNull()
            if (box != null) {
                return box
            }
            to += 1
        }
        return null
    }

    private fun getBoxBackwardByOffset(offset: Int): TextBox? {
        var from = offset - 1
        while (from >= 0) {
            val box = para.getRectsForRange(
                from, offset,
                RectHeightMode.STRUT, RectWidthMode.TIGHT
            ).firstOrNull()
            when {
                (box == null) -> from -= 1
                (text.get(from) == '\n') -> {
                    val bottom = box.rect.bottom + box.rect.bottom - box.rect.top
                    val rect = SkRect(0f, box.rect.bottom, 0f, bottom)
                    return TextBox(rect, box.direction)
                }
                else -> return box
            }
        }
        return null
    }

    private fun getHorizontalPositionForward(from: Int) =
        getBoxForwardByOffset(from)?.rect?.left

    private fun getHorizontalPositionBackward(to: Int) =
        getBoxBackwardByOffset(to)?.rect?.right

    override fun getParagraphDirection(offset: Int): ResolvedTextDirection =
        ResolvedTextDirection.Ltr

    override fun getBidiRunDirection(offset: Int): ResolvedTextDirection =
        ResolvedTextDirection.Ltr

    override fun getOffsetForPosition(position: Offset): Int {
        return para.getGlyphPositionAtCoordinate(position.x, position.y).position
    }

    override fun getBoundingBox(offset: Int) =
        getBoxForwardByOffset(offset)!!.rect.toComposeRect()

    override fun getWordBoundary(offset: Int) = para.getWordBoundary(offset).let {
        TextRange(it.start, it.end)
    }

    override fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?
    ) {
        var toRebuild = false
        var currentColor = paragraphIntrinsics.builder.textStyle.color
        var currentShadow = paragraphIntrinsics.builder.textStyle.shadow
        var currentTextDecoration = paragraphIntrinsics.builder.textStyle.textDecoration
        if (color.isSpecified && color != currentColor) {
            toRebuild = true
            currentColor = color
        }

        if (shadow != currentShadow) {
            toRebuild = true
            currentShadow = shadow
        }

        if (textDecoration != currentTextDecoration) {
            toRebuild = true
            currentTextDecoration = textDecoration
        }

        if (resetMaxLinesIfNeeded()) {
            toRebuild = true
        }

        if (toRebuild) {
            paragraphIntrinsics.builder.textStyle =
                paragraphIntrinsics.builder.textStyle.copy(
                    color = currentColor,
                    shadow = currentShadow,
                    textDecoration = currentTextDecoration
                )
            rebuildParagraph()
            para.layout(width)
        }
        para.paint(canvas.nativeCanvas, 0.0f, 0.0f)
    }

    fun resetMaxLinesIfNeeded(): Boolean {
        if (maxLines != paragraphIntrinsics.builder.maxLines) {
            paragraphIntrinsics.builder.maxLines = maxLines
            paragraphIntrinsics.builder.ellipsis = if (ellipsis) "\u2026" else ""
            return true
        } else {
            return false
        }
    }

    fun rebuildParagraph() {
        paragraphIntrinsics.para = paragraphIntrinsics.builder.build()
    }
}

internal class ParagraphBuilder(
    val fontLoader: FontLoader,
    val text: String,
    var textStyle: TextStyle,
    var ellipsis: String = "",
    var maxLines: Int = Int.MAX_VALUE,
    spanStyles: List<SpanStyleRange>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    val density: Density
) {
    private val cuts = makeCuts(spanStyles, placeholders)

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
    fun build(): SkParagraph {
        var pos = 0
        val ps = textStyleToParagraphStyle(textStyle)

        if (maxLines != Int.MAX_VALUE) {
            ps.maxLinesCount = maxLines.toLong()
            ps.ellipsis = ellipsis
        }

        val pb = ParagraphBuilder(ps, fontLoader.fonts)

        val currentStyles = mutableListOf(Pair(0, textStyle.toSpanStyle()))

        var addText = true
        var currentStyle: SkTextStyle? = null
        for (cut in cuts) {
            if (addText) {
                pb.addText(text.subSequence(pos, cut.position).toString())
            }
            pb.popStyle()

            when (cut) {
                is Cut.StyleAdd -> currentStyles.add(Pair(cut.priority, cut.style))
                is Cut.StyleRemove -> currentStyles.remove(Pair(cut.priority, cut.style))
                is Cut.PutPlaceholder -> {
                    val placeholderStyle = PlaceholderStyle(
                        calcFontSize(cut.placeholder.width, currentStyle),
                        calcFontSize(cut.placeholder.height, currentStyle),
                        cut.placeholder.placeholderVerticalAlign.toSkPlaceholderAlignment(),
                        // TODO: figure out how exactly we have to work with BaselineMode & offset
                        BaselineMode.ALPHABETIC,
                        0f
                    )

                    pb.addPlaceholder(placeholderStyle)
                    addText = false
                }
                is Cut.EndPlaceholder -> {
                    addText = true
                }
            }

            textStylesToSkStyle(currentStyles)?.let { ts ->
                pb.pushStyle(ts)
                currentStyle = ts
            }
            pos = cut.position
        }

        if (addText) {
            pb.addText(text.subSequence(pos, text.length).toString())
        }

        return pb.build()
    }

    private fun calcFontSize(units: TextUnit, currentStyle: SkTextStyle?): Float {
        with(density) {
            return when {
                units.isSp -> units.toPx()
                units.isInherit -> currentStyle?.fontSize ?: DefaultFontSize.toPx()
                units.isEm -> {
                    val currentFontSize: Float? = currentStyle?.fontSize
                    (currentFontSize ?: DefaultFontSize.toPx()) * units.value
                }
                else -> throw UnsupportedOperationException()
            }
        }
    }

    private sealed class Cut {
        abstract val position: Int

        data class StyleAdd(
            override val position: Int,
            val priority: Int,
            val style: SpanStyle
        ) : Cut()

        data class StyleRemove(
            override val position: Int,
            val priority: Int,
            val style: SpanStyle
        ) : Cut()

        data class PutPlaceholder(override val position: Int, val placeholder: Placeholder) : Cut()
        data class EndPlaceholder(override val position: Int) : Cut()
    }

    private fun makeCuts(
        spans: List<SpanStyleRange>,
        placeholders: List<AnnotatedString.Range<Placeholder>>
    ): List<Cut> {
        val positions = mutableMapOf<Int, MutableList<Cut>>()
        for ((i, span) in spans.withIndex()) {
            val positionsStart = positions.getOrPut(span.start) { mutableListOf() }
            positionsStart.add(Cut.StyleAdd(span.start, i, span.item))
            val positionsEnd = positions.getOrPut(span.end) { mutableListOf() }
            positionsEnd.add(Cut.StyleRemove(span.end, i, span.item))
        }

        for (placeholder in placeholders) {
            val positionsStart = positions.getOrPut(placeholder.start) { mutableListOf() }
            positionsStart.add(Cut.PutPlaceholder(placeholder.start, placeholder.item))
            val positionsEnd = positions.getOrPut(placeholder.start) { mutableListOf() }
            positionsEnd.add(Cut.EndPlaceholder(placeholder.end))
        }

        val cuts = ArrayList<Cut>(positions.size)

        for (v in positions.toSortedMap().values) {
            cuts.addAll(v)
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
        if (from.color != Color.Unspecified) {
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
        if (from.background != Color.Unspecified) {
            to.background = Paint().apply {
                color = from.background.toArgb()
            }
        }
        from.fontWeight?.let {
            to.fontStyle = to.fontStyle.withWeight(it.weight)
        }
        from.shadow?.let {
            to.addShadow(it.toSkShadow())
        }

        to.setFontSize(calcFontSize(from.fontSize, to))
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
        FontStyle.Normal -> org.jetbrains.skija.FontStyle.NORMAL
        FontStyle.Italic -> org.jetbrains.skija.FontStyle.ITALIC
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

fun PlaceholderVerticalAlign.toSkPlaceholderAlignment(): PlaceholderAlignment {
    return when (this) {
        PlaceholderVerticalAlign.AboveBaseline -> PlaceholderAlignment.ABOVE_BASELINE
        PlaceholderVerticalAlign.TextTop -> PlaceholderAlignment.TOP
        PlaceholderVerticalAlign.TextBottom -> PlaceholderAlignment.BOTTOM
        PlaceholderVerticalAlign.TextCenter -> PlaceholderAlignment.MIDDLE

        // TODO: figure out how we have to handle it properly
        PlaceholderVerticalAlign.Top -> PlaceholderAlignment.TOP
        PlaceholderVerticalAlign.Bottom -> PlaceholderAlignment.BOTTOM
        PlaceholderVerticalAlign.Center -> PlaceholderAlignment.MIDDLE
    }
}

fun Shadow.toSkShadow(): SkShadow {
    return SkShadow(color.toArgb(), offset.x, offset.y, blurRadius.toDouble())
}