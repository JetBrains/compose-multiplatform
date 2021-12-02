/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class DensityTest {
    val density = Density(2f, 3f)

    @Test
    fun testValues() {
        assertEquals(2f, density.density, 0.001f)
        assertEquals(3f, density.fontScale, 0.001f)
    }

    @Test
    fun testDpToPx() = with(density) {
        assertEquals(2f, 1.dp.toPx(), 0.001f)
    }

    @Test
    fun testDpRoundToPx() = with(density) {
        assertEquals(10, 4.95.dp.roundToPx())
        assertEquals(10, 4.75.dp.roundToPx())
        assertEquals(9, 4.74.dp.roundToPx())
    }

    @Test
    fun testDpToSp() = with(density) {
        assertEquals(2.sp, 6.dp.toSp())
    }

    @Test
    fun testTextUnitToPx() = with(density) {
        assertEquals(6f, 1.sp.toPx(), 0.001f)
    }

    @Test(expected = IllegalStateException::class)
    fun testTextUnitToPxFail() {
        with(density) {
            1.em.toPx()
        }
    }

    @Test
    fun testTextRoundUnitToPx() = with(density) {
        assertEquals(6, 1.sp.roundToPx())
        assertEquals(6, 1.05.sp.roundToPx())
        assertEquals(6, .95.sp.roundToPx())
    }

    @Test(expected = IllegalStateException::class)
    fun testTextUnitRoundToPxFail() {
        with(density) {
            1.em.roundToPx()
        }
    }

    @Test
    fun testTextUnitToDp() = with(density) {
        assertEquals(3.dp, 1.sp.toDp())
    }

    @Test
    fun testIntToDp() = with(density) {
        assertEquals(1.dp, 2.toDp())
    }

    @Test
    fun testIntToSp() = with(density) {
        assertEquals(1.sp, 6.toSp())
    }

    @Test
    fun testFloatToDp() = with(density) {
        assertEquals(1.dp, 2f.toDp())
    }

    @Test
    fun testFloatToSp() = with(density) {
        assertEquals(1.sp, 6f.toSp())
    }

    @Test
    fun testDpRectToRect() = with(density) {
        val rect = DpRect(1.dp, 2.dp, 3.dp, 4.dp).toRect()
        assertEquals(2f, rect.left, 0.001f)
        assertEquals(4f, rect.top, 0.001f)
        assertEquals(6f, rect.right, 0.001f)
        assertEquals(8f, rect.bottom, 0.001f)
    }

    @Test
    fun testDpSizeToSize() = with(density) {
        assertEquals(Size(2f, 6f), DpSize(1.dp, 3.dp).toSize())
    }

    @Test
    fun testSizeToDpSize() = with(density) {
        assertEquals(DpSize(1.dp, 3.dp), Size(2f, 6f).toDpSize())
    }

    @Test
    fun testDpSizeUnspecifiedToSize() = with(density) {
        assertEquals(Size.Unspecified, DpSize.Unspecified.toSize())
    }

    @Test
    fun testSizeUnspecifiedToDpSize() = with(density) {
        assertEquals(DpSize.Unspecified, Size.Unspecified.toDpSize())
    }
}