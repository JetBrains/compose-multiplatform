/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.tokens.TypographyTokens

/**
 * The Material Design type scale includes a range of contrasting styles that support the needs of
 * your product and its content.
 *
 * Use typography to make writing legible and beautiful. Material's default type scale includes
 * contrasting and flexible styles to support a wide range of use cases.
 *
 * The type scale is a combination of thirteen styles that are supported by the type system. It
 * contains reusable categories of text, each with an intended application and meaning.
 *
 * To learn more about typography, see [Material Design typography](https://m3.material.io/styles/typography/overview).
 *
 * @property displayLarge displayLarge is the largest display text.
 * @property displayMedium displayMedium is the second largest display text.
 * @property displaySmall displaySmall is the smallest display text.
 * @property headlineLarge headlineLarge is the largest headline, reserved for short, important text
 * or numerals. For headlines, you can choose an expressive font, such as a display, handwritten, or
 * script style. These unconventional font designs have details and intricacy that help attract the
 * eye.
 * @property headlineMedium headlineMedium is the second largest headline, reserved for short,
 * important text or numerals. For headlines, you can choose an expressive font, such as a display,
 * handwritten, or script style. These unconventional font designs have details and intricacy that
 * help attract the eye.
 * @property headlineSmall headlineSmall is the smallest headline, reserved for short, important
 * text or numerals. For headlines, you can choose an expressive font, such as a display,
 * handwritten, or script style. These unconventional font designs have details and intricacy that
 * help attract the eye.
 * @property titleLarge titleLarge is the largest title, and is typically reserved for
 * medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
 * subtitles.
 * @property titleMedium titleMedium is the second largest title, and is typically reserved for
 * medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
 * subtitles.
 * @property titleSmall titleSmall is the smallest title, and is typically reserved for
 * medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
 * subtitles.
 * @property bodyLarge bodyLarge is the largest body, and is typically used for long-form writing as
 * it works well for small text sizes. For longer sections of text, a serif or sans serif typeface
 * is recommended.
 * @property bodyMedium bodyMedium is the second largest body, and is typically used for long-form
 * writing as it works well for small text sizes. For longer sections of text, a serif or sans serif
 * typeface is recommended.
 * @property bodySmall bodySmall is the smallest body, and is typically used for long-form writing
 * as it works well for small text sizes. For longer sections of text, a serif or sans serif
 * typeface is recommended.
 * @property labelLarge labelLarge text is a call to action used in different types of buttons (such
 * as text, outlined and contained buttons) and in tabs, dialogs, and cards. Button text is
 * typically sans serif, using all caps text.
 * @property labelMedium labelMedium is one of the smallest font sizes. It is used sparingly to
 * annotate imagery or to introduce a headline.
 * @property labelSmall labelSmall is one of the smallest font sizes. It is used sparingly to
 * annotate imagery or to introduce a headline.
 */
