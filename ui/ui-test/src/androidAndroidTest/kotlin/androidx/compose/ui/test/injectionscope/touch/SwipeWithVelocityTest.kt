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

package androidx.compose.ui.test.injectionscope.touch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.assertOnlyLastEventIsUp
import androidx.compose.ui.test.util.assertSinglePointer
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.compose.ui.test.util.assertUpSameAsLastMove
import androidx.compose.ui.test.util.downEvents
import androidx.compose.ui.test.util.isAlmostEqualTo
import androidx.compose.ui.test.util.isMonotonicBetween
import androidx.compose.ui.test.util.recordedDurationMillis
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.max

/**
 * Test for [TouchInjectionScope.swipeWithVelocity] to see if we can generate gestures that end
 * with a specific velocity. Note that the "engine" is already extensively tested in
 * [VelocityPathFinderTest], so all we need to do here is verify a few swipes.
 */
@MediumTest
@RunWith(Parameterized::class)
class SwipeWithVelocityTest(private val config: TestConfig) {
    data class TestConfig(
        val durationMillis: Long,
        val velocity: Float
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (duration in listOf(100, 500, 1000)) {
                    for (velocity in listOf(100f, 999f, 5000f)) {
                        add(TestConfig(duration.toLong(), velocity))
                    }
                }
            }
        }

        private const val tag = "widget"

        private const val boxSize = 500.0f
        private const val boxStart = 1.0f
        private const val boxMiddle = boxSize / 2
        private const val boxEnd = boxSize - 1.0f

        private val start = Offset(boxStart, boxMiddle)
        private val end = Offset(boxEnd, boxMiddle)
    }

    @get:Rule
    val rule = createComposeRule()

    private val recorder = SinglePointerInputRecorder()

    @Test
    fun swipeWithVelocity() {
        rule.setContent {
            Box(Modifier.fillMaxSize().wrapContentSize(Alignment.BottomEnd)) {
                ClickableTestBox(recorder, boxSize, boxSize, tag = tag)
            }
        }

        rule.onNodeWithTag(tag).performTouchInput {
            swipeWithVelocity(start, end, config.velocity, config.durationMillis)
        }

        rule.runOnIdle {
            recorder.run {
                // At least the last 100ms should have velocity
                val minimumEventSize = max(2, (100 / eventPeriodMillis).toInt())
                assertThat(events.size).isAtLeast(minimumEventSize)
                assertOnlyLastEventIsUp()
                assertUpSameAsLastMove()
                assertSinglePointer()

                // Check coordinates
                events.first().position.isAlmostEqualTo(start)
                downEvents.isMonotonicBetween(start, end)
                events.last().position.isAlmostEqualTo(end)

                // Check timestamps
                assertTimestampsAreIncreasing()
                assertThat(recordedDurationMillis).isEqualTo(config.durationMillis)

                // Check velocity
                // Swipe goes from left to right, so vx = velocity (within 5%) and vy = 0
                assertThat(recordedVelocity.x).isWithin(0.05f * config.velocity).of(config.velocity)
                assertThat(recordedVelocity.y).isWithin(.1f).of(0f)
            }
        }
    }
}
