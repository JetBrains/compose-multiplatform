/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.ui.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CacheTextLayoutInputTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = createFontFamilyResolver(context)

    @Test
    fun default_ctor_should_be_equal() {
        val input1 = cacheTextLayoutInput()
        val input2 = cacheTextLayoutInput()

        assertThat(input1.hashCode()).isEqualTo(input2.hashCode())
        assertThat(input1).isEqualTo(input2)
    }

    @Test
    fun text_should_differ() {
        val input1 = cacheTextLayoutInput(AnnotatedString("Hello"))
        val input2 = cacheTextLayoutInput(AnnotatedString("Hello, World"))

        assertThat(input1.hashCode()).isNotEqualTo(input2.hashCode())
        assertThat(input1).isNotEqualTo(input2)
    }

    @Test
    fun placeholders_should_differ() {
        val input1 = cacheTextLayoutInput(placeholders = listOf(AnnotatedString.Range(
            Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.AboveBaseline), 0, 1
        )))
        val input2 = cacheTextLayoutInput(placeholders = listOf(AnnotatedString.Range(
            Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.AboveBaseline), 1, 2
        )))

        assertThat(input1.hashCode()).isNotEqualTo(input2.hashCode())
        assertThat(input1).isNotEqualTo(input2)
    }

    @Test
    fun maxLines_should_differ() {
        val input1 = cacheTextLayoutInput(maxLines = 1)
        val input2 = cacheTextLayoutInput(maxLines = 2)

        assertThat(input1.hashCode()).isNotEqualTo(input2.hashCode())
        assertThat(input1).isNotEqualTo(input2)
    }

    @Test
    fun softWrap_should_differ() {
        val input1 = cacheTextLayoutInput(softWrap = true)
        val input2 = cacheTextLayoutInput(softWrap = false)

        assertThat(input1.hashCode()).isNotEqualTo(input2.hashCode())
        assertThat(input1).isNotEqualTo(input2)
    }

    @Test
    fun overflow_should_differ() {
        val input1 = cacheTextLayoutInput(overflow = TextOverflow.Visible)
        val input2 = cacheTextLayoutInput(overflow = TextOverflow.Ellipsis)

        assertThat(input1.hashCode()).isNotEqualTo(input2.hashCode())
        assertThat(input1).isNotEqualTo(input2)
    }

    @Test
    fun density_should_differ() {
        val input1 = cacheTextLayoutInput(density = Density(1f))
        val input2 = cacheTextLayoutInput(density = Density(1.5f))

        assertThat(input1.hashCode()).isNotEqualTo(input2.hashCode())
        assertThat(input1).isNotEqualTo(input2)
    }

    @Test
    fun layoutDirection_should_differ() {
        val input1 = cacheTextLayoutInput(layoutDirection = LayoutDirection.Ltr)
        val input2 = cacheTextLayoutInput(layoutDirection = LayoutDirection.Rtl)

        assertThat(input1.hashCode()).isNotEqualTo(input2.hashCode())
        assertThat(input1).isNotEqualTo(input2)
    }

    @Test
    fun fontFamilyResolver_should_differ() {
        // FontFamilyResolver only checks for instance equality.
        val input1 = cacheTextLayoutInput(fontFamilyResolver = createFontFamilyResolver(context))
        val input2 = cacheTextLayoutInput(fontFamilyResolver = createFontFamilyResolver(context))

        assertThat(input1.hashCode()).isNotEqualTo(input2.hashCode())
        assertThat(input1).isNotEqualTo(input2)
    }

    @Test
    fun constraints_maxWidth_should_differ() {
        val input1 = cacheTextLayoutInput(constraints = Constraints(maxWidth = 100))
        val input2 = cacheTextLayoutInput(constraints = Constraints(maxWidth = 200))

        assertThat(input1.hashCode()).isNotEqualTo(input2.hashCode())
        assertThat(input1).isNotEqualTo(input2)
    }

    @Test
    fun constraints_maxHeight_should_differ() {
        val input1 = cacheTextLayoutInput(constraints = Constraints(maxHeight = 100))
        val input2 = cacheTextLayoutInput(constraints = Constraints(maxHeight = 200))

        assertThat(input1.hashCode()).isNotEqualTo(input2.hashCode())
        assertThat(input1).isNotEqualTo(input2)
    }

    @Test
    fun color_should_not_differ() {
        val input1 = cacheTextLayoutInput(style = TextStyle(color = Color.Red))
        val input2 = cacheTextLayoutInput(style = TextStyle(color = Color.Blue))

        assertThat(input1.hashCode()).isEqualTo(input2.hashCode())
        assertThat(input1).isEqualTo(input2)
    }

    @Test
    fun brush_should_not_differ() {
        val input1 = cacheTextLayoutInput(style = TextStyle(color = Color.Red))
        val input2 = cacheTextLayoutInput(style = TextStyle(brush = SolidColor(Color.Blue)))

        assertThat(input1.hashCode()).isEqualTo(input2.hashCode())
        assertThat(input1).isEqualTo(input2)
    }

    @Test
    fun shadow_should_not_differ() {
        val input1 = cacheTextLayoutInput(
            style = TextStyle(shadow = Shadow(Color.Red, Offset(10f, 10f)))
        )
        val input2 = cacheTextLayoutInput(
            style = TextStyle(shadow = Shadow(Color.Red, Offset(12f, 12f)))
        )

        assertThat(input1.hashCode()).isEqualTo(input2.hashCode())
        assertThat(input1).isEqualTo(input2)
    }

    @Test
    fun textDecoration_should_not_differ() {
        val input1 = cacheTextLayoutInput(
            style = TextStyle(textDecoration = TextDecoration.Underline)
        )
        val input2 = cacheTextLayoutInput(
            style = TextStyle(textDecoration = TextDecoration.LineThrough)
        )

        assertThat(input1.hashCode()).isEqualTo(input2.hashCode())
        assertThat(input1).isEqualTo(input2)
    }

    @Test
    fun minConstraints_should_not_differ() {
        val input1 = cacheTextLayoutInput(constraints = Constraints(minWidth = 10, minHeight = 20))
        val input2 = cacheTextLayoutInput(constraints = Constraints(minWidth = 20, minHeight = 10))

        assertThat(input1.hashCode()).isEqualTo(input2.hashCode())
        assertThat(input1).isEqualTo(input2)
    }

    private fun cacheTextLayoutInput(
        text: AnnotatedString = AnnotatedString("Hello"),
        style: TextStyle = TextStyle.Default,
        placeholders: List<AnnotatedString.Range<Placeholder>> = emptyList(),
        maxLines: Int = Int.MAX_VALUE,
        softWrap: Boolean = true,
        overflow: TextOverflow = TextOverflow.Clip,
        density: Density = Density(context),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        fontFamilyResolver: FontFamily.Resolver = this.fontFamilyResolver,
        constraints: Constraints = Constraints()
    ): CacheTextLayoutInput {
        return CacheTextLayoutInput(
            TextLayoutInput(
                text = text,
                style = style,
                placeholders = placeholders,
                maxLines = maxLines,
                softWrap = softWrap,
                overflow = overflow,
                density = density,
                layoutDirection = layoutDirection,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints
            )
        )
    }
}