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
        val converter = Float.VectorConverter
        assertEquals(15f, converter.convertToVector(15f).value)
        assertEquals(5f, converter.convertFromVector(AnimationVector1D(5f)))
    }

    @Test
    fun testIntToVectorConverter() {
        assertEquals(100f, Int.VectorConverter.convertToVector(100).value)
        assertEquals(5, Int.VectorConverter.convertFromVector(AnimationVector1D(5f)))
    }
}