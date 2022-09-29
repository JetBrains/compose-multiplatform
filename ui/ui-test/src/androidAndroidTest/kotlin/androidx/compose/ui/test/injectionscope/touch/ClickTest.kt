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

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.verify
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test for [TouchInjectionScope.click]
 */
@MediumTest
@RunWith(Parameterized::class)
class ClickTest(private val config: TestConfig) {
    data class TestConfig(val position: Offset?)

    companion object {
        private const val squareSize = 10.0f
        private val colors = listOf(Color.Red, Color.Yellow, Color.Blue, Color.Green, Color.Cyan)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (x in listOf(0.0f, squareSize - 1.0f)) {
                    for (y in listOf(0.0f, squareSize - 1.0f)) {
                        add(TestConfig(Offset(x, y)))
                    }
                }
                add(TestConfig(null))
            }
        }
    }

    @get:Rule
    val rule = createComposeRule()

    private val expectedClickPosition =
        config.position ?: Offset(squareSize / 2, squareSize / 2)

    @Test
    fun click() {
        val firstRecorder = SinglePointerInputRecorder()
        val lastRecorder = SinglePointerInputRecorder()

        // Given a column of 5 small components
        rule.setContent {
            Column {
                ClickableTestBox(firstRecorder, squareSize, squareSize, colors[0], "first")
                ClickableTestBox(Modifier, squareSize, squareSize, colors[1])
                ClickableTestBox(Modifier, squareSize, squareSize, colors[2])
                ClickableTestBox(Modifier, squareSize, squareSize, colors[3])
                ClickableTestBox(lastRecorder, squareSize, squareSize, colors[4], "last")
            }
        }

        // When I click the first and last of these components
        rule.click("first")
        rule.click("last")

        // Then those components have registered a click
        rule.runOnIdle {
            firstRecorder.assertIsClick(expectedClickPosition)
            lastRecorder.assertIsClick(expectedClickPosition)
        }
    }

    private fun SinglePointerInputRecorder.assertIsClick(position: Offset) {
        assertThat(events).hasSize(2)
        val t0 = events[0].timestamp
        val id = events[0].id

        events[0].verify(t0 + 0, id, true, position, Touch, Press)
        events[1].verify(t0 + eventPeriodMillis, id, false, position, Touch, Release)
    }

    private fun ComposeTestRule.click(tag: String) {
        onNodeWithTag(tag).performTouchInput {
            if (config.position != null) {
                click(config.position)
            } else {
                click()
            }
        }
    }
}
