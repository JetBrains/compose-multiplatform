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
import androidx.compose.ui.graphics.useOrElse
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.jetbrains.skija.Paint
import org.jetbrains.skija.paragraph.Alignment as SkAlignment
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
import java.util.WeakHashMap
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

    override fun getLineEnd(lineIndex: Int, visibleEnd: Boolean) =
        if (visibleEnd) {
            para.lineMetrics[lineIndex].endExcludingWhitespaces.toInt()
        } else {
            para.lineMetrics[lineIndex].endIndex.toInt()
        }

    @Deprecated(
        "This function will be removed.",
        replaceWith = ReplaceWith(
            "getLineEnd(lineIndex, true)",
            "androidx.compose.ui.text.platform"
        )
    )
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

// Building of SkTextStyle is a relatively expensive operation. We enable simple caching by
// mapping SpanStyle to SkTextStyle. To increase the efficiency of this mapping we are making
// most of the computations before converting Compose paragraph styles to Skia paragraph
private val skTextStylesCache = WeakHashMap<SpanStyle, SkTextStyle>()

internal class ParagraphBuilder(
    val fontLoader: FontLoader,
    val text: String,
    var textStyle: TextStyle,
    var ellipsis: String = "",
    var maxLines: Int = Int.MAX_VALUE,
    val spanStyles: List<SpanStyleRange>,
    val placeholders: List<AnnotatedString.Range<Placeholder>>,
    val density: Density
) {
    private lateinit var cuts: List<Cut>
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
        val cuts = makeCuts(
            spanStyles,
            placeholders,
            textStyle.toSpanStyle().withDefaultFontSize()
        )

        var pos = 0
        val ps = textStyleToParagraphStyle(textStyle)

        if (maxLines != Int.MAX_VALUE) {
            ps.maxLinesCount = maxLines.toLong()
            ps.ellipsis = ellipsis
        }

        val pb = ParagraphBuilder(ps, fontLoader.fonts)

        var addText = true

        for (cut in cuts) {
            if (addText && pos < cut.position) {
                pb.addText(text.subSequence(pos, cut.position).toString())
            }

            when (cut) {
                is Cut.StyleAdd -> {
                    pb.pushStyle(makeSkTextStyle(cut.style))
                }
                is Cut.PutPlaceholder -> {
                    val placeholderStyle =
                        with(density) {
                            PlaceholderStyle(
                                cut.width!!.toPx(),
                                cut.height!!.toPx(),
                                cut.placeholder.placeholderVerticalAlign.toSkPlaceholderAlignment(),
                                // TODO: figure out how exactly we have to work with BaselineMode & offset
                                BaselineMode.ALPHABETIC,
                                0f
                            )
                        }
                    pb.addPlaceholder(placeholderStyle)
                    addText = false
                }
                is Cut.EndPlaceholder -> {
                    addText = true
                }
            }

            pos = cut.position
        }

        if (addText && pos < text.length) {
            pb.addText(text.subSequence(pos, text.length).toString())
        }

        return pb.build()
    }

    private sealed class Cut {
        abstract val position: Int

        data class StyleAdd(
            override val position: Int,
            var style: SpanStyle
        ) : Cut()

        data class StyleRemove(
            override val position: Int,
            val style: SpanStyle
        ) : Cut()

        data class PutPlaceholder(
            override val position: Int,
            val placeholder: Placeholder,
            var width: TextUnit? = null,
            var height: TextUnit? = null
        ) : Cut()

        data class EndPlaceholder(override val position: Int) : Cut()
    }

    private fun makeCuts(
        spans: List<SpanStyleRange>,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
        initialStyle: SpanStyle
    ): List<Cut> {
        val rawCuts = mutableListOf<Cut>()
        for (span in spans) {
            rawCuts.add(Cut.StyleAdd(span.start, span.item))
            rawCuts.add(Cut.StyleRemove(span.end, span.item))
        }

        for (placeholder in placeholders) {
            rawCuts.add(Cut.PutPlaceholder(placeholder.start, placeholder.item))
            rawCuts.add(Cut.EndPlaceholder(placeholder.end))
        }

        val cuts = mutableListOf<Cut>(Cut.StyleAdd(0, initialStyle))
        rawCuts.sortBy { it.position }
        val activeStyles = mutableListOf(initialStyle)
        for (rawCut in rawCuts) {
            when {
                rawCut is Cut.StyleAdd -> {
                    activeStyles.add(rawCut.style)
                    val prev = previousStyleAddAtTheSamePosition(rawCut.position, cuts)
                    if (prev == null) {
                        rawCut.style = mergeStyle(mergeStyles(activeStyles), rawCut.style)
                        cuts.add(rawCut)
                    } else {
                        prev.style = mergeStyle(prev.style, rawCut.style)
                    }
                }
                rawCut is Cut.StyleRemove -> {
                    activeStyles.remove(rawCut.style)
                    cuts.add(Cut.StyleAdd(rawCut.position, mergeStyles(activeStyles)))
                }
                rawCut is Cut.PutPlaceholder -> {
                    val currentStyle = mergeStyles(activeStyles)
                    rawCut.width =
                        fontSizeInHierarchy(currentStyle.fontSize, rawCut.placeholder.width)
                    rawCut.height =
                        fontSizeInHierarchy(currentStyle.fontSize, rawCut.placeholder.height)
                    cuts.add(rawCut)
                }
                else -> cuts.add(rawCut)
            }
        }
        return cuts
    }

    private fun mergeStyles(activeStyles: List<SpanStyle>): SpanStyle {
        // there is always at least one active style
        var accStyle = activeStyles[0]
        for (i in 1 until activeStyles.size) {
            accStyle = mergeStyle(accStyle, activeStyles[i])
        }
        return accStyle
    }

    private fun fontSizeInHierarchy(base: TextUnit, other: TextUnit): TextUnit {
        return when {
            other.isUnspecified -> base
            other.isEm -> base * other.value
            other.isSp -> other
            else -> throw UnsupportedOperationException()
        }
    }

    // It's almost the same as [SpanStyle.merge] but handles fontSize and letterSpacing with
    // respect to hierarchy
    private fun mergeStyle(style: SpanStyle, other: SpanStyle): SpanStyle {
        val fontSize = fontSizeInHierarchy(style.fontSize, other.fontSize)
        return SpanStyle(
            color = other.color.useOrElse { style.color },
            fontFamily = other.fontFamily ?: style.fontFamily,
            fontSize = fontSize,
            fontWeight = other.fontWeight ?: style.fontWeight,
            fontStyle = other.fontStyle ?: style.fontStyle,
            fontSynthesis = other.fontSynthesis ?: style.fontSynthesis,
            fontFeatureSettings = other.fontFeatureSettings ?: style.fontFeatureSettings,
            letterSpacing = when {
                other.letterSpacing.isEm -> fontSize * other.letterSpacing.value
                else -> other.letterSpacing
            },
            baselineShift = other.baselineShift ?: style.baselineShift,
            textGeometricTransform = other.textGeometricTransform ?: style.textGeometricTransform,
            localeList = other.localeList ?: style.localeList,
            background = other.background.useOrElse { style.background },
            textDecoration = other.textDecoration ?: style.textDecoration,
            shadow = other.shadow ?: style.shadow
        )
    }

    private fun previousStyleAddAtTheSamePosition(position: Int, cuts: List<Cut>): Cut.StyleAdd? {
        for (prevCut in cuts.asReversed()) {
            if (prevCut.position < position) return null
            if (prevCut is Cut.StyleAdd) return prevCut
        }
        return null
    }

    private fun textStyleToParagraphStyle(style: TextStyle): ParagraphStyle {
        val pStyle = ParagraphStyle()
        style.textAlign?.let {
            pStyle.alignment = it.toSkAlignment()
        }
        return pStyle
    }

    private fun makeSkTextStyle(style: SpanStyle): SkTextStyle {
        return skTextStylesCache.getOrPut(style) {
            style.toSkTextStyle(density, fontLoader)
        }
    }
}

