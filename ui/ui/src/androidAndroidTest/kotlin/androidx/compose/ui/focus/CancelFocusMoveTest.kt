/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@ExperimentalComposeUiApi
@MediumTest
@RunWith(Parameterized::class)
class CancelFocusMoveTest(param: Param) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun init() = listOf(Left, Right, Up, Down, Enter, Exit, Previous, Next).map { Param(it) }
    }

    @get:Rule val rule = createComposeRule()
    private val focusDirection = param.focusDirection
    private lateinit var focusManager: FocusManager
    private val focusRequester = List(11) { FocusRequester() }
    private val focusState = MutableList(11) { false }
    private var initialFocusedItem = -1

    @Test
    fun defaultOrderIsUsed() {
        // Arrange.
        initialFocusedItem = 5
        rule.setContentWithInitialFocus {
            Column(Modifier.testFocusable(0, 100.dp)) {
                Row {
                    Box(Modifier.testFocusable(1))
                    Box(Modifier.testFocusable(2))
                    Box(Modifier.testFocusable(3))
                }
                Row {
                    Box(Modifier.testFocusable(4))
                    Box(Modifier.testFocusable(5)) {
                        Box(Modifier.testFocusable(7))
                    }
                    Box(Modifier.testFocusable(6))
                }
                Row {
                    Box(Modifier.testFocusable(8))
                    Box(Modifier.testFocusable(9))
                    Box(Modifier.testFocusable(10))
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            assertThat(focusState[defaultFocusMoveResult]).isTrue()
        }
    }

    @Test
    fun cancelLeft() {
        // Arrange.
        initialFocusedItem = 5
        rule.setContentWithInitialFocus {
            Column(Modifier.testFocusable(0, 100.dp)) {
                Row {
                    Box(Modifier.testFocusable(1))
                    Box(Modifier.testFocusable(2))
                    Box(Modifier.testFocusable(3))
                }
                Row {
                    Box(Modifier.testFocusable(4))
                    Box(
                        modifier = Modifier
                            .focusProperties { left = FocusRequester.Cancel }
                            .testFocusable(5)
                    ) {
                        Box(Modifier.testFocusable(7))
                    }
                    Box(Modifier.testFocusable(6))
                }
                Row {
                    Box(Modifier.testFocusable(8))
                    Box(Modifier.testFocusable(9))
                    Box(Modifier.testFocusable(10))
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(success).isFalse()
                    assertThat(focusState[initialFocusedItem]).isTrue()
                }
                else -> {
                    assertThat(success).isTrue()
                    assertThat(focusState[defaultFocusMoveResult]).isTrue()
                }
            }
        }
    }

    @Test
    fun cancelRight() {
        // Arrange.
        initialFocusedItem = 5
        rule.setContentWithInitialFocus {
            Column(Modifier.testFocusable(0, 100.dp)) {
                Row {
                    Box(Modifier.testFocusable(1))
                    Box(Modifier.testFocusable(2))
                    Box(Modifier.testFocusable(3))
                }
                Row {
                    Box(Modifier.testFocusable(4))
                    Box(
                        modifier = Modifier
                            .focusProperties { right = FocusRequester.Cancel }
                            .testFocusable(5)
                    ) {
                        Box(Modifier.testFocusable(7))
                    }
                    Box(Modifier.testFocusable(6))
                }
                Row {
                    Box(Modifier.testFocusable(8))
                    Box(Modifier.testFocusable(9))
                    Box(Modifier.testFocusable(10))
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(success).isFalse()
                    assertThat(focusState[initialFocusedItem]).isTrue()
                }
                else -> {
                    assertThat(success).isTrue()
                    assertThat(focusState[defaultFocusMoveResult]).isTrue()
                }
            }
        }
    }

    @Test
    fun cancelUp() {
        // Arrange.
        initialFocusedItem = 5
        rule.setContentWithInitialFocus {
            Column(Modifier.testFocusable(0, 100.dp)) {
                Row {
                    Box(Modifier.testFocusable(1))
                    Box(Modifier.testFocusable(2))
                    Box(Modifier.testFocusable(3))
                }
                Row {
                    Box(Modifier.testFocusable(4))
                    Box(
                        modifier = Modifier
                            .focusProperties { up = FocusRequester.Cancel }
                            .testFocusable(5)
                    ) {
                        Box(Modifier.testFocusable(7))
                    }
                    Box(Modifier.testFocusable(6))
                }
                Row {
                    Box(Modifier.testFocusable(8))
                    Box(Modifier.testFocusable(9))
                    Box(Modifier.testFocusable(10))
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(success).isFalse()
                    assertThat(focusState[initialFocusedItem]).isTrue()
                }
                else -> {
                    assertThat(success).isTrue()
                    assertThat(focusState[defaultFocusMoveResult]).isTrue()
                }
            }
        }
    }

    @Test
    fun cancelDown() {
        // Arrange.
        initialFocusedItem = 5
        rule.setContentWithInitialFocus {
            Column(Modifier.testFocusable(0, 100.dp)) {
                Row {
                    Box(Modifier.testFocusable(1))
                    Box(Modifier.testFocusable(2))
                    Box(Modifier.testFocusable(3))
                }
                Row {
                    Box(Modifier.testFocusable(4))
                    Box(
                        modifier = Modifier
                            .focusProperties { down = FocusRequester.Cancel }
                            .testFocusable(5)
                    ) {
                        Box(Modifier.testFocusable(7))
                    }
                    Box(Modifier.testFocusable(6))
                }
                Row {
                    Box(Modifier.testFocusable(8))
                    Box(Modifier.testFocusable(9))
                    Box(Modifier.testFocusable(10))
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(success).isFalse()
                    assertThat(focusState[initialFocusedItem]).isTrue()
                }
                else -> {
                    assertThat(success).isTrue()
                    assertThat(focusState[defaultFocusMoveResult]).isTrue()
                }
            }
        }
    }

    @Test
    fun cancelNext() {
        // Arrange.
        initialFocusedItem = 5
        rule.setContentWithInitialFocus {
            Column(Modifier.testFocusable(0, 100.dp)) {
                Row {
                    Box(Modifier.testFocusable(1))
                    Box(Modifier.testFocusable(2))
                    Box(Modifier.testFocusable(3))
                }
                Row {
                    Box(Modifier.testFocusable(4))
                    Box(
                        modifier = Modifier
                            .focusProperties { next = FocusRequester.Cancel }
                            .testFocusable(5)
                    ) {
                        Box(Modifier.testFocusable(7))
                    }
                    Box(Modifier.testFocusable(6))
                }
                Row {
                    Box(Modifier.testFocusable(8))
                    Box(Modifier.testFocusable(9))
                    Box(Modifier.testFocusable(10))
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Next -> {
                    assertThat(success).isFalse()
                    assertThat(focusState[initialFocusedItem]).isTrue()
                }
                else -> {
                    assertThat(success).isTrue()
                    assertThat(focusState[defaultFocusMoveResult]).isTrue()
                }
            }
        }
    }

    @Test
    fun cancelPrevious() {
        // Arrange.
        initialFocusedItem = 5
        rule.setContentWithInitialFocus {
            Column(Modifier.testFocusable(0, 100.dp)) {
                Row {
                    Box(Modifier.testFocusable(1))
                    Box(Modifier.testFocusable(2))
                    Box(Modifier.testFocusable(3))
                }
                Row {
                    Box(Modifier.testFocusable(4))
                    Box(
                        modifier = Modifier
                            .focusProperties { previous = FocusRequester.Cancel }
                            .testFocusable(5)
                    ) {
                        Box(Modifier.testFocusable(7))
                    }
                    Box(Modifier.testFocusable(6))
                }
                Row {
                    Box(Modifier.testFocusable(8))
                    Box(Modifier.testFocusable(9))
                    Box(Modifier.testFocusable(10))
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Previous -> {
                    assertThat(success).isFalse()
                    assertThat(focusState[initialFocusedItem]).isTrue()
                }
                else -> {
                    assertThat(success).isTrue()
                    assertThat(focusState[defaultFocusMoveResult]).isTrue()
                }
            }
        }
    }

    @Test
    fun cancelNextAndPrevious() {
        // Arrange.
        initialFocusedItem = 5
        rule.setContentWithInitialFocus {
            Column(Modifier.testFocusable(0, 100.dp)) {
                Row {
                    Box(Modifier.testFocusable(1))
                    Box(Modifier.testFocusable(2))
                    Box(Modifier.testFocusable(3))
                }
                Row {
                    Box(Modifier.testFocusable(4))
                    Box(
                        modifier = Modifier
                            .focusProperties {
                                next = FocusRequester.Cancel
                                previous = FocusRequester.Cancel
                            }
                            .testFocusable(5)
                    ) {
                        Box(Modifier.testFocusable(7))
                    }
                    Box(Modifier.testFocusable(6))
                }
                Row {
                    Box(Modifier.testFocusable(8))
                    Box(Modifier.testFocusable(9))
                    Box(Modifier.testFocusable(10))
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Next, Previous -> {
                    assertThat(success).isFalse()
                    assertThat(focusState[initialFocusedItem]).isTrue()
                }
                else -> {
                    assertThat(success).isTrue()
                    assertThat(focusState[defaultFocusMoveResult]).isTrue()
                }
            }
        }
    }

    @Test
    fun cancelLeftAndRight() {
        // Arrange.
        initialFocusedItem = 5
        rule.setContentWithInitialFocus {
            Column(Modifier.testFocusable(0, 100.dp)) {
                Row {
                    Box(Modifier.testFocusable(1))
                    Box(Modifier.testFocusable(2))
                    Box(Modifier.testFocusable(3))
                }
                Row {
                    Box(Modifier.testFocusable(4))
                    Box(
                        modifier = Modifier
                            .focusProperties {
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                            }
                            .testFocusable(5)
                    ) {
                        Box(Modifier.testFocusable(7))
                    }
                    Box(Modifier.testFocusable(6))
                }
                Row {
                    Box(Modifier.testFocusable(8))
                    Box(Modifier.testFocusable(9))
                    Box(Modifier.testFocusable(10))
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Right -> {
                    assertThat(success).isFalse()
                    assertThat(focusState[initialFocusedItem]).isTrue()
                }
                else -> {
                    assertThat(success).isTrue()
                    assertThat(focusState[defaultFocusMoveResult]).isTrue()
                }
            }
        }
    }

    @Test
    fun cancelAllDirections() {
        // Arrange.
        initialFocusedItem = 5
        rule.setContentWithInitialFocus {
            Column(Modifier.testFocusable(0, 100.dp)) {
                Row {
                    Box(Modifier.testFocusable(1))
                    Box(Modifier.testFocusable(2))
                    Box(Modifier.testFocusable(3))
                }
                Row {
                    Box(Modifier.testFocusable(4))
                    Box(
                        modifier = Modifier
                            .focusProperties {
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                                previous = FocusRequester.Cancel
                                next = FocusRequester.Cancel
                            }
                            .testFocusable(5)
                    ) {
                        Box(Modifier.testFocusable(7))
                    }
                    Box(Modifier.testFocusable(6))
                }
                Row {
                    Box(Modifier.testFocusable(8))
                    Box(Modifier.testFocusable(9))
                    Box(Modifier.testFocusable(10))
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                // TODO(b/183746982): remove this after we add custom Enter and Exit.
                Enter, Exit -> {
                    assertThat(success).isTrue()
                    assertThat(focusState[defaultFocusMoveResult]).isTrue()
                }
                else -> {
                    assertThat(success).isFalse()
                    assertThat(focusState[initialFocusedItem]).isTrue()
                }
            }
        }
    }

    @Test
    fun cancelRight_clearedByParent() {
        // Arrange.
        initialFocusedItem = 5
        rule.setContentWithInitialFocus {
            Column(Modifier.testFocusable(0, 100.dp)) {
                Row {
                    Box(Modifier.testFocusable(1))
                    Box(Modifier.testFocusable(2))
                    Box(Modifier.testFocusable(3))
                }
                Row {
                    Box(Modifier.testFocusable(4))
                    Box(
                        modifier = Modifier
                            .focusProperties { right = FocusRequester.Default }
                            .focusProperties { right = FocusRequester.Cancel }
                            .testFocusable(5)
                    ) {
                        Box(Modifier.testFocusable(7))
                    }
                    Box(Modifier.testFocusable(6))
                }
                Row {
                    Box(Modifier.testFocusable(8))
                    Box(Modifier.testFocusable(9))
                    Box(Modifier.testFocusable(10))
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            assertThat(focusState[defaultFocusMoveResult]).isTrue()
        }
    }

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(val focusDirection: FocusDirection) {
        override fun toString() = focusDirection.toString()
    }

    @Composable
    fun Modifier.testFocusable(index: Int, size: Dp = 10.dp) = this
        .size(size)
        .focusRequester(focusRequester[index])
        .onFocusChanged { focusState[index] = it.isFocused }
        .focusable()

    private fun ComposeContentTestRule.setContentWithInitialFocus(
        composable: @Composable () -> Unit
    ) {
        setContent {
            focusManager = LocalFocusManager.current
            composable()
        }
        rule.runOnIdle { focusRequester[initialFocusedItem].requestFocus() }
    }

    private val defaultFocusMoveResult: Int
        get() = when (focusDirection) {
            Left, Previous -> 4
            Right -> 6
            Up -> 2
            Down -> 9
            Enter, Next -> 7
            Exit -> 0
            else -> error("")
        }
}
