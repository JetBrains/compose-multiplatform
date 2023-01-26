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

import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParagraphStyleTest {

    @Test(expected = IllegalStateException::class)
    fun `negative lineHeight throws IllegalStateException`() {
        ParagraphStyle(lineHeight = (-1).sp)
    }

    @Test
    fun `merge textAlign uses other's textAlign`() {
        val style = ParagraphStyle(textAlign = TextAlign.Justify)
        val otherStyle = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textAlign).isEqualTo(otherStyle.textAlign)
    }

    @Test
    fun `merge textAlign other null, return original`() {
        val style = ParagraphStyle(textAlign = TextAlign.Justify)
        val otherStyle = ParagraphStyle(textAlign = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textAlign).isEqualTo(style.textAlign)
    }

    @Test
    fun `merge textAlign both null returns null`() {
        val style = ParagraphStyle(textAlign = null)
        val otherStyle = ParagraphStyle(textAlign = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textAlign).isNull()
    }

    @Test
    fun `merge textDirection uses other's textDirection`() {
        val style = ParagraphStyle(textDirection = TextDirection.Rtl)
        val otherStyle = ParagraphStyle(textDirection = TextDirection.Ltr)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textDirection).isEqualTo(
            otherStyle.textDirection
        )
    }

    @Test
    fun `merge textDirection other null, returns original`() {
        val style = ParagraphStyle(textDirection = TextDirection.Rtl)
        val otherStyle = ParagraphStyle(textDirection = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textDirection).isEqualTo(style.textDirection)
    }

    @Test
    fun `merge textDirection both null returns null`() {
        val style = ParagraphStyle(textDirection = null)
        val otherStyle = ParagraphStyle(textDirection = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textDirection).isNull()
    }

    @Test
    fun `merge non-null hyphens uses other's hyphens`() {
        val style = ParagraphStyle(hyphens = Hyphens.Auto)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.None)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.hyphens).isEqualTo(otherStyle.hyphens)
    }

    @Test
    fun `merge hyphens other null, returns original`() {
        val style = ParagraphStyle(hyphens = Hyphens.Auto)
        val otherStyle = ParagraphStyle(hyphens = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.hyphens).isEqualTo(style.hyphens)
    }

    @Test
    fun `merge null hyphens other non-null, returns other's hyphens`() {
        val style = ParagraphStyle(hyphens = null)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.hyphens).isEqualTo(otherStyle.hyphens)
    }

    @Test
    fun `merge hyphens both null returns null`() {
        val style = ParagraphStyle(hyphens = null)
        val otherStyle = ParagraphStyle(hyphens = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.hyphens).isNull()
    }

    @Test
    fun `merge lineHeight uses other's lineHeight`() {
        val style = ParagraphStyle(lineHeight = 12.sp)
        val otherStyle = ParagraphStyle(lineHeight = 20.sp)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeight).isEqualTo(otherStyle.lineHeight)
    }

    @Test
    fun `merge lineHeight other unspecified, return original`() {
        val style = ParagraphStyle(lineHeight = 12.sp)
        val otherStyle = ParagraphStyle(lineHeight = TextUnit.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeight).isEqualTo(style.lineHeight)
    }

    @Test
    fun `merge lineHeight both unspecified returns unspecified`() {
        val style = ParagraphStyle(lineHeight = TextUnit.Unspecified)
        val otherStyle = ParagraphStyle(lineHeight = TextUnit.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeight).isEqualTo(TextUnit.Unspecified)
    }

    @Test
    fun `merge textIndent uses other's textIndent`() {
        val style = ParagraphStyle(textIndent = TextIndent(firstLine = 12.sp))
        val otherStyle = ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp))

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textIndent).isEqualTo(otherStyle.textIndent)
    }

    @Test
    fun `merge textIndent other null, return original`() {
        val style = ParagraphStyle(textIndent = TextIndent(firstLine = 12.sp))
        val otherStyle = ParagraphStyle(textIndent = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textIndent).isEqualTo(style.textIndent)
    }

    @Test
    fun `merge textIndent both null returns null`() {
        val style = ParagraphStyle(textIndent = null)
        val otherStyle = ParagraphStyle(textIndent = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textIndent).isNull()
    }

    @Test
    fun `merge null with non-null lineBreak uses other's lineBreak`() {
        val style = ParagraphStyle(lineBreak = null)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Heading)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(otherStyle.lineBreak)
    }

    @Test
    fun `merge non-null with null lineBreak returns original's lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val otherStyle = ParagraphStyle(lineBreak = null)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(style.lineBreak)
    }

    @Test
    fun `merge null with null lineBreak returns null`() {
        val style = ParagraphStyle(lineBreak = null)
        val otherStyle = ParagraphStyle(lineBreak = null)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(null)
    }

    @Test
    fun `merge non-null with non-null lineBreak returns other's lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Heading)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(otherStyle.lineBreak)
    }

    @Test
    fun `merge null platformStyles`() {
        val style1 = ParagraphStyle(platformStyle = null)
        val style2 = ParagraphStyle(platformStyle = null)

        assertThat(style1.merge(style2).platformStyle).isNull()
    }

    @Test
    fun `plus operator merges`() {
        val style = ParagraphStyle(
            textAlign = TextAlign.Center,
            textDirection = TextDirection.Rtl
        ) + ParagraphStyle(
            textAlign = TextAlign.Justify,
            lineHeight = 12.sp
        )

        assertThat(style).isEqualTo(
            ParagraphStyle(
                textAlign = TextAlign.Justify, // overridden by RHS
                textDirection = TextDirection.Rtl, // from LHS,
                lineHeight = 12.sp // from RHS
            )
        )
    }

    @Test
    fun `lerp textAlign with a null, b not null and t is smaller than half`() {
        val style1 = ParagraphStyle(textAlign = null)
        val style2 = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textAlign).isNull()
    }

    @Test
    fun `lerp textAlign with a and b are not Null and t is smaller than half`() {
        val style1 = ParagraphStyle(textAlign = TextAlign.Left)
        val style2 = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textAlign).isEqualTo(style1.textAlign)
    }

    @Test
    fun `lerp textAlign with a and b are not Null and t is larger than half`() {
        val style1 = ParagraphStyle(textAlign = TextAlign.Left)
        val style2 = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.textAlign).isEqualTo(style2.textAlign)
    }

    @Test
    fun `lerp textDirection with a null, b not null and t is smaller than half`() {
        val style1 = ParagraphStyle(textDirection = null)
        val style2 = ParagraphStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textDirection).isNull()
    }

    @Test
    fun `lerp textDirection with a and b are not Null and t is smaller than half`() {
        val style1 = ParagraphStyle(textDirection = TextDirection.Ltr)
        val style2 = ParagraphStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textDirection).isEqualTo(style1.textDirection)
    }

    @Test
    fun `lerp textDirection with a and b are not Null and t is larger than half`() {
        val style1 = ParagraphStyle(textDirection = TextDirection.Ltr)
        val style2 = ParagraphStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.textDirection).isEqualTo(style2.textDirection)
    }

    @Test
    fun `lerp hyphens with a null, b not null and t is smaller than half`() {
        val style1 = ParagraphStyle(hyphens = null)
        val style2 = ParagraphStyle(hyphens = Hyphens.Auto)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.hyphens).isNull()
    }

    @Test
    fun `lerp hyphens with a null, b not null and t is equal to half`() {
        val style1 = ParagraphStyle(hyphens = null)
        val style2 = ParagraphStyle(hyphens = Hyphens.Auto)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.5f)

        assertThat(newStyle.hyphens).isEqualTo(style2.hyphens)
    }

    @Test
    fun `lerp hyphens with a and b are not null and t is smaller than half`() {
        val style1 = ParagraphStyle(hyphens = Hyphens.Auto)
        val style2 = ParagraphStyle(hyphens = Hyphens.None)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.hyphens).isEqualTo(style1.hyphens)
    }

    @Test
    fun `lerp hyphens with a and b are not Null and t is larger than half`() {
        val style1 = ParagraphStyle(hyphens = Hyphens.Auto)
        val style2 = ParagraphStyle(hyphens = Hyphens.None)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.hyphens).isEqualTo(style2.hyphens)
    }

    @Test
    fun `lerp textIndent with a null, b not null and t is smaller than half returns null`() {
        val style1 = ParagraphStyle(textIndent = null)
        val style2 = ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp))
        val fraction = 0.4f

        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.textIndent).isEqualTo(
            lerp(TextIndent(), style2.textIndent!!, fraction)
        )
    }

    @Test
    fun `lerp textIndent with a and b are not Null`() {
        val style1 = ParagraphStyle(textIndent = TextIndent(firstLine = 10.sp))
        val style2 = ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp))
        val fraction = 0.6f
        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.textIndent).isEqualTo(
            lerp(style1.textIndent!!, style2.textIndent!!, fraction)
        )
    }

    @Test
    fun `lerp lineHeight with a and b are specified`() {
        val style1 = ParagraphStyle(lineHeight = 10.sp)
        val style2 = ParagraphStyle(lineHeight = 20.sp)
        val fraction = 0.4f

        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.lineHeight).isEqualTo(
            lerp(style1.lineHeight, style2.lineHeight, fraction)
        )
    }

    @Test
    fun `lerp lineHeight with a and b are unspecified`() {
        val style1 = ParagraphStyle(lineHeight = TextUnit.Unspecified)
        val style2 = ParagraphStyle(lineHeight = TextUnit.Unspecified)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.lineHeight).isEqualTo(TextUnit.Unspecified)
    }

    @Test
    fun `lerp lineHeight with either a or b is unspecified`() {
        val style1 = ParagraphStyle(lineHeight = TextUnit.Unspecified)
        val style2 = ParagraphStyle(lineHeight = 22.sp)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)
        val anotherNewStyle = lerp(start = style1, stop = style2, fraction = 0.8f)

        assertThat(newStyle.lineHeight).isEqualTo(TextUnit.Unspecified)
        assertThat(anotherNewStyle.lineHeight).isEqualTo(22.sp)
    }

    @Test
    fun `lerp with null platformStyles has null platformStyle`() {
        val style = ParagraphStyle(platformStyle = null)
        val otherStyle = ParagraphStyle(platformStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.5f)

        assertThat(lerpedStyle.platformStyle).isNull()
    }

    @Test
    fun `lerp with null lineHeightStyles has null lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.5f)

        assertThat(lerpedStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `lerp with non-null start, null end, closer to start has non-null lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineHeightStyle).isSameInstanceAs(style.lineHeightStyle)
    }

    @Test
    fun `lerp with non-null start, null end, closer to end has null lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `lerp with null start, non-null end, closer to start has null lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `lerp with null start, non-null end, closer to end has non-null lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineHeightStyle).isSameInstanceAs(otherStyle.lineHeightStyle)
    }

    @Test
    fun `lerp with non-null start, null end, closer to start has non-null lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Heading)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineBreak).isSameInstanceAs(style.lineBreak)
    }

    @Test
    fun `lerp with non-null start, null end, closer to end has null lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Heading)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineBreak).isNull()
    }

    @Test
    fun `lerp with null start, non-null end, closer to start has null lineBreak`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Heading)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineBreak).isNull()
    }

    @Test
    fun `lerp with null start, non-null end, closer to end has non-null lineBreak`() {
        val style = ParagraphStyle(lineBreak = null)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Heading)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineBreak).isSameInstanceAs(otherStyle.lineBreak)
    }

    @Test
    fun `equals return false for different line height behavior`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        assertThat(style == otherStyle).isFalse()
    }

    @Test
    fun `equals return true for same line height behavior`() {
        val style = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        assertThat(style == otherStyle).isTrue()
    }

    @Test
    fun `hashCode is same for same line height behavior`() {
        val style = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        assertThat(style.hashCode()).isEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `hashCode is different for different line height behavior`() {
        val style = ParagraphStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = Alignment.Bottom,
                trim = Trim.None
            )
        )
        val otherStyle = ParagraphStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = Alignment.Center,
                trim = Trim.Both
            )
        )

        assertThat(style.hashCode()).isNotEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `copy with lineHeightStyle returns new lineHeightStyle`() {
        val style = ParagraphStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = Alignment.Bottom,
                trim = Trim.None
            )
        )
        val newLineHeightStyle = LineHeightStyle(
            alignment = Alignment.Center,
            trim = Trim.Both
        )
        val newStyle = style.copy(lineHeightStyle = newLineHeightStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(newLineHeightStyle)
    }

    @Test
    fun `copy without lineHeightStyle uses existing lineHeightStyle`() {
        val style = ParagraphStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = Alignment.Bottom,
                trim = Trim.Both
            )
        )
        val newStyle = style.copy()

        assertThat(newStyle.lineHeightStyle).isEqualTo(style.lineHeightStyle)
    }

    @Test
    fun `copy with hyphens returns new hyphens`() {
        val style = ParagraphStyle(hyphens = Hyphens.None)
        val newStyle = style.copy(hyphens = Hyphens.Auto)

        assertThat(newStyle.hyphens).isEqualTo(Hyphens.Auto)
    }

    @Test
    fun `copy without hyphens uses existing hyphens`() {
        val style = ParagraphStyle(hyphens = Hyphens.Auto)
        val newStyle = style.copy()

        assertThat(newStyle.hyphens).isEqualTo(style.hyphens)
    }

    @Test
    fun `equals return false for different and non-null hyphens behavior`() {
        val style = ParagraphStyle(hyphens = Hyphens.None)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        assertThat(style == otherStyle).isFalse()
    }

    @Test
    fun `equals return false for null and non-null hyphens`() {
        val style = ParagraphStyle(hyphens = null)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        assertThat(style == otherStyle).isFalse()
    }

    @Test
    fun `equals return true for same and non-null hyphens behavior`() {
        val style = ParagraphStyle(hyphens = Hyphens.Auto)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        assertThat(style == otherStyle).isTrue()
    }

    @Test
    fun `equals return true for both null hyphens`() {
        val style = ParagraphStyle(hyphens = null)
        val otherStyle = ParagraphStyle(hyphens = null)

        assertThat(style == otherStyle).isTrue()
    }

    @Test
    fun `hashCode is same for same hyphens behavior`() {
        val style = ParagraphStyle(hyphens = Hyphens.Auto)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        assertThat(style.hashCode()).isEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `hashCode is different for different hyphens behavior`() {
        val style = ParagraphStyle(hyphens = Hyphens.None)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        assertThat(style.hashCode()).isNotEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `merge with null lineHeightStyle uses other's lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(otherStyle.lineHeightStyle)
    }

    @Test
    fun `merge with non-null lineHeightStyle, returns original`() {
        val style = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(style.lineHeightStyle)
    }

    @Test
    fun `merge with both null lineHeightStyle returns null`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `merge with both non-null lineHeightStyle returns other's lineHeightStyle`() {
        val style = ParagraphStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = Alignment.Center,
                trim = Trim.None
            )
        )
        val otherStyle = ParagraphStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = Alignment.Bottom,
                trim = Trim.Both
            )
        )

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(otherStyle.lineHeightStyle)
    }

    @Test
    fun `constructor without lineHeightStyle sets lineHeightStyle to null`() {
        val style = ParagraphStyle(textAlign = TextAlign.Start)

        assertThat(style.lineHeightStyle).isNull()
    }

    @Test
    fun `copy with lineBreak returns new lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val newStyle = style.copy(lineBreak = LineBreak.Heading)

        assertThat(newStyle.lineBreak).isEqualTo(LineBreak.Heading)
    }

    @Test
    fun `copy without lineBreak uses existing lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val newStyle = style.copy()

        assertThat(newStyle.lineBreak).isEqualTo(style.lineBreak)
    }

    @Test
    fun `hashCode is same for same lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Paragraph)

        assertThat(style.hashCode()).isEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `hashCode is different for different lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Heading)

        assertThat(style.hashCode()).isNotEqualTo(otherStyle.hashCode())
    }
}
