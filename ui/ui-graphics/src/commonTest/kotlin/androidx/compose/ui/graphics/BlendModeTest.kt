/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.compose.ui.graphics

import kotlin.test.assertEquals
import kotlin.test.Test

class BlendModeTest {

    @Test
    fun testToString() {
        assertEquals("Clear", BlendMode.Clear.toString())
        assertEquals("Src", BlendMode.Src.toString())
        assertEquals("Dst", BlendMode.Dst.toString())
        assertEquals("SrcOver", BlendMode.SrcOver.toString())
        assertEquals("DstOver", BlendMode.DstOver.toString())
        assertEquals("SrcIn", BlendMode.SrcIn.toString())
        assertEquals("DstIn", BlendMode.DstIn.toString())
        assertEquals("SrcOut", BlendMode.SrcOut.toString())
        assertEquals("DstOut", BlendMode.DstOut.toString())
        assertEquals("SrcAtop", BlendMode.SrcAtop.toString())
        assertEquals("DstAtop", BlendMode.DstAtop.toString())
        assertEquals("Xor", BlendMode.Xor.toString())
        assertEquals("Plus", BlendMode.Plus.toString())
        assertEquals("Modulate", BlendMode.Modulate.toString())
        assertEquals("Screen", BlendMode.Screen.toString())
        assertEquals("Overlay", BlendMode.Overlay.toString())
        assertEquals("Darken", BlendMode.Darken.toString())
        assertEquals("Lighten", BlendMode.Lighten.toString())
        assertEquals("ColorDodge", BlendMode.ColorDodge.toString())
        assertEquals("ColorBurn", BlendMode.ColorBurn.toString())
        assertEquals("HardLight", BlendMode.Hardlight.toString())
        assertEquals("Softlight", BlendMode.Softlight.toString())
        assertEquals("Difference", BlendMode.Difference.toString())
        assertEquals("Exclusion", BlendMode.Exclusion.toString())
        assertEquals("Multiply", BlendMode.Multiply.toString())
        assertEquals("Hue", BlendMode.Hue.toString())
        assertEquals("Saturation", BlendMode.Saturation.toString())
        assertEquals("Color", BlendMode.Color.toString())
        assertEquals("Luminosity", BlendMode.Luminosity.toString())
    }
}
