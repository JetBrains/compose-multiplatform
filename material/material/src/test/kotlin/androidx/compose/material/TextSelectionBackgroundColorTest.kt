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
package androidx.compose.material

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test for [calculateSelectionBackgroundColor] to ensure that the resulting colors meet contrast
 * requirements.
 */
@RunWith(Parameterized::class)
class TextSelectionBackgroundColorTest(
    // Needs to be nullable for when this is instantiated from reflection since Color is inline
    primary: Color?
) {
    private val color = primary!!

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "color={0}")
        fun colors(): Array<Any> = arrayOf(
            // Baseline palette primary
            Color(0xFF6200EE),
            // 500 colors from the Material 2014 palette
            Color(0xFFF44336),
            Color(0xFFE91E63),
            Color(0xFF9C27B0),
            Color(0xFF673AB7),
            Color(0xFF3F51B5),
            Color(0xFF2196F3),
            Color(0xFF03A9F4),
            Color(0xFF00BCD4),
            Color(0xFF009688),
            Color(0xFF4CAF50),
            Color(0xFF8BC34A),
            Color(0xFFCDDC39),
            Color(0xFFFFEB3B),
            Color(0xFFFFC107),
            Color(0xFFFF9800),
            Color(0xFFFF5722),
            Color(0xFF795548),
            Color(0xFF9E9E9E),
            Color(0xFF607D8B)
        ).map {
            // Need to pass `null` as Color is an inline class and the `null` is needed when
            // reflectively instantiating the class
            arrayOf(it, null)
        }.toTypedArray()
    }

    @Test
    fun assertContrast_blackTextOnWhiteBackground() {
        assertContrastRatio(
            selectionColor = color,
            textColor = Color.Black,
            backgroundColor = Color.White
        )
    }

    @Test
    fun assertContrast_whiteTextOnBlackBackground() {
        assertContrastRatio(
            selectionColor = color,
            textColor = Color.White,
            backgroundColor = Color.Black
        )
    }

    @Test
    fun assertContrast_blackTextOnPrimaryBackground() {
        assertContrastRatio(
            selectionColor = color,
            textColor = Color.Black,
            backgroundColor = color
        )
    }

    @Test
    fun assertContrast_whiteTextOnPrimaryBackground() {
        assertContrastRatio(
            selectionColor = color,
            textColor = Color.White,
            backgroundColor = color
        )
    }
}

private fun assertContrastRatio(
    selectionColor: Color,
    textColor: Color,
    backgroundColor: Color
) {
    val selectionBackgroundColor = calculateSelectionBackgroundColor(
        selectionColor = selectionColor,
        textColor = textColor,
        backgroundColor = backgroundColor
    )

    // If the minimum alpha we allow still does not provide enough contrast, then we fall back to
    // using the default alpha for consistency with the spec.
    val minimumCompositeBackground = selectionBackgroundColor.copy(alpha = MinimumSelectionAlpha)
        .compositeOver(backgroundColor)
    val minimumCompositeTextColor = textColor.compositeOver(minimumCompositeBackground)
    val minimumContrastRatio = calculateContrastRatio(
        foreground = minimumCompositeTextColor,
        background = minimumCompositeBackground
    )

    if (minimumContrastRatio < RequiredContrastRatio) {
        assertThat(selectionBackgroundColor.alpha).isEqualTo(MinimumSelectionAlpha)
        return
    }

    // Otherwise, ensure that the value we choose is accessible
    val compositeBackground = selectionBackgroundColor.compositeOver(backgroundColor)
    val compositeTextColor = textColor.compositeOver(compositeBackground)
    val contrastRatio = calculateContrastRatio(
        foreground = compositeTextColor,
        background = compositeBackground
    )

    // The contrast ratio must always be >= 4.5
    assertThat(contrastRatio).isAtLeast(RequiredContrastRatio)
    if (selectionBackgroundColor.alpha != DefaultSelectionAlpha) {
        // If we searched for a new alpha, this alpha should be the maximal alpha that meets
        // contrast requirements, so the contrast ratio should be just above 4.5f in order to
        // maximize alpha
        assertThat(contrastRatio).isWithin(0.05f).of(RequiredContrastRatio)
    }
}

private const val RequiredContrastRatio = 4.5f
private const val MinimumSelectionAlpha = 0.2f
private const val DefaultSelectionAlpha = 0.4f
