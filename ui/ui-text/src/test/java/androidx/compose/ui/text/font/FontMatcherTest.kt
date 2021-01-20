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

import androidx.compose.ui.text.font.FontTestData.Companion.FONT_100_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_100_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_200_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_200_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_300_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_300_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_400_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_400_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_500_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_500_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_600_ITALIC
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_600_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_700_REGULAR
import androidx.compose.ui.text.font.FontTestData.Companion.FONT_800_ITALIC
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

        assertThat(font).isEqualTo(FONT_100_ITALIC)
    }

    @Test
    fun `family with single normal font matches`() {
        val font = FontMatcher().matchFont(
            FontFamily(FONT_100_REGULAR),
            FontWeight.W100,
            FontStyle.Normal
        )

        assertThat(font).isEqualTo(FONT_100_REGULAR)
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

        assertThat(font).isEqualTo(FONT_200_ITALIC)
    }

    @Test
    fun `normal style query against family with multiple fonts matches`() {
        val font = FontMatcher().matchFont(
            FontFamily(
                FONT_100_REGULAR,
                FONT_200_REGULAR,
                FONT_300_REGULAR
            ),
            FontWeight.W200,
            FontStyle.Normal
        )

        assertThat(font).isEqualTo(FONT_200_REGULAR)
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

        assertThat(font).isEqualTo(FONT_100_ITALIC)
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

        assertThat(font).isEqualTo(FONT_400_ITALIC)
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

        assertThat(font).isEqualTo(FONT_800_ITALIC)
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

        assertThat(font).isEqualTo(FONT_400_ITALIC)
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

        assertThat(font).isEqualTo(FONT_500_ITALIC)
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

        assertThat(font).isEqualTo(FONT_300_ITALIC)
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

        assertThat(font).isEqualTo(FONT_300_ITALIC)
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

        assertThat(font).isEqualTo(FONT_600_ITALIC)
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

        assertThat(font).isEqualTo(FONT_600_ITALIC)
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

        assertThat(font).isEqualTo(FONT_600_ITALIC)
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

        assertThat(font).isEqualTo(FONT_100_ITALIC)
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

        assertThat(font).isEqualTo(FONT_300_REGULAR)
    }

    @Test
    fun `italic style at 500 returns larger weight regular if no other italic exists`() {
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

        assertThat(font).isEqualTo(FONT_600_REGULAR)
    }
}