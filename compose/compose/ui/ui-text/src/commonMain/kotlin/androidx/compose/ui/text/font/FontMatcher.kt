/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.compose.ui.text.fastFilter

/**
 * Given a [FontFamily], [FontWeight] and [FontStyle], matches the best font in the [FontFamily]
 * that satisfies the requirements of [FontWeight] and [FontStyle].
 *
 * For the case without font synthesis, applies the rules at
 * [CSS 4 Font Matching](https://www.w3.org/TR/css-fonts-4/#font-style-matching).
 */
internal class FontMatcher {

    /**
     * Given a [FontFamily], [FontWeight] and [FontStyle], matches the best font in the
     * [FontFamily] that satisfies the requirements of [FontWeight] and [FontStyle]. If there is
     * not a font that exactly satisfies the given constraints of [FontWeight] and [FontStyle], the
     * best match will be returned. The rules for the best match are defined in
     * [CSS 4 Font Matching](https://www.w3.org/TR/css-fonts-4/#font-style-matching).
     *
     * If no fonts match, an empty list is returned.
     *
     * @param fontList iterable of fonts to choose the [Font] from
     * @param fontWeight desired [FontWeight]
     * @param fontStyle desired [FontStyle]
     */
    fun matchFont(
        fontList: List<Font>,
        fontWeight: FontWeight,
        fontStyle: FontStyle
    ): List<Font> {
        // check for exact match first
        fontList.fastFilter { it.weight == fontWeight && it.style == fontStyle }.let {
            // TODO(b/130797349): IR compiler bug was here
            if (it.isNotEmpty()) {
                return it
            }
        }

        // if no exact match, filter with style
        val fontsToSearch = fontList.fastFilter { it.style == fontStyle }.ifEmpty { fontList }

        val result = when {
            fontWeight < FontWeight.W400 -> {
                // If the desired weight is less than 400
                // - weights less than or equal to the desired weight are checked in descending order
                // - followed by weights above the desired weight in ascending order

                fontsToSearch.filterByClosestWeight(fontWeight, preferBelow = true)
            }
            fontWeight > FontWeight.W500 -> {
                // If the desired weight is greater than 500
                // - weights greater than or equal to the desired weight are checked in ascending order
                // - followed by weights below the desired weight in descending order
                fontsToSearch.filterByClosestWeight(fontWeight, preferBelow = false)
            }
            else -> {
                // If the desired weight is inclusively between 400 and 500
                // - weights greater than or equal to the target weight are checked in ascending order
                // until 500 is hit and checked,
                // - followed by weights less than the target weight in descending order,
                // - followed by weights greater than 500
                fontsToSearch
                    .filterByClosestWeight(
                        fontWeight,
                        preferBelow = false,
                        minSearchRange = null,
                        maxSearchRange = FontWeight.W500
                    )
                    .ifEmpty {
                        fontsToSearch.filterByClosestWeight(
                            fontWeight,
                            preferBelow = false,
                            minSearchRange = FontWeight.W500,
                            maxSearchRange = null
                        )
                    }
            }
        }

        return result
    }

    @Suppress("NOTHING_TO_INLINE")
    // @VisibleForTesting
    internal inline fun List<Font>.filterByClosestWeight(
        fontWeight: FontWeight,
        preferBelow: Boolean,
        minSearchRange: FontWeight? = null,
        maxSearchRange: FontWeight? = null,
    ): List<Font> {
        var bestWeightAbove: FontWeight? = null
        var bestWeightBelow: FontWeight? = null
        for (index in indices) {
            val font = get(index)
            val possibleWeight = font.weight
            if (minSearchRange != null && possibleWeight < minSearchRange) { continue }
            if (maxSearchRange != null && possibleWeight > maxSearchRange) { continue }
            if (possibleWeight < fontWeight) {
                if (bestWeightBelow == null || possibleWeight > bestWeightBelow) {
                    bestWeightBelow = possibleWeight
                }
            } else if (possibleWeight > fontWeight) {
                if (bestWeightAbove == null || possibleWeight < bestWeightAbove) {
                    bestWeightAbove = possibleWeight
                }
            } else {
                // exact weight match, we can exit now
                bestWeightAbove = possibleWeight
                bestWeightBelow = possibleWeight
                break
            }
        }
        val bestWeight = if (preferBelow) {
            bestWeightBelow ?: bestWeightAbove
        } else {
            bestWeightAbove ?: bestWeightBelow
        }
        return fastFilter { it.weight == bestWeight }
    }

    /**
     * @see matchFont
     */
    fun matchFont(
        fontFamily: FontFamily,
        fontWeight: FontWeight,
        fontStyle: FontStyle
    ): List<Font> {
        if (fontFamily !is FontListFontFamily) throw IllegalArgumentException(
            "Only FontFamily instances that presents a list of Fonts can be used"
        )

        return matchFont(fontFamily, fontWeight, fontStyle)
    }

    /**
     * Required to disambiguate matchFont(fontListFontFamilyInstance).
     *
     * @see matchFont
     */
    fun matchFont(
        fontFamily: FontListFontFamily,
        fontWeight: FontWeight,
        fontStyle: FontStyle
    ): List<Font> {
        return matchFont(fontFamily.fonts, fontWeight, fontStyle)
    }
}