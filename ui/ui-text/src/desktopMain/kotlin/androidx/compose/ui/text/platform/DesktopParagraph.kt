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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphConstraints
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import org.jetbrains.skija.paragraph.LineMetrics
import org.jetbrains.skija.paragraph.RectHeightMode
import org.jetbrains.skija.paragraph.RectWidthMode
import org.jetbrains.skija.paragraph.TextBox
import org.jetbrains.skija.Rect as SkRect
import java.nio.charset.Charset
import kotlin.math.floor

@Suppress("UNUSED_PARAMETER")
internal actual fun ActualParagraph(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    maxLines: Int,
    ellipsis: Boolean,
    constraints: ParagraphConstraints,
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
    constraints
)

@Suppress("UNUSED_PARAMETER")
internal actual fun ActualParagraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    maxLines: Int,
    ellipsis: Boolean,
    constraints: ParagraphConstraints
): Paragraph = DesktopParagraph(
    paragraphIntrinsics as DesktopParagraphIntrinsics,
    maxLines,
    ellipsis,
    constraints
)

internal class DesktopParagraph(
    intrinsics: ParagraphIntrinsics,
    val maxLines: Int,
    val ellipsis: Boolean,
    val constraints: ParagraphConstraints
) : Paragraph {

    val paragraphIntrinsics = intrinsics as DesktopParagraphIntrinsics

    val para = paragraphIntrinsics.para

    init {
        para.layout(constraints.width)
    }

    private val text: String
        get() = paragraphIntrinsics.text

    override val width: Float
        get() = para.getMaxWidth()

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
        // TODO: support text ellipsize.
        get() = para.lineNumber < maxLines

    override val lineCount: Int
        get() = para.lineNumber.toInt()

    override val placeholderRects: List<Rect?>
        get() {
            println("Paragraph.placeholderRects")
            return listOf()
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

    override fun getWordBoundary(offset: Int): TextRange {
        println("Paragraph.getWordBoundary $offset")
        return TextRange(0, 0)
    }

    override fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?
    ) {
        // TODO: Implement color, shadow, textDecoration. When color is not Color.Unset or shadow
        // is not null, or textDecoration is not null, this paint call will overwrite the style
        // passed to this Paragraph, and then draw on the canvas.
        // Calling this function is expected to NOT have a huge performance impact.
        para.paint(canvas.nativeCanvas, 0.0f, 0.0f)
    }
}
