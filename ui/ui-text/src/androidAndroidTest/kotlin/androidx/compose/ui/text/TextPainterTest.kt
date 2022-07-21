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

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TextPainterTest {

    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = createFontFamilyResolver(context)
    private var defaultDensity = Density(density = 1f)
    private var layoutDirection = LayoutDirection.Ltr

    private val longText = AnnotatedString(
        "Lorem ipsum dolor sit amet, consectetur " +
            "adipiscing elit. Curabitur augue leo, finibus vitae felis ac, pretium condimentum " +
            "augue. Nullam non libero sed lectus aliquet venenatis non at purus. Fusce id arcu " +
            "eu mauris pulvinar laoreet."
    )

    @Test
    fun drawTextWithMeasurer_shouldBeEqualTo_drawTextLayoutResult() {
        val measurer = textMeasurer()
        val textLayoutResult = measurer.measure(
            text = longText,
            style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 20.sp),
            size = IntSize(400, 400)
        )

        val bitmap = draw {
            drawText(textLayoutResult)
        }
        val bitmap2 = draw {
            drawText(
                measurer,
                text = longText,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 20.sp),
                size = IntSize(400, 400)
            )
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun textMeasurerCache_shouldNotAffectTheResult_forColor() {
        val measurer = textMeasurer(cacheSize = 8)

        val bitmap = draw {
            drawText(
                textMeasurer = measurer,
                text = longText,
                style = TextStyle(
                    color = Color.Red,
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = 20.sp
                ),
                size = IntSize(400, 400)
            )
        }
        val bitmap2 = draw {
            drawText(
                textMeasurer = measurer,
                text = longText,
                style = TextStyle(
                    color = Color.Blue,
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = 20.sp
                ),
                size = IntSize(400, 400)
            )
        }

        assertThat(bitmap).isNotEqualToBitmap(bitmap2)
    }

    @Test
    fun textMeasurerCache_shouldNotAffectTheResult_forFontSize() {
        val measurer = textMeasurer(cacheSize = 8)

        val bitmap = draw {
            drawText(
                textMeasurer = measurer,
                text = longText,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 20.sp),
                size = IntSize(400, 400)
            )
        }
        val bitmap2 = draw {
            drawText(
                textMeasurer = measurer,
                text = longText,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 24.sp),
                size = IntSize(400, 400)
            )
        }

        assertThat(bitmap).isNotEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextLayout_shouldChangeColor() {
        val measurer = textMeasurer()
        val textLayoutResultRed = measurer.measure(
            text = longText,
            style = TextStyle(
                color = Color.Red,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            size = IntSize(400, 400)
        )

        val textLayoutResultBlue = measurer.measure(
            text = longText,
            style = TextStyle(
                color = Color.Blue,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            size = IntSize(400, 400)
        )

        val bitmap = draw {
            drawText(textLayoutResultRed, color = Color.Blue)
        }
        val bitmap2 = draw {
            drawText(textLayoutResultBlue)
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextLayout_shouldChangeAlphaColor() {
        val measurer = textMeasurer()
        val textLayoutResultOpaque = measurer.measure(
            text = longText,
            style = TextStyle(
                color = Color.Red,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            size = IntSize(400, 400)
        )

        val textLayoutResultHalfOpaque = measurer.measure(
            text = longText,
            style = TextStyle(
                color = Color.Red.copy(alpha = 0.5f),
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            size = IntSize(400, 400)
        )

        val bitmap = draw {
            drawText(textLayoutResultOpaque, alpha = 0.5f)
        }
        val bitmap2 = draw {
            drawText(textLayoutResultHalfOpaque)
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextLayout_shouldChangeBrush() {
        val rbBrush = Brush.radialGradient(listOf(Color.Red, Color.Blue))
        val gyBrush = Brush.radialGradient(listOf(Color.Green, Color.Yellow))
        val measurer = textMeasurer()
        val textLayoutResultRB = measurer.measure(
            text = longText,
            style = TextStyle(
                brush = rbBrush,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            size = IntSize(400, 400)
        )

        val textLayoutResultGY = measurer.measure(
            text = longText,
            style = TextStyle(
                brush = gyBrush,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            size = IntSize(400, 400)
        )

        val bitmap = draw {
            drawText(textLayoutResultRB, brush = gyBrush)
        }
        val bitmap2 = draw {
            drawText(textLayoutResultGY)
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextLayout_shouldChangeAlphaForBrush() {
        val rbBrush = Brush.radialGradient(listOf(Color.Red, Color.Blue))
        val measurer = textMeasurer()
        val textLayoutResultOpaque = measurer.measure(
            text = longText,
            style = TextStyle(
                brush = rbBrush,
                alpha = 1f,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            size = IntSize(400, 400)
        )

        val textLayoutResultHalfOpaque = measurer.measure(
            text = longText,
            style = TextStyle(
                brush = rbBrush,
                alpha = 0.5f,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            size = IntSize(400, 400)
        )

        val bitmap = draw {
            drawText(textLayoutResultOpaque, alpha = 0.5f)
        }
        val bitmap2 = draw {
            drawText(textLayoutResultHalfOpaque)
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun textMeasurerDraw_isConstrainedTo_canvasSizeByDefault() {
        val measurer = textMeasurer()
        // constrain the width, height is ignored
        val textLayoutResult = measurer.measure(
            text = longText,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            size = IntSize(200, 4000)
        )

        val bitmap = draw(200f, 4000f) {
            drawText(textLayoutResult)
        }
        val bitmap2 = draw(200f, 4000f) {
            drawText(measurer, longText, style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ))
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun textMeasurerDraw_usesCanvasDensity_ByDefault() {
        val measurer = textMeasurer()
        // constrain the width, height is ignored
        val textLayoutResult = measurer.measure(
            text = longText,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            density = Density(4f),
            size = IntSize(1000, 1000)
        )

        val bitmap = draw {
            drawText(textLayoutResult)
        }

        defaultDensity = Density(4f)
        val bitmap2 = draw {
            drawText(measurer, longText, style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ))
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextClipsTheContent_ifOverflowIsClip() {
        val measurer = textMeasurer()
        // constrain the width, height is ignored
        val textLayoutResult = measurer.measure(
            text = longText,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 14.sp
            ),
            softWrap = false,
            overflow = TextOverflow.Clip,
            size = IntSize(200, 200)
        )

        val bitmap = draw(400f, 200f) {
            drawText(textLayoutResult)
        }
        val croppedBitmap = Bitmap.createBitmap(bitmap, 200, 0, 200, 200)

        // cropped part should be empty
        assertThat(croppedBitmap).isEqualToBitmap(Bitmap.createBitmap(
            200,
            200,
            Bitmap.Config.ARGB_8888))
    }

    @Test
    fun drawTextDoesNotClipTheContent_ifOverflowIsVisible() {
        val measurer = textMeasurer()
        // constrain the width, height is ignored
        val textLayoutResult = measurer.measure(
            text = longText,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 14.sp
            ),
            softWrap = false,
            overflow = TextOverflow.Clip,
            size = IntSize(400, 200)
        )

        val textLayoutResultNoClip = measurer.measure(
            text = longText,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 14.sp
            ),
            softWrap = false,
            overflow = TextOverflow.Visible,
            size = IntSize(200, 200)
        )

        val bitmap = draw(400f, 200f) {
            drawText(textLayoutResult)
        }

        val bitmapNoClip = draw(400f, 200f) {
            drawText(textLayoutResultNoClip)
        }

        // cropped part should be empty
        assertThat(bitmap).isEqualToBitmap(bitmapNoClip)
    }

    private fun textMeasurer(
        fontFamilyResolver: FontFamily.Resolver = this.fontFamilyResolver,
        density: Density = this.defaultDensity,
        layoutDirection: LayoutDirection = this.layoutDirection,
        cacheSize: Int = 0
    ): TextMeasurer = TextMeasurer(
        fontFamilyResolver,
        density,
        layoutDirection,
        cacheSize
    )

    fun draw(
        width: Float = 1000f,
        height: Float = 1000f,
        block: DrawScope.() -> Unit
    ): Bitmap {
        val size = Size(width, height)
        val bitmap = Bitmap.createBitmap(
            size.width.toIntPx(),
            size.height.toIntPx(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap.asImageBitmap())
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            defaultDensity,
            layoutDirection,
            canvas,
            size,
            block
        )
        return bitmap
    }
}