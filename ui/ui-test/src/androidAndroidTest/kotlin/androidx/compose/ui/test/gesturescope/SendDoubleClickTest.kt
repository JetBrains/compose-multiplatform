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

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.ClickableTestBox.defaultSize
import androidx.compose.ui.test.util.ClickableTestBox.defaultTag
import androidx.compose.ui.test.util.InputDispatcherTestRule
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.recordedDurationMillis
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class SendDoubleClickTest(private val config: TestConfig) {
    data class TestConfig(val position: Offset?, val delayMillis: Long?)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (delay in listOf(null, 50L)) {
                    for (x in listOf(1.0f, 33.0f, 99.0f)) {
                        for (y in listOf(1.0f, 33.0f, 99.0f)) {
                            add(TestConfig(Offset(x, y), delay))
                        }
                    }
                    add(TestConfig(null, delay))
                }
            }
        }
    }

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val inputDispatcherRule: TestRule = InputDispatcherTestRule()

    private val recordedDoubleClicks = mutableListOf<Offset>()
    private val expectedClickPosition =
        config.position ?: Offset(defaultSize / 2, defaultSize / 2)

    // The delay plus 2 clicks
    private val expectedDurationMillis =
        (config.delayMillis ?: 145L) + (2 * eventPeriodMillis)

    private fun recordDoubleClick(position: Offset) {
        recordedDoubleClicks.add(position)
    }

    @Test
    fun testDoubleClick() {
        // Given some content
        val recorder = SinglePointerInputRecorder()
        rule.setContent {
            ClickableTestBox(
                Modifier
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = ::recordDoubleClick) }
                    .then(recorder)
            )
        }

        // When we inject a double click
        rule.onNodeWithTag(defaultTag).performGesture {
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
        assertThat(recorder.recordedDurationMillis).isEqualTo(expectedDurationMillis)
    }
}
