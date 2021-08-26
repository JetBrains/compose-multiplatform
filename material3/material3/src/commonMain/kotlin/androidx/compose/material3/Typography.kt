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

import androidx.compose.material3.tokens.TypeScale
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

// TODO(b/197880751): Update description after spec draft is available.
/**
 * The Material Design type scale includes a range of contrasting styles that support the needs of
 * your product and its content.
 *
 * The type scale is a combination of thirteen styles that are supported by the type system. It
 * contains reusable categories of text, each with an intended application and meaning.
 *
 * @property displayLarge displayLarge is the largest display text.
 * @property displayMedium displayMedium is the second largest display text.
 * @property displaySmall displaySmall is the smallest display text.
 * @property headlineLarge headlineLarge is the largest headline, reserved for short, important
 * text or numerals. For headlines, you can choose an expressive font, such as a display,
 * handwritten, or script style. These unconventional font designs have details and intricacy
 * that help attract the eye.
 * @property headlineMedium headlineMedium is the second largest headline, reserved for short,
 * important text or numerals. For headlines, you can choose an expressive font, such as a
 * display, handwritten, or script style. These unconventional font designs have details and
 * intricacy that help attract the eye.
 * @property headlineSmall headlineSmall is the smallest headline, reserved for short, important text or
 * numerals. For headlines, you can choose an expressive font, such as a display, handwritten, or
 * script style. These unconventional font designs have details and intricacy that help attract
 * the eye.
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
 * @property labelLarge labelLarge text is a call to action used in different types of buttons
 * (such as text, outlined and contained buttons) and in tabs, dialogs, and cards. Button text is
 * typically sans serif, using all caps text.
 * @property labelMedium labelMedium is one of the smallest font sizes. It is used sparingly to
 * annotate imagery or to introduce a headline.
 * @property labelSmall labelSmall is one of the smallest font sizes. It is used sparingly to
 * annotate imagery or to introduce a headline.
 */
