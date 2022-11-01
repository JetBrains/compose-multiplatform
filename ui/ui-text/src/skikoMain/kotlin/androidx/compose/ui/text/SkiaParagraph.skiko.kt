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

package androidx.compose.ui.text

import org.jetbrains.skia.Rect as SkRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asSkiaPath
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.text.platform.SkiaParagraphIntrinsics
import androidx.compose.ui.text.platform.cursorHorizontalPosition
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import kotlin.math.floor
import org.jetbrains.skia.paragraph.LineMetrics
import org.jetbrains.skia.paragraph.RectHeightMode
import org.jetbrains.skia.paragraph.RectWidthMode
import org.jetbrains.skia.paragraph.TextBox

internal class SkiaParagraph(
    intrinsics: ParagraphIntrinsics,
    val maxLines: Int,
    val ellipsis: Boolean,
    val constraints: Constraints
) : Paragraph {

    private val ellipsisChar = if (ellipsis) "\u2026" else ""

    private val paragraphIntrinsics = intrinsics as SkiaParagraphIntrinsics

    private val layouter = paragraphIntrinsics.layouter()

    /**
     * Paragraph isn't always immutable, it could be changed via [paint] method without
     * rerunning layout
     */
    private var para = layouter.layoutParagraph(
        width = width,
        maxLines = maxLines,
        ellipsis = ellipsisChar
    )

    init {
        para.layout(width)
    }

    private val text: String
        get() = paragraphIntrinsics.text

    override val width: Float
        get() = constraints.maxWidth.toFloat()

    override val height: Float
        get() = para.height

    override val minIntrinsicWidth: Float
        get() = paragraphIntrinsics.minIntrinsicWidth

    override val maxIntrinsicWidth: Float
        get() = paragraphIntrinsics.maxIntrinsicWidth

    override val firstBaseline: Float
        get() = lineMetrics.firstOrNull()?.run { baseline.toFloat() } ?: 0f

    override val lastBaseline: Float
        get() = lineMetrics.lastOrNull()?.run { baseline.toFloat() } ?: 0f

    override val didExceedMaxLines: Boolean
        get() = para.didExceedMaxLines()

    override val lineCount: Int
        // workaround for https://bugs.chromium.org/p/skia/issues/detail?id=11321
        get() = if (text == "") {
            1
        } else {
            para.lineNumber.toInt()
        }

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
        val path = Path()
        for (b in boxes) {
            path.asSkiaPath().addRect(b.rect)
        }
        return path
    }

    override fun getCursorRect(offset: Int): Rect {
        val horizontal = getHorizontalPosition(offset, true)
        val line = lineMetricsForOffset(offset)!!

        return Rect(
            horizontal,
            (line.baseline - line.ascent).toFloat(),
            horizontal,
            (line.baseline + line.descent).toFloat()
        )
    }

    override fun getLineLeft(lineIndex: Int): Float =
        lineMetrics.getOrNull(lineIndex)?.left?.toFloat() ?: 0f

    override fun getLineRight(lineIndex: Int): Float =
        lineMetrics.getOrNull(lineIndex)?.right?.toFloat() ?: 0f

    override fun getLineTop(lineIndex: Int) =
        lineMetrics.getOrNull(lineIndex)?.let { line ->
            floor((line.baseline - line.ascent).toFloat())
        } ?: 0f

    override fun getLineBottom(lineIndex: Int) =
        lineMetrics.getOrNull(lineIndex)?.let { line ->
            floor((line.baseline + line.descent).toFloat())
        } ?: 0f

    private fun lineMetricsForOffset(offset: Int): LineMetrics? {
        val metrics = lineMetrics
        for (line in metrics) {
            if (offset < line.endIncludingNewline) {
                return line
            }
        }
        if (metrics.isEmpty()) {
            return null
        }
        return metrics.last()
    }

    override fun getLineHeight(lineIndex: Int) = lineMetrics[lineIndex].height.toFloat()

    override fun getLineWidth(lineIndex: Int) = lineMetrics[lineIndex].width.toFloat()

    override fun getLineStart(lineIndex: Int) = lineMetrics[lineIndex].startIndex.toInt()

    override fun getLineEnd(lineIndex: Int, visibleEnd: Boolean) =
        if (visibleEnd) {
            val metrics = lineMetrics[lineIndex]
            // workarounds for https://bugs.chromium.org/p/skia/issues/detail?id=11321 :(
            // we are waiting for fixes
            if (lineIndex > 0 && metrics.startIndex < lineMetrics[lineIndex - 1].endIndex) {
                metrics.endIndex.toInt()
            } else if (
                metrics.startIndex < text.length &&
                text[metrics.startIndex.toInt()] == '\n'
            ) {
                metrics.startIndex.toInt()
            } else {
                metrics.endExcludingWhitespaces.toInt()
            }
        } else {
            lineMetrics[lineIndex].endIndex.toInt()
        }

    override fun isLineEllipsized(lineIndex: Int) = false

    override fun getLineForOffset(offset: Int) =
        lineMetricsForOffset(offset)?.run { lineNumber.toInt() }
            ?: 0

    override fun getLineForVerticalPosition(vertical: Float): Int {
        return 0
    }

    override fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float {
        val prevBox = getBoxBackwardByOffset(offset)
        val nextBox = getBoxForwardByOffset(offset)
        return when {
            prevBox == null && nextBox == null -> 0f
            prevBox == null -> nextBox!!.cursorHorizontalPosition(true)
            nextBox == null -> prevBox.cursorHorizontalPosition()
            nextBox.direction == prevBox.direction -> nextBox.cursorHorizontalPosition(true)
            // BiDi transition offset, we need to resolve ambiguity with usePrimaryDirection
            // for details see comment for MultiParagraph.getHorizontalPosition
            usePrimaryDirection -> prevBox.cursorHorizontalPosition()
            else -> nextBox.cursorHorizontalPosition(true)
        }
    }

    // workaround for https://bugs.chromium.org/p/skia/issues/detail?id=11321 :(
    private val lineMetrics: Array<LineMetrics>
        get() = if (text == "") {
            val height = layouter.defaultHeight.toDouble()
            arrayOf(
                LineMetrics(
                    0, 0, 0, 0, true,
                    height, 0.0, height, height, 0.0, 0.0, height, 0
                )
            )
        } else {
            @Suppress("UNCHECKED_CAST", "USELESS_CAST")
            para.lineMetrics as Array<LineMetrics>
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

    private fun getBoxBackwardByOffset(offset: Int, end: Int = offset): TextBox? {
        var from = offset - 1
        while (from >= 0) {
            val box = para.getRectsForRange(
                from, end,
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

    override fun getParagraphDirection(offset: Int): ResolvedTextDirection =
        paragraphIntrinsics.textDirection

    override fun getBidiRunDirection(offset: Int): ResolvedTextDirection =
        when (getBoxForwardByOffset(offset)?.direction) {
            org.jetbrains.skia.paragraph.Direction.RTL -> ResolvedTextDirection.Rtl
            org.jetbrains.skia.paragraph.Direction.LTR -> ResolvedTextDirection.Ltr
            null -> ResolvedTextDirection.Ltr
        }

    override fun getOffsetForPosition(position: Offset): Int {
        return para.getGlyphPositionAtCoordinate(position.x, position.y).position
    }

    override fun getBoundingBox(offset: Int): Rect {
        val box = getBoxForwardByOffset(offset) ?: getBoxBackwardByOffset(offset, text.length)!!
        return box.rect.toComposeRect()
    }

    override fun getWordBoundary(offset: Int): TextRange {
        return when {
            (text[offset].isLetterOrDigit()) -> para.getWordBoundary(offset).let {
                TextRange(it.start, it.end)
            }
            (text.getOrNull(offset - 1)?.isLetterOrDigit() ?: false) ->
                para.getWordBoundary(offset - 1).let {
                    TextRange(it.start, it.end)
                }
            else -> TextRange(offset, offset)
        }
    }

    override fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?
    ) {
        para = layouter.layoutParagraph(
            width = width,
            maxLines = maxLines,
            ellipsis = ellipsisChar,
            color = color,
            shadow = shadow,
            textDecoration = textDecoration
        )

        para.paint(canvas.nativeCanvas, 0.0f, 0.0f)
    }

    @ExperimentalTextApi
    override fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    ) {
        para = layouter.layoutParagraph(
            width = width,
            maxLines = maxLines,
            ellipsis = ellipsisChar,
            color = color,
            shadow = shadow,
            textDecoration = textDecoration
        )

        para.paint(canvas.nativeCanvas, 0.0f, 0.0f)
    }

    // TODO(b/229518449): Implement this paint function that draws text with a Brush.
    @ExperimentalTextApi
    override fun paint(
        canvas: Canvas,
        brush: Brush,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    ) {
        throw UnsupportedOperationException(
            "Using brush for painting the paragraph is a separate functionality that " +
                "is not supported on this platform")
    }
}
