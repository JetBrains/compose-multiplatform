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

package androidx.compose.ui.test.gesturescope

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.assertOnlyLastEventIsUp
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
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
 * Tests if we can generate gestures that end with a specific velocity
 */
@MediumTest
@RunWith(Parameterized::class)
class SendSwipeVelocityTest(private val config: TestConfig) {
    data class TestConfig(
        val direction: Direction,
        val durationMillis: Long,
        val velocity: Float
    )

    enum class Direction(val from: Offset, val to: Offset) {
        LeftToRight(Offset(boxStart, boxMiddle), Offset(boxEnd, boxMiddle)),
        RightToLeft(Offset(boxEnd, boxMiddle), Offset(boxStart, boxMiddle)),
        TopToBottom(Offset(boxMiddle, boxStart), Offset(boxMiddle, boxEnd)),
        BottomToTop(Offset(boxMiddle, boxEnd), Offset(boxMiddle, boxStart))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (direction in Direction.values()) {
                    for (duration in listOf(100, 500, 1000)) {
                        for (velocity in listOf(79f, 200f, 1500f, 4691f)) {
                            add(TestConfig(direction, duration.toLong(), velocity))
                        }
                    }
                }
            }
        }

        private const val tag = "widget"

        private val boxSize = 500.0f
        private val boxStart = 1.0f
        private val boxMiddle = boxSize / 2
        private val boxEnd = boxSize - 1.0f
    }

    private val start get() = config.direction.from
    private val end get() = config.direction.to
    private val duration get() = config.durationMillis
    private val velocity get() = config.velocity

    private val expectedXVelocity = when (config.direction) {
        Direction.LeftToRight -> velocity
        Direction.RightToLeft -> -velocity
        else -> 0f
    }

    private val expectedYVelocity = when (config.direction) {
        Direction.TopToBottom -> velocity
        Direction.BottomToTop -> -velocity
        else -> 0f
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

        @Suppress("DEPRECATION")
        rule.onNodeWithTag(tag).performGesture {
            swipeWithVelocity(start, end, velocity, duration)
        }

        rule.runOnIdle {
            recorder.run {
                // The last 100ms is guaranteed to have velocity
                val minimumEventSize = max(2, (100 / eventPeriodMillis).toInt())
                assertThat(events.size).isAtLeast(minimumEventSize)
                assertOnlyLastEventIsUp()

                // Check coordinates
                events.first().position.isAlmostEqualTo(start)
                downEvents.isMonotonicBetween(start, end)
                events.last().position.isAlmostEqualTo(end)

                // Check timestamps
                assertTimestampsAreIncreasing()
                assertThat(recordedDurationMillis).isEqualTo(duration)

                // Check velocity
                assertThat(recordedVelocity.x).isWithin(.1f).of(expectedXVelocity)
                assertThat(recordedVelocity.y).isWithin(.1f).of(expectedYVelocity)
            }
        }
    }
}
