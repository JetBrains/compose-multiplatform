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

package androidx.compose.ui.text.android

import android.text.Layout
import android.text.TextUtils
import androidx.annotation.IntRange
import java.text.Bidi

private const val LINE_FEED = '\n'

/**
 * Provide utilities for Layout class
 *
 * This class is not thread-safe. Do not share an instance with multiple threads.
 *
 * @suppress
 */
@InternalPlatformTextApi
class LayoutHelper(val layout: Layout) {

    private val paragraphEnds: List<Int>

    // Stores the list of Bidi object for each paragraph. This could be null if Bidi is not
    // necessary, i.e. single direction text. Do not use this directly. Use analyzeBidi function
    // instead.
    private val paragraphBidi: MutableList<Bidi?>

    // Stores true if the each paragraph already has bidi analyze result. Do not use this
    // directly. Use analyzeBidi function instead.
    private val bidiProcessedParagraphs: BooleanArray

    // Temporary buffer for bidi processing.
    private var tmpBuffer: CharArray? = null

    init {
        var paragraphEnd = 0
        val lineFeeds = mutableListOf<Int>()
        do {
            paragraphEnd = layout.text.indexOf(char = LINE_FEED, startIndex = paragraphEnd)
            if (paragraphEnd < 0) {
                // No more LINE_FEED char found. Use the end of the text as the paragraph end.
                paragraphEnd = layout.text.length
            } else {
                // increment since end offset is exclusive.
                paragraphEnd++
            }
            lineFeeds.add(paragraphEnd)
        } while (paragraphEnd < layout.text.length)
        paragraphEnds = lineFeeds
        paragraphBidi = MutableList(paragraphEnds.size) { null }
        bidiProcessedParagraphs = BooleanArray(paragraphEnds.size)
    }

    /**
     *  Analyze the BiDi runs for the paragraphs and returns result object.
     *
     *  Layout#isRtlCharAt or Layout#getLineDirection is not useful for determining preceding or
     *  following run in visual order. We need to analyze by ourselves.
     *
     *  This may return null if the Bidi process is not necessary, i.e. there is only single bidi
     *  run.
     *
     *  @param paragraphIndex a paragraph index
     */
    fun analyzeBidi(paragraphIndex: Int): Bidi? {
        // If we already analyzed target paragraph, just return the result.
        if (bidiProcessedParagraphs[paragraphIndex]) {
            return paragraphBidi[paragraphIndex]
        }

        val paragraphStart = if (paragraphIndex == 0) 0 else paragraphEnds[paragraphIndex - 1]
        val paragraphEnd = paragraphEnds[paragraphIndex]
        val paragraphLength = paragraphEnd - paragraphStart

        // We allocate the character buffer for saving memories. The internal implementation
        // anyway allocate character buffer even if we pass text through
        // AttributedCharacterIterator. Also there is no way of passing
        // Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT via AttributedCharacterIterator.
        //
        // We also cannot always reuse this buffer since the internal Bidi object keeps this
        // reference and use it for creating lineBidi. We may be able to share buffer by avoiding
        // using lineBidi but this is internal implementation details, so share memory as
        // much as possible and allocate new buffer if we need Bidi object.
        var buffer = tmpBuffer
        buffer = if (buffer == null || buffer.size < paragraphLength) {
            CharArray(paragraphLength)
        } else {
            buffer
        }
        TextUtils.getChars(layout.text, paragraphStart, paragraphEnd, buffer, 0)

        val result = if (Bidi.requiresBidi(buffer, 0, paragraphLength)) {
            val flag = if (isRTLParagraph(paragraphIndex)) {
                Bidi.DIRECTION_RIGHT_TO_LEFT
            } else {
                Bidi.DIRECTION_LEFT_TO_RIGHT
            }
            val bidi = Bidi(buffer, 0, null, 0, paragraphLength, flag)

            if (bidi.runCount == 1) {
                // This corresponds to the all text is Right-to-Left case. We don't need to keep
                // Bidi object
                null
            } else {
                bidi
            }
        } else {
            null
        }

        paragraphBidi[paragraphIndex] = result
        bidiProcessedParagraphs[paragraphIndex] = true

        if (result != null) {
            // The ownership of buffer is now passed to Bidi object.
            // Release tmpBuffer if we didn't allocated in this time.
            tmpBuffer = if (buffer === tmpBuffer) null else tmpBuffer
        } else {
            // We might allocate larger buffer in this time. Update tmpBuffer with latest one.
            // (the latest buffer may be same as tmpBuffer)
            tmpBuffer = buffer
        }
        return result
    }