@Immutable
class Typography(
    val displayLarge: TextStyle = TypographyTokens.DisplayLarge,
    val displayMedium: TextStyle = TypographyTokens.DisplayMedium,
    val displaySmall: TextStyle = TypographyTokens.DisplaySmall,
    val headlineLarge: TextStyle = TypographyTokens.HeadlineLarge,
    val headlineMedium: TextStyle = TypographyTokens.HeadlineMedium,
    val headlineSmall: TextStyle = TypographyTokens.HeadlineSmall,
    val titleLarge: TextStyle = TypographyTokens.TitleLarge,
    val titleMedium: TextStyle = TypographyTokens.TitleMedium,
    val titleSmall: TextStyle = TypographyTokens.TitleSmall,
    val bodyLarge: TextStyle = TypographyTokens.BodyLarge,
    val bodyMedium: TextStyle = TypographyTokens.BodyMedium,
    val bodySmall: TextStyle = TypographyTokens.BodySmall,
    val labelLarge: TextStyle = TypographyTokens.LabelLarge,
    val labelMedium: TextStyle = TypographyTokens.LabelMedium,
    val labelSmall: TextStyle = TypographyTokens.LabelSmall,
) {

    /** Returns a copy of this Typography, optionally overriding some of the values. */
    fun copy(
        displayLarge: TextStyle = this.displayLarge,
        displayMedium: TextStyle = this.displayMedium,
        displaySmall: TextStyle = this.displaySmall,
        headlineLarge: TextStyle = this.headlineLarge,
        headlineMedium: TextStyle = this.headlineMedium,
        headlineSmall: TextStyle = this.headlineSmall,
        titleLarge: TextStyle = this.titleLarge,
        titleMedium: TextStyle = this.titleMedium,
        titleSmall: TextStyle = this.titleSmall,
        bodyLarge: TextStyle = this.bodyLarge,
        bodyMedium: TextStyle = this.bodyMedium,
        bodySmall: TextStyle = this.bodySmall,
        labelLarge: TextStyle = this.labelLarge,
        labelMedium: TextStyle = this.labelMedium,
        labelSmall: TextStyle = this.labelSmall,
    ): Typography =
        Typography(
            displayLarge = displayLarge,
            displayMedium = displayMedium,
            displaySmall = displaySmall,
            headlineLarge = headlineLarge,
            headlineMedium = headlineMedium,
            headlineSmall = headlineSmall,
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
            labelLarge = labelLarge,
            labelMedium = labelMedium,
            labelSmall = labelSmall
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Typography) return false

        if (displayLarge != other.displayLarge) return false
        if (displayMedium != other.displayMedium) return false
        if (displaySmall != other.displaySmall) return false
        if (headlineLarge != other.headlineLarge) return false
        if (headlineMedium != other.headlineMedium) return false
        if (headlineSmall != other.headlineSmall) return false
        if (titleLarge != other.titleLarge) return false
        if (titleMedium != other.titleMedium) return false
        if (titleSmall != other.titleSmall) return false
        if (bodyLarge != other.bodyLarge) return false
        if (bodyMedium != other.bodyMedium) return false
        if (bodySmall != other.bodySmall) return false
        if (labelLarge != other.labelLarge) return false
        if (labelMedium != other.labelMedium) return false
        if (labelSmall != other.labelSmall) return false
        return true
    }

    override fun hashCode(): Int {
        var result = displayLarge.hashCode()
        result = 31 * result + displayMedium.hashCode()
        result = 31 * result + displaySmall.hashCode()
        result = 31 * result + headlineLarge.hashCode()
        result = 31 * result + headlineMedium.hashCode()
        result = 31 * result + headlineSmall.hashCode()
        result = 31 * result + titleLarge.hashCode()
        result = 31 * result + titleMedium.hashCode()
        result = 31 * result + titleSmall.hashCode()
        result = 31 * result + bodyLarge.hashCode()
        result = 31 * result + bodyMedium.hashCode()
        result = 31 * result + bodySmall.hashCode()
        result = 31 * result + labelLarge.hashCode()
        result = 31 * result + labelMedium.hashCode()
        result = 31 * result + labelSmall.hashCode()
        return result
    }

    override fun toString(): String {
        return "Typography(displayLarge=$displayLarge, displayMedium=$displayMedium," +
            "displaySmall=$displaySmall, " +
            "headlineLarge=$headlineLarge, headlineMedium=$headlineMedium," +
            " headlineSmall=$headlineSmall, " +
            "titleLarge=$titleLarge, titleMedium=$titleMedium, titleSmall=$titleSmall, " +
            "bodyLarge=$bodyLarge, bodyMedium=$bodyMedium, bodySmall=$bodySmall, " +
            "labelLarge=$labelLarge, labelMedium=$labelMedium, labelSmall=$labelSmall)"
    }
}

/**
 * Helper function for component typography tokens.
 */
internal fun Typography.fromToken(value: TypographyKeyTokens): TextStyle {
    return when (value) {
        TypographyKeyTokens.DisplayLarge -> displayLarge
        TypographyKeyTokens.DisplayMedium -> displayMedium
        TypographyKeyTokens.DisplaySmall -> displaySmall
        TypographyKeyTokens.HeadlineLarge -> headlineLarge
        TypographyKeyTokens.HeadlineMedium -> headlineMedium
        TypographyKeyTokens.HeadlineSmall -> headlineSmall
        TypographyKeyTokens.TitleLarge -> titleLarge
        TypographyKeyTokens.TitleMedium -> titleMedium
        TypographyKeyTokens.TitleSmall -> titleSmall
        TypographyKeyTokens.BodyLarge -> bodyLarge
        TypographyKeyTokens.BodyMedium -> bodyMedium
        TypographyKeyTokens.BodySmall -> bodySmall
        TypographyKeyTokens.LabelLarge -> labelLarge
        TypographyKeyTokens.LabelMedium -> labelMedium
        TypographyKeyTokens.LabelSmall -> labelSmall
    }
}

internal val LocalTypography = staticCompositionLocalOf { Typography() }
