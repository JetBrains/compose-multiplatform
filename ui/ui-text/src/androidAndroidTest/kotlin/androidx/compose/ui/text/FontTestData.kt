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

package androidx.compose.ui.text

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.testutils.fonts.R

class FontTestData {
    @OptIn(ExperimentalTextApi::class)
    companion object {
        // This sample font provides the following features:
        // 1. The width of most of visible characters equals to font size.
        // 2. The LTR/RTL characters are rendered as ▶/◀.
        // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
        val BASIC_MEASURE_FONT = Font(
            resId = R.font.sample_font,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        )

        // The kern_font provides the following features:
        // 1. Characters from A to Z are rendered as ▲ while a to z are rendered as ▼.
        // 2. When kerning is off, the width of each character is equal to font size.
        // 3. When kerning is on, it will reduce the space between two characters by 0.4 * width.
        val BASIC_KERN_FONT = Font(
            resId = R.font.kern_font,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        )

        val FONT_100_REGULAR = Font(
            resId = R.font.test_100_regular,
            weight = FontWeight.W100,
            style = FontStyle.Normal
        )

        val FONT_100_ITALIC = Font(
            resId = R.font.test_100_italic,
            weight = FontWeight.W100,
            style = FontStyle.Italic
        )

        val FONT_200_REGULAR = Font(
            resId = R.font.test_200_regular,
            weight = FontWeight.W200,
            style = FontStyle.Normal
        )

        val FONT_200_ITALIC = Font(
            resId = R.font.test_200_italic,
            weight = FontWeight.W200,
            style = FontStyle.Italic
        )

        val FONT_200_ITALIC_FALLBACK = Font(
            resId = R.font.test_200_italic,
            weight = FontWeight.W200,
            style = FontStyle.Italic
        )

        val FONT_300_REGULAR = Font(
            resId = R.font.test_300_regular,
            weight = FontWeight.W300,
            style = FontStyle.Normal
        )

        val FONT_300_ITALIC = Font(
            resId = R.font.test_300_italic,
            weight = FontWeight.W300,
            style = FontStyle.Italic
        )

        val FONT_400_REGULAR = Font(
            resId = R.font.test_400_regular,
            weight = FontWeight.W400,
            style = FontStyle.Normal
        )

        val FONT_400_ITALIC = Font(
            resId = R.font.test_400_italic,
            weight = FontWeight.W400,
            style = FontStyle.Italic
        )

        val FONT_500_REGULAR = Font(
            resId = R.font.test_500_regular,
            weight = FontWeight.W500,
            style = FontStyle.Normal
        )

        val FONT_500_ITALIC = Font(
            resId = R.font.test_500_italic,
            weight = FontWeight.W500,
            style = FontStyle.Italic
        )

        val FONT_600_REGULAR = Font(
            resId = R.font.test_600_regular,
            weight = FontWeight.W600,
            style = FontStyle.Normal
        )

        val FONT_600_ITALIC = Font(
            resId = R.font.test_600_italic,
            weight = FontWeight.W600,
            style = FontStyle.Italic
        )

        val FONT_700_REGULAR = Font(
            resId = R.font.test_700_regular,
            weight = FontWeight.W700,
            style = FontStyle.Normal
        )

        val FONT_700_ITALIC = Font(
            resId = R.font.test_700_italic,
            weight = FontWeight.W700,
            style = FontStyle.Italic
        )

        val FONT_800_REGULAR = Font(
            resId = R.font.test_800_regular,
            weight = FontWeight.W800,
            style = FontStyle.Normal
        )

        val FONT_800_ITALIC = Font(
            resId = R.font.test_800_italic,
            weight = FontWeight.W800,
            style = FontStyle.Italic
        )

        val FONT_900_REGULAR = Font(
            resId = R.font.test_900_regular,
            weight = FontWeight.W900,
            style = FontStyle.Normal
        )

        val FONT_900_ITALIC = Font(
            resId = R.font.test_900_italic,
            weight = FontWeight.W900,
            style = FontStyle.Italic
        )

        val FONT_INVALID = Font(
            resId = R.font.invalid_font,
            weight = FontWeight.W900,
            style = FontStyle.Italic
        )
    }
}
