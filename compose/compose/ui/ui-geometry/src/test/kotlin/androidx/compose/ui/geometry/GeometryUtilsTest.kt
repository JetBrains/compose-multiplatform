/*
 * Copyright 2021 The Android Open Source Project
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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GeometryUtilsTest {

    @Test
    fun testRoundDownToNearestTenth() {
        assertEquals("1.2", 1.234f.toStringAsFixed(1))
    }

    @Test
    fun testRoundUpToNearestTenth() {
        assertEquals("1.3", 1.25f.toStringAsFixed(1))
    }

    @Test
    fun testRoundDownToNearestHundreth() {
        assertEquals("1.23", 1.234f.toStringAsFixed(2))
    }

    @Test
    fun testRoundUpToNearestHundreth() {
        assertEquals("1.24", 1.235f.toStringAsFixed(2))
    }

    @Test
    fun testRoundUpToNearestInt() {
        assertEquals("1", 0.5f.toStringAsFixed(0))
    }

    @Test
    fun testRoundDownToNearestInt() {
        assertEquals("0", 0.49f.toStringAsFixed(0))
    }
}