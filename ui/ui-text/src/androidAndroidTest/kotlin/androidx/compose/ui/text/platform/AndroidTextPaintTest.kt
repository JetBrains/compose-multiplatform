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

package androidx.compose.ui.text.platform

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidTextPaintTest {
    @Test
    fun constructor() {
        val density = 15.0f
        val textPaint = AndroidTextPaint(Paint.ANTI_ALIAS_FLAG, density)

        assertThat(textPaint.density).isEqualTo(15.0f)
        assertThat(textPaint.flags and Paint.ANTI_ALIAS_FLAG).isNotEqualTo(0)
    }

    @Test
    fun textDecoration_defaultValues() {
        val textPaint = defaultTextPaint
        assertThat(textPaint.isUnderlineText).isFalse()
        assertThat(textPaint.isStrikeThruText).isFalse()
    }

    @Test
    fun setTextDecoration_withNone() {
        val textPaint = defaultTextPaint
        textPaint.setTextDecoration(TextDecoration.None)
        assertThat(textPaint.isUnderlineText).isFalse()
        assertThat(textPaint.isStrikeThruText).isFalse()
    }

    @Test
    fun setTextDecoration_withNull() {
        val textPaint = defaultTextPaint
        textPaint.setTextDecoration(null)
        assertThat(textPaint.isUnderlineText).isFalse()
        assertThat(textPaint.isStrikeThruText).isFalse()
    }

    @Test
    fun setTextDecoration_withUnderline() {
        val textPaint = defaultTextPaint
        textPaint.setTextDecoration(TextDecoration.Underline)
        assertThat(textPaint.isUnderlineText).isTrue()
        assertThat(textPaint.isStrikeThruText).isFalse()
    }

    @Test
    fun setTextDecoration_withLineThrough() {
        val textPaint = defaultTextPaint
        textPaint.setTextDecoration(TextDecoration.LineThrough)
        assertThat(textPaint.isUnderlineText).isFalse()
        assertThat(textPaint.isStrikeThruText).isTrue()
    }

    @Test
    fun setTextDecoration_withLineThroughAndUnderline() {
        val textPaint = defaultTextPaint
        textPaint.setTextDecoration(
            TextDecoration.combine(
                listOf(TextDecoration.LineThrough, TextDecoration.Underline)
            )
        )
        assertThat(textPaint.isUnderlineText).isTrue()
        assertThat(textPaint.isStrikeThruText).isTrue()
    }

    @Test
    fun setTextDecoration_changeDecorationToNull() {
        val textPaint = defaultTextPaint
        textPaint.setTextDecoration(
            TextDecoration.combine(
                listOf(TextDecoration.LineThrough, TextDecoration.Underline)
            )
        )
        assertThat(textPaint.isUnderlineText).isTrue()
        assertThat(textPaint.isStrikeThruText).isTrue()

        textPaint.setTextDecoration(null)
        assertThat(textPaint.isUnderlineText).isFalse()
        assertThat(textPaint.isStrikeThruText).isFalse()
    }

    @Test
    fun setColor_to_valid_value() {
        val color = Color.Red
        val textPaint = defaultTextPaint
        textPaint.setColor(color)

        assertThat(textPaint.color).isEqualTo(color.toArgb())
    }

    @Test
    fun setColor_to_unspecified() {
        val textPaint = defaultTextPaint
        textPaint.setColor(Color.Unspecified)
        assertThat(textPaint.color).isNotEqualTo(Color.Unspecified.toArgb())

        textPaint.setColor(Color.Red)
        assertThat(textPaint.color).isEqualTo(Color.Red.toArgb())

        textPaint.setColor(Color.Unspecified)
        assertThat(textPaint.color).isEqualTo(Color.Red.toArgb())
    }

    @Test
    fun setColor_to_transparent() {
        val textPaint = defaultTextPaint
        textPaint.setColor(Color.Transparent)
        assertThat(textPaint.color).isEqualTo(Color.Transparent.toArgb())

        textPaint.setColor(Color.Red)
        assertThat(textPaint.color).isEqualTo(Color.Red.toArgb())

        textPaint.setColor(Color.Transparent)
        assertThat(textPaint.color).isEqualTo(Color.Transparent.toArgb())
    }

    @Test
    fun setShaderBrush_with_specified_size() {
        var calls = 0
        val brush = object : ShaderBrush() {
            val brush = linearGradient(listOf(Color.Red, Color.Blue))
            override fun createShader(size: Size): Shader {
                calls++
                return (brush as ShaderBrush).createShader(size)
            }
        }

        val size = Size(10f, 10f)
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size)

        assertThat(textPaint.shader).isNotNull()
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun setShaderBrush_with_unspecified_size() {
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val size = Size.Unspecified
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size)

        assertThat(textPaint.shader).isNull()
    }

    @Test
    fun setColorBrush_with_specified_size() {
        val brush = SolidColor(Color.Red)
        val size = Size(10f, 10f)
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size)

        assertThat(textPaint.shader).isNull()
        assertThat(textPaint.color).isEqualTo(Color.Red.toArgb())
    }

    @Test
    fun setColorBrush_with_unspecified_size() {
        val brush = SolidColor(Color.Red)
        val size = Size.Unspecified
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size)

        assertThat(textPaint.shader).isNull()
        assertThat(textPaint.color).isEqualTo(Color.Red.toArgb())
    }

    @Test
    fun setColorBrush_with_alpha() {
        val brush = SolidColor(Color.Red)
        val size = Size.Unspecified
        val alpha = 0.6f
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size, alpha)

        assertThat(textPaint.shader).isNull()
        assertThat(textPaint.color).isEqualTo(Color.Red.copy(0.6f).toArgb())
    }

    @Test
    fun setTransparentColorBrush_with_alpha_modulates() {
        val brush = SolidColor(Color.Red.copy(0.8f))
        val size = Size.Unspecified
        val alpha = 0.6f
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size, alpha)

        assertThat(textPaint.shader).isNull()
        assertThat(textPaint.color).isEqualTo(Color.Red.copy(0.48f).toArgb())
    }

    @Test
    fun setBrush_with_tooHigh_alpha() {
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val size = Size(10f, 10f)
        val alpha = 10e5f
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size, alpha)

        assertThat(textPaint.shader).isNotNull()
        assertThat(textPaint.alpha).isEqualTo(255)
    }

    @Test
    fun setBrush_with_tooLow_alpha() {
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val size = Size(10f, 10f)
        val alpha = -10e5f
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size, alpha)

        assertThat(textPaint.shader).isNotNull()
        assertThat(textPaint.alpha).isEqualTo(0)
    }

    @Test
    fun setUnspecifiedBrush_with_specified_size() {
        val brush = SolidColor(Color.Unspecified)
        val size = Size(10f, 10f)
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size)

        assertThat(textPaint.shader).isNull()
        assertThat(textPaint.color).isNotEqualTo(Color.Unspecified.toArgb())

        textPaint.setBrush(SolidColor(Color.Red), size)

        assertThat(textPaint.shader).isNull()
        assertThat(textPaint.color).isEqualTo(Color.Red.toArgb())
    }

    @Test
    fun setNullBrush_with_specified_size() {
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val size = Size(10f, 10f)
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size)

        assertThat(textPaint.shader).isNotNull()

        textPaint.setBrush(null, size)

        assertThat(textPaint.shader).isNull()
    }

    @Test
    fun setBrush_with_alpha() {
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val size = Size(10f, 10f)
        val alpha = 0.6f
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size, alpha)

        assertThat(textPaint.shader).isNotNull()
        assertThat(textPaint.alpha).isEqualTo((255 * 0.6f).roundToInt())

        textPaint.setBrush(null, size)

        assertThat(textPaint.shader).isNull()
        assertThat(textPaint.alpha).isEqualTo((255 * 0.6f).roundToInt())
    }

    @Test
    fun setBrush_with_alpha_only_alpha_changes() {
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val size = Size(10f, 10f)
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size, 0.6f)

        assertThat(textPaint.shader).isNotNull()
        assertThat(textPaint.alpha).isEqualTo((255 * 0.6f).roundToInt())

        textPaint.setBrush(brush, size, 0.8f)

        assertThat(textPaint.shader).isNotNull()
        assertThat(textPaint.alpha).isEqualTo((255 * 0.8f).roundToInt())
    }

    @Test
    fun setShaderBrush_after_setColor() {
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val size = Size(10f, 10f)
        val alpha = 0.6f
        val textPaint = defaultTextPaint
        textPaint.setColor(Color.Red)
        textPaint.setBrush(brush, size, alpha)

        assertThat(textPaint.shader).isNotNull()
        assertThat(textPaint.alpha).isEqualTo((255 * 0.6f).roundToInt())
    }

    @Test
    fun setColor_after_setShaderBrush() {
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val size = Size(10f, 10f)
        val alpha = 0.6f
        val textPaint = defaultTextPaint
        textPaint.setBrush(brush, size, alpha)
        textPaint.setColor(Color.Red)

        assertThat(textPaint.shader).isNull()
        assertThat(textPaint.color).isEqualTo(Color.Red.toArgb())
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun shadow_default_values() {
        val textPaint = defaultTextPaint

        // default color is 0 since we do not update it
        assertThat(textPaint.shadowLayerDx).isEqualTo(0f)
        assertThat(textPaint.shadowLayerDy).isEqualTo(0f)
        assertThat(textPaint.shadowLayerRadius).isEqualTo(0f)
        assertThat(textPaint.shadowLayerColor).isEqualTo(0)
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun setShadow() {
        val dx = 1f
        val dy = 2f
        val radius = 3f
        val color = Color.Red
        val textPaint = defaultTextPaint

        textPaint.setShadow(Shadow(color, Offset(dx, dy), radius))

        assertThat(textPaint.shadowLayerDx).isEqualTo(dx)
        assertThat(textPaint.shadowLayerDy).isEqualTo(dy)
        assertThat(textPaint.shadowLayerRadius).isEqualTo(radius)
        assertThat(textPaint.shadowLayerColor).isEqualTo(color.toArgb())
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun resetShadow_to_None() {
        val dx = 1f
        val dy = 2f
        val radius = 3f
        val color = Color.Red
        val textPaint = defaultTextPaint

        textPaint.setShadow(Shadow(color, Offset(dx, dy), radius))
        textPaint.setShadow(Shadow.None)

        assertThat(textPaint.shadowLayerDx).isEqualTo(0f)
        assertThat(textPaint.shadowLayerDy).isEqualTo(0f)
        assertThat(textPaint.shadowLayerRadius).isEqualTo(0f)
        assertThat(textPaint.shadowLayerColor).isEqualTo(0)
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun resetShadow_to_null() {
        val dx = 1f
        val dy = 2f
        val radius = 3f
        val color = Color.Red
        val textPaint = defaultTextPaint

        textPaint.setShadow(Shadow(color, Offset(dx, dy), radius))
        textPaint.setShadow(null)

        assertThat(textPaint.shadowLayerDx).isEqualTo(0f)
        assertThat(textPaint.shadowLayerDy).isEqualTo(0f)
        assertThat(textPaint.shadowLayerRadius).isEqualTo(0f)
        assertThat(textPaint.shadowLayerColor).isEqualTo(0)
    }

    private val defaultTextPaint get() = AndroidTextPaint(flags = 0, density = 1.0f)
}
