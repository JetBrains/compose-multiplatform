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

package androidx.compose.ui.unit

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntBoundsTest {
    @Test
    fun boundsWidth() {
        val bounds = IntBounds(10, 5, 25, 15)
        Assert.assertEquals(15, bounds.width)
    }

    @Test
    fun boundsHeight() {
        val bounds = IntBounds(10, 5, 25, 15)
        Assert.assertEquals(10, bounds.height)
    }

    @Test
    fun toBounds() {
        val size = IntSize(15, 10)
        val bounds = IntBounds(0, 0, 15, 10)
        Assert.assertEquals(bounds, size.toBounds())
    }

    @Test
    fun toSize() {
        val size = IntSize(15, 10)
        val bounds = IntBounds(10, 5, 25, 15)
        Assert.assertEquals(size, bounds.toSize())
    }
}