    /**
     * Retrieve the number of the paragraph in this layout.
     */
    val paragraphCount = paragraphEnds.size

    /**
     * Returns the zero based paragraph number at the offset.
     *
     * The paragraphs are divided by line feed character (U+000A) and line feed character is
     * included in the preceding paragraph, i.e. if the offset points the line feed character,
     * this function returns preceding paragraph index.
     *
     * @param offset a character offset in the text
     * @return the paragraph number
     */
    fun getParagraphForOffset(@IntRange(from = 0) offset: Int): Int =
        paragraphEnds.binarySearch(offset).let { if (it < 0) - (it + 1) else it + 1 }

    /**
     * Returns the inclusive paragraph starting offset of the given paragraph index.
     *
     * @param paragraphIndex a paragraph index.
     * @return an inclusive start character offset of the given paragraph.
     */
    fun getParagraphStart(@IntRange(from = 0) paragraphIndex: Int) =
        if (paragraphIndex == 0) 0 else paragraphEnds[paragraphIndex - 1]

    /**
     * Returns the exclusive paragraph end offset of the given paragraph index.
     *
     * @param paragraphIndex a paragraph index.
     * @return an exclusive end character offset of the given paragraph.
     */
    fun getParagraphEnd(@IntRange(from = 0) paragraphIndex: Int) = paragraphEnds[paragraphIndex]

    /**
     * Returns true if the resolved paragraph direction is RTL, otherwise return false.
     *
     * @param paragraphIndex a paragraph index
     * @return true if the paragraph is RTL, otherwise false
     */
    fun isRTLParagraph(@IntRange(from = 0) paragraphIndex: Int): Boolean {
        val lineNumber = layout.getLineForOffset(getParagraphStart(paragraphIndex))
        return layout.getParagraphDirection(lineNumber) == Layout.DIR_RIGHT_TO_LEFT
    }