private fun SpanStyle.withDefaultFontSize(): SpanStyle {
    return when {
        this.fontSize.isUnspecified -> this.copy(fontSize = DefaultFontSize)
        this.fontSize.isEm -> this.copy(fontSize = DefaultFontSize * this.fontSize.value)
        else -> this
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

fun TextAlign.toSkAlignment(): SkAlignment {
    return when (this) {
        TextAlign.Left -> SkAlignment.LEFT
        TextAlign.Right -> SkAlignment.RIGHT
        TextAlign.Center -> SkAlignment.CENTER
        TextAlign.Justify -> SkAlignment.JUSTIFY
        TextAlign.Start -> SkAlignment.START
        TextAlign.End -> SkAlignment.END
    }
}

private fun SpanStyle.toSkTextStyle(density: Density, fontLoader: FontLoader): SkTextStyle {
    val res = SkTextStyle()
    if (this.color != Color.Unspecified) {
        res.color = this.color.toArgb()
    }
    this.fontFamily?.let {
        val fontFamilies = fontLoader.ensureRegistered(it)
        res.setFontFamilies(fontFamilies.toTypedArray())
    }
    this.fontStyle?.let {
        res.fontStyle = it.toSkFontStyle()
    }
    this.textDecoration?.let {
        res.decorationStyle = it.toSkDecorationStyle(this.color)
    }
    if (this.background != Color.Unspecified) {
        res.background = Paint().also {
            it.color = this.background.toArgb()
        }
    }
    this.fontWeight?.let {
        res.fontStyle = res.fontStyle.withWeight(it.weight)
    }
    this.shadow?.let {
        res.addShadow(it.toSkShadow())
    }

    if (!this.letterSpacing.isUnspecified) {
        res.letterSpacing = with(density) {
            this@toSkTextStyle.letterSpacing.toPx()
        }
    }

    res.fontSize = with(density) {
        this@toSkTextStyle.fontSize.toPx()
    }
    return res
}
