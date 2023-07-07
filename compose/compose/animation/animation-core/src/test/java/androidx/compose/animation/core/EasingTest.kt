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

import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EasingTest {

    @Test
    fun cubicBezierStartsAt0() {
        val easing = FastOutSlowInEasing
        assertThat(easing.transform(0f)).isZero()
    }

    @Test
    fun cubicBezierEndsAt1() {
        val easing = FastOutLinearInEasing
        assertThat(easing.transform(0f)).isZero()
    }

    @Test
    fun testCubicBezierEquals() {
        val curve1 = CubicBezierEasing(1f, 2f, 3f, 4f)
        val curve1Dup = CubicBezierEasing(1f, 2f, 3f, 4f)
        val curve2 = CubicBezierEasing(0f, 2f, 3f, 4f)
        val curve3 = CubicBezierEasing(1f, 0f, 3f, 4f)
        val curve4 = CubicBezierEasing(1f, 2f, 0f, 4f)
        val curve5 = CubicBezierEasing(1f, 2f, 3f, 0f)
        val curve6 = CubicBezierEasing(4f, 3f, 2f, 1f)

        assertEquals(curve1, curve1Dup)
        assertNotEquals(curve1, curve2)
        assertNotEquals(curve1, curve3)
        assertNotEquals(curve1, curve4)
        assertNotEquals(curve1, curve5)
        assertNotEquals(curve1, curve6)

        assertEquals(curve1.hashCode(), curve1Dup.hashCode())
        assertNotEquals(curve1.hashCode(), curve2.hashCode())
        assertNotEquals(curve1.hashCode(), curve3.hashCode())
        assertNotEquals(curve1.hashCode(), curve4.hashCode())
        assertNotEquals(curve1.hashCode(), curve5.hashCode())
        assertNotEquals(curve1.hashCode(), curve6.hashCode())
    }
}
