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

package androidx.compose.ui.input.pointer
//TODO(demin): uncomment this when we upstream this to androidx, and move onPointerEvent to commonMain
/*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@MediumTest
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class OnPointerEventTest {
    @get:Rule
    val rule = createComposeRule()

    val tag = "Tagged Layout"

    @Test
    fun pressAndRelease() {
        val events = mutableListOf<PointerEvent>()

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(Alignment.Center).size(50.dp)
                        .testTag(tag)
                        .onPointerEvent(PointerEventType.Press) {
                            events += it
                        }
                        .onPointerEvent(PointerEventType.Release) {
                            events += it
                        }
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(tag).performMouseInput {
            moveTo(Offset(2f, 3f))
            press()
            moveTo(Offset(5f, 6f))
        }

        assertThat(events).hasSize(1)
        assertThat(events.last().changes.first().position).isEqualTo(Offset(2f, 3f))
        assertThat(events.last().type).isEqualTo(PointerEventType.Press)

        rule.onNodeWithTag(tag).performMouseInput {
            release()
        }

        assertThat(events).hasSize(2)
        assertThat(events.last().changes.first().position).isEqualTo(Offset(5f, 6f))
        assertThat(events.last().type).isEqualTo(PointerEventType.Release)
    }

    @Test
    fun enterMoveExit() {
        val events = mutableListOf<PointerEvent>()

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(Alignment.Center).size(50.dp)
                        .testTag(tag)
                        .onPointerEvent(PointerEventType.Enter) {
                            events += it
                        }
                        .onPointerEvent(PointerEventType.Move) {
                            events += it
                        }
                        .onPointerEvent(PointerEventType.Exit) {
                            events += it
                        }
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(tag).performMouseInput {
            moveTo(Offset(2f, 3f))
            press()
            moveTo(Offset(5f, 6f))
            release()
            moveTo(Offset(10000f, 6f))
        }

        assertThat(events).hasSize(3)
        assertThat(events[0].changes.first().position).isEqualTo(Offset(2f, 3f))
        assertThat(events[0].type).isEqualTo(PointerEventType.Enter)
        assertThat(events[1].changes.first().position).isEqualTo(Offset(5f, 6f))
        assertThat(events[1].type).isEqualTo(PointerEventType.Move)
        assertThat(events[2].changes.first().position).isEqualTo(Offset(10000f, 6f))
        assertThat(events[2].type).isEqualTo(PointerEventType.Exit)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun scroll() {
        val events = mutableListOf<PointerEvent>()

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(Alignment.Center).size(50.dp)
                        .testTag(tag)
                        .onPointerEvent(PointerEventType.Scroll) {
                            events += it
                        }
                        .onPointerEvent(PointerEventType.Release) {
                            events += it
                        }
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(tag).performMouseInput {
            moveTo(Offset(2f, 3f))
            // TODO(b/224992993): we should invert scroll delta in ui-test module
            scroll(2f)
        }

        assertThat(events).hasSize(1)
        assertThat(events.last().changes.first().position).isEqualTo(Offset(2f, 3f))
        assertThat(events.last().changes.first().scrollDelta).isEqualTo(Offset(0f, -2f))
        assertThat(events.last().type).isEqualTo(PointerEventType.Scroll)

        rule.onNodeWithTag(tag).performMouseInput {
            moveTo(Offset(5f, 3f))
            scroll(3f, ScrollWheel.Horizontal)
        }

        assertThat(events).hasSize(2)
        assertThat(events.last().changes.first().position).isEqualTo(Offset(5f, 3f))
        assertThat(events.last().changes.first().scrollDelta).isEqualTo(Offset(3f, -0f))
        assertThat(events.last().type).isEqualTo(PointerEventType.Scroll)
    }
}
 */