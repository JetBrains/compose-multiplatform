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

package androidx.compose.ui.focus

import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.ACTION_DOWN as KeyDown
import android.view.KeyEvent.META_SHIFT_ON
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.Tab
import androidx.compose.ui.input.key.Key.Companion.DPadUp
import androidx.compose.ui.input.key.Key.Companion.DPadDown
import androidx.compose.ui.input.key.Key.Companion.DPadLeft
import androidx.compose.ui.input.key.Key.Companion.DPadRight
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat

@MediumTest
@RunWith(AndroidJUnit4::class)
class CustomFocusTraversalTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun focusOrder_next() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Row {
                Box(
                    Modifier
                        .focusOrder(item1) { next = item3 }
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusModifier()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, Tab.nativeKeyCode)))

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isTrue()
        }
    }

    @Test
    fun focusOrder_previous() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Row {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusOrder(item3) { previous = item1 }
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusModifier()
                )
            }
        }
        rule.runOnIdle { item3.requestFocus() }

        // Act.
        val nativeKeyEvent = AndroidKeyEvent(0L, 0L, KeyDown, Tab.nativeKeyCode, 0, META_SHIFT_ON)
        rule.onRoot().performKeyPress(KeyEvent(nativeKeyEvent))

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isTrue()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isFalse()
        }
    }

    @Test
    fun focusOrder_up() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Column {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusOrder(item3) { up = item1 }
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusModifier()
                )
            }
        }
        rule.runOnIdle { item3.requestFocus() }

        // Act.
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, DPadUp.nativeKeyCode)))

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isTrue()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isFalse()
        }
    }

    @Test
    fun focusOrder_down() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Column {
                Box(
                    Modifier
                        .focusOrder(item1) { down = item3 }
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusModifier()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, DPadDown.nativeKeyCode)))

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isTrue()
        }
    }

    @Test
    fun focusOrder_left() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Row {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusOrder(item3) { left = item1 }
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusModifier()
                )
            }
        }
        rule.runOnIdle { item3.requestFocus() }

        // Act.
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, DPadLeft.nativeKeyCode)))

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isTrue()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isFalse()
        }
    }

    @Test
    fun focusOrder_right() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Row {
                Box(
                    Modifier
                        .focusOrder(item1) { right = item3 }
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusModifier()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, DPadRight.nativeKeyCode)))

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isTrue()
        }
    }

    // TODO(b/176847718): Verify that this test works correctly when the AmbientLayoutDirection
    //  changes.
    @Test
    fun focusOrder_start() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Row {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusOrder(item3) { start = item1 }
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusModifier()
                )
            }
        }
        rule.runOnIdle { item3.requestFocus() }

        // Act.
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, DPadLeft.nativeKeyCode)))

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isTrue()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isFalse()
        }
    }

    // TODO(b/176847718): Verify that this test works correctly when the AmbientLayoutDirection
    //  changes.
    @Test
    fun focusOrder_end() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Row {
                Box(
                    Modifier
                        .focusOrder(item1) { end = item3 }
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusModifier()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, DPadRight.nativeKeyCode)))

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isTrue()
        }
    }

    @Test
    fun focusOrder_outermostParentWins() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        var item4Focused = false
        val (item1, item3, item4) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Row {
                Box(Modifier.focusOrder { next = item4 }) {
                    Box(
                        Modifier
                            .focusOrder(item1) { next = item3 }
                            .onFocusChanged { item1Focused = it.isFocused }
                            .focusModifier()
                    )
                }
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusRequester(item4)
                        .onFocusChanged { item4Focused = it.isFocused }
                        .focusModifier()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, Tab.nativeKeyCode)))

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isFalse()
            assertThat(item4Focused).isTrue()
        }
    }

    @Test
    fun focusOrder_parentCanResetCustomNextSetByChild() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Row {
                Box(Modifier.focusOrder { next = FocusRequester.Default }) {
                    Box(
                        Modifier
                            .focusOrder(item1) { next = item3 }
                            .onFocusChanged { item1Focused = it.isFocused }
                            .focusModifier()
                    )
                }
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusModifier()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, Tab.nativeKeyCode)))

        // Assert.
        rule.runOnIdle {
            // TODO(b/170155659): After implementing one-dimensional focus search, update this test
            //  so that item2 is focused.
            assertThat(item1Focused).isTrue()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isFalse()
        }
    }

    @Test
    fun focusOrder_emptyFocusOrderInParent_doesNotResetCustomNextSetByChild() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Row {
                Box(Modifier.focusOrder { }) {
                    Box(
                        Modifier
                            .focusOrder(item1) { next = item3 }
                            .onFocusChanged { item1Focused = it.isFocused }
                            .focusModifier()
                    )
                }
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusModifier()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusModifier()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, Tab.nativeKeyCode)))

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isTrue()
        }
    }
}
