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

package androidx.compose.ui.text.style

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class LineBreakTest : TextLineBreaker() {
    private val textToLineBreak = "This is an example text"

    @Test
    fun correct_simple_preset_flags() {
        val lineBreak = LineBreak.Simple

        assertThat(lineBreak.strategy).isEqualTo(LineBreak.Strategy.Simple)
        assertThat(lineBreak.strictness).isEqualTo(LineBreak.Strictness.Normal)
        assertThat(lineBreak.wordBreak).isEqualTo(LineBreak.WordBreak.Default)
    }

    @Test
    fun correct_heading_preset_flags() {
        val lineBreak = LineBreak.Heading

        assertThat(lineBreak.strategy).isEqualTo(LineBreak.Strategy.Balanced)
        assertThat(lineBreak.strictness).isEqualTo(LineBreak.Strictness.Loose)
        assertThat(lineBreak.wordBreak).isEqualTo(LineBreak.WordBreak.Phrase)
    }

    @Test
    fun correct_paragraph_preset_flags() {
        val lineBreak = LineBreak.Paragraph

        assertThat(lineBreak.strategy).isEqualTo(LineBreak.Strategy.HighQuality)
        assertThat(lineBreak.strictness).isEqualTo(LineBreak.Strictness.Strict)
        assertThat(lineBreak.wordBreak).isEqualTo(LineBreak.WordBreak.Default)
    }

    @Test
    fun equals_different_strategy_returns_false() {
        val lineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )
        val otherLineBreak = LineBreak(
            strategy = LineBreak.Strategy.HighQuality,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )

        assertThat(lineBreak.equals(otherLineBreak)).isFalse()
    }

    @Test
    fun equals_different_style_returns_false() {
        val lineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )
        val otherLineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Loose,
            wordBreak = LineBreak.WordBreak.Phrase
        )

        assertThat(lineBreak.equals(otherLineBreak)).isFalse()
    }

    @Test
    fun equals_different_wordBreak_returns_false() {
        val lineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )
        val otherLineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Default
        )

        assertThat(lineBreak.equals(otherLineBreak)).isFalse()
    }

    @Test
    fun equals_same_flags_returns_true() {
        val lineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )
        val otherLineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )

        assertThat(lineBreak.equals(otherLineBreak)).isTrue()
    }

    @Test
    fun hashCode_different_for_different_strategy() {
        val lineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )
        val otherLineBreak = LineBreak(
            strategy = LineBreak.Strategy.HighQuality,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )

        assertThat(lineBreak.hashCode()).isNotEqualTo(otherLineBreak.hashCode())
    }

    @Test
    fun hashCode_different_for_different_style() {
        val lineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )
        val otherLineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Loose,
            wordBreak = LineBreak.WordBreak.Phrase
        )

        assertThat(lineBreak.hashCode()).isNotEqualTo(otherLineBreak.hashCode())
    }

    @Test
    fun hashCode_different_for_different_wordBreak() {
        val lineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )
        val otherLineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Default
        )

        assertThat(lineBreak.hashCode()).isNotEqualTo(otherLineBreak.hashCode())
    }

    @Test
    fun hashCode_same_for_same_flags() {
        val lineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )
        val otherLineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )

        assertThat(lineBreak.hashCode()).isEqualTo(otherLineBreak.hashCode())
    }

    @Test
    fun correct_lineBreak_simple_result_narrow() {
        val expectedBrokenLines = listOf(
            "This is ",
            "an ",
            "example ",
            "text"
        )
        val brokenLines = breakTextIntoLines(
            text = textToLineBreak,
            lineBreak = LineBreak.Simple,
            maxWidth = 50
        )

        assertThat(brokenLines).isEqualTo(expectedBrokenLines)
    }

    @Test
    fun correct_lineBreak_simple_result_wide() {
        val expectedBrokenLines = listOf(
            "This is an example ",
            "text"
        )
        val brokenLines = breakTextIntoLines(
            text = textToLineBreak,
            lineBreak = LineBreak.Simple,
            maxWidth = 120
        )

        assertThat(brokenLines).isEqualTo(expectedBrokenLines)
    }

    @Test
    fun correct_lineBreak_paragraph_result() {
        val expectedBrokenLines = listOf(
            "This ",
            "is an ",
            "example ",
            "text"
        )
        val brokenLines = breakTextIntoLines(
            text = textToLineBreak,
            lineBreak = LineBreak.Paragraph,
            maxWidth = 50
        )

        assertThat(brokenLines).isEqualTo(expectedBrokenLines)
    }

    @Test
    fun correct_lineBreak_heading_result() {
        val expectedBrokenLines = listOf(
            "This is an ",
            "example text"
        )
        val brokenLines = breakTextIntoLines(
            text = textToLineBreak,
            lineBreak = LineBreak.Heading,
            maxWidth = 120
        )

        assertThat(brokenLines).isEqualTo(expectedBrokenLines)
    }
}