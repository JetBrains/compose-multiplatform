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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.assertOnlyLastEventIsUp
import androidx.compose.ui.test.util.assertSinglePointer
import androidx.compose.ui.test.util.assertUpSameAsLastMove
import androidx.compose.ui.test.util.downEvents
import androidx.compose.ui.test.util.hasSameTimeBetweenEvents
import androidx.compose.ui.test.util.verify
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test for [TouchInjectionScope.swipe] between two [positions][Offset]
 */
@MediumTest
@RunWith(Parameterized::class)
class SwipeStartEndTest(private val config: TestConfig) {
    data class TestConfig(val duration: Long)

    companion object {
        private const val tag = "widget"
        private val start = Offset(5f, 7f)
        private val end = Offset(23f, 29f)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return (1L..100L step 11).map { TestConfig(it) }
        }
    }

    @get:Rule
    val rule = createComposeRule()

    private val recorder = SinglePointerInputRecorder()

    @Before
    fun setContent() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ClickableTestBox(modifier = recorder, tag = tag)
            }
        }
    }

    @Test
    fun swipe() {
        rule.onNodeWithTag(tag).performTouchInput {
            swipe(start, end, config.duration)
        }
        rule.runOnIdle {
            recorder.apply {
                assertThat(events.size).isAtLeast(3)
                assertOnlyLastEventIsUp()
                assertUpSameAsLastMove()
                assertSinglePointer()

                val t0 = events[0].timestamp
                val id = events[0].id

                // We start at `start`
                events.first().verify(t0, id, true, start, Touch, Press)
                // We end at `end`
                events.last().verify(t0 + config.duration, id, false, end, Touch, Release)
                // All events are evenly spaced in time
                downEvents.hasSameTimeBetweenEvents()
                // And the distance between each event is the same
                downEvents.zipWithNext { a, b ->
                    (b.position - a.position).getDistance() / (b.timestamp - a.timestamp)
                }.sorted().apply {
                    assertThat(last() - first()).isAtMost(1e-3f)
                }
            }
        }
    }
}
