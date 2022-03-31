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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.Tab
import androidx.compose.ui.input.key.Key.Companion.DirectionUp
import androidx.compose.ui.input.key.Key.Companion.DirectionDown
import androidx.compose.ui.input.key.Key.Companion.DirectionLeft
import androidx.compose.ui.input.key.Key.Companion.DirectionRight
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat
import org.junit.runners.Parameterized

@ExperimentalComposeUiApi
@MediumTest
@RunWith(Parameterized::class)
class CustomFocusTraversalTest(
    private val moveFocusProgrammatically: Boolean,
    private val useFocusOrderModifier: Boolean
) {
    @get:Rule
    val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "moveFocusProgrammatically = {0}, useFocusModifier = {1}")
        fun initParameters() = listOf(
            arrayOf(true, true),
            arrayOf(true, false),
            arrayOf(false, true),
            arrayOf(false, false)
        )
    }

    @Test
    fun focusProperties_next() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Row {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .dynamicFocusProperties { next = item3 }
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        if (moveFocusProgrammatically) {
            rule.runOnIdle {
                focusManager.moveFocus(FocusDirection.Next)
            }
        } else {
            rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, Tab.nativeKeyCode)))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isTrue()
        }
    }

    @Test
    fun focusProperties_previous() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Row {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .dynamicFocusProperties { previous = item1 }
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { item3.requestFocus() }

        // Act.
        if (moveFocusProgrammatically) {
            rule.runOnIdle {
                focusManager.moveFocus(FocusDirection.Previous)
            }
        } else {
            val nativeEvent = AndroidKeyEvent(0L, 0L, KeyDown, Tab.nativeKeyCode, 0, META_SHIFT_ON)
            rule.onRoot().performKeyPress(KeyEvent(nativeEvent))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isTrue()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isFalse()
        }
    }

    @Test
    fun focusProperties_up() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Column {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .dynamicFocusProperties { up = item1 }
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { item3.requestFocus() }

        // Act.
        if (moveFocusProgrammatically) {
            rule.runOnIdle {
                focusManager.moveFocus(FocusDirection.Up)
            }
        } else {
            val nativeKeyEvent = AndroidKeyEvent(KeyDown, DirectionUp.nativeKeyCode)
            rule.onRoot().performKeyPress(KeyEvent(nativeKeyEvent))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isTrue()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isFalse()
        }
    }

    @Test
    fun focusProperties_down() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Column {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .dynamicFocusProperties { down = item3 }
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        if (moveFocusProgrammatically) {
            rule.runOnIdle {
                focusManager.moveFocus(FocusDirection.Down)
            }
        } else {
            val nativeKeyEvent = AndroidKeyEvent(KeyDown, DirectionDown.nativeKeyCode)
            rule.onRoot().performKeyPress(KeyEvent(nativeKeyEvent))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isTrue()
        }
    }

    @Test
    fun focusProperties_left() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Row {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .dynamicFocusProperties { left = item1 }
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { item3.requestFocus() }

        // Act.
        if (moveFocusProgrammatically) {
            rule.runOnIdle {
                focusManager.moveFocus(FocusDirection.Left)
            }
        } else {
            val nativeKeyEvent = AndroidKeyEvent(KeyDown, DirectionLeft.nativeKeyCode)
            rule.onRoot().performKeyPress(KeyEvent(nativeKeyEvent))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isTrue()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isFalse()
        }
    }

    @Test
    fun focusProperties_right() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Row {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .dynamicFocusProperties { right = item3 }
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        if (moveFocusProgrammatically) {
            rule.runOnIdle {
                focusManager.moveFocus(FocusDirection.Right)
            }
        } else {
            val nativeKeyEvent = AndroidKeyEvent(KeyDown, DirectionRight.nativeKeyCode)
            rule.onRoot().performKeyPress(KeyEvent(nativeKeyEvent))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isTrue()
        }
    }

    // TODO(b/176847718): Verify that this test works correctly when the LocalLayoutDirection
    //  changes.
    @Test
    fun focusProperties_start() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Row {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .dynamicFocusProperties { start = item1 }
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { item3.requestFocus() }

        // Act.
        if (moveFocusProgrammatically) {
            rule.runOnIdle {
                focusManager.moveFocus(FocusDirection.Left)
            }
        } else {
            val nativeKeyEvent = AndroidKeyEvent(KeyDown, DirectionLeft.nativeKeyCode)
            rule.onRoot().performKeyPress(KeyEvent(nativeKeyEvent))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isTrue()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isFalse()
        }
    }

    // TODO(b/176847718): Verify that this test works correctly when the LocalLayoutDirection
    //  changes.
    @Test
    fun focusProperties_end() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Row {
                Box(
                    Modifier
                        .focusRequester(item1)
                        .dynamicFocusProperties { end = item3 }
                        .onFocusChanged { item1Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        if (moveFocusProgrammatically) {
            rule.runOnIdle {
                focusManager.moveFocus(FocusDirection.Right)
            }
        } else {
            val nativeKeyEvent = AndroidKeyEvent(KeyDown, DirectionRight.nativeKeyCode)
            rule.onRoot().performKeyPress(KeyEvent(nativeKeyEvent))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isTrue()
        }
    }

    @Test
    fun focusProperties_outermostParentWins() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        var item4Focused = false
        val (item1, item3, item4) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Row {
                Box(Modifier.dynamicFocusProperties { next = item4 }) {
                    Box(
                        Modifier
                            .focusRequester(item1)
                            .dynamicFocusProperties { next = item3 }
                            .onFocusChanged { item1Focused = it.isFocused }
                            .focusTarget()
                    )
                }
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item4)
                        .onFocusChanged { item4Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        if (moveFocusProgrammatically) {
            rule.runOnIdle {
                focusManager.moveFocus(FocusDirection.Next)
            }
        } else {
            rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, Tab.nativeKeyCode)))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isFalse()
            assertThat(item4Focused).isTrue()
        }
    }

    @Test
    fun focusProperties_parentCanResetCustomNextSetByChild() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Row {
                Box(Modifier.dynamicFocusProperties { next = FocusRequester.Default }) {
                    Box(
                        Modifier
                            .focusRequester(item1)
                            .dynamicFocusProperties { next = item3 }
                            .onFocusChanged { item1Focused = it.isFocused }
                            .focusTarget()
                    )
                }
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        if (moveFocusProgrammatically) {
            rule.runOnIdle {
                focusManager.moveFocus(FocusDirection.Next)
            }
        } else {
            rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, Tab.nativeKeyCode)))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isTrue()
            assertThat(item3Focused).isFalse()
        }
    }

    @Test
    fun focusProperties_emptyfocusPropertiesInParent_doesNotResetCustomNextSetByChild() {
        // Arrange.
        var item1Focused = false
        var item2Focused = false
        var item3Focused = false
        val (item1, item3) = FocusRequester.createRefs()
        lateinit var focusManager: FocusManager
        rule.setFocusableContent {
            focusManager = LocalFocusManager.current
            Row {
                Box(Modifier.dynamicFocusProperties { }) {
                    Box(
                        Modifier
                            .focusRequester(item1)
                            .dynamicFocusProperties { next = item3 }
                            .onFocusChanged { item1Focused = it.isFocused }
                            .focusTarget()
                    )
                }
                Box(
                    Modifier
                        .onFocusChanged { item2Focused = it.isFocused }
                        .focusTarget()
                )
                Box(
                    Modifier
                        .focusRequester(item3)
                        .onFocusChanged { item3Focused = it.isFocused }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        if (moveFocusProgrammatically) {
            rule.runOnIdle {
                focusManager.moveFocus(FocusDirection.Next)
            }
        } else {
            rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(KeyDown, Tab.nativeKeyCode)))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(item1Focused).isFalse()
            assertThat(item2Focused).isFalse()
            assertThat(item3Focused).isTrue()
        }
    }

    @Suppress("DEPRECATION")
    fun Modifier.dynamicFocusProperties(block: FocusOrder.() -> Unit): Modifier =
        if (useFocusOrderModifier) {
            this.then(ReceiverFocusOrderModifier(block))
        } else {
            focusProperties(FocusOrderToProperties(block))
        }

    @Suppress("DEPRECATION")
    class ReceiverFocusOrderModifier(
        val block: FocusOrder.() -> Unit
    ) : FocusOrderModifier {
        override fun populateFocusOrder(focusOrder: FocusOrder) {
            focusOrder.block()
        }
    }
}
