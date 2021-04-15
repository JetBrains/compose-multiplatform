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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

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