    /**
     * Returns horizontal offset from the drawing origin
     *
     * This is the location where a new character would be inserted. If offset points the line
     * broken offset, this return the insertion offset of preceding line if upstream is true.
     * Otherwise returns the following line's insertion offset.
     *
     * In case of Bi-Directional text, the offset may points graphically different location.
     * Here primary means that the inserting character's direction will be resolved to the
     * same direction to the paragraph direction. For example, set usePrimaryHorizontal to true if
     * you want to get LTR character insertion position for the LTR paragraph, or if you want to get
     * RTL character insertion position for the RTL paragraph.
     * Set usePrimaryDirection to false if you want to get RTL character insertion position for the
     * LTR paragraph, or if you want to get LTR character insertion position for the RTL paragraph.
     *
     * @param offset an offset to be insert a character
     * @param usePrimaryDirection no effect if the given offset does not point the directionally
     *                            transition point. If offset points the directional transition
     *                            point and this argument is true, treat the given offset as the
     *                            offset of the Bidi run that has the same direction to the
     *                            paragraph direction. Otherwise treat the given offset  as the
     *                            offset of the Bidi run that has the different direction to the
     *                            paragraph direction.
     * @param upstream if offset points the line broken offset, use upstream offset if true,
     *                 otherwise false.
     * @return the horizontal offset from the drawing origin.
     */
    fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean, upstream: Boolean): Float {
        val lineNo = layout.getLineForOffset(offset, upstream)
        val lineStart = layout.getLineStart(lineNo)
        val lineEnd = layout.getLineEnd(lineNo)

        // Early exit if the offset points not an edge of line. There is no difference between
        // downstream and upstream horizontals. This includes out-of-range request
        if (offset != lineStart && offset != lineEnd) {
            return getDownstreamHorizontal(offset, usePrimaryDirection)
        }

        // Similarly, even if the offset points the edge of the line start and line end, we can
        // use downstream result.
        if (offset == 0 || offset == layout.text.length) {
            return getDownstreamHorizontal(offset, usePrimaryDirection)
        }

        val paraNo = getParagraphForOffset(offset)
        val isParaRtl = isRTLParagraph(paraNo)

        // Use line visible end for creating bidi object since invisible whitespaces should not be
        // considered for location retrieval.
        val lineBidi = analyzeBidi(paraNo)?.createLineBidi(lineStart, lineEndToVisibleEnd(lineEnd))
        if (lineBidi == null || lineBidi.runCount == 1) { // easy case. All directions are the same
            val runDirection = layout.isRtlCharAt(lineStart)
            val isStartLeft = if (usePrimaryDirection || isParaRtl == runDirection) {
                !isParaRtl
            } else {
                isParaRtl
            }
            val isOffsetLeft = if (offset == lineStart) isStartLeft else !isStartLeft
            return if (isOffsetLeft) layout.getLineLeft(lineNo) else layout.getLineRight(lineNo)
        }

        // Somehow need to find the character's position without using getPrimaryHorizontal.
        val runs = Array(lineBidi.runCount) {
            // We may be able to reduce this Bidi Run allocation by using run indices
            // but unfortunately, Bidi#reorderVisually only accepts array of Object. So auto
            // boxing happens anyway. Also, looks like Bidi#getRunStart and Bidi#getRunLimit
            // does non-trivial amount of work. So we save the result into BidiRun.
            BidiRun(
                start = lineStart + lineBidi.getRunStart(it),
                end = lineStart + lineBidi.getRunLimit(it),
                isRtl = lineBidi.getRunLevel(it) % 2 == 1
            )
        }
        val levels = ByteArray(lineBidi.runCount) { lineBidi.getRunLevel(it).toByte() }
        Bidi.reorderVisually(levels, 0, runs, 0, runs.size)

        if (offset == lineStart) {
            // find the visual position of the last character
            val index = runs.indexOfFirst { it.start == offset }
            val run = runs[index]
            // True if the requesting end offset is left edge of the run.
            val isLeftRequested = if (usePrimaryDirection || isParaRtl == run.isRtl) {
                !isParaRtl
            } else {
                isParaRtl
            }

            if (index == 0 && isLeftRequested) {
                // Requesting most left run's left offset, just use line left.
                return layout.getLineLeft(lineNo)
            } else if (index == runs.lastIndex && !isLeftRequested) {
                // Requesting most right run's right offset, just use line right.
                return layout.getLineRight(lineNo)
            } else if (isLeftRequested) {
                // Reaching here means the run is LTR, since RTL run cannot be start from the
                // middle of the text in RTL context.
                // This is LTR run, so left position of this run is the same to left
                // RTL run's right (i.e. start) position.
                return layout.getPrimaryHorizontal(runs[index - 1].start)
            } else {
                // Reaching here means the run is RTL, since LTR run cannot be start from the
                // middle of the text in LTR context.
                // This is RTL run, so right position of this run is the same to right
                // LTR run's left (i.e. start) position.
                return layout.getPrimaryHorizontal(runs[index + 1].start)
            }
        } else {
            // find the visual position of the last character
            val index = runs.indexOfFirst { it.end == offset }
            val run = runs[index]
            // True if the requesting end offset is left edge of the run.
            val isLeftRequested = if (usePrimaryDirection || isParaRtl == run.isRtl) {
                isParaRtl
            } else {
                !isParaRtl
            }
            if (index == 0 && isLeftRequested) {
                // Requesting most left run's left offset, just use line left.
                return layout.getLineLeft(lineNo)
            } else if (index == runs.lastIndex && !isLeftRequested) {
                // Requesting most right run's right offset, just use line right.
                return layout.getLineRight(lineNo)
            } else if (isLeftRequested) {
                // Reaching here means the run is RTL, since LTR run cannot be broken from the
                // middle of the text in LTR context.
                // This is RTL run, so left position of this run is the same to left
                // LTR run's right (i.e. end) position.
                return layout.getPrimaryHorizontal(runs[index - 1].end)
            } else { // !isEndLeft
                // Reaching here means the run is LTR, since RTL run cannot be broken from the
                // middle of the text in RTL context.
                // This is LTR run, so right position of this run is the same to right
                // RTL run's left (i.e. end) position.
                return layout.getPrimaryHorizontal(runs[index + 1].end)
            }
        }
    }

    private fun getDownstreamHorizontal(offset: Int, primary: Boolean) = if (primary) {
        layout.getPrimaryHorizontal(offset)
    } else {
        layout.getSecondaryHorizontal(offset)
    }

    private data class BidiRun(val start: Int, val end: Int, val isRtl: Boolean)

    // Convert line end offset to the offset that is the last visible character.
    private fun lineEndToVisibleEnd(lineEnd: Int): Int {
        var visibleEnd = lineEnd
        while (visibleEnd > 0) {
            if (isLineEndSpace(layout.text.get(visibleEnd - 1 /* visibleEnd is exclusive */))) {
                visibleEnd--
            } else {
                break
            }
        }
        return visibleEnd
    }

    // The spaces that will not be rendered if they are placed at the line end. In most case, it is
    // whitespace or line feed character, hence checking linearly should be enough.
    fun isLineEndSpace(c: Char) = c == ' ' || c == '\n' || c == '\u1680' ||
        (c in '\u2000'..'\u200A' && c != '\u2007') || c == '\u205F' || c == '\u3000'
}