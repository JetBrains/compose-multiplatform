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
import androidx.compose.ui.text.android.animation.SegmentBreaker
import androidx.compose.ui.text.android.animation.SegmentType
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
class SegmentBreakerTest {

    private val sampleTypeface = ResourcesCompat.getFont(
        InstrumentationRegistry.getInstrumentation().targetContext,
        R.font.sample_font
    )

    private val LTR = TextDirectionHeuristics.LTR
    private val RTL = TextDirectionHeuristics.RTL

    private fun getLayout(
        text: String,
        dir: TextDirectionHeuristic
    ): LayoutHelper {
        val paint = TextPaint().apply {
            textSize = 10f
            typeface = sampleTypeface
        }
        val layout = StaticLayoutFactory.create(
            text = text,
            paint = paint,
            width = 50,
            textDir = dir
        )
        return LayoutHelper(layout)
    }

    @Test
    fun testWhole() {
        val layout = getLayout("a b c d e", LTR)
        SegmentBreaker.breakOffsets(layout, SegmentType.Document).also {
            assertThat(it).isEqualTo(listOf(0, 9))
        }
    }

    @Test
    fun testParagraph() {
        val layout = getLayout("a b c\nd e", LTR)
        SegmentBreaker.breakOffsets(layout, SegmentType.Paragraph).also {
            assertThat(it).isEqualTo(listOf(0, 6, 9))
        }
    }

    @Test
    fun testLine() {
        // The input (logical): a b c d e f g h
        //
        // The text is layout as follows:
        // |a b c|
        // |d e f|
        // |g h  |
        val layout = getLayout("a b c d e f g h", LTR)
        SegmentBreaker.breakOffsets(layout, SegmentType.Line).also {
            assertThat(it).isEqualTo(listOf(0, 6, 12, 15))
        }
    }

    @Test
    fun testWords() {
        val layout = getLayout("ab cd ef gh", LTR)
        SegmentBreaker.breakOffsets(layout, SegmentType.Word).also {
            assertThat(it).isEqualTo(listOf(0, 3, 6, 9, 11))
        }
    }

    @Test
    fun testWords_bidi() {
        val layout = getLayout("aא אd אf gא", LTR)
        SegmentBreaker.breakOffsets(layout, SegmentType.Word).also {
            assertThat(it).isEqualTo(listOf(0, 1, 3, 4, 6, 7, 9, 10, 11))
        }
    }

    @Test
    fun testChar() {
        val layout = getLayout("abcdefg", LTR)
        SegmentBreaker.breakOffsets(layout, SegmentType.Character).also {
            assertThat(it).isEqualTo(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        }
    }

    @Test
    fun testChar_grapheme() {
        val layout = getLayout("\uD83D\uDE0AA\u030A", LTR)
        SegmentBreaker.breakOffsets(layout, SegmentType.Character).also {
            assertThat(it).isEqualTo(listOf(0, 2, 4))
        }
    }
}