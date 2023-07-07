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

package androidx.compose.ui.text.font

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalTextApi::class)
@RunWith(JUnit4::class)
class FontVariationTest {
    @Test
    fun ital_setsItal() {
        val fontVariation = FontVariation.italic(0.7f)
        assertThat(fontVariation.toVariationValue(null)).isEqualTo(0.7f)
        assertThat(fontVariation.axisName).isEqualTo("ital")
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun ital_throws_whenTooLow() {
        FontVariation.italic(-1.0f)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun ital_throws_whenTooHigh() {
        FontVariation.italic(1.1f)
    }

    @Test
    fun Opsz_sets() {
        val fontVariation = FontVariation.opticalSizing(18.sp)
        assertThat(fontVariation.toVariationValue(Density(1f))).isEqualTo(18f)
        assertThat(fontVariation.axisName).isEqualTo("opsz")
    }

    @Test
    fun Opsz_convertsWithDensity() {
        val fontVariation = FontVariation.opticalSizing(18.sp)
        val density = Density(10f, 3f)
        assertThat(fontVariation.toVariationValue(density)).isEqualTo(54f)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun Opsz_throws_whenTooLow() {
        FontVariation.opticalSizing(TextUnit.Unspecified)
    }

    @Test
    fun Opsz_needsDensity() {
        val fontVariation = FontVariation.opticalSizing(18.sp)
        assertThat(fontVariation.needsDensity).isTrue()
    }

    @Test
    fun Slnt_sets() {
        val fontVariation = FontVariation.slant(0.7f)
        assertThat(fontVariation.toVariationValue(null)).isEqualTo(0.7f)
        assertThat(fontVariation.axisName).isEqualTo("slnt")
    }

    @Test(expected = IllegalArgumentException::class)
    fun Slnt_throws_whenTooSmall() {
        FontVariation.slant(-91f)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun Slnt_throws_whenTooBig() {
        FontVariation.slant(91f)
    }

    @Test
    fun Wdth_sets() {
        val fontVariation = FontVariation.width(0.7f)
        assertThat(fontVariation.toVariationValue(null)).isEqualTo(0.7f)
        assertThat(fontVariation.axisName).isEqualTo("wdth")
    }

    @Test
    fun Wdth_sets_atMaxSize() {
        val fontVariation = FontVariation.width(Float.MAX_VALUE)
        assertThat(fontVariation.toVariationValue(null)).isEqualTo(Float.MAX_VALUE)
        assertThat(fontVariation.axisName).isEqualTo("wdth")
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun Wdth_throws_whenTooSmall() {
        FontVariation.width(0f)
    }

    @Test
    fun Wght_sets() {
        val fontVariation = FontVariation.weight(200)
        assertThat(fontVariation.toVariationValue(null)).isEqualTo(200)
        assertThat(fontVariation.axisName).isEqualTo("wght")
    }

    @Test(expected = IllegalArgumentException::class)
    fun Wght_throws_whenTooSmall() {
        FontVariation.weight(0)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun Wght_throws_whenTooBig() {
        FontVariation.weight(1001)
    }

    @Test
    fun grad_sets() {
        val fontVariation = FontVariation.grade(200)
        assertThat(fontVariation.toVariationValue(null)).isEqualTo(200)
        assertThat(fontVariation.axisName).isEqualTo("GRAD")
    }

    @Test(expected = IllegalArgumentException::class)
    fun grad_throws_whenTooSmall() {
        FontVariation.grade(-1001)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun grad_throws_whenTooBig() {
        FontVariation.grade(1001)
    }

    @Test
    fun setting_makesSetting() {
        val setting: FontVariation.Setting = FontVariation.Setting("1234", 8.9f)
        assertThat(setting.axisName).isEqualTo("1234")
        assertThat(setting.toVariationValue(null)).isEqualTo(8.9f)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun setting_throws_whenBadName() {
        FontVariation.Setting("Weight", 500f)
    }

    @Test
    fun canExtend() {
        fun FontVariation.fizzable(fiz: Int): FontVariation.Setting {
            require(fiz in 1..11) { "'fzzt' must be in 1..11" }
            return Setting("fzzt", fiz.toFloat())
        }
        val variation = FontVariation.fizzable(7)
        assertThat(variation.axisName).isEqualTo("fzzt")
        assertThat(variation.toVariationValue(null)).isEqualTo(7f)
    }
}