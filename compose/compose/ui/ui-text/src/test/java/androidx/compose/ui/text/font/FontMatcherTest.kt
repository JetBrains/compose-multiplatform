/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text.font

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_100_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_100_ITALIC_FALLBACK
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_100_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_200_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_200_ITALIC_FALLBACK
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_200_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_300_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_300_ITALIC_FALLBACK
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_300_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_300_REGULAR_FALLBACK
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_400_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_400_ITALIC_FALLBACK
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_400_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_400_REGULAR_FALLBACK
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_500_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_500_ITALIC_FALLBACK
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_500_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_600_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_600_ITALIC_FALLBACK
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_600_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_600_REGULAR_FALLBACK
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_700_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_800_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_800_ITALIC_FALLBACK
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_800_REGULAR
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FontMatcherTest {

    @Test
    fun `family with single italic font matches`() {
        val font = FontMatcher().matchFont(
            FontFamily(FONT_100_ITALIC),
            FontWeight.W100,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_100_ITALIC))
    }

    @Test
    fun `family with fallback italic font matches`() {
        val font = FontMatcher().matchFont(
            FontFamily(FONT_100_ITALIC, FONT_100_ITALIC_FALLBACK),
            FontWeight.W100,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_100_ITALIC, FONT_100_ITALIC_FALLBACK))
    }

    @Test
    fun `family with single normal font matches`() {
        val font = FontMatcher().matchFont(
            FontFamily(FONT_100_REGULAR),
            FontWeight.W100,
            FontStyle.Normal
        )

        assertThat(font).isEqualTo(listOf(FONT_100_REGULAR))
    }

    @Test
    fun `italic query against family with multiple fonts matches`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_REGULAR,
                FONT_100_ITALIC,
                FONT_200_REGULAR,
                FONT_200_ITALIC
            ),
            FontWeight.W200,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_200_ITALIC))
    }

    @Test
    fun `italic query against family with multiple fonts and fallback matches`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_REGULAR,
                FONT_100_ITALIC,
                FONT_200_REGULAR,
                FONT_200_ITALIC,
                FONT_200_ITALIC_FALLBACK
            ),
            FontWeight.W200,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_200_ITALIC, FONT_200_ITALIC_FALLBACK))
    }

    @Test
    fun `normal style query against family with multiple fonts matches`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_REGULAR,
                FONT_200_ITALIC,
                FONT_200_REGULAR,
                FONT_300_REGULAR
            ),
            FontWeight.W200,
            FontStyle.Normal
        )

        assertThat(font).isEqualTo(listOf(FONT_200_REGULAR))
    }

    @Test
    fun `italic style below 400 with no exact result matches a smaller italic value`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is less than 400,
        // --> (THIS TEST) weights less than or equal to the desired weight are checked in
        //     descending order
        // --> followed by weights above the desired weight in ascending order
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_REGULAR,
                FONT_100_ITALIC,
                FONT_300_REGULAR,
                FONT_400_REGULAR,
                FONT_400_ITALIC
            ),
            FontWeight.W300,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_100_ITALIC))
    }

    @Test
    fun `italic style below 400 with no exact result matches a smaller italic and fallback`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is less than 400,
        // --> (THIS TEST) weights less than or equal to the desired weight are checked in
        //     descending order
        // --> followed by weights above the desired weight in ascending order
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_REGULAR,
                FONT_100_ITALIC,
                FONT_100_ITALIC_FALLBACK,
                FONT_300_REGULAR,
                FONT_400_REGULAR,
                FONT_400_ITALIC
            ),
            FontWeight.W300,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_100_ITALIC, FONT_100_ITALIC_FALLBACK))
    }

    @Test
    fun `italic style below 400 with no smaller weight matches larger weight and style`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is less than 400,
        // --> weights less than or equal to the desired weight are checked in
        //     descending order
        // --> (THIS TEST) followed by weights above the desired weight in ascending order
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_REGULAR,
                FONT_200_REGULAR,
                FONT_300_REGULAR,
                FONT_400_REGULAR,
                FONT_400_ITALIC
            ),
            FontWeight.W200,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_400_ITALIC))
    }

    @Test
    fun `italic style below 400 with no smaller weight matches larger weight and style fallback`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is less than 400,
        // --> weights less than or equal to the desired weight are checked in
        //     descending order
        // --> (THIS TEST) followed by weights above the desired weight in ascending order
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_REGULAR,
                FONT_200_REGULAR,
                FONT_300_REGULAR,
                FONT_400_REGULAR,
                FONT_400_ITALIC,
                FONT_400_ITALIC_FALLBACK
            ),
            FontWeight.W200,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_400_ITALIC, FONT_400_ITALIC_FALLBACK))
    }

    @Test
    fun `italic style above 500 with no match returns a larger weight`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is greater than 500,
        // --> (THIS TEST) weights greater than or equal to the desired weight are checked in
        //     ascending order
        // --> followed by weights below the desired weight in descending order
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_ITALIC,
                FONT_300_ITALIC,
                FONT_400_ITALIC,
                FONT_600_REGULAR,
                FONT_700_REGULAR,
                FONT_800_REGULAR,
                FONT_800_ITALIC
            ),
            FontWeight.W600,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_800_ITALIC))
    }

    @Test
    fun `italic style above 500 with no match returns a larger weight and fallback`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is greater than 500,
        // --> (THIS TEST) weights greater than or equal to the desired weight are checked in
        //     ascending order
        // --> followed by weights below the desired weight in descending order
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_ITALIC,
                FONT_300_ITALIC,
                FONT_400_ITALIC,
                FONT_600_REGULAR,
                FONT_700_REGULAR,
                FONT_800_REGULAR,
                FONT_800_ITALIC,
                FONT_800_ITALIC_FALLBACK
            ),
            FontWeight.W600,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_800_ITALIC, FONT_800_ITALIC_FALLBACK))
    }

    @Test
    fun `italic style above 500 with no greater weight matches a smaller weight`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is greater than 500,
        // --> weights greater than or equal to the desired weight are checked in
        //     ascending order
        // --> (THIS TEST) followed by weights below the desired weight in descending order
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_ITALIC,
                FONT_400_REGULAR,
                FONT_400_ITALIC,
                FONT_500_REGULAR,
                FONT_600_REGULAR,
                FONT_700_REGULAR
            ),
            FontWeight.W600,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_400_ITALIC))
    }

    @Test
    fun `italic style above 500 with no greater weight matches a smaller weight and fallback`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is greater than 500,
        // --> weights greater than or equal to the desired weight are checked in
        //     ascending order
        // --> (THIS TEST) followed by weights below the desired weight in descending order
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_ITALIC,
                FONT_400_REGULAR,
                FONT_400_REGULAR_FALLBACK,
                FONT_400_ITALIC,
                FONT_400_ITALIC_FALLBACK,
                FONT_500_REGULAR,
                FONT_600_REGULAR,
                FONT_700_REGULAR
            ),
            FontWeight.W600,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_400_ITALIC, FONT_400_ITALIC_FALLBACK))
    }

    @Test
    fun `italic style at 400 first check if 500 italic is there`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is inclusively between 400 and 500,
        // --> (THIS TEST) weights greater than or equal to the target weight are checked in
        //     ascending order until 500 is hit and checked,
        // --> followed by weights less than the target weight in descending order
        // --> followed by weights greater than 500,
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_ITALIC,
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_500_ITALIC,
                FONT_600_ITALIC
            ),
            FontWeight.W400,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_500_ITALIC))
    }

    @Test
    fun `italic style at 400 first check if 500 italic is there and fallback`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is inclusively between 400 and 500,
        // --> (THIS TEST) weights greater than or equal to the target weight are checked in
        //     ascending order until 500 is hit and checked,
        // --> followed by weights less than the target weight in descending order
        // --> followed by weights greater than 500,
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_ITALIC,
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_500_ITALIC,
                FONT_600_ITALIC,
                FONT_500_ITALIC_FALLBACK
            ),
            FontWeight.W400,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_500_ITALIC, FONT_500_ITALIC_FALLBACK))
    }

    @Test
    fun `italic style at 400 returns a smaller italic if 500 is not there`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is inclusively between 400 and 500,
        // --> weights greater than or equal to the target weight are checked in
        //     ascending order until 500 is hit and checked,
        // --> (THIS TEST) followed by weights less than the target weight in descending order
        // --> followed by weights greater than 500,
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_ITALIC,
                FONT_500_REGULAR,
                FONT_600_ITALIC
            ),
            FontWeight.W400,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_300_ITALIC))
    }

    @Test
    fun `italic style at 400 returns a smaller italic if 500 is not there and fallback`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is inclusively between 400 and 500,
        // --> weights greater than or equal to the target weight are checked in
        //     ascending order until 500 is hit and checked,
        // --> (THIS TEST) followed by weights less than the target weight in descending order
        // --> followed by weights greater than 500,
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_ITALIC,
                FONT_500_REGULAR,
                FONT_600_ITALIC,
                FONT_300_ITALIC_FALLBACK
            ),
            FontWeight.W400,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_300_ITALIC, FONT_300_ITALIC_FALLBACK))
    }

    @Test
    fun `italic style at 500 returns a smaller italic if exists`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is inclusively between 400 and 500,
        // --> weights greater than or equal to the target weight are checked in
        //     ascending order until 500 is hit and checked,
        // --> (THIS TEST) followed by weights less than the target weight in descending order
        // --> followed by weights greater than 500,
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_ITALIC,
                FONT_500_REGULAR,
                FONT_600_ITALIC
            ),
            FontWeight.W500,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_300_ITALIC))
    }

    @Test
    fun `italic style at 500 returns a smaller italic and fallback if exists`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is inclusively between 400 and 500,
        // --> weights greater than or equal to the target weight are checked in
        //     ascending order until 500 is hit and checked,
        // --> (THIS TEST) followed by weights less than the target weight in descending order
        // --> followed by weights greater than 500,
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_ITALIC,
                FONT_500_REGULAR,
                FONT_600_ITALIC,
                FONT_300_ITALIC_FALLBACK
            ),
            FontWeight.W500,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_300_ITALIC, FONT_300_ITALIC_FALLBACK))
    }

    @Test
    fun `italic style at 400 returns a larger italic if there is no 500 or smaller italic`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is inclusively between 400 and 500,
        // --> weights greater than or equal to the target weight are checked in
        //     ascending order until 500 is hit and checked,
        // --> followed by weights less than the target weight in descending order
        // --> (THIS TEST) followed by weights greater than 500,
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_REGULAR,
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_600_ITALIC
            ),
            FontWeight.W400,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_600_ITALIC))
    }

    @Test
    fun `italic style at 400 returns a larger italic and fallback when no 500 or smaller italic`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is inclusively between 400 and 500,
        // --> weights greater than or equal to the target weight are checked in
        //     ascending order until 500 is hit and checked,
        // --> followed by weights less than the target weight in descending order
        // --> (THIS TEST) followed by weights greater than 500,
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_REGULAR,
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_600_ITALIC,
                FONT_600_ITALIC_FALLBACK
            ),
            FontWeight.W400,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_600_ITALIC, FONT_600_ITALIC_FALLBACK))
    }

    @Test
    fun `italic style at 500 returns a larger italic if there is no smaller italic`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is inclusively between 400 and 500,
        // --> weights greater than or equal to the target weight are checked in
        //     ascending order until 500 is hit and checked,
        // --> followed by weights less than the target weight in descending order
        // --> (THIS TEST) followed by weights greater than 500,
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_REGULAR,
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_600_ITALIC
            ),
            FontWeight.W500,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_600_ITALIC))
    }

    @Test
    fun `italic style at 500 returns a larger italic if there is no smaller italic and fallback`() {
        // https://www.w3.org/TR/css-fonts-4/#font-style-matching
        // If the desired weight is inclusively between 400 and 500,
        // --> weights greater than or equal to the target weight are checked in
        //     ascending order until 500 is hit and checked,
        // --> followed by weights less than the target weight in descending order
        // --> (THIS TEST) followed by weights greater than 500,
        // until a match is found.
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_300_REGULAR,
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_600_ITALIC,
                FONT_600_ITALIC_FALLBACK
            ),
            FontWeight.W500,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_600_ITALIC, FONT_600_ITALIC_FALLBACK))
    }

    @Test
    fun `italic style returns another italic if exists`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_REGULAR,
                FONT_200_REGULAR,
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_600_ITALIC
            ),
            FontWeight.W100,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_600_ITALIC))
    }

    @Test
    fun `italic style returns another italic and fallback if exists`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_REGULAR,
                FONT_200_REGULAR,
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_600_ITALIC,
                FONT_600_ITALIC_FALLBACK
            ),
            FontWeight.W100,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_600_ITALIC, FONT_600_ITALIC_FALLBACK))
    }

    @Test
    fun `italic style returns another italic if exists even when smaller`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_ITALIC,
                FONT_200_REGULAR,
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_600_REGULAR
            ),
            FontWeight.W600,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_100_ITALIC))
    }

    @Test
    fun `italic style returns another italic and fallback if exists even when smaller`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_ITALIC,
                FONT_200_REGULAR,
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_600_REGULAR,
                FONT_100_ITALIC_FALLBACK
            ),
            FontWeight.W600,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_100_ITALIC, FONT_100_ITALIC_FALLBACK))
    }

    @Test
    fun `italic style returns same weight regular if no other italic exists`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_200_REGULAR,
                FONT_300_REGULAR,
                FONT_400_REGULAR
            ),
            FontWeight.W300,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_300_REGULAR))
    }

    @Test
    fun `italic style returns same weight regular and fallback if no other italic exists`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_200_REGULAR,
                FONT_300_REGULAR,
                FONT_300_REGULAR_FALLBACK,
                FONT_400_REGULAR
            ),
            FontWeight.W300,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_300_REGULAR, FONT_300_REGULAR_FALLBACK))
    }

    @Test
    fun `italic style at 600 returns larger weight regular if no other italic exists`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_600_REGULAR,
                FONT_700_REGULAR
            ),
            FontWeight.W600,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_600_REGULAR))
    }

    @Test
    fun `italic style at 600 returns larger weight regular and fallback if no other italic`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_400_REGULAR,
                FONT_500_REGULAR,
                FONT_600_REGULAR,
                FONT_700_REGULAR,
                FONT_600_REGULAR_FALLBACK
            ),
            FontWeight.W600,
            FontStyle.Italic
        )

        assertThat(font).isEqualTo(listOf(FONT_600_REGULAR, FONT_600_REGULAR_FALLBACK))
    }

    @Test
    fun filterByClosestWeight_onlyChoosesExactWeight_whenPreferBelow() {
        val fonts = mutableListOf<Font>()
        for (weight in 400..500) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        val result = with(FontMatcher()) {
            fonts.filterByClosestWeight(
                fontWeight = FontWeight(417),
                preferBelow = true,
                minSearchRange = null,
                maxSearchRange = null
            )
        }

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(fonts.filter { it.weight.weight == 417 })
    }

    @Test
    fun filterByClosestWeight_onlyChoosesExactWeight_whenPreferAbove() {
        val fonts = mutableListOf<Font>()
        for (weight in 400..500) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        val result = with(FontMatcher()) {
            fonts.filterByClosestWeight(
                fontWeight = FontWeight(417),
                preferBelow = false,
                minSearchRange = null,
                maxSearchRange = null
            )
        }

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(fonts.filter { it.weight.weight == 417 })
    }

    @Test
    fun filterByClosestWeight_onlyChoosesClosestAbove_whenPreferAbove() {
        val fonts = mutableListOf<Font>()
        for (weight in 400..416) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        for (weight in 418..500) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        val result = with(FontMatcher()) {
            fonts.filterByClosestWeight(
                fontWeight = FontWeight(417),
                preferBelow = false,
                minSearchRange = null,
                maxSearchRange = null
            )
        }

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(fonts.filter { it.weight.weight == 418 })
    }

    @Test
    fun filterByClosestWeight_onlyChoosesClosestBelow_whenNoneAbove_whenPreferAbove() {
        val fonts = mutableListOf<Font>()
        for (weight in 400..416) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        val result = with(FontMatcher()) {
            fonts.filterByClosestWeight(
                fontWeight = FontWeight(417),
                preferBelow = false,
                minSearchRange = null,
                maxSearchRange = null
            )
        }

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(fonts.filter { it.weight.weight == 416 })
    }

    @Test
    fun filterByClosestWeight_onlyChoosesClosestBelow_whenPreferBelow() {
        val fonts = mutableListOf<Font>()
        for (weight in 400..416) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        for (weight in 418..500) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        val result = with(FontMatcher()) {
            fonts.filterByClosestWeight(
                fontWeight = FontWeight(417),
                preferBelow = true,
                minSearchRange = null,
                maxSearchRange = null
            )
        }

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(fonts.filter { it.weight.weight == 416 })
    }

    @Test
    fun filterByClosestWeight_onlyChoosesClosestAbove_whenNoneBelow_whenPreferBelow() {
        val fonts = mutableListOf<Font>()
        for (weight in 418..500) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        val result = with(FontMatcher()) {
            fonts.filterByClosestWeight(
                fontWeight = FontWeight(417),
                preferBelow = true,
                minSearchRange = null,
                maxSearchRange = null
            )
        }

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(fonts.filter { it.weight.weight == 418 })
    }

    @Test
    fun filterByClosestWeight_respectsMinSearchRange() {
        val fonts = mutableListOf<Font>()
        for (weight in 400..500) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        val result = with(FontMatcher()) {
            fonts.filterByClosestWeight(
                fontWeight = FontWeight(417),
                preferBelow = true,
                minSearchRange = FontWeight(450),
                maxSearchRange = null
            )
        }

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(fonts.filter { it.weight.weight == 450 })
    }

    @Test
    fun filterByClosestWeight_respectsMaxSearchRange() {
        val fonts = mutableListOf<Font>()
        for (weight in 400..500) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        val result = with(FontMatcher()) {
            fonts.filterByClosestWeight(
                fontWeight = FontWeight(417),
                preferBelow = true,
                minSearchRange = null,
                maxSearchRange = FontWeight(401),
            )
        }

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(fonts.filter { it.weight.weight == 401 })
    }

    @Test
    fun filterByClosestWeight_respectsMinAndMaxSearchRange_evenIfConflicting() {
        val fonts = mutableListOf<Font>()
        for (weight in 100 until 1000) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        val result = with(FontMatcher()) {
            fonts.filterByClosestWeight(
                fontWeight = FontWeight(417),
                preferBelow = true,
                minSearchRange = FontWeight(500),
                maxSearchRange = FontWeight(400),
            )
        }

        assertThat(result).isEmpty()
    }

    @Test
    fun filterByClosestWeight_respectsMinAndMaxSearchRangeAbove() {
        val fonts = mutableListOf<Font>()
        for (weight in 100 until 1000) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        val result = with(FontMatcher()) {
            fonts.filterByClosestWeight(
                fontWeight = FontWeight(417),
                preferBelow = true,
                minSearchRange = FontWeight(900),
                maxSearchRange = FontWeight(900),
            )
        }

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(fonts.filter { it.weight.weight == 900 })
    }

    @Test
    fun filterByClosestWeight_respectsMinAndMaxSearchRange_whenBelow() {
        val fonts = mutableListOf<Font>()
        for (weight in 100 until 1000) {
            fonts.add(FontForWeight(weight))
            fonts.add(FontForWeight(weight))
        }
        val result = with(FontMatcher()) {
            fonts.filterByClosestWeight(
                fontWeight = FontWeight(417),
                preferBelow = true,
                minSearchRange = FontWeight(100),
                maxSearchRange = FontWeight(100),
            )
        }

        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(fonts.filter { it.weight.weight == 100 })
    }

    private fun FontForWeight(it: Int) = object : Font {
        override val weight: FontWeight = FontWeight(it)
        override val style: FontStyle = FontStyle.Normal

        @ExperimentalTextApi
        override val loadingStrategy: FontLoadingStrategy = FontLoadingStrategy.Blocking
    }
}