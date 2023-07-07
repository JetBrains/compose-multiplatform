/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.center
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performMultiModalInput
import androidx.compose.ui.test.topLeft
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.ClickableTestBox.defaultTag
import androidx.compose.ui.test.width
import androidx.compose.ui.unit.Density
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@MediumTest
class PositionsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testCornersEdgesAndCenter() {
        rule.setContent { ClickableTestBox(width = 3f, height = 100f) }

        rule.onNodeWithTag(defaultTag).performMultiModalInput {
            assertThat(width).isEqualTo(3)
            assertThat(height).isEqualTo(100)

            assertThat(left).isEqualTo(0f)
            assertThat(centerX).isEqualTo(1.5f)
            assertThat(right).isEqualTo(2f)

            assertThat(top).isEqualTo(0f)
            assertThat(centerY).isEqualTo(50f)
            assertThat(bottom).isEqualTo(99f)

            assertThat(topLeft).isEqualTo(Offset(0f, 0f))
            assertThat(topCenter).isEqualTo(Offset(1.5f, 0f))
            assertThat(topRight).isEqualTo(Offset(2f, 0f))
            assertThat(centerLeft).isEqualTo(Offset(0f, 50f))
            assertThat(center).isEqualTo(Offset(1.5f, 50f))
            assertThat(centerRight).isEqualTo(Offset(2f, 50f))
            assertThat(bottomLeft).isEqualTo(Offset(0f, 99f))
            assertThat(bottomCenter).isEqualTo(Offset(1.5f, 99f))
            assertThat(bottomRight).isEqualTo(Offset(2f, 99f))
        }
    }

    @Test
    fun testRelativeOffset() {
        rule.setContent { ClickableTestBox() }

        rule.onNodeWithTag(defaultTag).performMultiModalInput {
            assertThat(percentOffset(.1f, .1f)).isEqualTo(Offset(10f, 10f))
            assertThat(percentOffset(-.2f, 0f)).isEqualTo(Offset(-20f, 0f))
            assertThat(percentOffset(.25f, -.5f)).isEqualTo(Offset(25f, -50f))
            assertThat(percentOffset(0f, .5f)).isEqualTo(Offset(0f, 50f))
            assertThat(percentOffset(2f, -2f)).isEqualTo(Offset(200f, -200f))
        }
    }

    @Test
    fun testDensity() {
        val expectedDensity = Density(0.8238974f, 0.923457f) // random value
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides expectedDensity
            ) {
                ClickableTestBox(width = 3f, height = 100f)
            }
        }
        rule.onNodeWithTag(defaultTag).performMultiModalInput {
            assertThat(this.density).isEqualTo(expectedDensity.density)
            assertThat(this.fontScale).isEqualTo(expectedDensity.fontScale)
        }
    }

    @Test
    fun testSizeInViewport_column_startAtStart() {
        testPositionsInViewport(isVertical = true, reverseScrollDirection = false)
    }

    @Test
    fun testSizeInViewport_column_startAtEnd() {
        testPositionsInViewport(isVertical = true, reverseScrollDirection = true)
    }

    @Test
    fun testSizeInViewport_row_startAtStart() {
        testPositionsInViewport(isVertical = false, reverseScrollDirection = false)
    }

    @Test
    fun testSizeInViewport_row_startAtEnd() {
        testPositionsInViewport(isVertical = false, reverseScrollDirection = true)
    }

    private fun testPositionsInViewport(isVertical: Boolean, reverseScrollDirection: Boolean) {
        rule.setContent {
            with(LocalDensity.current) {
                if (isVertical) {
                    Column(
                        Modifier.requiredSize(100.toDp())
                            .testTag("viewport")
                            .verticalScroll(
                                rememberScrollState(),
                                reverseScrolling = reverseScrollDirection
                            )
                    ) {
                        ClickableTestBox(width = 200f, height = 200f)
                        ClickableTestBox(width = 200f, height = 200f)
                    }
                } else {
                    Row(
                        Modifier.requiredSize(100.toDp())
                            .testTag("viewport")
                            .horizontalScroll(
                                rememberScrollState(),
                                reverseScrolling = reverseScrollDirection
                            )
                    ) {
                        ClickableTestBox(width = 200f, height = 200f)
                        ClickableTestBox(width = 200f, height = 200f)
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        rule.onNodeWithTag("viewport").performGesture {
            assertThat(width).isEqualTo(100)
            assertThat(height).isEqualTo(100)
            assertThat(center).isEqualTo(Offset(50f, 50f))
            assertThat(topLeft).isEqualTo(Offset.Zero)
        }
    }
}
