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

import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.android.LayoutHelper
import androidx.compose.ui.text.android.StaticLayoutFactory
import androidx.compose.ui.text.font.test.R
import androidx.core.content.res.ResourcesCompat
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * In this test cases, use following notations:
 *
 * - L1-LF shows an example strong LTR character.
 * - R1-RF shows an example strong RTL character
 * - SP shows whitespace character (U+0020)
 * - LF shows line-feed character (U+000A)
 */
@SmallTest
@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
class SegmentBreakerBreakSegmentTest {
    private val sampleTypeface = ResourcesCompat.getFont(
        InstrumentationRegistry.getInstrumentation().targetContext,
        R.font.sample_font
    )

    // Reference Strong LTR character. All characters are supported by sample_font.ttf and they
    // have 1em width.
    private val L1 = "\u0061"
    private val L2 = "\u0062"
    private val L3 = "\u0063"
    private val L4 = "\u0064"
    private val L5 = "\u0065"
    private val L6 = "\u0066"
    private val L7 = "\u0067"
    private val L8 = "\u0068"
    private val L9 = "\u0069"
    private val LA = "\u006A"

    // Reference Strong RTL character. All characters are supported by sample_font.ttf and they
    // have 1em width.
    private val R1 = "\u05D1"
    private val R2 = "\u05D2"
    private val R3 = "\u05D3"
    private val R4 = "\u05D4"
    private val R5 = "\u05D5"
    private val R6 = "\u05D6"
    private val R7 = "\u05D7"
    private val R8 = "\u05D8"
    private val R9 = "\u05D9"
    private val RA = "\u05DA"

    // White space character. This is supported by sample_font.ttf and this has 1em width.
    private val SP = " "
    private val LF = "\n"

    private val LTR = TextDirectionHeuristics.LTR
    private val RTL = TextDirectionHeuristics.RTL

    // sample_font.ttf has ascent=1000 and descent=-200, hence the line height is 1.2em.
    private val TEXT_SIZE = 10f
    private val LINE_HEIGHT = (1.2f * TEXT_SIZE).toInt()

