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

package androidx.compose.animation.core

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeConverterTest {
    @Test
    fun testFloatToVectorConverter() {
        verifyFloatConverter(Float.VectorConverter)
        verifyFloatConverter(FloatPropKey().typeConverter)
        verifyFloatConverter(AnimatedFloat(5f, ManualAnimationClock(0L)).typeConverter)
    }

    @Test
    fun testIntToVectorConverter() {
        assertEquals(100f, Int.VectorConverter.convertToVector(100).value)
        assertEquals(5, Int.VectorConverter.convertFromVector(AnimationVector1D(5f)))

        assertEquals(30f, IntPropKey().typeConverter.convertToVector(30).value)
        assertEquals(22, IntPropKey().typeConverter.convertFromVector(AnimationVector1D(22f)))
    }

    @Test
    fun testAnimatedVectorConverter() {
        verifyV2VConverter(AnimationVector1D(100f))
        verifyV2VConverter(AnimationVector2D(40f, 50f))
        verifyV2VConverter(AnimationVector3D(300f, -20f, 1f))
        verifyV2VConverter(AnimationVector4D(100f, -20f, 3000f, 4f))
    }

    private fun <V : AnimationVector> verifyV2VConverter(value: V) {
        val converter = AnimatedVector(value, ManualAnimationClock(0L)).typeConverter
        assertEquals(converter.convertFromVector(value), converter.convertToVector(value))
    }

    private fun verifyFloatConverter(converter: TwoWayConverter<Float, AnimationVector1D>) {
        assertEquals(15f, converter.convertToVector(15f).value)
        assertEquals(5f, converter.convertFromVector(AnimationVector1D(5f)))
    }
}