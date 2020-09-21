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

package androidx.compose.ui.text.android.animation

import android.text.Layout
import androidx.compose.ui.text.android.CharSequenceCharacterIterator
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.android.LayoutHelper
import androidx.compose.ui.text.android.getLineForOffset
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A class represents animation segment.
 *
 * @param startOffset an inclusive start character offset of this segment.
 * @param endOffset an exclusive end character offset of this segment.
 * @param left a graphical left position from the layout origin.
 * @param top a graphical top position from the layout origin.
 * @param right a graphical right position from the layout origin.
 * @param bottom a graphical bottom position from the layout origin.
 *
 * @suppress
 */
@InternalPlatformTextApi
data class Segment(
    val startOffset: Int,
    val endOffset: Int,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

/**
 * Porvide a segmentation breaker for the text animation.
 * @suppress
 */
@InternalPlatformTextApi
object SegmentBreaker {
    private fun breakInWords(layoutHelper: LayoutHelper): List<Int> {
        val text = layoutHelper.layout.text
        val words = breakWithBreakIterator(text, BreakIterator.getLineInstance(Locale.getDefault()))

        val set = words.toSortedSet()
        for (paraIndex in 0 until layoutHelper.paragraphCount) {
            val bidi = layoutHelper.analyzeBidi(paraIndex) ?: continue
            val paragraphStart = layoutHelper.getParagraphStart(paraIndex)
            for (i in 0 until bidi.runCount) {
                set.add(bidi.getRunStart(i) + paragraphStart)
            }
        }
        return set.toList()
    }

    private fun breakWithBreakIterator(text: CharSequence, breaker: BreakIterator): List<Int> {
        val iter = CharSequenceCharacterIterator(text, 0, text.length)

        val res = mutableListOf(0)
        breaker.setText(iter)
        while (breaker.next() != BreakIterator.DONE) {
            res.add(breaker.current())
        }
        return res
    }

    /**
     * Gets all offsets of the given segment type for animation.
     *
     * @param layoutHelper a layout helper
     * @param segmentType a segmentation type
     * @return all break offsets of the given segmentation type including 0 and text length.
     */
    fun breakOffsets(layoutHelper: LayoutHelper, segmentType: SegmentType): List<Int> {
        val layout = layoutHelper.layout
        val text = layout.text

        return when (segmentType) {
            SegmentType.Document -> listOf(0, text.length)
            SegmentType.Paragraph -> {
                mutableListOf(0).also {
                    for (i in 0 until layoutHelper.paragraphCount) {
                        it.add(layoutHelper.getParagraphEnd(i))
                    }
                }
            }
            SegmentType.Line -> {
                mutableListOf(0).also {
                    for (i in 0 until layout.lineCount) {
                        it.add(layout.getLineEnd(i))
                    }
                }
            }
            SegmentType.Word -> breakInWords(layoutHelper)
            SegmentType.Character -> breakWithBreakIterator(
                text,
                BreakIterator.getCharacterInstance(Locale.getDefault())
            )
        }
    }

    /**
     * Break Layout into list of segments.
     *
     * A segment represents a unit of text animation. For example, if you specify, SegmentType
     * .Line, this function will give you a list of Line segments which have line start offset and
     * line end offset, and also line bounding box.
     *
     * The dropSpaces argument is ignored if segmentType is Document or Paragraph.
     *
     * If segmentType is Line and dropSpaces is true, this removes trailing spaces. If
     * segmentType is Line and dropSpace is false, this use layout width as the right position of
     * the line.
     *
     * If segmentType is Word and dropSpaces is true, this removes trailing spaces if there. If
     * segmentType is Word and dropSpace is false, this includes the trailing whitespace into
     * segment.
     *
     * If segmentType is Character and dropSpace is true, this drops whitespace only segment. If
     * segmentType is Character and dropSpace is true, this include whitespace only segment.
     *
     * @param layoutHelper a layout helper
     * @param segmentType a segmentation type
     * @param dropSpaces whether dropping spacing. See function comment for more details.
     * @return list of segment object
     */
    fun breakSegments(
        layoutHelper: LayoutHelper,
        segmentType: SegmentType,
        dropSpaces: Boolean
    ): List<Segment> {
        return when (segmentType) {
            SegmentType.Document -> breakSegmentWithDocument(layoutHelper)
            SegmentType.Paragraph -> breakSegmentWithParagraph(layoutHelper)
            SegmentType.Line -> breakSegmentWithLine(layoutHelper, dropSpaces)
            SegmentType.Word -> breakSegmentWithWord(layoutHelper, dropSpaces)
            SegmentType.Character -> breakSegmentWithChar(layoutHelper, dropSpaces)
        }
    }

    private fun breakSegmentWithDocument(layoutHelper: LayoutHelper): List<Segment> {
        return listOf(
            Segment(
                startOffset = 0,
                endOffset = layoutHelper.layout.text.length,
                left = 0,
                top = 0,
                right = layoutHelper.layout.width,
                bottom = layoutHelper.layout.height
            )
        )
    }

    private fun breakSegmentWithParagraph(layoutHelper: LayoutHelper): List<Segment> {
        val result = mutableListOf<Segment>()
        val layout = layoutHelper.layout
        for (i in 0 until layoutHelper.paragraphCount) {
            val paraStart = layoutHelper.getParagraphStart(i)
            val paraEnd = layoutHelper.getParagraphEnd(i)
            val paraFirstLine = layout.getLineForOffset(paraStart, false /* downstream */)
            val paraLastLine = layout.getLineForOffset(paraEnd, true /* upstream */)
            result.add(
                Segment(
                    startOffset = paraStart,
                    endOffset = paraEnd,
                    left = 0,
                    top = layout.getLineTop(paraFirstLine),
                    right = layout.width,
                    bottom = layout.getLineBottom(paraLastLine)
                )
            )
        }
        return result
    }

    private fun breakSegmentWithLine(
        layoutHelper: LayoutHelper,
        dropSpaces: Boolean
    ): List<Segment> {
        val result = mutableListOf<Segment>()
        val layout = layoutHelper.layout
        for (i in 0 until layoutHelper.layout.lineCount) {
            result.add(
                Segment(
                    startOffset = layout.getLineStart(i),
                    endOffset = layout.getLineEnd(i),
                    left = if (dropSpaces) ceil(layout.getLineLeft(i)).toInt() else 0,
                    top = layout.getLineTop(i),
                    right = if (dropSpaces) ceil(layout.getLineRight(i)).toInt() else layout.width,
                    bottom = layout.getLineBottom(i)
                )
            )
        }
        return result
    }

    private fun breakSegmentWithWord(
        layoutHelper: LayoutHelper,
        dropSpaces: Boolean
    ): List<Segment> {
        val layout = layoutHelper.layout
        val wsWidth = ceil(layout.paint.measureText(" ")).toInt()
        return breakOffsets(layoutHelper, SegmentType.Word).zipWithNext { start, end ->
            val lineNo = layout.getLineForOffset(start, false /* downstream */)
            val paraRTL = layout.getParagraphDirection(lineNo) == Layout.DIR_RIGHT_TO_LEFT
            val runRtl = layout.isRtlCharAt(start) // no bidi transition inside segment
            val startPos = ceil(
                layoutHelper.getHorizontalPosition(
                    offset = start,
                    usePrimaryDirection = runRtl == paraRTL,
                    upstream = false
                )
            ).toInt()
            val endPos = ceil(
                layoutHelper.getHorizontalPosition(
                    offset = end,
                    usePrimaryDirection = runRtl == paraRTL,
                    upstream = true
                )
            ).toInt()

            // Drop trailing space is the line does not end with this word.
            var left = min(startPos, endPos)
            var right = max(startPos, endPos)
            if (dropSpaces && end != 0 && layout.text.get(end - 1) == ' ') {
                val lineEnd = layout.getLineEnd(lineNo)
                if (lineEnd != end) {
                    if (runRtl) {
                        left += wsWidth
                    } else {
                        right -= wsWidth
                    }
                }
            }

            Segment(
                startOffset = start,
                endOffset = end,
                left = left,
                top = layout.getLineTop(lineNo),
                right = right,
                bottom = layout.getLineBottom(lineNo)
            )
        }
    }

    private fun breakSegmentWithChar(
        layoutHelper: LayoutHelper,
        dropSpaces: Boolean
    ): List<Segment> {
        val res = mutableListOf<Segment>()
        breakOffsets(layoutHelper, SegmentType.Character).zipWithNext lambda@{ start, end ->
            val layout = layoutHelper.layout

            if (dropSpaces && end == start + 1 &&
                layoutHelper.isLineEndSpace(layout.text.get(start))
            )
                return@lambda
            val lineNo = layout.getLineForOffset(start, false /* downstream */)
            val paraRTL = layout.getParagraphDirection(lineNo) == Layout.DIR_RIGHT_TO_LEFT
            val runRtl = layout.isRtlCharAt(start) // no bidi transition inside segment
            val startPos = ceil(
                layoutHelper.getHorizontalPosition(
                    offset = start,
                    usePrimaryDirection = runRtl == paraRTL,
                    upstream = false
                )
            ).toInt()
            val endPos = ceil(
                layoutHelper.getHorizontalPosition(
                    offset = end,
                    usePrimaryDirection = runRtl == paraRTL,
                    upstream = true
                )
            ).toInt()
            res.add(
                Segment(
                    startOffset = start,
                    endOffset = end,
                    left = min(startPos, endPos),
                    top = layout.getLineTop(lineNo),
                    right = max(startPos, endPos),
                    bottom = layout.getLineBottom(lineNo)
                )
            )
        }
        return res
    }
}