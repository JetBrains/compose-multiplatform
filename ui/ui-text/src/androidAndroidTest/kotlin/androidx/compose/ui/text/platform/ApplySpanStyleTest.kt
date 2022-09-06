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

package androidx.compose.ui.text.platform

import android.graphics.Typeface
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.extensions.applySpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ApplySpanStyleTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext!!
    private val density = Density(context)
    private val resolveTypeface: (FontFamily?, FontWeight, FontStyle, FontSynthesis) -> Typeface =
        { _, _, _, _ ->
            Typeface.DEFAULT
        }

    @Test
    fun textDecorationUnderline_shouldBeLeftAsSpan() {
        val textDecoration = TextDecoration.Underline
        val spanStyle = SpanStyle(textDecoration = textDecoration)
        val tp = AndroidTextPaint(0, density.density)
        tp.isUnderlineText = false

        val notApplied = tp.applySpanStyle(spanStyle, resolveTypeface, density)

        assertThat(tp.isUnderlineText).isEqualTo(false)
        assertThat(notApplied.textDecoration).isEqualTo(textDecoration)
    }

    @Test
    fun textDecorationLineThrough_shouldBeLeftAsSpan() {
        val textDecoration = TextDecoration.LineThrough
        val spanStyle = SpanStyle(textDecoration = textDecoration)
        val tp = AndroidTextPaint(0, density.density)
        tp.isStrikeThruText = false

        val notApplied = tp.applySpanStyle(spanStyle, resolveTypeface, density)

        assertThat(tp.isStrikeThruText).isEqualTo(false)
        assertThat(notApplied.textDecoration).isEqualTo(textDecoration)
    }

    @Test
    fun textDecorationNone_shouldNotBeLeftAsSpan() {
        val textDecoration = TextDecoration.None
        val spanStyle = SpanStyle(textDecoration = textDecoration)
        val tp = AndroidTextPaint(0, density.density)
        tp.isUnderlineText = false
        tp.isStrikeThruText = false

        val notApplied = tp.applySpanStyle(spanStyle, resolveTypeface, density)

        assertThat(tp.isUnderlineText).isEqualTo(false)
        assertThat(tp.isStrikeThruText).isEqualTo(false)
        assertThat(notApplied.textDecoration).isNull()
    }
}