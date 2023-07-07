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

package androidx.compose.ui.graphics.colorspace

import androidx.compose.ui.util.packInts
import kotlin.test.assertEquals
import kotlin.test.Test

class ColorModelTest {

    @Test
    fun testRgb() {
        assertEquals(packInts(3, 0), ColorModel.Rgb.packedValue)
        assertEquals("Rgb", ColorModel.Rgb.toString())
        assertEquals(3, ColorModel.Rgb.componentCount)
    }

    @Test
    fun testXyz() {
        assertEquals(packInts(3, 1), ColorModel.Xyz.packedValue)
        assertEquals("Xyz", ColorModel.Xyz.toString())
        assertEquals(3, ColorModel.Xyz.componentCount)
    }

    @Test
    fun testLab() {
        assertEquals(packInts(3, 2), ColorModel.Lab.packedValue)
        assertEquals("Lab", ColorModel.Lab.toString())
        assertEquals(3, ColorModel.Lab.componentCount)
    }

    @Test
    fun testCmyk() {
        assertEquals(packInts(4, 3), ColorModel.Cmyk.packedValue)
        assertEquals("Cmyk", ColorModel.Cmyk.toString())
        assertEquals(4, ColorModel.Cmyk.componentCount)
    }
}