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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * <a href="https://material.io/design/typography/the-type-system.html#type-scale" class="external" target="_blank">Material Design type scale</a>.
 *
 * The Material Design type scale includes a range of contrasting styles that support the needs of
 * your product and its content.
 *
 * The type scale is a combination of thirteen styles that are supported by the type system. It
 * contains reusable categories of text, each with an intended application and meaning.
 *
 * ![Typography image](https://developer.android.com/images/reference/androidx/compose/material/typography.png)
 *
 * @property h1 h1 is the largest headline, reserved for short, important text or numerals.
 * For headlines, you can choose an expressive font, such as a display, handwritten, or script
 * style. These unconventional font designs have details and intricacy that help attract the eye.
 * @property h2 h2 is the second largest headline, reserved for short, important text or numerals.
 * For headlines, you can choose an expressive font, such as a display, handwritten, or script
 * style. These unconventional font designs have details and intricacy that help attract the eye.
 * @property h3 h3 is the third largest headline, reserved for short, important text or numerals.
 * For headlines, you can choose an expressive font, such as a display, handwritten, or script
 * style. These unconventional font designs have details and intricacy that help attract the eye.
 * @property h4 h4 is the fourth largest headline, reserved for short, important text or numerals.
 * For headlines, you can choose an expressive font, such as a display, handwritten, or script
 * style. These unconventional font designs have details and intricacy that help attract the eye.
 * @property h5 h5 is the fifth largest headline, reserved for short, important text or numerals.
 * For headlines, you can choose an expressive font, such as a display, handwritten, or script
 * style. These unconventional font designs have details and intricacy that help attract the eye.
 * @property h6 h6 is the sixth largest headline, reserved for short, important text or numerals.
 * For headlines, you can choose an expressive font, such as a display, handwritten, or script
 * style. These unconventional font designs have details and intricacy that help attract the eye.
 * @property subtitle1 subtitle1 is the largest subtitle, and is typically reserved for
 * medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
 * subtitles.
 * @property subtitle2 subtitle2 is the smallest subtitle, and is typically reserved for
 * medium-emphasis text that is shorter in length. Serif or sans serif typefaces work well for
 * subtitles.
 * @property body1 body1 is the largest body, and is typically used for long-form writing as it
 * works well for small text sizes. For longer sections of text, a serif or sans serif typeface
 * is recommended.
 * @property body2 body2 is the smallest body, and is typically used for long-form writing as it
 * works well for small text sizes. For longer sections of text, a serif or sans serif typeface
 * is recommended.
 * @property button button text is a call to action used in different types of buttons (such as
 * text, outlined and contained buttons) and in tabs, dialogs, and cards. Button text is
 * typically sans serif, using all caps text.
 * @property caption caption is one of the smallest font sizes. It is used sparingly to
 * annotate imagery or to introduce a headline.
 * @property overline overline is one of the smallest font sizes. It is used sparingly to
 * annotate imagery or to introduce a headline.
 */
@Immutable
class Typography internal constructor(
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val h4: TextStyle,
    val h5: TextStyle,
    val h6: TextStyle,
    val subtitle1: TextStyle,
    val subtitle2: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val button: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle
) {
    /**
     * Constructor to create a [Typography]. For information on the types of style defined in
     * this constructor, see the property documentation for [Typography].
     *
     * @param defaultFontFamily the default [FontFamily] to be used for [TextStyle]s provided in
     * this constructor. This default will be used if the [FontFamily] on the [TextStyle] is `null`.
     * @param h1 h1 is the largest headline, reserved for short, important text or numerals.
     * @param h2 h2 is the second largest headline, reserved for short, important text or numerals.
     * @param h3 h3 is the third largest headline, reserved for short, important text or numerals.
     * @param h4 h4 is the fourth largest headline, reserved for short, important text or numerals.
     * @param h5 h5 is the fifth largest headline, reserved for short, important text or numerals.
     * @param h6 h6 is the sixth largest headline, reserved for short, important text or numerals.
     * @param subtitle1 subtitle1 is the largest subtitle, and is typically reserved for
     * medium-emphasis text that is shorter in length.
     * @param subtitle2 subtitle2 is the smallest subtitle, and is typically reserved for
     * medium-emphasis text that is shorter in length.
     * @param body1 body1 is the largest body, and is typically used for long-form writing as it
     * works well for small text sizes.
     * @param body2 body2 is the smallest body, and is typically used for long-form writing as it
     * works well for small text sizes.
     * @param button button text is a call to action used in different types of buttons (such as
     * text, outlined and contained buttons) and in tabs, dialogs, and cards.
     * @param caption caption is one of the smallest font sizes. It is used sparingly to annotate
     * imagery or to introduce a headline.
     * @param overline overline is one of the smallest font sizes. It is used sparingly to annotate
     * imagery or to introduce a headline.
     */
    constructor(
        defaultFontFamily: FontFamily = FontFamily.Default,
        h1: TextStyle = TextStyle(
            fontWeight = FontWeight.Light,
            fontSize = 96.sp,
            letterSpacing = (-1.5).sp
        ),
        h2: TextStyle = TextStyle(
            fontWeight = FontWeight.Light,
            fontSize = 60.sp,
            letterSpacing = (-0.5).sp
        ),
        h3: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 48.sp,
            letterSpacing = 0.sp
        ),
        h4: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 34.sp,
            letterSpacing = 0.25.sp
        ),
        h5: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            letterSpacing = 0.sp
        ),
        h6: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            letterSpacing = 0.15.sp
        ),
        subtitle1: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 0.15.sp
        ),
        subtitle2: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 0.1.sp
        ),
        body1: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 0.5.sp
        ),
        body2: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            letterSpacing = 0.25.sp
        ),
        button: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 1.25.sp
        ),
        caption: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            letterSpacing = 0.4.sp
        ),
        overline: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp
        )
    ) : this(
        h1 = h1.withDefaultFontFamily(defaultFontFamily),
        h2 = h2.withDefaultFontFamily(defaultFontFamily),
        h3 = h3.withDefaultFontFamily(defaultFontFamily),
        h4 = h4.withDefaultFontFamily(defaultFontFamily),
        h5 = h5.withDefaultFontFamily(defaultFontFamily),
        h6 = h6.withDefaultFontFamily(defaultFontFamily),
        subtitle1 = subtitle1.withDefaultFontFamily(defaultFontFamily),
        subtitle2 = subtitle2.withDefaultFontFamily(defaultFontFamily),
        body1 = body1.withDefaultFontFamily(defaultFontFamily),
        body2 = body2.withDefaultFontFamily(defaultFontFamily),
        button = button.withDefaultFontFamily(defaultFontFamily),
        caption = caption.withDefaultFontFamily(defaultFontFamily),
        overline = overline.withDefaultFontFamily(defaultFontFamily)
    )

    /**
     * Returns a copy of this Typography, optionally overriding some of the values.
     */
    fun copy(
        h1: TextStyle = this.h1,
        h2: TextStyle = this.h2,
        h3: TextStyle = this.h3,
        h4: TextStyle = this.h4,
        h5: TextStyle = this.h5,
        h6: TextStyle = this.h6,
        subtitle1: TextStyle = this.subtitle1,
        subtitle2: TextStyle = this.subtitle2,
        body1: TextStyle = this.body1,
        body2: TextStyle = this.body2,
        button: TextStyle = this.button,
        caption: TextStyle = this.caption,
        overline: TextStyle = this.overline
    ): Typography = Typography(
        h1 = h1,
        h2 = h2,
        h3 = h3,
        h4 = h4,
        h5 = h5,
        h6 = h6,
        subtitle1 = subtitle1,
        subtitle2 = subtitle2,
        body1 = body1,
        body2 = body2,
        button = button,
        caption = caption,
        overline = overline
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Typography) return false

        if (h1 != other.h1) return false
        if (h2 != other.h2) return false
        if (h3 != other.h3) return false
        if (h4 != other.h4) return false
        if (h5 != other.h5) return false
        if (h6 != other.h6) return false
        if (subtitle1 != other.subtitle1) return false
        if (subtitle2 != other.subtitle2) return false
        if (body1 != other.body1) return false
        if (body2 != other.body2) return false
        if (button != other.button) return false
        if (caption != other.caption) return false
        if (overline != other.overline) return false

        return true
    }

    override fun hashCode(): Int {
        var result = h1.hashCode()
        result = 31 * result + h2.hashCode()
        result = 31 * result + h3.hashCode()
        result = 31 * result + h4.hashCode()
        result = 31 * result + h5.hashCode()
        result = 31 * result + h6.hashCode()
        result = 31 * result + subtitle1.hashCode()
        result = 31 * result + subtitle2.hashCode()
        result = 31 * result + body1.hashCode()
        result = 31 * result + body2.hashCode()
        result = 31 * result + button.hashCode()
        result = 31 * result + caption.hashCode()
        result = 31 * result + overline.hashCode()
        return result
    }

    override fun toString(): String {
        return "Typography(h1=$h1, h2=$h2, h3=$h3, h4=$h4, h5=$h5, h6=$h6, " +
            "subtitle1=$subtitle1, subtitle2=$subtitle2, body1=$body1, " +
            "body2=$body2, button=$button, caption=$caption, overline=$overline)"
    }
}

/**
 * @return [this] if there is a [FontFamily] defined, otherwise copies [this] with [default] as
 * the [FontFamily].
 */
private fun TextStyle.withDefaultFontFamily(default: FontFamily): TextStyle {
    return if (fontFamily != null) this else copy(fontFamily = default)
}

/**
 * This CompositionLocal holds on to the current definition of typography for this application as
 * described by the Material spec. You can read the values in it when creating custom components
 * that want to use Material types, as well as override the values when you want to re-style a
 * part of your hierarchy. Material components related to text such as [Button] will use this
 * CompositionLocal to set values with which to style children text components.
 *
 * To access values within this CompositionLocal, use [MaterialTheme.typography].
 */
internal val LocalTypography = staticCompositionLocalOf { Typography() }
