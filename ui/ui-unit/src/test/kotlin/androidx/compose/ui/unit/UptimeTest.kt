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

package androidx.compose.ui.unit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UptimeTest {

    @Test
    fun compareTo_greaterThan_isCorrect() {
        assertTrue(Uptime(-1) > Uptime(-2))
        assertTrue(Uptime(0) > Uptime(-1))
        assertTrue(Uptime(1) > Uptime(0))
        assertTrue(Uptime(2) > Uptime(1))
    }

    @Test
    fun compareTo_lessThan_isCorrect() {
        assertTrue(Uptime(-2) < Uptime(-1))
        assertTrue(Uptime(-1) < Uptime(0))
        assertTrue(Uptime(0) < Uptime(1))
        assertTrue(Uptime(1) < Uptime(2))
    }

    @Test
    fun compareTo_equals_isCorrect() {
        assertEquals(0, Uptime(-1).compareTo(Uptime(-1)))
        assertEquals(0, Uptime(0).compareTo(Uptime(0)))
        assertEquals(0, Uptime(1).compareTo(Uptime(1)))
    }

    @Test
    fun plus_duration_isCorrect() {
        assertEquals(Uptime(5), Uptime(2) + 3.nanoseconds)
    }

    @Test
    fun plus_durationInverted_isCorrect() {
        assertEquals(Uptime(5), 3.nanoseconds + Uptime(2))
    }

    @Test
    fun minus_duration_isCorrect() {
        assertEquals(Uptime(2), Uptime(5) - 3.nanoseconds)
    }

    @Test
    fun minus_uptime_isCorrect() {
        assertEquals(Duration(3), Uptime(5) - Uptime(2))
    }
}