@Immutable
class Typography(
    val displayLarge: TextStyle = DefaultDisplayLarge,
    val displayMedium: TextStyle = DefaultDisplayMedium,
    val displaySmall: TextStyle = DefaultDisplaySmall,
    val headlineLarge: TextStyle = DefaultHeadlineLarge,
    val headlineMedium: TextStyle = DefaultHeadlineMedium,
    val headlineSmall: TextStyle = DefaultHeadlineSmall,
    val titleLarge: TextStyle = DefaultTitleLarge,
    val titleMedium: TextStyle = DefaultTitleMedium,
    val titleSmall: TextStyle = DefaultTitleSmall,
    val bodyLarge: TextStyle = DefaultBodyLarge,
    val bodyMedium: TextStyle = DefaultBodyMedium,
    val bodySmall: TextStyle = DefaultBodySmall,
    val labelLarge: TextStyle = DefaultLabelLarge,
    val labelMedium: TextStyle = DefaultLabelMedium,
    val labelSmall: TextStyle = DefaultLabelSmall,
) {
    /**
     * Returns a copy of this Typography, optionally overriding some of the values.
     */
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
    ): Typography = Typography(
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

private val DefaultDisplayLarge = TextStyle(
    fontFamily = TypeScale.DisplayLargeFont,
    fontWeight = TypeScale.DisplayLargeWeight,
    fontSize = TypeScale.DisplayLargeSizePoints,
    lineHeight = TypeScale.DisplayLargeLineHeightPoints,
    letterSpacing = TypeScale.DisplayLargeTrackingPoints
)

private val DefaultDisplayMedium = TextStyle(
    fontFamily = TypeScale.DisplayMediumFont,
    fontWeight = TypeScale.DisplayMediumWeight,
    fontSize = TypeScale.DisplayMediumSizePoints,
    lineHeight = TypeScale.DisplayMediumLineHeightPoints,
    letterSpacing = TypeScale.DisplayMediumTrackingPoints
)

private val DefaultDisplaySmall = TextStyle(
    fontFamily = TypeScale.DisplaySmallFont,
    fontWeight = TypeScale.DisplaySmallWeight,
    fontSize = TypeScale.DisplaySmallSizePoints,
    lineHeight = TypeScale.DisplaySmallLineHeightPoints,
    letterSpacing = TypeScale.DisplaySmallTrackingPoints
)

private val DefaultHeadlineLarge = TextStyle(
    fontFamily = TypeScale.HeadlineLargeFont,
    fontWeight = TypeScale.HeadlineLargeWeight,
    fontSize = TypeScale.HeadlineLargeSizePoints,
    lineHeight = TypeScale.HeadlineLargeLineHeightPoints,
    letterSpacing = TypeScale.HeadlineLargeTrackingPoints
)

private val DefaultHeadlineMedium = TextStyle(
    fontFamily = TypeScale.HeadlineMediumFont,
    fontWeight = TypeScale.HeadlineMediumWeight,
    fontSize = TypeScale.HeadlineMediumSizePoints,
    lineHeight = TypeScale.HeadlineMediumLineHeightPoints,
    letterSpacing = TypeScale.HeadlineMediumTrackingPoints
)

private val DefaultHeadlineSmall = TextStyle(
    fontFamily = TypeScale.HeadlineSmallFont,
    fontWeight = TypeScale.HeadlineSmallWeight,
    fontSize = TypeScale.HeadlineSmallSizePoints,
    lineHeight = TypeScale.HeadlineSmallLineHeightPoints,
    letterSpacing = TypeScale.HeadlineSmallTrackingPoints
)

private val DefaultTitleLarge = TextStyle(
    fontFamily = TypeScale.TitleLargeFont,
    fontWeight = TypeScale.TitleLargeWeight,
    fontSize = TypeScale.TitleLargeSizePoints,
    lineHeight = TypeScale.TitleLargeLineHeightPoints,
    letterSpacing = TypeScale.TitleLargeTrackingPoints
)

private val DefaultTitleMedium = TextStyle(
    fontFamily = TypeScale.TitleMediumFont,
    fontWeight = TypeScale.TitleMediumWeight,
    fontSize = TypeScale.TitleMediumSizePoints,
    lineHeight = TypeScale.TitleMediumLineHeightPoints,
    letterSpacing = TypeScale.TitleMediumTrackingPoints
)

private val DefaultTitleSmall = TextStyle(
    fontFamily = TypeScale.TitleSmallFont,
    fontWeight = TypeScale.TitleSmallWeight,
    fontSize = TypeScale.TitleSmallSizePoints,
    lineHeight = TypeScale.TitleSmallLineHeightPoints,
    letterSpacing = TypeScale.TitleSmallTrackingPoints
)

private val DefaultBodyLarge = TextStyle(
    fontFamily = TypeScale.BodyLargeFont,
    fontWeight = TypeScale.BodyLargeWeight,
    fontSize = TypeScale.BodyLargeSizePoints,
    lineHeight = TypeScale.BodyLargeLineHeightPoints,
    letterSpacing = TypeScale.BodyLargeTrackingPoints
)

private val DefaultBodyMedium = TextStyle(
    fontFamily = TypeScale.BodyMediumFont,
    fontWeight = TypeScale.BodyMediumWeight,
    fontSize = TypeScale.BodyMediumSizePoints,
    lineHeight = TypeScale.BodyMediumLineHeightPoints,
    letterSpacing = TypeScale.BodyMediumTrackingPoints
)

private val DefaultBodySmall = TextStyle(
    fontFamily = TypeScale.BodySmallFont,
    fontWeight = TypeScale.BodySmallWeight,
    fontSize = TypeScale.BodySmallSizePoints,
    lineHeight = TypeScale.BodySmallLineHeightPoints,
    letterSpacing = TypeScale.BodySmallTrackingPoints
)

private val DefaultLabelLarge = TextStyle(
    fontFamily = TypeScale.LabelLargeFont,
    fontWeight = TypeScale.LabelLargeWeight,
    fontSize = TypeScale.LabelLargeSizePoints,
    lineHeight = TypeScale.LabelLargeLineHeightPoints,
    letterSpacing = TypeScale.LabelLargeTrackingPoints
)

private val DefaultLabelMedium = TextStyle(
    fontFamily = TypeScale.LabelMediumFont,
    fontWeight = TypeScale.LabelMediumWeight,
    fontSize = TypeScale.LabelMediumSizePoints,
    lineHeight = TypeScale.LabelMediumLineHeightPoints,
    letterSpacing = TypeScale.LabelMediumTrackingPoints
)

private val DefaultLabelSmall = TextStyle(
    fontFamily = TypeScale.LabelSmallFont,
    fontWeight = TypeScale.LabelSmallWeight,
    fontSize = TypeScale.LabelSmallSizePoints,
    lineHeight = TypeScale.LabelSmallLineHeightPoints,
    letterSpacing = TypeScale.LabelSmallTrackingPoints
)

internal val LocalTypography = staticCompositionLocalOf { Typography() }
