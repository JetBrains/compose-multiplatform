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

package androidx.compose.animation

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.random.Random

@RunWith(JUnit4::class)
class ConverterTest {
    @Test
    fun testColorConverter() {
        val converter = (Color.VectorConverter)(ColorSpaces.Srgb)
        val vectorFromRed = converter.convertToVector(Color.Red)
        assertEquals(Color.Red, converter.convertFromVector(vectorFromRed))
        val vectorFromGreen = converter.convertToVector(Color.Green)
        assertEquals(Color.Green, converter.convertFromVector(vectorFromGreen))
        val vectorFromBlue = converter.convertToVector(Color.Blue)
        assertEquals(Color.Blue, converter.convertFromVector(vectorFromBlue))
    }

    @Test
    fun testColorConverterClampValuesOutOfRange() {
        val converter = (Color.VectorConverter)(ColorSpaces.Srgb)

        // Alpha channel above 1.0f clamps to 1.0f and result is red
        assertEquals(
            1f,
            converter.convertFromVector(AnimationVector4D(1.1f, 1f, 0f, 0f)).alpha,
            0f
        )
        // Alpha channel below 0.0f clamps to 0.0f and the result is transparent red
        assertEquals(
            0f,
            converter.convertFromVector(AnimationVector4D(-0.1f, 1f, 0f, 0f))
                .alpha,
            0f
        )

        // all channels should clamp:
        assertEquals(
            1f,
            converter.convertFromVector(AnimationVector4D(1.0f, 3f, 3f, 3f)).red,
            0f
        )
        assertEquals(
            1f,
            converter.convertFromVector(AnimationVector4D(1.0f, 3f, 3f, 3f)).green,
            0f
        )
        assertEquals(
            1f,
            converter.convertFromVector(AnimationVector4D(1.0f, 3f, 3f, 3f)).blue,
            0f
        )

        // All channel below 0.0f clamps to 0.0f and the result is black
        assertEquals(
            0f,
            converter.convertFromVector(AnimationVector4D(1.0f, -3f, -3f, -3f))
                .red,
            0f
        )
        assertEquals(
            0f,
            converter.convertFromVector(AnimationVector4D(1.0f, -3f, -3f, -3f))
                .green,
            0f
        )
        assertEquals(
            0f,
            converter.convertFromVector(AnimationVector4D(1.0f, -3f, -3f, -3f))
                .blue,
            0f
        )

        // Green channel above 1.0f clamps to 1.0f and the result is green
        assertEquals(
            converter.convertFromVector(AnimationVector4D(1.0f, 0.0f, 1.1f, 0f)),
            Color.Green
        )
    }

    @Test
    fun testRectConverter() {
        assertEquals(
            Rect.VectorConverter.convertToVector(Rect(1f, 2f, 3f, 4f)),
            AnimationVector4D(1f, 2f, 3f, 4f)
        )
        assertEquals(
            Rect.VectorConverter.convertFromVector(
                AnimationVector4D(-400f, -300f, -200f, -100f)
            ),
            Rect(-400f, -300f, -200f, -100f)
        )
    }

    @Test
    fun testDpConverter() {
        val value = Random.nextFloat()
        assertEquals(Dp.VectorConverter.convertFromVector(AnimationVector1D(value)), value.dp)

        val value2 = Random.nextFloat()
        assertEquals(Dp.VectorConverter.convertToVector(value2.dp), AnimationVector1D(value2))
    }

    @Test
    fun testOffsetConverter() {
        val x = Random.nextFloat()
        val y = Random.nextFloat()
        assertEquals(
            Offset(x, y),
            Offset.VectorConverter.convertFromVector(AnimationVector2D(x, y))
        )
        assertEquals(
            AnimationVector2D(x, y),
            Offset.VectorConverter.convertToVector(Offset(x, y))
        )
    }
}