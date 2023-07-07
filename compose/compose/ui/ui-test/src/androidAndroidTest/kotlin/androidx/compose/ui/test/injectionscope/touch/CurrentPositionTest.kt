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

import androidx.compose.ui.test.injectionscope.touch.Common.performTouchInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CurrentPositionTest {

    @get:Rule
    val rule = createComposeRule()

    private val recorder = MultiPointerInputRecorder()

    @Before
    fun setUp() {
        // Given some content
        rule.setContent {
            ClickableTestBox(recorder)
        }
    }

    @Test
    fun currentPosition_noPointersDown() {
        // When we have no pointers down
        rule.performTouchInput {
            // Then the current position is null
            assertThat(currentPosition(0)).isNull()
            assertThat(currentPosition(1)).isNull()
        }
    }

    @Test
    fun currentPosition_pointer0Down() {
        rule.performTouchInput {
            // When pointer 0 is down
            down(0, center)
            // It is at that position
            assertThat(currentPosition(0)).isEqualTo(center)
            // But pointer 1 is null
            assertThat(currentPosition(1)).isNull()
        }
        // And this remains the same in the next invocation
        rule.performTouchInput {
            assertThat(currentPosition(0)).isEqualTo(center)
            assertThat(currentPosition(1)).isNull()
        }
    }

    @Test
    fun currentPosition_pointer1Down() {
        rule.performTouchInput {
            // When pointer 1 is down
            down(1, center)
            // It is at that position
            assertThat(currentPosition(1)).isEqualTo(center)
            // But pointer 0 is null
            assertThat(currentPosition(0)).isNull()
        }
        // And this remains the same in the next invocation
        rule.performTouchInput {
            assertThat(currentPosition(1)).isEqualTo(center)
            assertThat(currentPosition(0)).isNull()
        }
    }

    @Test
    fun currentPosition_pointer0And1Down() {
        rule.performTouchInput {
            // When pointers 0 and 1 are down
            down(0, topLeft)
            down(1, center)
            // They are at that position
            assertThat(currentPosition(0)).isEqualTo(topLeft)
            assertThat(currentPosition(1)).isEqualTo(center)
        }
        // And this remains the same in the next invocation
        rule.performTouchInput {
            assertThat(currentPosition(0)).isEqualTo(topLeft)
            assertThat(currentPosition(1)).isEqualTo(center)
        }
    }

    @Test
    fun currentPosition_pointerMoved() {
        rule.performTouchInput {
            // When a pointer is down and moved around
            down(2, topLeft)
            moveTo(2, center)
            // It is at the new position
            assertThat(currentPosition(2)).isEqualTo(center)
        }
        // And this remains the same in the next invocation
        rule.performTouchInput {
            assertThat(currentPosition(2)).isEqualTo(center)
        }
    }

    @Test
    fun currentPosition_pointerUp() {
        rule.performTouchInput {
            // When a pointer is down, moved around and is up again
            down(3, topLeft)
            moveTo(3, center)
            up(3)
            // Its position is null
            assertThat(currentPosition(3)).isNull()
        }
        // And this remains the same in the next invocation
        rule.performTouchInput {
            assertThat(currentPosition(3)).isNull()
        }
    }

    @Test
    fun currentPosition_pointerCancel() {
        rule.performTouchInput {
            // When a pointer is down, moved around and the gesture is cancelled
            down(4, topLeft)
            moveTo(4, center)
            cancel()
            // Its position is null
            assertThat(currentPosition(4)).isNull()
        }
        // And this remains the same in the next invocation
        rule.performTouchInput {
            assertThat(currentPosition(4)).isNull()
        }
    }
}