    private fun getLayout(
        text: String,
        dir: TextDirectionHeuristic
    ): LayoutHelper {
        val paint = TextPaint().apply {
            textSize = TEXT_SIZE
            typeface = sampleTypeface
        }
        val layout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = 50,
            textDir = dir,
            includePadding = false
        )
        return LayoutHelper(layout)
    }

    private fun getSegments(
        text: String,
        dir: TextDirectionHeuristic,
        type: SegmentType,
        dropSpaces: Boolean
    ): List<Segment> {
        val layout = getLayout(text = text, dir = dir)
        return SegmentBreaker.breakSegments(
            layoutHelper = layout, segmentType = type, dropSpaces = dropSpaces
        )
    }

    @Test
    fun document_LTRText_LTRPara() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 SP L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |L5 L6 SP L7 L8| (SP)
        // |L9 LA         |
        //
        // Note that trailing whitespace is not counted in line width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$SP$L7$L8$SP$L9$LA"
        val segments = getSegments(text, LTR, SegmentType.Document, dropSpaces = false)
        assertThat(segments.size).isEqualTo(1)
        assertThat(segments[0]).isEqualTo(
            Segment(
                startOffset = 0,
                endOffset = text.length,
                left = 0,
                top = 0,
                right = 50,
                bottom = LINE_HEIGHT * 3
            )
        )
    }

    @Test
    fun document_LTRText_RTLPara() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 SP L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |L5 L6 SP L7 L8| (SP)
        // |         L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$SP$L7$L8$SP$L9$LA"
        val segments = getSegments(text, RTL, SegmentType.Document, dropSpaces = false)
        assertThat(segments.size).isEqualTo(1)
        assertThat(segments[0]).isEqualTo(
            Segment(
                startOffset = 0,
                endOffset = text.length,
                left = 0,
                top = 0,
                right = 50,
                bottom = LINE_HEIGHT * 3
            )
        )
    }

    @Test
    fun document_RTLText_LTRPara() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 SP R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        // (SP) |R8 R7 SP R6 R5|
        //      |RA R9         |
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$SP$R7$R8$SP$R9$RA"
        val segments = getSegments(text, LTR, SegmentType.Document, dropSpaces = false)
        assertThat(segments.size).isEqualTo(1)
        assertThat(segments[0]).isEqualTo(
            Segment(
                startOffset = 0,
                endOffset = text.length,
                left = 0,
                top = 0,
                right = 50,
                bottom = LINE_HEIGHT * 3
            )
        )
    }

    @Test
    fun document_RTLText_RTLPara() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 SP R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        // (SP) |R8 R7 SP R6 R5|
        //      |         RA R9|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$SP$R7$R8$SP$R9$RA"
        val segments = getSegments(text, RTL, SegmentType.Document, dropSpaces = false)
        assertThat(segments.size).isEqualTo(1)
        assertThat(segments[0]).isEqualTo(
            Segment(
                startOffset = 0,
                endOffset = text.length,
                left = 0,
                top = 0,
                right = 50,
                bottom = LINE_HEIGHT * 3
            )
        )
    }

    @Test
    fun paragraph_LTRText_LTRPara() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |L5 L6 (LF)    |
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, LTR, SegmentType.Paragraph, dropSpaces = false)
        assertThat(segments.size).isEqualTo(2)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun paragraph_LTRText_RTLPara() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |         L5 L6| (LF)
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, RTL, SegmentType.Paragraph, dropSpaces = false)
        assertThat(segments.size).isEqualTo(2)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun paragraph_RTLText_LTRPara() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        // (LF) |R6 R5         |
        //      |R8 R7 SP RA R9|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, LTR, SegmentType.Paragraph, dropSpaces = false)
        assertThat(segments.size).isEqualTo(2)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun paragraph_RTLText_RTLPara() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        //      |     (LF)R6 R5|
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, RTL, SegmentType.Paragraph, dropSpaces = false)
        assertThat(segments.size).isEqualTo(2)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun line_LTRText_LTRPara_includeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |L5 L6(LF)     |
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, LTR, SegmentType.Line, dropSpaces = false)
        assertThat(segments.size).isEqualTo(3)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 6, // 2nd SP char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = LINE_HEIGHT,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun line_LTRText_RTLPara_includeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |         L5 L6| (LF)
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, RTL, SegmentType.Line, dropSpaces = false)
        assertThat(segments.size).isEqualTo(3)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 6, // 2nd SP char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = LINE_HEIGHT,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun line_RTLText_LTRPara_includeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        // (LF) |R6 R5         |
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, LTR, SegmentType.Line, dropSpaces = false)
        assertThat(segments.size).isEqualTo(3)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 6, // 2nd SP char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = LINE_HEIGHT,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun line_RTLText_RTLPara_includeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        //      |     (LF)R6 R5|
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, RTL, SegmentType.Line, dropSpaces = false)
        assertThat(segments.size).isEqualTo(3)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 6, // 2nd SP char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = LINE_HEIGHT,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun line_LTRText_LTRPara_excludeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |L5 L6(LF)     |
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, LTR, SegmentType.Line, dropSpaces = true)
        assertThat(segments.size).isEqualTo(3)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 6, // 2nd SP char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = LINE_HEIGHT,
                    right = 20,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun line_LTRText_RTLPara_excludeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |         L5 L6| (LF)
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, RTL, SegmentType.Line, dropSpaces = true)
        assertThat(segments.size).isEqualTo(3)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 6, // 2nd SP char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 30,
                    top = LINE_HEIGHT,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun line_RTLText_LTRPara_excludeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        // (LF) |R6 R5         |
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, LTR, SegmentType.Line, dropSpaces = true)
        assertThat(segments.size).isEqualTo(3)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 6, // 2nd SP char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = LINE_HEIGHT,
                    right = 20,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun line_RTLText_RTLPara_excludeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        //      |     (LF)R6 R5|
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, RTL, SegmentType.Line, dropSpaces = true)
        assertThat(segments.size).isEqualTo(3)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 6, // 2nd SP char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 30,
                    top = LINE_HEIGHT,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun line_Bidi() {
        // input (Logical): L1 L2 SP R1 R2 SP R3 R4 SP L3 L4
        //
        // |L1 L2 SP (SP) R2 R1|
        // |R4 R3 SP L3 L4|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$R1$R2$SP$R3$R4$SP$L3$L4"
        val segments = getSegments(text, LTR, SegmentType.Line, dropSpaces = false)
        assertThat(segments.size).isEqualTo(2)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 6, // 2nd SP char offset
                    left = 0,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                )
            )
        )
    }

    @Test
    fun word_LTRText_LTRPara_includeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |L5 L6(LF)     |
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, LTR, SegmentType.Word, dropSpaces = false)
        assertThat(segments.size).isEqualTo(5)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 3, // 1st SP char offset
                    left = 0,
                    top = 0,
                    right = 30,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 3,
                    endOffset = 6, // 2st SP char offset
                    left = 30,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = LINE_HEIGHT,
                    right = 20,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = 12, // 3rd SP char offset
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 30,
                    bottom = LINE_HEIGHT * 3
                ),
                Segment(
                    startOffset = 12,
                    endOffset = text.length,
                    left = 30,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun word_LTRText_RTLPara_includeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |         L5 L6| (LF)
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, RTL, SegmentType.Word, dropSpaces = false)
        assertThat(segments.size).isEqualTo(5)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 3, // 1st SP char offset
                    left = 0,
                    top = 0,
                    right = 30,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 3,
                    endOffset = 6, // 2st SP char offset
                    left = 30,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 30,
                    top = LINE_HEIGHT,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = 12, // 3rd SP char offset
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 30,
                    bottom = LINE_HEIGHT * 3
                ),
                Segment(
                    startOffset = 12,
                    endOffset = text.length,
                    left = 30,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun word_RTLText_LTRPara_includeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        // (LF) |R6 R5         |
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, LTR, SegmentType.Word, dropSpaces = false)
        assertThat(segments.size).isEqualTo(6)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 3, // 1st SP char offset
                    left = 20,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 3,
                    endOffset = 6, // 2st SP char offset
                    left = 0,
                    top = 0,
                    right = 20,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 8,
                    left = 0,
                    top = LINE_HEIGHT,
                    right = 20,
                    bottom = LINE_HEIGHT * 2
                ),
                // Bidi assigns LF character to LTR. Do we want to include preceding run?
                Segment(
                    startOffset = 8,
                    endOffset = 9,
                    left = 20,
                    top = LINE_HEIGHT,
                    right = 20,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = 12, // 3rd SP char offset
                    left = 20,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                ),
                Segment(
                    startOffset = 12,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 20,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun word_RTLText_RTLPara_includeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        //      |     (LF)R6 R5|
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, RTL, SegmentType.Word, dropSpaces = false)
        assertThat(segments.size).isEqualTo(5)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 3, // 1st SP char offset
                    left = 20,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 3,
                    endOffset = 6, // 2st SP char offset
                    left = 0,
                    top = 0,
                    right = 20,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 30,
                    top = LINE_HEIGHT,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = 12, // 3rd SP char offset
                    left = 20,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                ),
                Segment(
                    startOffset = 12,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 20,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun word_LTRText_LTRPara_excludeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |L5 L6(LF)     |
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, LTR, SegmentType.Word, dropSpaces = true)
        assertThat(segments.size).isEqualTo(5)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 3, // 1st SP char offset
                    left = 0,
                    top = 0,
                    right = 20,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 3,
                    endOffset = 6, // 2st SP char offset
                    left = 30,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 0,
                    top = LINE_HEIGHT,
                    right = 20,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = 12, // 3rd SP char offset
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 20,
                    bottom = LINE_HEIGHT * 3
                ),
                Segment(
                    startOffset = 12,
                    endOffset = text.length,
                    left = 30,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun word_LTRText_RTLPara_excludeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |         L5 L6| (LF)
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, RTL, SegmentType.Word, dropSpaces = true)
        assertThat(segments.size).isEqualTo(5)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 3, // 1st SP char offset
                    left = 0,
                    top = 0,
                    right = 20,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 3,
                    endOffset = 6, // 2st SP char offset
                    left = 30,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 30,
                    top = LINE_HEIGHT,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = 12, // 3rd SP char offset
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 20,
                    bottom = LINE_HEIGHT * 3
                ),
                Segment(
                    startOffset = 12,
                    endOffset = text.length,
                    left = 30,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun word_RTLText_LTRPara_excludeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        // (LF) |R6 R5         |
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, LTR, SegmentType.Word, dropSpaces = true)
        assertThat(segments.size).isEqualTo(6)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 3, // 1st SP char offset
                    left = 30,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 3,
                    endOffset = 6, // 2st SP char offset
                    left = 0,
                    top = 0,
                    right = 20,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 8,
                    left = 0,
                    top = LINE_HEIGHT,
                    right = 20,
                    bottom = LINE_HEIGHT * 2
                ),
                // Bidi assigns LF character to LTR. Do we want to include preceding run?
                Segment(
                    startOffset = 8,
                    endOffset = 9,
                    left = 20,
                    top = LINE_HEIGHT,
                    right = 20,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = 12, // 3rd SP char offset
                    left = 30,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                ),
                Segment(
                    startOffset = 12,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 20,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun word_RTLText_RTLPara_excludeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        //      |     (LF)R6 R5|
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, RTL, SegmentType.Word, dropSpaces = true)
        assertThat(segments.size).isEqualTo(5)
        assertThat(segments).isEqualTo(
            listOf(
                Segment(
                    startOffset = 0,
                    endOffset = 3, // 1st SP char offset
                    left = 30,
                    top = 0,
                    right = 50,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 3,
                    endOffset = 6, // 2st SP char offset
                    left = 0,
                    top = 0,
                    right = 20,
                    bottom = LINE_HEIGHT
                ),
                Segment(
                    startOffset = 6,
                    endOffset = 9, // LF char offset
                    left = 30,
                    top = LINE_HEIGHT,
                    right = 50,
                    bottom = LINE_HEIGHT * 2
                ),
                Segment(
                    startOffset = 9,
                    endOffset = 12, // 3rd SP char offset
                    left = 30,
                    top = LINE_HEIGHT * 2,
                    right = 50,
                    bottom = LINE_HEIGHT * 3
                ),
                Segment(
                    startOffset = 12,
                    endOffset = text.length,
                    left = 0,
                    top = LINE_HEIGHT * 2,
                    right = 20,
                    bottom = LINE_HEIGHT * 3
                )
            )
        )
    }

    @Test
    fun char_LTRText_LTRPara_includeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |L5 L6(LF)     |
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, LTR, SegmentType.Character, dropSpaces = false)
        assertThat(segments.size).isEqualTo(14)
        assertThat(segments[0]).isEqualTo(
            Segment( // L1 character
                startOffset = 0,
                endOffset = 1,
                left = 0,
                top = 0,
                right = 10,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[2]).isEqualTo(
            Segment( // 1st SP location
                startOffset = 2,
                endOffset = 3,
                left = 20,
                top = 0,
                right = 30,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[5]).isEqualTo(
            Segment( // 2nd SP location. not rendered.
                startOffset = 5,
                endOffset = 6,
                left = 50,
                top = 0,
                right = 50,
                bottom = LINE_HEIGHT
            )
        )
    }

    @Test
    fun char_LTRText_RTLPara_includeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // (SP) |L1 L2 SP L3 L4|
        //      |     (LF)L5 L6|
        //      |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, RTL, SegmentType.Character, dropSpaces = false)
        assertThat(segments.size).isEqualTo(14)
        assertThat(segments[0]).isEqualTo(
            Segment( // L1 character
                startOffset = 0,
                endOffset = 1,
                left = 0,
                top = 0,
                right = 10,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[2]).isEqualTo(
            Segment( // 1st SP location
                startOffset = 2,
                endOffset = 3,
                left = 20,
                top = 0,
                right = 30,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[5]).isEqualTo(
            Segment( // 2nd SP location. not rendered.
                startOffset = 5,
                endOffset = 6,
                left = 0,
                top = 0,
                right = 0,
                bottom = LINE_HEIGHT
            )
        )
    }

    @Test
    fun char_RTLText_LTRPara_includeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // |R4 R3 SP R2 R1| (SP)
        // |R6 R5(LF)     |
        // |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, LTR, SegmentType.Character, dropSpaces = false)
        assertThat(segments.size).isEqualTo(14)
        assertThat(segments[0]).isEqualTo(
            Segment( // R1 character
                startOffset = 0,
                endOffset = 1,
                left = 40,
                top = 0,
                right = 50,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[2]).isEqualTo(
            Segment( // 1st SP location
                startOffset = 2,
                endOffset = 3,
                left = 20,
                top = 0,
                right = 30,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[5]).isEqualTo(
            Segment( // 2nd SP location. not rendered.
                startOffset = 5,
                endOffset = 6,
                left = 50,
                top = 0,
                right = 50,
                bottom = LINE_HEIGHT
            )
        )
    }

    @Test
    fun char_RTLText_RTLPara_includeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        //      |     (LF)R6 R5|
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, RTL, SegmentType.Character, dropSpaces = false)
        assertThat(segments.size).isEqualTo(14)
        assertThat(segments[0]).isEqualTo(
            Segment( // R1 character
                startOffset = 0,
                endOffset = 1,
                left = 40,
                top = 0,
                right = 50,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[2]).isEqualTo(
            Segment( // 1st SP location
                startOffset = 2,
                endOffset = 3,
                left = 20,
                top = 0,
                right = 30,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[5]).isEqualTo(
            Segment( // 2nd SP location. not rendered.
                startOffset = 5,
                endOffset = 6,
                left = 0,
                top = 0,
                right = 0,
                bottom = LINE_HEIGHT
            )
        )
    }

    @Test
    fun char_LTRText_LTRPara_excludeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // |L1 L2 SP L3 L4| (SP)
        // |L5 L6(LF)     |
        // |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, LTR, SegmentType.Character, dropSpaces = true)
        assertThat(segments.size).isEqualTo(10) // three spaces and one line feeds are excluded
        assertThat(segments[0]).isEqualTo(
            Segment( // L1 character
                startOffset = 0,
                endOffset = 1,
                left = 0,
                top = 0,
                right = 10,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[2]).isEqualTo(
            Segment( // 1st SP is skipped. L3 character
                startOffset = 3,
                endOffset = 4,
                left = 30,
                top = 0,
                right = 40,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[4]).isEqualTo(
            Segment( // 2nd SP is skipped. L5 character
                startOffset = 6,
                endOffset = 7,
                left = 0,
                top = LINE_HEIGHT,
                right = 10,
                bottom = LINE_HEIGHT * 2
            )
        )
    }

    @Test
    fun char_LTRText_RTLPara_excludeSpaces() {
        // input (Logical): L1 L2 SP L3 L4 SP L5 L6 LF L7 L8 SP L9 LA
        //
        // (SP) |L1 L2 SP L3 L4|
        //      |     (LF)L5 L6|
        //      |L7 L8 SP L9 LA|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$L1$L2$SP$L3$L4$SP$L5$L6$LF$L7$L8$SP$L9$LA"
        val segments = getSegments(text, RTL, SegmentType.Character, dropSpaces = true)
        assertThat(segments.size).isEqualTo(10) // three spaces and one line feeds are excluded
        assertThat(segments[0]).isEqualTo(
            Segment( // L1 character
                startOffset = 0,
                endOffset = 1,
                left = 0,
                top = 0,
                right = 10,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[2]).isEqualTo(
            Segment( // 1st SP is skipped. L3 character
                startOffset = 3,
                endOffset = 4,
                left = 30,
                top = 0,
                right = 40,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[4]).isEqualTo(
            Segment( // 2nd SP is skipped. L5 character
                startOffset = 6,
                endOffset = 7,
                left = 30,
                top = LINE_HEIGHT,
                right = 40,
                bottom = LINE_HEIGHT * 2
            )
        )
    }

    @Test
    fun char_RTLText_LTRPara_excludeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        // (LF) |R6 R5         |
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, LTR, SegmentType.Character, dropSpaces = true)
        assertThat(segments.size).isEqualTo(10) // three spaces and one line feeds are excluded
        assertThat(segments[0]).isEqualTo(
            Segment( // R1 character
                startOffset = 0,
                endOffset = 1,
                left = 40,
                top = 0,
                right = 50,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[2]).isEqualTo(
            Segment( // 1st SP is skipped. R3 character
                startOffset = 3,
                endOffset = 4,
                left = 10,
                top = 0,
                right = 20,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[4]).isEqualTo(
            Segment( // 2nd SP is skipped. R5 character
                startOffset = 6,
                endOffset = 7,
                left = 10,
                top = LINE_HEIGHT,
                right = 20,
                bottom = LINE_HEIGHT * 2
            )
        )
    }

    @Test
    fun char_RTLText_RTLPara_excludeSpaces() {
        // input (Logical): R1 R2 SP R3 R4 SP R5 R6 LF R7 T8 SP R9 RA
        //
        // (SP) |R4 R3 SP R2 R1|
        //      |     (LF)R6 R5|
        //      |RA R9 SP R8 R7|
        //
        // Note that trailing whitespace is not counted in line width. The characters with
        // parenthesis are not counted as width.
        val text = "$R1$R2$SP$R3$R4$SP$R5$R6$LF$R7$R8$SP$R9$RA"
        val segments = getSegments(text, RTL, SegmentType.Character, dropSpaces = true)
        assertThat(segments.size).isEqualTo(10) // three spaces and one line feeds are excluded
        assertThat(segments[0]).isEqualTo(
            Segment( // R1 character
                startOffset = 0,
                endOffset = 1,
                left = 40,
                top = 0,
                right = 50,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[2]).isEqualTo(
            Segment( // 1st SP is skipped. R3 character
                startOffset = 3,
                endOffset = 4,
                left = 10,
                top = 0,
                right = 20,
                bottom = LINE_HEIGHT
            )
        )
        assertThat(segments[4]).isEqualTo(
            Segment( // 2nd SP is skipped. R5 character
                startOffset = 6,
                endOffset = 7,
                left = 40,
                top = LINE_HEIGHT,
                right = 50,
                bottom = LINE_HEIGHT * 2
            )
        )
    }
}