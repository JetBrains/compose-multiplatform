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

package androidx.compose.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.foundation.text.selection.TextSelectionColors
import kotlin.math.max
import kotlin.math.min

/**
 * Remembers a [TextSelectionColors] based on [colors]. The handle color will be [Colors.primary] and
 * the background color will be [Colors.primary] with alpha applied.
 *
 * See [calculateSelectionBackgroundColor].
 */
@Composable
internal fun rememberTextSelectionColors(colors: Colors): TextSelectionColors {
    val primaryColor = colors.primary
    val backgroundColor = colors.background
    // Test with ContentAlpha.medium to ensure that the selection background is accessible in the
    // 'worst case' scenario. We explicitly don't test with ContentAlpha.disabled, as disabled
    // text shouldn't be selectable / is noted as disabled for accessibility purposes.
    val textColorWithLowestAlpha = colors.contentColorFor(backgroundColor)
        .takeOrElse {
            LocalContentColor.current
        }.copy(
            alpha = ContentAlpha.medium
        )
    return remember(primaryColor, backgroundColor, textColorWithLowestAlpha) {
        TextSelectionColors(
            handleColor = colors.primary,
            backgroundColor = calculateSelectionBackgroundColor(
                selectionColor = primaryColor,
                textColor = textColorWithLowestAlpha,
                backgroundColor = backgroundColor
            )
        )
    }
}

/**
 * Best-effort calculates a color (with alpha) for the selection background that (if possible)
 * will have at least [DesiredContrastRatio] with [textColor], when the selection background
 * is on top of [backgroundColor].
 *
 * Since this is a minimum contrast ratio, [textColor] should have the lowest alpha that
 * may be applied to content so we can ensure that the selection background color is accessible
 * in that worst-case scenario for contrast.
 *
 * @param selectionColor the 'raw' (without alpha) selection color that we should search alpha for
 * @param textColor the color of text with minimal alpha applied to test for contrast with
 * @param backgroundColor the color of the background that the selection color will typically be
 * placed against
 *
 * @return a resulting [selectionColor] with alpha applied that results in acceptable contrast
 * (if possible with the values for [selectionColor], [textColor] and [backgroundColor]).
 */
/*@VisibleForTesting*/
internal fun calculateSelectionBackgroundColor(
    selectionColor: Color,
    textColor: Color,
    backgroundColor: Color
): Color {
    val maximumContrastRatio = calculateContrastRatio(
        selectionColor = selectionColor,
        selectionColorAlpha = DefaultSelectionBackgroundAlpha,
        textColor = textColor,
        backgroundColor = backgroundColor
    )

    val minimumContrastRatio = calculateContrastRatio(
        selectionColor = selectionColor,
        selectionColorAlpha = MinimumSelectionBackgroundAlpha,
        textColor = textColor,
        backgroundColor = backgroundColor
    )

    val alpha = when {
        // If the default alpha has enough contrast, use that
        maximumContrastRatio >= DesiredContrastRatio -> DefaultSelectionBackgroundAlpha
        // If the minimum alpha still does not have enough contrast, just use the minimum and return
        minimumContrastRatio < DesiredContrastRatio -> MinimumSelectionBackgroundAlpha
        else -> binarySearchForAccessibleSelectionColorAlpha(
            selectionColor = selectionColor,
            textColor = textColor,
            backgroundColor = backgroundColor
        )
    }

    return selectionColor.copy(alpha = alpha)
}

/**
 * Binary searches for the highest alpha for selection color that results in a contrast ratio at
 * least equal to and within 1% of [DesiredContrastRatio].
 *
 * The resulting alpha will be within the range of [MinimumSelectionBackgroundAlpha] and
 * [DefaultSelectionBackgroundAlpha] - since not all values for [selectionColor], [textColor] and
 * [backgroundColor] can be guaranteed to produce an accessible contrast ratio, this is a
 * best-effort attempt and [MinimumSelectionBackgroundAlpha] might still not produce an
 * accessible contrast ratio. In this case developers are encouraged to manually choose a
 * different color for selection that _is_ accessible with their chosen content and background
 * colors.
 *
 * Caps the number of attempts at 7 for performance and to avoid infinite searching when there is
 * no value that results in an accessible contrast ratio. Because alpha is limited to [0,1], 7
 * steps results in a precision of ~0.01, since log2(1/0.01) ≈ 7.
 *
 * Note: binary searching here is chosen since it is not possible to 'solve' for alpha, since the
 * transformation from color -> contrast ratio is not linear (the gamma exponent for sRGB colors
 * is 2.4). We can approximate this to 2, but this results in not that accurate solutions, and we
 * need to guarantee that they are at least above [DesiredContrastRatio] - falling just below is
 * not an acceptable result.
 *
 * @param selectionColor the 'raw' (without alpha) selection color that we should search alpha for
 * @param textColor the color of text with minimal alpha applied to test for contrast with
 * @param backgroundColor the color of the background that the selection color will typically be
 * placed against
 */
