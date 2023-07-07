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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.util.lerp
import kotlin.test.Test
import kotlin.test.assertEquals

class ShadowTest {
    @Test
    fun defaultValue() {
        val shadow = Shadow()
        assertEquals(Color(0xFF000000), shadow.color)
        assertEquals(0.0f, shadow.blurRadius)
        assertEquals(Offset.Zero, shadow.offset)
    }

    @Test
    fun testConstructor() {
        val color = Color(0xFF00FF00)
        val offset = Offset(2f, 3f)
        val blurRadius = 1.0f

        val shadow = Shadow(color, offset, blurRadius)
        assertEquals(color, shadow.color)
        assertEquals(offset, shadow.offset)
        assertEquals(blurRadius, shadow.blurRadius)
    }

    @Test
    fun testLerp() {
        val colorA = Color(0xFF00FF00)
        val colorB = Color(0xFF0000FF)
        val offsetA = Offset(5f, 10f)
        val offsetB = Offset(0f, 5f)
        val radiusA = 0.0f
        val radiusB = 3.0f
        val shadowA = Shadow(colorA, offsetA, radiusA)
        val shadowB = Shadow(colorB, offsetB, radiusB)
        val t = 0.4f

        val shadow = lerp(shadowA, shadowB, t)
        assertEquals(lerp(colorA, colorB, t), shadow.color)
        assertEquals(lerp(offsetA, offsetB, t), shadow.offset)
        assertEquals(lerp(radiusA, radiusB, t), shadow.blurRadius)
    }
}
