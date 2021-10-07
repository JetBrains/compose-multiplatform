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

package androidx.compose.ui.input.pointer

import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class MotionEventSpyTest {
    @get:Rule
    val rule = createComposeRule()

    val Tag = "Test Tag"

    /**
     * When the events are inside the pointer input area, they should be received.
     */
    @Test
    fun eventInside() {
        val events = mutableListOf<Int>()
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.size(50.dp).testTag(Tag).motionEventSpy { events += it.actionMasked })
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .performTouchInput {
                down(Offset.Zero)
                moveBy(Offset(1f, 1f))
                up()
            }

        rule.waitForIdle()

        assertThat(events).containsExactly(
            ACTION_DOWN, ACTION_MOVE, ACTION_UP
        )
    }

    /**
     * When the events are inside the child's pointer input area, they should be received.
     */
    @Test
    fun eventInsideChild() {
        val events = mutableListOf<Int>()
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.size(50.dp).motionEventSpy { events += it.actionMasked }) {
                    Box(Modifier.size(50.dp).testTag(Tag).offset(55.dp, 0.dp).pointerInput(Unit) {
                    })
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .performTouchInput {
                down(Offset.Zero)
                moveBy(Offset(1f, 1f))
                up()
            }

        rule.waitForIdle()

        assertThat(events).containsExactly(
            ACTION_DOWN, ACTION_MOVE, ACTION_UP
        )
    }
}