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

package androidx.compose.ui.unit

import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("Deprecation")

@RunWith(JUnit4::class)
class TextUnitTest {
    @Test
    fun construct_sp_from_float() {
        5f.sp.also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_sp_from_int() {
        5.sp.also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_sp_from_double() {
        5.0.sp.also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_sp_from_float_extension() {
        5f.sp.also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_sp_from_int_extension() {
        5.sp.also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_sp_from_double_extension() {
        5.0.sp.also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_float() {
        5f.em.also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_int() {
        5.em.also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_double() {
        5.0.em.also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_float_extension() {
        5f.em.also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_int_extension() {
        5.em.also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_double_extension() {
        5.0.em.also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun unspecified_type_check() {
        TextUnit.Unspecified.also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isTrue()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Unspecified)
        }
    }

    // Unary minuses
    @Test
    fun minus_em() {
        -(1.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(1f)
        }
    }

    @Test
    fun minus_sp() {
        -(1.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(1f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun minus_unspecified() {
        -TextUnit.Unspecified
    }

    // Multiplications
    @Test
    fun multiply_sp_float() {
        (2.sp * 3f).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_sp_double() {
        (2.sp * 3.0).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_sp_int() {
        (2.sp * 3).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_float_sp() {
        (2f * 3.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_double_sp() {
        (2.0 * 3.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_int_sp() {
        (2 * 3.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_float_em() {
        (2f * 3.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_double_em() {
        (2.0 * 3.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_int_em() {
        (2 * 3.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_em_float() {
        (2.em * 3f).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_em_double() {
        (2.em * 3.0).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_em_int() {
        (2.em * 3).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isUnspecified).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun multiply_unspecified_float() {
        TextUnit.Unspecified * 3f
    }

    @Test(expected = RuntimeException::class)
    fun multiply_unspecified_double() {
        TextUnit.Unspecified * 3.0
    }

    @Test(expected = RuntimeException::class)
    fun multiply_unspecified_int() {
        TextUnit.Unspecified * 3
    }

    @Test(expected = RuntimeException::class)
    fun multiply_float_unspecified() {
        3f * TextUnit.Unspecified
    }

    @Test(expected = RuntimeException::class)
    fun multiply_double_unspecified() {
        3.0f * TextUnit.Unspecified
    }

    @Test(expected = RuntimeException::class)
    fun multiply_int_unspecified() {
        3 * TextUnit.Unspecified
    }

    // Comparisons
    @Test
    fun compare_sp_sp() {
        assertThat(1.sp > 2.sp).isFalse()
        assertThat(1.sp >= 2.sp).isFalse()
        assertThat(1.sp < 2.sp).isTrue()
        assertThat(1.sp <= 2.sp).isTrue()
        assertThat(1.sp == 2.sp).isFalse()

        assertThat(2.sp > 1.sp).isTrue()
        assertThat(2.sp >= 1.sp).isTrue()
        assertThat(2.sp < 1.sp).isFalse()
        assertThat(2.sp <= 1.sp).isFalse()
        assertThat(2.sp == 1.sp).isFalse()

        assertThat(2.sp > 2.sp).isFalse()
        assertThat(2.sp >= 2.sp).isTrue()
        assertThat(2.sp < 2.sp).isFalse()
        assertThat(2.sp <= 2.sp).isTrue()
        assertThat(2.sp == 2.sp).isTrue()
    }

    @Test
    fun compare_em_em() {
        assertThat(1.em > 2.em).isFalse()
        assertThat(1.em >= 2.em).isFalse()
        assertThat(1.em < 2.em).isTrue()
        assertThat(1.em <= 2.em).isTrue()
        assertThat(1.em == 2.em).isFalse()

        assertThat(2.em > 1.em).isTrue()
        assertThat(2.em >= 1.em).isTrue()
        assertThat(2.em < 1.em).isFalse()
        assertThat(2.em <= 1.em).isFalse()
        assertThat(2.em == 1.em).isFalse()

        assertThat(2.em > 2.em).isFalse()
        assertThat(2.em >= 2.em).isTrue()
        assertThat(2.em < 2.em).isFalse()
        assertThat(2.em <= 2.em).isTrue()
        assertThat(2.em == 2.em).isTrue()
    }

    @Test(expected = RuntimeException::class)
    fun compare_sp_em() {
        1.sp > 2.em
    }

    @Test(expected = RuntimeException::class)
    fun compare_em_sp() {
        1.em > 2.sp
    }

    @Test(expected = RuntimeException::class)
    fun compare_unspecified_sp() {
        TextUnit.Unspecified > 2.sp
    }

    @Test(expected = RuntimeException::class)
    fun compare_sp_unspecified() {
        1.sp > TextUnit.Unspecified
    }

    @Test(expected = RuntimeException::class)
    fun compare_unspecified_em() {
        TextUnit.Unspecified > 2.em
    }

    @Test(expected = RuntimeException::class)
    fun compare_em_unspecified() {
        1.em > TextUnit.Unspecified
    }

    @Test(expected = RuntimeException::class)
    fun compare_unspecified_unspecified() {
        TextUnit.Unspecified > TextUnit.Unspecified
    }

    // Equalities
    @Test
    fun equals_sp_sp() {
        assertThat(2.sp == 2.0.sp).isTrue()
        assertThat(2.sp == 1.0.sp).isFalse()
    }

    @Test
    fun equals_em_em() {
        assertThat(2.em == 2.0.em).isTrue()
        assertThat(2.em == 1.0.em).isFalse()
    }

    @Test
    fun equals_unspecified_unspecified() {
        assertThat(TextUnit.Unspecified == TextUnit.Unspecified).isTrue()
    }

    @Test
    fun equals_sp_em() {
        assertThat(2.sp == 2.0.em).isFalse()
        assertThat(2.sp == 1.0.em).isFalse()
    }

    @Test
    fun equals_em_sp() {
        assertThat(2.em == 2.0.sp).isFalse()
        assertThat(2.em == 1.0.sp).isFalse()
    }

    @Test
    fun equals_sp_unspecified() {
        assertThat(2.sp == TextUnit.Unspecified).isFalse()
    }

    @Test
    fun equals_unspecified_sp() {
        assertThat(TextUnit.Unspecified == 2.sp).isFalse()
    }

    @Test
    fun equals_em_unspecified() {
        assertThat(2.em == TextUnit.Unspecified).isFalse()
    }

    @Test
    fun equals_unspecified_em() {
        assertThat(TextUnit.Unspecified == 2.em).isFalse()
    }

    @Test
    fun unspecified_value_equals_nan() {
        assertThat(TextUnit.Unspecified.value).isEqualTo(Float.NaN)
    }

    @Test
    fun testIsSpecified() {
        Assert.assertFalse(TextUnit.Unspecified.isSpecified)
        Assert.assertTrue(1.sp.isSpecified)
    }

    @Test
    fun testIsUnspecified() {
        Assert.assertTrue(TextUnit.Unspecified.isUnspecified)
        Assert.assertFalse(1.sp.isUnspecified)
    }

    @Test
    fun testTakeOrElseTrue() {
        Assert.assertTrue(1.sp.takeOrElse { TextUnit.Unspecified }.isSpecified)
    }

    @Test
    fun testTakeOrElseFalse() {
        Assert.assertTrue(TextUnit.Unspecified.takeOrElse { 1.sp }.isSpecified)
    }
}