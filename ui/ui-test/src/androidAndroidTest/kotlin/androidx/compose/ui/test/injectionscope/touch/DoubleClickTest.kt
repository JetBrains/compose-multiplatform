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

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.testutils.WithViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.ClickableTestBox.defaultSize
import androidx.compose.ui.test.util.ClickableTestBox.defaultTag
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.verify
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test for [TouchInjectionScope.doubleClick]
 */
@MediumTest
@RunWith(Parameterized::class)
class DoubleClickTest(private val config: TestConfig) {
    data class TestConfig(val position: Offset?, val delayMillis: Long?)

    companion object {
        private const val DoubleTapMin = 40L
        private const val DoubleTapMax = 200L
        private const val DefaultDoubleTapTimeMillis = (DoubleTapMin + DoubleTapMax) / 2
        private val testViewConfiguration = TestViewConfiguration(
            doubleTapMinTimeMillis = DoubleTapMin,
            doubleTapTimeoutMillis = DoubleTapMax
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (delay in listOf(null, 50L)) {
                    add(TestConfig(Offset(10f, 10f), delay))
                    add(TestConfig(null, delay))
                }
            }
        }
    }

    @get:Rule
    val rule = createComposeRule()

    private val recordedDoubleClicks = mutableListOf<Offset>()

    private val expectedClickPosition =
        config.position ?: Offset(defaultSize / 2, defaultSize / 2)
    private val expectedDelay = config.delayMillis ?: DefaultDoubleTapTimeMillis

    private fun recordDoubleClick(position: Offset) {
        recordedDoubleClicks.add(position)
    }

    @Test
    fun doubleClick() {
        // Given some content
        val recorder = SinglePointerInputRecorder()
        rule.setContent {
            WithViewConfiguration(testViewConfiguration) {
                ClickableTestBox(
                    Modifier
                        .pointerInput(Unit) { detectTapGestures(onDoubleTap = ::recordDoubleClick) }
                        .then(recorder)
                )
            }
        }

        // When we inject a double click
        rule.onNodeWithTag(defaultTag).performTouchInput {
            if (config.position != null && config.delayMillis != null) {
                doubleClick(config.position, config.delayMillis)
            } else if (config.position != null) {
                doubleClick(config.position)
            } else if (config.delayMillis != null) {
                doubleClick(delayMillis = config.delayMillis)
            } else {
                doubleClick()
            }
        }

        rule.waitForIdle()

        // Then we record 1 double click at the expected position
        assertThat(recordedDoubleClicks).isEqualTo(listOf(expectedClickPosition))

        // And that the duration was as expected
        recorder.assertIsDoubleClick(expectedClickPosition)
    }

    private fun SinglePointerInputRecorder.assertIsDoubleClick(position: Offset) {
        assertThat(events).hasSize(4)
        val t0 = events[0].timestamp
        val id0 = events[0].id

        events[0].verify(t0 + 0, id0, true, position, Touch, Press)
        events[1].verify(t0 + eventPeriodMillis, id0, false, position, Touch, Release)

        val t1 = events[1].timestamp + expectedDelay
        val id1 = events[2].id

        events[2].verify(t1 + 0, id1, true, position, Touch, Press)
        events[3].verify(t1 + eventPeriodMillis, id1, false, position, Touch, Release)
    }
}
