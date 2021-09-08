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

package androidx.compose.ui.test.util

import androidx.compose.testutils.expectAssertionError
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests for `List<Float>.`[isMonotonicBetween]. See documentation of that method for expected
 * results.
 */
@RunWith(Parameterized::class)
class IsMonotonicBetweenTest(private val config: TestConfig) {
    data class TestConfig(
        val values: List<Float>,
        val a: Float,
        val b: Float,
        val tolerance: Float,
        val expectSuccess: Boolean
    ) {
        val expectError = !expectSuccess
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                // Is [1] between 1 and 1 with no tolerance? Yes
                TestConfig(listOf(1f), 1f, 1f, 0f, true),
                // Is [1.1] between 1 and 1 with no tolerance? No
                TestConfig(listOf(1.1f), 1f, 1f, 0f, false),
                // Is [1.01] between 1 and 1 with 0.01 tolerance? Yes
                TestConfig(listOf(1.01f), 1f, 1f, 0.01f, true),
                // Is [10.05] between 10 and 10 with 0.01 tolerance? No
                TestConfig(listOf(10.05f), 10f, 10f, 0.01f, false),
                // Is [1.02] between 1 and 1 with 0.01 tolerance? No
                TestConfig(listOf(1.02f), 1f, 1f, 0.01f, false),
                // Is [1.01] between 1 and 1 with 0.00999 tolerance? No
                TestConfig(listOf(1.1f), 1f, 1f, 0.00999f, false),
                // Is [1, 1, 1] between 1 and 1 with no tolerance? Yes
                TestConfig(listOf(1f, 1f, 1f), 1f, 1f, 0f, true),
                // Is [1, 1, 1.01] between 1 and 1 with no tolerance? No
                TestConfig(listOf(1f, 1f, 1.01f), 1f, 1f, 0f, false),
                // Is [2, 2, 2] between 1 and 1 with 0.01 tolerance? No
                TestConfig(listOf(2f, 2f, 2f), 1f, 1f, 0.01f, false),
                // Is [1, 2, 3] between 2 and 2 with 0.01 tolerance? No
                TestConfig(listOf(1f, 2f, 3f), 1f, 1f, 0.01f, false),
                // Is [1, 2, 3] between 1 and 3 with no tolerance? Yes
                TestConfig(listOf(1f, 2f, 3f), 1f, 3f, 0f, true),
                // Is [1, 2, 2, 3] between 1 and 3 with no tolerance? Yes
                TestConfig(listOf(1f, 2f, 2f, 3f), 1f, 3f, 0f, true),
                // Is [1, 2, 3] between 3 and 1 with no tolerance? No
                TestConfig(listOf(1f, 2f, 3f), 3f, 1f, 0f, false),
                // Is [1, 2, 3] between 0 and 2 with no tolerance? No
                TestConfig(listOf(1f, 2f, 3f), 0f, 2f, 0f, false),
                // Is [1, 2, 3] between 1.01 and 4 with 0.01 tolerance? Yes
                TestConfig(listOf(1f, 2f, 3f), 1.01f, 4f, 0.01f, true),
                // Is [3, 2, 1] between 3 and 1 with no tolerance? Yes
                TestConfig(listOf(3f, 2f, 1f), 3f, 1f, 0f, true),
                // Is [3, 3, 2, 1] between 3 and 1 with no tolerance? No
                TestConfig(listOf(3f, 3f, 2f, 1f), 3f, 1f, 0f, true)
            )
        }
    }

    @Test
    fun testIsMonotonicBetween() {
        config.apply {
            expectAssertionError(expectError) {
                values.isMonotonicBetween(a, b, tolerance)
            }
        }
    }
}
