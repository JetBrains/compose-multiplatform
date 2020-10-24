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

import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
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
 */
@SmallTest
@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
class LayoutGetHorizontalTest {

    private val sampleTypeface = ResourcesCompat.getFont(
        InstrumentationRegistry.getInstrumentation().targetContext,
        R.font.sample_font
    )

    private val LTR = TextDirectionHeuristics.LTR
    private val RTL = TextDirectionHeuristics.RTL

    private fun LayoutHelper.getUpstreamPrimaryHorizontalPosition(offset: Int) =
        getHorizontalPosition(offset = offset, usePrimaryDirection = true, upstream = true)
    private fun LayoutHelper.getDownstreamPrimaryHorizontalPosition(offset: Int) =
        getHorizontalPosition(offset = offset, usePrimaryDirection = true, upstream = false)
    private fun LayoutHelper.getUpstreamSecondaryHorizontalPosition(offset: Int) =
        getHorizontalPosition(offset = offset, usePrimaryDirection = false, upstream = true)
    private fun LayoutHelper.getDownstreamSecondaryHorizontalPosition(offset: Int) =
        getHorizontalPosition(offset = offset, usePrimaryDirection = false, upstream = false)

    private fun getLayout(
        text: String,
        fontSize: Int,
        width: Int,
        dir: TextDirectionHeuristic
    ): LayoutHelper {
        val paint = TextPaint().apply {
            textSize = fontSize.toFloat()
            typeface = sampleTypeface
        }
        val layout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = width,
            textDir = dir
        )
        return LayoutHelper(layout)
    }

    @Test
    fun getHorizontal_LTRText_LTRParagraph_FirstCharacter() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 L6 L7 L8 L9 LA LB LC
        //
        // |L1 L2 L3 L4 L5|
        // |L6 L7 L8 L9 LA|
        // |LB LC         |
        //
        val layout = getLayout(
            text = "\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = 0 // Before L1
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }

    @Test
    fun getHorizontal_LTRText_LTRParagraph_LineBreakOffset() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 L6 L7 L8 L9 LA LB LC
        //
        // |L1 L2 L3 L4 L5|
        // |L6 L7 L8 L9 LA|
        // |LB LC         |
        //
        val layout = getLayout(
            text = "\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = 5 // before L6
        // If insert LX to first line, it will be |L1 L2 L3 L4 L5 LX|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to first line, it will be |L1 L2 L3 L4 L5 RX|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to second line, it will be |LX L6 L7 L8 L9 LA|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to second line, it will be |RX L6 L7 L8 L9 LA|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }

    @Test
    fun getHorizontal_LTRText_LTRParagraph_LastOffset() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 L6 L7 L8 L9 LA LB LC
        //
        // |L1 L2 L3 L4 L5|
        // |L6 L7 L8 L9 LA|
        // |LB LC         |
        //
        val layout = getLayout(
            text = "\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = layout.layout.text.length // after LC
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(20)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(20)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(20)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(20)
    }

    @Test
    fun getHorizontal_LTRText_LTRParagraph_Other() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 L6 L7 L8 L9 LA LB LC
        //
        // |L1 L2 L3 L4 L5|
        // |L6 L7 L8 L9 LA|
        // |LB LC         |
        //
        val layout = getLayout(
            text = "\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = 7 // before L8
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(20)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(20)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(20)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(20)
    }

    @Test
    fun getHorizontal_LTRText_RTLParagraph_FirstCharacter() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 L6 L7 L8 L9 LA LB LC
        //
        // |L1 L2 L3 L4 L5|
        // |L6 L7 L8 L9 LA|
        // |         LB LC|
        //
        val layout = getLayout(
            text = "\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = 0 // before L1
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }

    @Test
    fun getHorizontal_LTRText_RTLParagraph_LineBreakOffset() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 L6 L7 L8 L9 LA LB LC
        //
        // |L1 L2 L3 L4 L5|
        // |L6 L7 L8 L9 LA|
        // |         LB LC|
        //
        val layout = getLayout(
            text = "\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = 5 // before L6 == after L5
        // If insert RX to the first line, it will be |RX L1 L2 L3 L4 L5|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to the first line, it will be |L1 L2 L3 L4 L5 LX|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to the second line, it will be |L6 L7 L8 L9 LA RX|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to the second line, it will be |LX L6 L7 L8 L9 LA|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }

    @Test
    fun getHorizontal_LTRText_RTLParagraph_LastOffset() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 L6 L7 L8 L9 LA LB LC
        //
        // |L1 L2 L3 L4 L5|
        // |L6 L7 L8 L9 LA|
        // |         LB LC|
        //
        val layout = getLayout(
            text = "\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = layout.layout.text.length // after LC
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(30)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(30)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
    }

    @Test
    fun getHorizontal_LTRText_RTLParagraph_Other() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 L6 L7 L8 L9 LA LB LC
        //
        // |L1 L2 L3 L4 L5|
        // |L6 L7 L8 L9 LA|
        // |         LB LC|
        //
        val layout = getLayout(
            text = "\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = 7 // before L8
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(20)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(20)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(20)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(20)
    }

    @Test
    fun getHorizontal_RTLText_RTLParagraph_FirstCharacter() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 R6 R7 R8 R9 RA RB RC
        //
        // |R5 R4 R3 R2 R1|
        // |RA R9 R8 R7 R6|
        // |         RC RB|
        //
        val layout = getLayout(
            text = "\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7\u05D8\u05D9\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = 0 // before R1
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
    }

    @Test
    fun getHorizontal_RTLText_RTLParagraph_LineBreakOffset() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 R6 R7 R8 R9 RA RB RC
        //
        // |R5 R4 R3 R2 R1|
        // |RA R9 R8 R7 R6|
        // |         RC RB|
        //
        val layout = getLayout(
            text = "\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7\u05D8\u05D9\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = 5 // before R6 == after R5
        // If insert RX to the first line, it will be |RX R5 R4 R3 R2 R1|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to the first line, it will be |LX R5 R4 R3 R2 R1|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to the second line, it will be |RA R9 R8 R7 R6 RX|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to the second line, it will be |RA R9 R8 R7 R6 LX|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
    }

    @Test
    fun getHorizontal_RTLText_RTLParagraph_LastCharacter() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 R6 R7 R8 R9 RA RB RC
        //
        // |R5 R4 R3 R2 R1|
        // |RA R9 R8 R7 R6|
        // |         RC RB|
        //
        val layout = getLayout(
            text = "\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7\u05D8\u05D9\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = layout.layout.text.length // after RC
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(30)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(30)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(30)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(30)
    }

    @Test
    fun getHorizontal_RTLText_RTLParagraph_Other() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 R6 R7 R8 R9 RA RB RC
        //
        // |R5 R4 R3 R2 R1|
        // |RA R9 R8 R7 R6|
        // |         RC RB|
        //
        val layout = getLayout(
            text = "\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7\u05D8\u05D9\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = 7 // before R8
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(30)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(30)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(30)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(30)
    }

    @Test
    fun getHorizontal_RTLText_LTRParagraph_FirstCharacter() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 R6 R7 R8 R9 RA RB RC
        //
        // |R5 R4 R3 R2 R1|
        // |RA R9 R8 R7 R6|
        // |RC RB         |
        //
        val layout = getLayout(
            text = "\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7\u05D8\u05D9\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = 0 // before R1
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
    }

    @Test
    fun getHorizontal_RTLText_LTRParagraph_LineBreakOffset() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 R6 R7 R8 R9 RA RB RC
        //
        // |R5 R4 R3 R2 R1|
        // |RA R9 R8 R7 R6|
        // |RC RB         |
        //
        val layout = getLayout(
            text = "\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7\u05D8\u05D9\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = 5 // befroe R6 == after R5
        // If insert LX to the first line, it will be |R5 R4 R3 R2 R1 LX|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to the first line, it will be |RX R5 R4 R3 R2 R1|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to the second line, it will be |LX RA R9 R8 R7 R6|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to the second line, it will be |RA R9 R8 R7 R6 RX|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
    }

    @Test
    fun getHorizontal_RTLText_LTRParagraph_LastCharacter() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 R6 R7 R8 R9 RA RB RC
        //
        // |R5 R4 R3 R2 R1|
        // |RA R9 R8 R7 R6|
        // |RC RB         |
        //
        val layout = getLayout(
            text = "\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7\u05D8\u05D9\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = layout.layout.text.length // after RB
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(20)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(20)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }

    @Test
    fun getHorizontal_RTLText_LTRParagraph_Other() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 R6 R7 R8 R9 RA RB RC
        //
        // |R5 R4 R3 R2 R1|
        // |RA R9 R8 R7 R6|
        // |RC RB         |
        //
        val layout = getLayout(
            text = "\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7\u05D8\u05D9\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = 7 // before R8
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(30)
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(30)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(30)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(30)
    }

    @Test
    fun getHorizontal_BidiText_BiDiTransitionNotLineBreakOffset_FromLTRToRTL_LTRParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): L1 R1 R2 R3 L2 L3 L5 L6 L7 L8
        //
        // |L1 R3 R2 R1 L2|
        // |L3 L4 L5 L6 L7|
        // |L8 L9         |
        //
        val layout = getLayout(
            text = "\u0061\u05D1\u05D2\u05D3\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = 1 // before R1
        // If insert LX to first line, it will be |L1 LX R3 R2 R1 L2|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(10)
        // If insert RX to first line, it will be |L1 R3 R2 R1 RX L2|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(40)
        // If insert LX to first line, it will be |L1 LX R3 R2 R1 L2|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(10)
        // If insert RX to first line, it will be |L1 R3 R2 R1 RX L2|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(40)
    }

    @Test
    fun getHorizontal_BidiText_BiDiTransitionNotLineBreakOffset_FromLTRToRTL_RTLParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): L1 R1 R2 R3 L2 L3 L5 L6 L7 L8 L9
        //
        // |L2 R3 R2 R1 L1|
        // |L3 L4 L5 L6 L7|
        // |         L8 L9|
        //
        val layout = getLayout(
            text = "\u0061\u05D1\u05D2\u05D3\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = 1 // before R1
        // If insert RX to first line, it will be |L2 R3 R2 R1 RX L1|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(40)
        // If insert LX to first line, it will be |L2 R3 R2 R1 L1 LX|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to first line, it will be |L2 R3 R2 R1 RX L1|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(40)
        // If insert LX to first line, it will be |L2 R3 R2 R1 L1 LX|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
    }

    @Test
    fun getHorizontal_BidiText_BiDiTransitionNotLineBreakOffset_FromRTLtoLTR_RTLParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): R1 L1 L2 L3 R2 R3 R4 R5 R6 R7 R8 R9
        //
        // |R2 L1 L2 L3 R1|
        // |R7 R6 R5 R4 R3|
        // |         R9 R8|
        //
        val layout = getLayout(
            text = "\u05D0\u0061\u0062\u0063\u05D4\u05D5\u05D6\u05D7\u05D8\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = 1 // before L1
        // If insert RX to first line, it will be |R2 L1 L2 L3 RX R1|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(40)
        // If insert LX to first line, it will be |R2 LX L1 L2 L3 R1|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(10)
        // If insert RX to first line, it will be |R2 L1 L2 L3 RX R1|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(40)
        // If insert LX to first line, it will be |R2 LX L1 L2 L3 R1|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(10)
    }

    @Test
    fun getHorizontal_BidiText_BiDiTransitionNotLineBreakOffset_FromRTLtoLTR_LTRParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): R1 L1 L2 L3 R2 R3 R4 R5 R6 R7 R8 R9
        //
        // |R1 L1 L2 L3 R2|
        // |R7 R6 R5 R4 R3|
        // |R9 R8         |
        //
        val layout = getLayout(
            text = "\u05D0\u0061\u0062\u0063\u05D4\u05D5\u05D6\u05D7\u05D8\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = 1 // before L1
        // If insert LX to first line, it will be |R2 LX L1 L2 L3 R1|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(10)
        // If insert RX to first line, it will be |RX R1 L1 L2 L3 R2|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to first line, it will be |R2 LX L1 L2 L3 R1|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(10)
        // If insert RX to first line, it will be |RX R1 L1 L2 L3 R2|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }

    @Test
    fun getHorizontal_BidiText_BiDiTransitionLineBreakOffset_FromLTRToRTL_LTRParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 R1 R2 R3 R4 R5 R6 R7
        //
        // |L1 L2 L3 L4 L5|
        // |R5 R4 R3 R2 R1|
        // |R7 R6         |
        //
        val layout = getLayout(
            text = "\u0061\u0062\u0063\u0064\u0065\u05D5\u05D6\u05D7\u05D8\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = 5 // before R1 == after L5
        // If insert LX to first line, it will be |L1 L2 L3 L4 L5 LX|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to first line, it will be |L1 L2 L3 L4 L5 RX|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to second line, it will be |L1 R5 R4 R3 R2 R1|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to second line, it will be |R5 R4 R3 R2 R1 RX|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
    }

    @Test
    fun getHorizontal_BidiText_BiDiTransitionLineBreakOffset_FromLTRToRTL_RTLParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 R1 R2 R3 R4 R5 R6 R7
        //
        // |L1 L2 L3 L4 L5|
        // |R5 R4 R3 R2 R1|
        // |         R7 R6|
        //
        val layout = getLayout(
            text = "\u0061\u0062\u0063\u0064\u0065\u05D5\u05D6\u05D7\u05D8\u05DA\u05DB\u05DC",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = 5 // before R1 == after L5
        // If insert RX to first line, it will be |RX L1 L2 L3 L4 L5|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to first line, it will be |L1 L2 L3 L4 L5 LX|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to second line, it will be |R5 R4 R3 R2 R1 RX|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to second line, it will be |R5 R4 R3 R2 R1 LX|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
    }

    @Test
    fun getHorizontal_BidiText_BiDiTransitionLineBreakOffset_FromRTLToLTR_RTLParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 L1 L2 L3 L4 L5 L6 L7
        //
        // |R5 R4 R3 R2 R1|
        // |L1 L2 L3 L4 L5|
        // |         L6 L7|
        //
        val layout = getLayout(
            text = "\u05D0\u05D1\u05D2\u05D3\u05D4\u0061\u0062\u0063\u0064\u0065\u0066\u0067",
            fontSize = 10,
            width = 50,
            dir = RTL
        )

        val offset = 5 // before L1 == after R5
        // If insert RX to first line, it will be |RX R5 R4 R3 R2 R1|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to first line, it will be |LX R5 R4 R3 R2 R1|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to second line, it will be |L1 L2 L3 L4 L5 RX|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to second line, it will be |LX L1 L2 L3 L4 L5|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }

    @Test
    fun getHorizontal_BidiText_BiDiTransitionLineBreakOffset_FromRTLToLTR_LTRParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 L1 L2 L3 L4 L5 L6 L7
        //
        // |R5 R4 R3 R2 R1|
        // |L1 L2 L3 L4 L5|
        // |L6 L7         |
        //
        val layout = getLayout(
            text = "\u05D0\u05D1\u05D2\u05D3\u05D4\u0061\u0062\u0063\u0064\u0065\u0066\u0067",
            fontSize = 10,
            width = 50,
            dir = LTR
        )

        val offset = 5 // before L1 == after R5
        // If insert LX to first line, it will be |R5 R4 R3 R2 R1 LX|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to first line, it will be |RX R5 R4 R3 R2 R1|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to second line, it will be |LX L1 L2 L3 L4 L5|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to second line, it will be |R1 L1 L2 L3 L4 L5|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }

    @Test
    fun getHorizontal_BidiText_LineBreakOffset_FromMiddle_FromLTRToRTL_LTRParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 R1 R2 R3 R4 R5 R6 R7 R8 R9 RA
        //
        // |L1 L2 R3 R2 R1|
        // |R8 R7 R6 R5 R4|
        // |RA R9         |
        //
        val layout = getLayout(
            "\u0061\u0062\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7\u05D8\u05DA\u05DB",
            10,
            50,
            LTR
        )

        val offset = 5 // before R4 == after R3
        // If insert LX to first line, it will be |L1 L2 R3 R2 R1 LX|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to first line, it will be |L1 L2 RX R3 R2 R1|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(20)
        // If insert LX to second line, it will be |LX R5 R7 R6 R5 R4|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to second line, it will be |R5 R7 R6 R5 R4 RX|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
    }

    @Test
    fun getHorizontal_BidiText_LineBreakOffset_FromMiddle_FromLTRToRTL_RTLParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 R1 R2 R3 R4 R5 R6 R7 R8 R9 RA
        //
        // |R3 R2 R1 L1 L2|
        // |R8 R7 R6 R5 R4|
        // |         RA R9|
        //
        val layout = getLayout(
            "\u0061\u0062\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7\u05D8\u05DA",
            10,
            50,
            RTL
        )

        val offset = 5 // before R4 == after R3
        // If insert RX to first line, it will be |RX R3 R2 R1 L1 L2|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to first line, it will be |LX R3 R2 R1 L1 L2|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to second line, it will be |R8 R7 R6 R5 R4 RX|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to second line, it will be |R8 R7 R6 R5 R4 LX|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
    }

    @Test
    fun getHorizontal_BidiText_LineBreakOffset_FromMiddle_FromRTLToLTR_LTRParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 L1 L2 L3 L4 L5 L6 L7 L8 L9 LA
        //
        // |R2 R1 L1 L2 L3|
        // |L4 L5 L6 L7 L8|
        // |L9 RA         |
        //
        val layout = getLayout(
            "\u05D0\u05D1\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A",
            10,
            50,
            LTR
        )

        val offset = 5 // before L4 == after L3
        // If insert LX to first line, it will be |R2 R1 L1 L2 L3 LX|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to first line, it will be |R2 R1 L1 L2 L3 RX|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to second line, it will be |LX L4 L5 L6 L7 L8|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to second line, it will be |RX L4 L5 L6 L7 L8|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }

    @Test
    fun getHorizontal_BidiText_LineBreakOffset_FromMiddle_FromRTLToLTR_RTLParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 L1 L2 L3 L4 L5 L6 L7 L8 L9 LA
        //
        // |L1 L2 L3 R2 R1|
        // |L4 L5 L6 L7 L8|
        // |         L9 RA|
        //
        val layout = getLayout(
            "\u05D0\u05D1\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A",
            10,
            50,
            RTL
        )

        val offset = 5 // before L4 == after L3
        // If insert RX to first line, it will be |RX L1 L2 L3 R2 R1|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to first line, it will be |L1 L2 L3 L4 R2 R1|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(30)
        // If insert RX to second line, it will be |L4 L5 L6 L7 L8 RX|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to second line, it will be |LX L4 L5 L6 L7 L8|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }

    @Test
    fun getHorizontal_BidiText_LineBreakOffset_ToMiddle_FromLTRToRTL_LTRParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 L6 L7 R1 R2 R3 R4 R5
        //
        // |L1 L2 L3 L4 L5|
        // |L6 L7 R3 R2 R1|
        // |R5 R4         |
        //
        val layout = getLayout(
            "\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u05D0\u05D1\u05D2\u05D3\u05D4",
            10,
            50,
            LTR
        )

        val offset = 5 // before L6 == after L5
        // If insert LX to first line, it will be |L1 L2 L3 L4 L5 LX|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to first line, it will be |L1 L2 L3 L4 L5 RX|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to second line, it will be |LX L6 L7 R3 R2 R1|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to second line, it will be |RX L6 L7 R3 R2 R1|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }

    @Test
    fun getHorizontal_BidiText_LineBreakOffset_ToMiddle_FromLTRToRTL_RTLParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): L1 L2 L3 L4 L5 L6 L7 R1 R2 R3 R4 R5
        //
        // |L1 L2 L3 L4 L5|
        // |R3 R2 R1 L6 L7|
        // |         R5 R4|
        //
        val layout = getLayout(
            "\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u05D0\u05D1\u05D2\u05D3\u05D4",
            10,
            50,
            RTL
        )

        val offset = 5 // before L6 == after L5
        // If insert RX to first line, it will be |RX L1 L2 L3 L4 L5|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to first line, it will be |L1 L2 L3 L4 L5 LX|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to second line, it will be |R3 R2 R1 L6 L7 RX|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to second line, it will be |R3 R2 R1 LX L6 L7|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(30)
    }

    @Test
    fun getHorizontal_BidiText_LineBreakOffset_ToMiddle_FromRTLToLTR_LTRParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 R6 R7 L1 L2 L3 L4 L5
        //
        // |R5 R4 R3 R2 R1|
        // |R7 R6 L1 L2 L3|
        // |L4 L5         |
        //
        val layout = getLayout(
            "\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u0061\u0062\u0063\u0064\u0065",
            10,
            50,
            LTR
        )

        val offset = 5 // before R6 == after R5
        // If insert LX to first line, it will be |R5 R4 R3 R2 R1 LX|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to first line, it will be |RX R5 R4 R3 R2 R1|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to second line, it will be |LX R7 R6 L1 L2 L3|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to second line, it will be |R7 R6 RX L1 L2 L3|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(20)
    }

    @Test
    fun getHorizontal_BidiText_LineBreakOffset_ToMiddle_FromRTLToLTR_RTLParagraph() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 R3 R4 R5 R6 R7 L1 L2 L3 L4 L5
        //
        // |R5 R4 R3 R2 R1|
        // |L1 L2 L3 R7 R6|
        // |         L4 L5|
        //
        val layout = getLayout(
            "\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u0061\u0062\u0063\u0064\u0065",
            10,
            50,
            RTL
        )

        val offset = 5 // before R6 == after R5
        // If insert RX to first line, it will be |RX R5 R4 R3 R2 R1|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to first line, it will be |LX R5 R4 R3 R2 R1|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to second line, it will be |L1 L2 L3 R7 R6 RX|
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert LX to second line, it will be |L1 L2 L3 R7 R6 LX|
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(50)
    }

    @Test
    fun getHorizontal_BiDi_Whitspace() {
        // The line break happens like as follows
        //
        // input (Logical): R1 R2 SP R3 R4 R6 SP R7 R8 SP R9 RA
        //
        // |R4 R3 SP R2 R1| (SP)
        // |L1 L2 SP L3 L4| (SP)
        // |L5 L6         |
        val layout = getLayout(
            "\u05D0\u05D1 \u05D2\u05D3 \u0061\u0062 \u0063\u0064 \u0065\u0066",
            10,
            50,
            LTR
        )

        val offset = 6 // before L1 == after SP
        // If insert LX to first line, it will be |R4 R3 SP R2 R1 SP LX|
        assertThat(layout.getUpstreamPrimaryHorizontalPosition(offset)).isEqualTo(50)
        // If insert RX to first line, it will be |RX SP R4 R3 SP R2 R1|
        assertThat(layout.getUpstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert LX to second line, it will be |LX L2 SP L3 L4| (SP)
        assertThat(layout.getDownstreamPrimaryHorizontalPosition(offset)).isEqualTo(0)
        // If insert RX to second line, it will be |RX L1 L2 SP L3 L4| (SP)
        assertThat(layout.getDownstreamSecondaryHorizontalPosition(offset)).isEqualTo(0)
    }
}