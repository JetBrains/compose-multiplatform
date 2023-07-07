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

import android.view.MotionEvent
import androidx.compose.testutils.expectAssertionError
import androidx.compose.ui.test.RobolectricMinSdk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for `List<MotionEvent>.`[splitsDurationEquallyInto]. See documentation of that method
 * for expected results.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(minSdk = RobolectricMinSdk)
class SplitsDurationEquallyIntoTest(private val config: TestConfig) {
    data class TestConfig(
        val timestamps: List<Long>,
        val duration: Long,
        val expectSuccess: Boolean
    ) {
        val expectError = !expectSuccess
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                // 1/1: ideally  [1.0]
                TestConfig(listOf(1L), 1L, true),
                TestConfig(listOf(), 1L, false),
                // 2/1: ideally  [2.0]
                TestConfig(listOf(2L), 2L, true),
                TestConfig(listOf(1L), 2L, false),
                TestConfig(listOf(1L, 2L), 2L, false),
                // 10/1: ideally [10.0]
                TestConfig(listOf(10L), 10L, true),
                TestConfig(listOf(9L), 10L, false),
                TestConfig(listOf(11L), 10L, false),
                TestConfig(listOf(5L, 10L), 10L, false),
                // 14/1: ideally [14.0]
                TestConfig(listOf(14L), 14L, true),
                TestConfig(listOf(7L, 14L), 14L, false),
                // 15/2: ideally [7.5, 15]
                TestConfig(listOf(8L, 15L), 15L, true),
                TestConfig(listOf(7L, 15L), 15L, true),
                TestConfig(listOf(7L, 14L), 15L, false),
                TestConfig(listOf(8L, 16L), 15L, false),
                TestConfig(listOf(15L), 15L, false),
                TestConfig(listOf(5L, 5L, 5L), 15L, false),
                // 28/3: ideally [9.33, 18.66, 28]
                TestConfig(listOf(9L, 19L, 28L), 28L, true),
                TestConfig(listOf(9L, 18L, 28L), 28L, false),
                TestConfig(listOf(10L, 19L, 28L), 28L, false),
                // 43/4: ideally [10.75, 21.5, 32.25, 43]
                TestConfig(listOf(11L, 21L, 32L, 43L), 43L, true),
                TestConfig(listOf(11L, 22L, 32L, 43L), 43L, true),
                TestConfig(listOf(10L, 21L, 32L, 43L), 43L, false),
                TestConfig(listOf(11L, 22L, 33L, 43L), 43L, false),
                TestConfig(listOf(11L, 22L, 33L), 43L, false),
                TestConfig(listOf(11L, 22L, 33L, 43L, 43L), 43L, false)
            )
        }
    }

    private lateinit var motionEvents: List<MotionEvent>

    @Before
    fun setUp() {
        motionEvents = config.timestamps.map { time ->
            MotionEvent.obtain(0L, time, 0, 0f, 0f, 0)
        }
    }

    @After
    fun tearDown() {
        motionEvents.map { it.recycle() }
    }

    @Test
    fun testSplitsDurationEquallyInto() {
        expectAssertionError(config.expectError) {
            motionEvents.splitsDurationEquallyInto(0L, config.duration, 10L)
        }
    }
}
