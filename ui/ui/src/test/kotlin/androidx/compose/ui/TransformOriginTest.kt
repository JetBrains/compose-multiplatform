/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TransformOriginTest {

    @Test
    fun testTransformOriginCopy() {
        val transformOrigin = TransformOrigin(0.6f, 0.8f)
        assertEquals(transformOrigin, transformOrigin.copy())
    }

    @Test
    fun testPositionCopyOverwriteX() {
        val transformOrigin = TransformOrigin(0.7f, 0.9f)
        val copy = transformOrigin.copy(pivotFractionX = 0.3f)
        assertEquals(0.3f, copy.pivotFractionX)
        assertEquals(0.9f, copy.pivotFractionY)
    }

    @Test
    fun testPositionCopyOverwriteY() {
        val position = TransformOrigin(0.4f, 0.65f)
        val copy = position.copy(pivotFractionY = 0.1f)
        assertEquals(0.4f, copy.pivotFractionX)
        assertEquals(0.1f, copy.pivotFractionY)
    }
}