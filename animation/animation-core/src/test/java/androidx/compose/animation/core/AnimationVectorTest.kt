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

import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnimationVectorTest {
    @Test
    fun testReset() {
        assertEquals(AnimationVector1D(0f), AnimationVector(1f).apply { reset() })
        assertEquals(AnimationVector2D(0f, 0f), AnimationVector(1f, 2f).apply { reset() })
        assertEquals(
            AnimationVector3D(0f, 0f, 0f),
            AnimationVector(1f, 2f, 3f).apply { reset() }
        )
        assertEquals(
            AnimationVector4D(0f, 0f, 0f, 0f),
            AnimationVector(1f, 2f, 3f, 4f).apply { reset() }
        )
    }

    @Test
    fun testAnimationVectorFactoryMethod() {
        assertEquals(AnimationVector1D(200f), AnimationVector(200f))
        assertEquals(AnimationVector2D(7f, 500f), AnimationVector(7f, 500f))
        assertNotEquals(AnimationVector2D(7f, 501f), AnimationVector(7f, 500f))
        assertEquals(
            AnimationVector3D(35f, 26f, 50f),
            AnimationVector(35f, 26f, 50f)
        )
        assertEquals(
            AnimationVector4D(1f, 2f, 3f, 4f),
            AnimationVector(1f, 2f, 3f, 4f)
        )
    }
}