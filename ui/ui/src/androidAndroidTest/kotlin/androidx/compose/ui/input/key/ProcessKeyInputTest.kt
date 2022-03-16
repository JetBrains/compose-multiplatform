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

package androidx.compose.ui.input.key

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.setFocusableContent
import android.view.KeyEvent.KEYCODE_A as KeyCodeA
import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.Key.Companion.A
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("DEPRECATION")
@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
class ProcessKeyInputTest {
    @get:Rule
    val rule = createComposeRule()

    @Test(expected = IllegalStateException::class)
    fun noRootFocusTarget_throwsException() {
        // Arrange.
        rule.setContent {
            Box(modifier = KeyInputModifier(null, null))
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))
    }

    @Test(expected = IllegalStateException::class)
    fun noFocusTarget_throwsException() {
        // Arrange.
        rule.setFocusableContent {
            Box(modifier = Modifier.onKeyEvent { true })
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))
    }

    @Test(expected = IllegalStateException::class)
    fun focusTargetNotFocused_throwsException() {

        // Arrange.
        rule.setFocusableContent {
            Box(modifier = Modifier.focusTarget().onKeyEvent { true })
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))
    }

    @Test
    fun onKeyEvent_triggered() {
        // Arrange.
        val focusRequester = FocusRequester()
        var receivedKeyEvent: KeyEvent? = null
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onKeyEvent {
                        receivedKeyEvent = it
                        true
                    }
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        val keyConsumed = rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            val keyEvent = checkNotNull(receivedKeyEvent)
            assertThat(keyEvent.key).isEqualTo(A)
            assertThat(keyEvent.type).isEqualTo(KeyUp)
            assertThat(keyConsumed).isTrue()
        }
    }

    @Test
    fun onPreviewKeyEvent_triggered() {
        // Arrange.
        val focusRequester = FocusRequester()
        var receivedKeyEvent: KeyEvent? = null
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onPreviewKeyEvent {
                        receivedKeyEvent = it
                        true
                    }
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        val keyConsumed = rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            val keyEvent = checkNotNull(receivedKeyEvent)
            assertThat(keyEvent.key).isEqualTo(A)
            assertThat(keyEvent.type).isEqualTo(KeyUp)
            assertThat(keyConsumed).isTrue()
        }
    }

    @Test
    fun onKeyEventNotTriggered_ifOnPreviewKeyEventConsumesEvent() {
        // Arrange.
        val focusRequester = FocusRequester()
        var receivedPreviewKeyEvent: KeyEvent? = null
        var receivedKeyEvent: KeyEvent? = null
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onKeyEvent {
                        receivedKeyEvent = it
                        true
                    }
                    .onPreviewKeyEvent {
                        receivedPreviewKeyEvent = it
                        true
                    }
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            val keyEvent = checkNotNull(receivedPreviewKeyEvent)
            assertThat(keyEvent.type).isEqualTo(KeyUp)
            assertThat(receivedKeyEvent).isNull()
        }
    }

    @Test
    fun onKeyEvent_triggeredAfter_onPreviewKeyEvent() {
        // Arrange.
        val focusRequester = FocusRequester()
        var triggerIndex = 1
        var onKeyEventTrigger = 0
        var onPreviewKeyEventTrigger = 0
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onKeyEvent {
                        onKeyEventTrigger = triggerIndex++
                        true
                    }
                    .onPreviewKeyEvent {
                        onPreviewKeyEventTrigger = triggerIndex++
                        false
                    }
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            assertThat(onPreviewKeyEventTrigger).isEqualTo(1)
            assertThat(onKeyEventTrigger).isEqualTo(2)
        }
    }

    @Test
    fun parent_child() {
        // Arrange.
        val focusRequester = FocusRequester()
        var triggerIndex = 1
        var parentOnKeyEventTrigger = 0
        var parentOnPreviewKeyEventTrigger = 0
        var childOnKeyEventTrigger = 0
        var childOnPreviewKeyEventTrigger = 0
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusTarget()
                    .onKeyEvent {
                        parentOnKeyEventTrigger = triggerIndex++
                        false
                    }
                    .onPreviewKeyEvent {
                        parentOnPreviewKeyEventTrigger = triggerIndex++
                        false
                    }
            ) {
                Box(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                        .onKeyEvent {
                            childOnKeyEventTrigger = triggerIndex++
                            false
                        }
                        .onPreviewKeyEvent {
                            childOnPreviewKeyEventTrigger = triggerIndex++
                            false
                        }
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            assertThat(parentOnPreviewKeyEventTrigger).isEqualTo(1)
            assertThat(childOnPreviewKeyEventTrigger).isEqualTo(2)
            assertThat(childOnKeyEventTrigger).isEqualTo(3)
            assertThat(parentOnKeyEventTrigger).isEqualTo(4)
        }
    }

    @Test
    fun parent_child_noFocusModifierForParent() {
        // Arrange.
        val focusRequester = FocusRequester()
        var triggerIndex = 1
        var parentOnKeyEventTrigger = 0
        var parentOnPreviewKeyEventTrigger = 0
        var childOnKeyEventTrigger = 0
        var childOnPreviewKeyEventTrigger = 0
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onKeyEvent {
                        parentOnKeyEventTrigger = triggerIndex++
                        false
                    }
                    .onPreviewKeyEvent {
                        parentOnPreviewKeyEventTrigger = triggerIndex++
                        false
                    }
            ) {
                Box(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                        .onKeyEvent {
                            childOnKeyEventTrigger = triggerIndex++
                            false
                        }
                        .onPreviewKeyEvent {
                            childOnPreviewKeyEventTrigger = triggerIndex++
                            false
                        }
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            assertThat(parentOnPreviewKeyEventTrigger).isEqualTo(1)
            assertThat(childOnPreviewKeyEventTrigger).isEqualTo(2)
            assertThat(childOnKeyEventTrigger).isEqualTo(3)
            assertThat(parentOnKeyEventTrigger).isEqualTo(4)
        }
    }

    @Test
    fun parent_child_noKeyInputModifierForChild() {
        // Arrange.
        val focusRequester = FocusRequester()
        var triggerIndex = 1
        var parentOnKeyEventTrigger = 0
        var parentOnPreviewKeyEventTrigger = 0
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onKeyEvent {
                        parentOnKeyEventTrigger = triggerIndex++
                        false
                    }
                    .onPreviewKeyEvent {
                        parentOnPreviewKeyEventTrigger = triggerIndex++
                        false
                    }
            ) {
                Box(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            assertThat(parentOnPreviewKeyEventTrigger).isEqualTo(1)
            assertThat(parentOnKeyEventTrigger).isEqualTo(2)
        }
    }

    @Test
    fun grandParent_parent_child() {
        // Arrange.
        val focusRequester = FocusRequester()
        var triggerIndex = 1
        var grandParentOnKeyEventTrigger = 0
        var grandParentOnPreviewKeyEventTrigger = 0
        var parentOnKeyEventTrigger = 0
        var parentOnPreviewKeyEventTrigger = 0
        var childOnKeyEventTrigger = 0
        var childOnPreviewKeyEventTrigger = 0
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .focusTarget()
                    .onKeyEvent {
                        grandParentOnKeyEventTrigger = triggerIndex++
                        false
                    }
                    .onPreviewKeyEvent {
                        grandParentOnPreviewKeyEventTrigger = triggerIndex++
                        false
                    }
            ) {
                Box(
                    modifier = Modifier
                        .focusTarget()
                        .onKeyEvent {
                            parentOnKeyEventTrigger = triggerIndex++
                            false
                        }
                        .onPreviewKeyEvent {
                            parentOnPreviewKeyEventTrigger = triggerIndex++
                            false
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusTarget()
                            .onKeyEvent {
                                childOnKeyEventTrigger = triggerIndex++
                                false
                            }
                            .onPreviewKeyEvent {
                                childOnPreviewKeyEventTrigger = triggerIndex++
                                false
                            }
                    )
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.onRoot().performKeyPress(keyEvent(KeyCodeA, KeyUp))

        // Assert.
        rule.runOnIdle {
            assertThat(grandParentOnPreviewKeyEventTrigger).isEqualTo(1)
            assertThat(parentOnPreviewKeyEventTrigger).isEqualTo(2)
            assertThat(childOnPreviewKeyEventTrigger).isEqualTo(3)
            assertThat(childOnKeyEventTrigger).isEqualTo(4)
            assertThat(parentOnKeyEventTrigger).isEqualTo(5)
            assertThat(grandParentOnKeyEventTrigger).isEqualTo(6)
        }
    }

    /**
     * The [KeyEvent] is usually created by the system. This function creates an instance of
     * [KeyEvent] that can be used in tests.
     */
    private fun keyEvent(keycode: Int, keyEventType: KeyEventType): KeyEvent {
        val action = when (keyEventType) {
            KeyDown -> ACTION_DOWN
            KeyUp -> ACTION_UP
            else -> error("Unknown key event type")
        }
        return KeyEvent(AndroidKeyEvent(0L, 0L, action, keycode, 0, 0))
    }
}
