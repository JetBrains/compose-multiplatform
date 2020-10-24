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
package androidx.compose.foundation.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextDelegate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.input.OffsetMap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.res.ResourcesCompat
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(InternalTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class TextFieldDelegateIntegrationTest {
    private val density = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val resourceLoader = TestFontResourceLoader(context)
    private val layoutDirection = LayoutDirection.Ltr

    private class TestFontResourceLoader(val context: Context) : Font.ResourceLoader {
        override fun load(font: Font): Typeface {
            return when (font) {
                is ResourceFont -> ResourcesCompat.getFont(context, font.resId)!!
                else -> throw IllegalArgumentException("Unknown font type: $font")
            }
        }
    }

    @Test
    fun draw_selection_test() {
        val textDelegate = TextDelegate(
            text = AnnotatedString("Hello, World"),
            style = TextStyle.Default,
            maxLines = 2,
            density = density,
            resourceLoader = resourceLoader
        )
        val selection = TextRange(0, 1)
        val selectionColor = Color.Blue
        val layoutResult = textDelegate.layout(Constraints.fixedWidth(1024), layoutDirection)

        val expectedBitmap = layoutResult.toBitmap()
        val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
        TextDelegate.paintBackground(
            0,
            1,
            Paint().also { it.color = selectionColor },
            expectedCanvas,
            layoutResult
        )
        TextPainter.paint(expectedCanvas, layoutResult)

        val actualBitmap = layoutResult.toBitmap()
        val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))
        TextFieldDelegate.draw(
            canvas = actualCanvas,
            value = TextFieldValue(text = "Hello, World", selection = selection),
            selectionColor = selectionColor,
            offsetMap = OffsetMap.identityOffsetMap,
            textLayoutResult = layoutResult
        )

        assertThat(actualBitmap.sameAs(expectedBitmap)).isTrue()
    }

    @Test
    fun layout_height_constraint_max_height() {
        val textDelegate = TextDelegate(
            text = AnnotatedString("Hello, World"),
            style = TextStyle.Default,
            maxLines = 2,
            density = density,
            resourceLoader = resourceLoader
        )
        val layoutResult = textDelegate.layout(Constraints.fixedWidth(1024), layoutDirection)
        val requestHeight = layoutResult.size.height / 2

        val (_, height, _) = TextFieldDelegate.layout(
            textDelegate,
            Constraints.fixedHeight(requestHeight),
            layoutDirection
        )

        assertThat(height).isEqualTo(requestHeight)
    }

    @Test
    fun layout_height_constraint_min_height() {
        val textDelegate = TextDelegate(
            text = AnnotatedString("Hello, World"),
            style = TextStyle.Default,
            maxLines = 2,
            density = density,
            resourceLoader = resourceLoader
        )
        val layoutResult = textDelegate.layout(Constraints.fixedWidth(1024), layoutDirection)
        val requestHeight = layoutResult.size.height * 2

        val (_, height, _) = TextFieldDelegate.layout(
            textDelegate,
            Constraints.fixedHeight(requestHeight),
            layoutDirection
        )

        assertThat(height).isEqualTo(requestHeight)
    }
}

private fun TextLayoutResult.toBitmap() = Bitmap.createBitmap(
    size.width,
    size.height,
    Bitmap.Config.ARGB_8888
)