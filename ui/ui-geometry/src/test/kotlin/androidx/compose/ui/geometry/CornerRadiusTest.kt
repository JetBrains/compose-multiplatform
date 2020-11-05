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

package androidx.compose.ui.geometry

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CornerRadiusTest {

    @Test
    fun testRadiusCopy() {
        val radius = CornerRadius(100f, 200f)
        Assert.assertEquals(radius, radius.copy())
    }

    @Test
    fun testRadiusCopyOverwriteX() {
        val radius = CornerRadius(100f, 200f)
        val copy = radius.copy(x = 50f)
        Assert.assertEquals(50f, copy.x)
        Assert.assertEquals(200f, copy.y)
    }

    @Test
    fun testRadiusCopyOverwriteY() {
        val radius = CornerRadius(100f, 200f)
        val copy = radius.copy(y = 300f)
        Assert.assertEquals(100f, copy.x)
        Assert.assertEquals(300f, copy.y)
    }

    @Test
    fun testDestructuringAssignment() {
        val (x, y) = CornerRadius(17f, 42f)
        assertEquals(17f, x)
        assertEquals(42f, y)
    }
}