private fun binarySearchForAccessibleSelectionColorAlpha(
    selectionColor: Color,
    textColor: Color,
    backgroundColor: Color
): Float {
    var attempts = 0
    val maxAttempts = 7

    var lowAlpha = MinimumSelectionBackgroundAlpha
    var alpha = DefaultSelectionBackgroundAlpha
    var highAlpha = DefaultSelectionBackgroundAlpha

    while (attempts < maxAttempts) {
        val contrastRatio = calculateContrastRatio(
            selectionColor = selectionColor,
            selectionColorAlpha = alpha,
            textColor = textColor,
            backgroundColor = backgroundColor
        )

        // Percentage error of the calculated contrast compared to the actual contrast. Positive
        // numbers here mean we have higher contrast than needed.
        val percentageError = (contrastRatio / DesiredContrastRatio) - 1f
        when {
            // Contrast is at most 1% above the guideline, return
            percentageError in 0f..0.01f -> break
            // Contrast too low, decrease alpha
            percentageError < 0f -> highAlpha = alpha
            // Contrast higher than required, increase alpha
            else -> lowAlpha = alpha
        }
        alpha = (highAlpha + lowAlpha) / 2f
        attempts++
    }

    return alpha
}

// TODO: this and other utilities might want to be commonized / made public at some point.
/**
 * Calculates the contrast ratio of [textColor] against [selectionColor] with
 * [selectionColorAlpha], all on top of [backgroundColor].
 *
 * Both the [selectionColor] and [textColor] will be composited to handle transparency.
 *
 * @param selectionColor the 'raw' (without alpha) selection color that we should search alpha for
 * @param selectionColorAlpha the alpha for [selectionColor] to test contrast with
 * @param textColor the color of text with minimal alpha applied to test for contrast with
 * @param backgroundColor the color of the background that the selection color will typically be
 * placed against
 *
 * @return the contrast ratio as a value between 1 and 21. See [calculateContrastRatio]
 */
private fun calculateContrastRatio(
    selectionColor: Color,
    selectionColorAlpha: Float,
    textColor: Color,
    backgroundColor: Color
): Float {
    val compositeBackground = selectionColor.copy(alpha = selectionColorAlpha)
        .compositeOver(backgroundColor)
    val compositeTextColor = textColor.compositeOver(compositeBackground)
    return calculateContrastRatio(compositeTextColor, compositeBackground)
}

/**
 * Calculates the contrast ratio of [foreground] against [background], returning a value between
 * 1 and 21. (1:1 and 21:1 ratios).
 *
 * Formula taken from [WCAG 2.0](https://www.w3.org/TR/UNDERSTANDING-WCAG20/visual-audio-contrast-contrast.html#contrast-ratiodef)
 *
 * Note: [foreground] and [background] *must* be opaque. See [Color.compositeOver] to pre-composite
 * a translucent foreground over the background.
 *
 * @return the contrast ratio as a value between 1 and 21. See [calculateContrastRatio]
 */
/*@VisibleForTesting*/
internal fun calculateContrastRatio(foreground: Color, background: Color): Float {
    val foregroundLuminance = foreground.luminance() + 0.05f
    val backgroundLuminance = background.luminance() + 0.05f

    return max(foregroundLuminance, backgroundLuminance) /
        min(foregroundLuminance, backgroundLuminance)
}

/**
 * Default selection background alpha - we will try and use this if it is accessible and produces
 * the correct contrast ratio.
 */
private const val DefaultSelectionBackgroundAlpha = 0.4f

/**
 * Not all combinations of text color and selection color will have a reasonable alpha that
 * produces a contrast ratio of at least [DesiredContrastRatio] - in this case just pick a low
 * but still visible alpha so at least the contrast ratio is as good as it can be - this is
 * preferable to crashing at runtime.
 */
private const val MinimumSelectionBackgroundAlpha = DefaultSelectionBackgroundAlpha / 2f

/**
 * Material and WCAG 2.0 sc 1.4.3 minimum contrast for AA text
 */
private const val DesiredContrastRatio = 4.5f
