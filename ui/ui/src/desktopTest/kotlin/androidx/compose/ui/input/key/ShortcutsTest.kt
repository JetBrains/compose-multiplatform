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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.FocusReference
import androidx.compose.ui.focus.focusReference
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import com.google.common.truth.Truth
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ShortcutsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun shortcuts_triggered() {
        val focusReference = FocusReference()
        var triggered = 0
        rule.setContent {
            Box(
                modifier = Modifier
                    .size(10.dp, 10.dp)
                    .focusReference(focusReference)
                    .focusModifier()
                    .shortcuts {
                        on(Key.MetaLeft + Key.Enter) {
                            triggered += 1
                        }
                    }
            )
        }
        rule.runOnIdle {
            focusReference.requestFocus()
        }

        val firstKeyConsumed = rule.onRoot().performKeyPress(
            keyEvent(
                Key.MetaLeft, KeyEventType.KeyDown
            )
        )

        val secondKeyConsumed = rule.onRoot().performKeyPress(
            keyEvent(
                Key.Enter, KeyEventType.KeyDown
            )
        )

        rule.onRoot().performKeyPress(
            keyTypedEvent(Key.Enter)
        )

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.MetaLeft, KeyEventType.KeyUp
            )
        )

        rule.runOnIdle {
            Truth.assertThat(triggered).isEqualTo(1)
            Truth.assertThat(firstKeyConsumed).isFalse()
            Truth.assertThat(secondKeyConsumed).isTrue()
        }
    }

    @Test
    fun shortcuts_states() {
        val focusReference = FocusReference()
        var triggered = 0
        var setShortcuts by mutableStateOf(true)
        rule.setContent {
            Box(
                modifier = Modifier
                    .size(10.dp, 10.dp)
                    .focusReference(focusReference)
                    .focusModifier()
                    .shortcuts {
                        if (setShortcuts) {
                            on(Key.Enter) {
                                triggered += 1
                            }
                        }
                    }
            )
        }

        rule.runOnIdle {
            focusReference.requestFocus()
        }

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.Enter, KeyEventType.KeyDown
            )
        )

        rule.runOnIdle {
            Truth.assertThat(triggered).isEqualTo(1)
        }

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.Enter, KeyEventType.KeyUp
            )
        )

        // Disables shortcuts
        rule.runOnIdle {
            setShortcuts = false
        }

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.Enter, KeyEventType.KeyDown
            )
        )

        rule.runOnIdle {
            Truth.assertThat(triggered).isEqualTo(1)
        }
    }

    @Test
    fun shortcuts_priority() {
        val focusReference = FocusReference()
        var enterTriggered = 0
        var shortcutTriggered = 0
        rule.setContent {
            Box(
                modifier = Modifier
                    .size(10.dp, 10.dp)
                    .focusReference(focusReference)
                    .focusModifier()
                    .shortcuts {
                        on(Key.Enter) {
                            enterTriggered += 1
                        }

                        on(Key.ShiftLeft + Key.Enter) {
                            shortcutTriggered += 1
                        }
                    }
            )
        }

        rule.runOnIdle {
            focusReference.requestFocus()
        }

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.ShiftLeft, KeyEventType.KeyDown
            )
        )

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.Enter, KeyEventType.KeyDown
            )
        )

        rule.runOnIdle {
            Truth.assertThat(enterTriggered).isEqualTo(0)
            Truth.assertThat(shortcutTriggered).isEqualTo(1)
        }

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.Enter, KeyEventType.KeyUp
            )
        )

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.ShiftLeft, KeyEventType.KeyUp
            )
        )

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.Enter, KeyEventType.KeyDown
            )
        )

        rule.runOnIdle {
            Truth.assertThat(enterTriggered).isEqualTo(1)
            Truth.assertThat(shortcutTriggered).isEqualTo(1)
        }
    }

    @Test
    fun shortcuts_multiple() {
        val focusReference = FocusReference()
        var aTriggered = 0
        var cTriggered = 0
        rule.setContent {
            Box(
                modifier = Modifier
                    .size(10.dp, 10.dp)
                    .focusReference(focusReference)
                    .focusModifier()
                    .shortcuts {
                        on(Key.MetaLeft + Key.A) {
                            aTriggered += 1
                        }

                        on(Key.MetaLeft + Key.C) {
                            cTriggered += 1
                        }
                    }
            )
        }

        rule.runOnIdle {
            focusReference.requestFocus()
        }

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.MetaLeft, KeyEventType.KeyDown
            )
        )

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.C, KeyEventType.KeyDown
            )
        )

        rule.runOnIdle {
            Truth.assertThat(aTriggered).isEqualTo(0)
            Truth.assertThat(cTriggered).isEqualTo(1)
        }

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.C, KeyEventType.KeyUp
            )
        )

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.A, KeyEventType.KeyDown
            )
        )

        rule.runOnIdle {
            Truth.assertThat(aTriggered).isEqualTo(1)
            Truth.assertThat(cTriggered).isEqualTo(1)
        }
    }
}