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
import org.junit.Test

class DurationTest {
    /**
     * Check that construction via the literal extension time units syntax is equivalent to
     * the [Duration] factory function
     */
    @Test
    fun equivalentConstruction() {
        listOf(
            10.days to Duration(days = 10),
            24.hours to Duration(hours = 24),
            56.minutes to Duration(minutes = 56),
            45.seconds to Duration(seconds = 45),
            525.milliseconds to Duration(milliseconds = 525),
            9001.microseconds to Duration(microseconds = 9001)
        ).forEachIndexed { i, (fromLiteral, fromFactory) ->
            assertEquals("literal to factory check $i", fromLiteral, fromFactory)
        }
    }
}