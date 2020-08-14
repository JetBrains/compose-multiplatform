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

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.compose.ui.text.font.asFontFamily
import androidx.compose.ui.text.matchers.isZero
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalTextApi::class)
@RunWith(JUnit4::class)
@SmallTest
class TextLayoutResultIntegrationTest {

    private val fontFamily = BASIC_MEASURE_FONT.asFontFamily()
    private val density = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val resourceLoader = TestFontResourceLoader(context)
    private val layoutDirection = LayoutDirection.Ltr

    @Test
    fun width_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                style = TextStyle.Default,
                density = this,
                resourceLoader = resourceLoader
            )

            val layoutResult = textDelegate.layout(Constraints(0, 200), layoutDirection)

            assertThat(layoutResult.size.width).isEqualTo(
                (fontSize.toPx() * text.length).toIntPx()
            )
        }
    }

    @Test
    fun width_getter_with_small_width() {
        val text = "Hello"
        val width = 80
        val spanStyle = SpanStyle(fontSize = 20.sp, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(text, spanStyle)
        val textDelegate = TextDelegate(
            text = annotatedString,
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
        )

        val layoutResult = textDelegate.layout(Constraints(maxWidth = width), layoutDirection)

        assertThat(layoutResult.size.width).isEqualTo(width)
    }

    @Test
    fun height_getter() {
        with(density) {
            val fontSize = 20.sp
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val text = "hello"
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                style = TextStyle.Default,
                density = this,
                resourceLoader = resourceLoader
            )

            val layoutResult = textDelegate.layout(Constraints(), layoutDirection)

            assertThat(layoutResult.size.height).isEqualTo((fontSize.toPx()).toIntPx())
        }
    }

    @Test
    fun layout_build_layoutResult() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = "Hello"),
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
        )

        val layoutResult = textDelegate.layout(Constraints(0, 20), layoutDirection)

        assertThat(layoutResult).isNotNull()
    }

    @Test
    fun getPositionForOffset_First_Character() {
        val text = "Hello"
        val annotatedString = AnnotatedString(
            text,
            SpanStyle(fontSize = 20.sp, fontFamily = fontFamily)
        )

        val textDelegate = TextDelegate(
            text = annotatedString,
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
        )
        val layoutResult = textDelegate.layout(Constraints(), layoutDirection)

        val selection = layoutResult.getOffsetForPosition(Offset.Zero)

        assertThat(selection).isZero()
    }

    @Test
    fun getPositionForOffset_other_Character() {
        with(density) {
            val fontSize = 20.sp
            val characterIndex = 2 // Start from 0.
            val text = "Hello"

            val annotatedString = AnnotatedString(
                text,
                SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            )

            val textDelegate = TextDelegate(
                text = annotatedString,
                style = TextStyle.Default,
                density = this,
                resourceLoader = resourceLoader
            )
            val layoutResult = textDelegate.layout(Constraints(), layoutDirection)

            val selection = layoutResult.getOffsetForPosition(
                position = Offset((fontSize.toPx() * characterIndex + 1), 0f)
            )

            assertThat(selection).isEqualTo(characterIndex)
        }
    }

    @Test
    fun hasOverflowShaderFalse() {
        val text = "Hello"
        val spanStyle = SpanStyle(fontSize = 20.sp, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(text, spanStyle)
        val textDelegate = TextDelegate(
            text = annotatedString,
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
        )

        val layoutResult = textDelegate.layout(Constraints(), layoutDirection)

        assertThat(layoutResult.hasVisualOverflow).isFalse()

        // paint should not throw exception
        TextDelegate.paint(Canvas(android.graphics.Canvas()), layoutResult)
    }
}
