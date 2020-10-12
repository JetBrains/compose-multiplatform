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

class FontTestData {
    companion object {
        private val resourceId = 1

        val BASIC_MEASURE_FONT = font(
            resId = resourceId,
            weight = FontWeight.W100,
            style = FontStyle.Normal
        )

        val FONT_100_REGULAR = font(
            resId = resourceId,
            weight = FontWeight.W100,
            style = FontStyle.Normal
        )

        val FONT_100_ITALIC = font(
            resId = resourceId,
            weight = FontWeight.W100,
            style = FontStyle.Italic
        )

        val FONT_200_REGULAR = font(
            resId = resourceId,
            weight = FontWeight.W200,
            style = FontStyle.Normal
        )

        val FONT_200_ITALIC = font(
            resId = resourceId,
            weight = FontWeight.W200,
            style = FontStyle.Italic
        )

        val FONT_300_REGULAR = font(
            resId = resourceId,
            weight = FontWeight.W300,
            style = FontStyle.Normal
        )

        val FONT_300_ITALIC = font(
            resId = resourceId,
            weight = FontWeight.W300,
            style = FontStyle.Italic
        )

        val FONT_400_REGULAR = font(
            resId = resourceId,
            weight = FontWeight.W400,
            style = FontStyle.Normal
        )

        val FONT_400_ITALIC = font(
            resId = resourceId,
            weight = FontWeight.W400,
            style = FontStyle.Italic
        )

        val FONT_500_REGULAR = font(
            resId = resourceId,
            weight = FontWeight.W500,
            style = FontStyle.Normal
        )

        val FONT_500_ITALIC = font(
            resId = resourceId,
            weight = FontWeight.W500,
            style = FontStyle.Italic
        )

        val FONT_600_REGULAR = font(
            resId = resourceId,
            weight = FontWeight.W600,
            style = FontStyle.Normal
        )

        val FONT_600_ITALIC = font(
            resId = resourceId,
            weight = FontWeight.W600,
            style = FontStyle.Italic
        )

        val FONT_700_REGULAR = font(
            resId = resourceId,
            weight = FontWeight.W700,
            style = FontStyle.Normal
        )

        val FONT_700_ITALIC = font(
            resId = resourceId,
            weight = FontWeight.W700,
            style = FontStyle.Italic
        )

        val FONT_800_REGULAR = font(
            resId = resourceId,
            weight = FontWeight.W800,
            style = FontStyle.Normal
        )

        val FONT_800_ITALIC = font(
            resId = resourceId,
            weight = FontWeight.W800,
            style = FontStyle.Italic
        )

        val FONT_900_REGULAR = font(
            resId = resourceId,
            weight = FontWeight.W900,
            style = FontStyle.Normal
        )

        val FONT_900_ITALIC = font(
            resId = resourceId,
            weight = FontWeight.W900,
            style = FontStyle.Italic
        )
    }
}