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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextUnitTest {
    @Test
    fun construct_sp_from_float() {
        TextUnit.Sp(5f).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_sp_from_int() {
        TextUnit.Sp(5).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_sp_from_double() {
        TextUnit.Sp(5.0).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_sp_from_float_extension() {
        5f.sp.also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_sp_from_int_extension() {
        5.sp.also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_sp_from_double_extension() {
        5.0.sp.also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_float() {
        TextUnit.Em(5f).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_int() {
        TextUnit.Em(5).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_double() {
        TextUnit.Em(5.0).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_float_extension() {
        5f.em.also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_int_extension() {
        5.em.also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun construct_em_from_double_extension() {
        5.0.em.also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(5f)
        }
    }

    @Test
    fun inherit_type_check() {
        TextUnit.Inherit.also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isTrue()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Inherit)
        }
    }

    // Additions
    @Test
    fun add_sp_sp() {
        (1.sp + 2.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(3f)
        }
    }

    @Test
    fun add_em_em() {
        (1.em + 2.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(3f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun add_sp_em() {
        1.sp + 2.em
    }

    @Test(expected = RuntimeException::class)
    fun add_sp_inherit() {
        1.sp + TextUnit.Inherit
    }

    @Test(expected = RuntimeException::class)
    fun add_em_sp() {
        1.em + 2.sp
    }

    @Test(expected = RuntimeException::class)
    fun add_em_inherit() {
        1.em + TextUnit.Inherit
    }

    @Test(expected = RuntimeException::class)
    fun add_inherit_inherit() {
        TextUnit.Inherit + TextUnit.Inherit
    }

    // Subtractions
    @Test
    fun sub_sp_sp() {
        (1.sp - 2.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(-1f)
        }
    }

    @Test
    fun sub_em_em() {
        (1.em - 2.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(-1f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun sub_sp_em() {
        1.sp - 2.em
    }

    @Test(expected = RuntimeException::class)
    fun sub_sp_inherit() {
        1.sp - TextUnit.Inherit
    }

    @Test(expected = RuntimeException::class)
    fun sub_em_sp() {
        1.em - 2.sp
    }

    @Test(expected = RuntimeException::class)
    fun sub_em_inherit() {
        1.em - TextUnit.Inherit
    }

    @Test(expected = RuntimeException::class)
    fun sub_inherit_inherit() {
        TextUnit.Inherit - TextUnit.Inherit
    }

    // Unary minuses
    @Test
    fun minus_em() {
        -(1.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(1f)
        }
    }

    @Test
    fun minus_sp() {
        -(1.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(1f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun minus_inherit() {
        -TextUnit.Inherit
    }

    // Multiplications
    @Test
    fun multiply_sp_float() {
        (2.sp * 3f).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_sp_double() {
        (2.sp * 3.0).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_sp_int() {
        (2.sp * 3).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_float_sp() {
        (2f * 3.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_double_sp() {
        (2.0 * 3.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_int_sp() {
        (2 * 3.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_float_em() {
        (2f * 3.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_double_em() {
        (2.0 * 3.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_int_em() {
        (2 * 3.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_em_float() {
        (2.em * 3f).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_em_double() {
        (2.em * 3.0).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test
    fun multiply_em_int() {
        (2.em * 3).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(6f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun multiply_inherit_float() {
        TextUnit.Inherit * 3f
    }

    @Test(expected = RuntimeException::class)
    fun multiply_inherit_double() {
        TextUnit.Inherit * 3.0
    }

    @Test(expected = RuntimeException::class)
    fun multiply_inherit_int() {
        TextUnit.Inherit * 3
    }

    @Test(expected = RuntimeException::class)
    fun multiply_float_inherit() {
        3f * TextUnit.Inherit
    }

    @Test(expected = RuntimeException::class)
    fun multiply_double_inherit() {
        3.0f * TextUnit.Inherit
    }

    @Test(expected = RuntimeException::class)
    fun multiply_int_inherit() {
        3 * TextUnit.Inherit
    }

    // Divisions
    @Test
    fun divide_sp_float() {
        (1.sp / 2f).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(0.5f)
        }
    }

    @Test
    fun divide_sp_double() {
        (1.sp / 2.0).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(0.5f)
        }
    }

    @Test
    fun divide_sp_int() {
        (1.sp / 2).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(0.5f)
        }
    }

    @Test
    fun divide_em_float() {
        (1.em / 2f).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(0.5f)
        }
    }

    @Test
    fun divide_em_double() {
        (1.em / 2.0).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(0.5f)
        }
    }

    @Test
    fun divide_em_int() {
        (1.em / 2).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(0.5f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun divide_inherit_float() {
        TextUnit.Inherit / 2f
    }

    @Test(expected = RuntimeException::class)
    fun divide_inherit_double() {
        TextUnit.Inherit / 2.0
    }

    @Test(expected = RuntimeException::class)
    fun divide_inherit_int() {
        TextUnit.Inherit / 2
    }

    @Test
    fun divide_sp_sp() {
        assertThat(1.sp / 2.sp).isEqualTo(0.5f)
    }

    @Test
    fun divide_em_em() {
        assertThat(1.em / 2.em).isEqualTo(0.5f)
    }

    @Test(expected = RuntimeException::class)
    fun divide_sp_em() {
        1.sp / 2.em
    }

    @Test(expected = RuntimeException::class)
    fun divide_em_sp() {
        1.em / 2.sp
    }

    @Test(expected = RuntimeException::class)
    fun divide_inherit_sp() {
        TextUnit.Inherit / 2.sp
    }

    @Test(expected = RuntimeException::class)
    fun divide_inherit_em() {
        TextUnit.Inherit / 2.em
    }

    @Test(expected = RuntimeException::class)
    fun divide_sp_inherit() {
        1.sp / TextUnit.Inherit
    }

    @Test(expected = RuntimeException::class)
    fun divide_em_inherit() {
        1.em / TextUnit.Inherit
    }

    @Test(expected = RuntimeException::class)
    fun divide_inherit_inherit() {
        TextUnit.Inherit / TextUnit.Inherit
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
    fun compare_inherit_sp() {
        TextUnit.Inherit > 2.sp
    }

    @Test(expected = RuntimeException::class)
    fun compare_sp_inherit() {
        1.sp > TextUnit.Inherit
    }

    @Test(expected = RuntimeException::class)
    fun compare_inherit_em() {
        TextUnit.Inherit > 2.em
    }

    @Test(expected = RuntimeException::class)
    fun compare_em_inherit() {
        1.em > TextUnit.Inherit
    }

    @Test(expected = RuntimeException::class)
    fun compare_inherit_inherit() {
        TextUnit.Inherit > TextUnit.Inherit
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
    fun equals_inherit_inherit() {
        assertThat(TextUnit.Inherit == TextUnit.Inherit).isTrue()
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
    fun equals_sp_inherit() {
        assertThat(2.sp == TextUnit.Inherit).isFalse()
    }

    @Test
    fun equals_inherit_sp() {
        assertThat(TextUnit.Inherit == 2.sp).isFalse()
    }

    @Test
    fun equals_em_inherit() {
        assertThat(2.em == TextUnit.Inherit).isFalse()
    }

    @Test
    fun equals_inherit_em() {
        assertThat(TextUnit.Inherit == 2.em).isFalse()
    }

    // Mins
    @Test
    fun min_sp_sp() {
        min(1.sp, 2.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(1f)
        }
    }

    @Test
    fun min_em_em() {
        min(1.em, 2.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(1f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun min_sp_em() {
        min(1.sp, 2.em)
    }

    @Test(expected = RuntimeException::class)
    fun min_sp_inherit() {
        min(1.sp, TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun min_em_sp() {
        min(1.em, 2.sp)
    }

    @Test(expected = RuntimeException::class)
    fun min_em_inherit() {
        min(1.em, TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun min_inherit_inherit() {
        min(TextUnit.Inherit, TextUnit.Inherit)
    }

    // Maxes
    @Test
    fun max_sp_sp() {
        max(1.sp, 2.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(2f)
        }
    }

    @Test
    fun max_em_em() {
        max(1.em, 2.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(2f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun max_sp_em() {
        max(1.sp, 2.em)
    }

    @Test(expected = RuntimeException::class)
    fun max_sp_inherit() {
        max(1.sp, TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun max_em_sp() {
        max(1.em, 2.sp)
    }

    @Test(expected = RuntimeException::class)
    fun max_em_inherit() {
        max(1.em, TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun max_inherit_inherit() {
        max(TextUnit.Inherit, TextUnit.Inherit)
    }

    // coerceIns
    @Test
    fun coerceIn_sp_sp_sp() {
        3.sp.coerceIn(1.sp, 2.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(2f)
        }

        0.sp.coerceIn(1.sp, 2.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(1f)
        }
    }

    @Test
    fun coerceIn_em_em_em() {
        3.em.coerceIn(1.em, 2.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(2f)
        }

        0.em.coerceIn(1.em, 2.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(1f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_sp_sp_em() {
        1.sp.coerceIn(1.sp, 1.em)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_sp_sp_inherit() {
        1.sp.coerceIn(1.sp, TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_sp_em_sp() {
        1.sp.coerceIn(1.em, 1.sp)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_sp_em_em() {
        1.sp.coerceIn(1.em, 1.em)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_sp_em_inherit() {
        1.sp.coerceIn(1.em, TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_em_sp_sp() {
        1.em.coerceIn(1.sp, 1.sp)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_em_sp_em() {
        1.em.coerceIn(1.sp, 1.em)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_em_sp_inherit() {
        1.em.coerceIn(1.sp, TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_em_em_sp() {
        1.em.coerceIn(1.em, 1.sp)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_em_em_inherit() {
        1.em.coerceIn(1.em, TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_inherit_sp_sp() {
        TextUnit.Inherit.coerceIn(1.sp, 1.sp)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_inherit_sp_em() {
        TextUnit.Inherit.coerceIn(1.sp, 1.em)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_inherit_sp_inherit() {
        TextUnit.Inherit.coerceIn(1.sp, TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_inherit_em_sp() {
        TextUnit.Inherit.coerceIn(1.em, 1.sp)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_inherit_em_em() {
        TextUnit.Inherit.coerceIn(1.em, 1.em)
    }

    @Test(expected = RuntimeException::class)
    fun coerceIn_inherit_em_inherit() {
        TextUnit.Inherit.coerceIn(TextUnit.Inherit, TextUnit.Inherit)
    }

    // coerceAtLeasts
    @Test
    fun coerceAtLeast_sp_sp() {
        1.sp.coerceAtLeast(2.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(2f)
        }
        3.sp.coerceAtLeast(2.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(3f)
        }
    }

    @Test
    fun coerceAtLeast_em_em() {
        1.em.coerceAtLeast(2.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(2f)
        }
        3.em.coerceAtLeast(2.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(3f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun coerceAtLeast_sp_em() {
        1.sp.coerceAtLeast(2.em)
    }

    @Test(expected = RuntimeException::class)
    fun coerceAtLeast_sp_inherit() {
        1.sp.coerceAtLeast(TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun coerceAtLeast_em_sp() {
        1.em.coerceAtLeast(1.sp)
    }

    @Test(expected = RuntimeException::class)
    fun coerceAtLeast_em_inherit() {
        1.em.coerceAtLeast(TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun coerceAtLeast_inherit_inherit() {
        TextUnit.Inherit.coerceAtLeast(TextUnit.Inherit)
    }

    // coerceAtMosts
    @Test
    fun coerceAtMost_sp_sp() {
        1.sp.coerceAtMost(2.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(1f)
        }
        3.sp.coerceAtMost(2.sp).also {
            assertThat(it.isSp).isTrue()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isFalse()
            assertThat(it.type).isEqualTo(TextUnitType.Sp)
            assertThat(it.value).isEqualTo(2f)
        }
    }

    @Test
    fun coerceAtMost_em_em() {
        1.em.coerceAtMost(2.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(1f)
        }
        3.em.coerceAtMost(2.em).also {
            assertThat(it.isSp).isFalse()
            assertThat(it.isInherit).isFalse()
            assertThat(it.isEm).isTrue()
            assertThat(it.type).isEqualTo(TextUnitType.Em)
            assertThat(it.value).isEqualTo(2f)
        }
    }

    @Test(expected = RuntimeException::class)
    fun coerceAtMost_sp_em() {
        1.sp.coerceAtMost(2.em)
    }

    @Test(expected = RuntimeException::class)
    fun coerceAtMost_sp_inherit() {
        1.sp.coerceAtMost(TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun coerceAtMost_em_sp() {
        1.em.coerceAtMost(1.sp)
    }

    @Test(expected = RuntimeException::class)
    fun coerceAtMost_em_inherit() {
        1.em.coerceAtMost(TextUnit.Inherit)
    }

    @Test(expected = RuntimeException::class)
    fun coerceAtMost_inherit_inherit() {
        TextUnit.Inherit.coerceAtMost(TextUnit.Inherit)
    }
}