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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(Parameterized::class)
class TwoDimensionalFocusTraversalImplicitExitTest(param: Param) {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var focusManager: FocusManager
    private val focusDirection = param.focusDirection
    private val initialFocus: FocusRequester = FocusRequester()
    private val focusedItem = mutableStateOf(false)

    /**
     *                    ________________
     *                   |      top      |
     *                   |_______________|
     *     __________     ________________      __________
     *    |   left  |    |  focusedItem  |     |  right  |
     *    |_________|    |_______________|     |_________|
     *                    ________________      __________
     *                   |    bottom     |     |  other  |
     *                   |_______________|     |_________|
     */
    @Test
    fun implicitExit_notTriggeredWhenFocusLeavesItem() {
        // Arrange.
        val (focusedItem, other) = List(2) { mutableStateOf(false) }
        val (left, right, top, bottom) = List(4) { mutableStateOf(false) }
        val otherItem = FocusRequester()
        rule.setContentForTest {
            val customExit = Modifier.focusProperties { exit = { otherItem } }
            FocusableBox(top, x = 20, y = 0, width = 10, height = 10, otherItem)
            FocusableBox(left, x = 0, y = 20, width = 10, height = 10, otherItem)
            FocusableBox(focusedItem, 20, 20, 10, 10, initialFocus, modifier = customExit)
            FocusableBox(right, x = 40, y = 20, width = 10, height = 10, otherItem)
            FocusableBox(bottom, x = 20, y = 40, width = 10, height = 10, otherItem)
            FocusableBox(other, x = 20, y = 40, width = 10, height = 10, otherItem)
        }

            // Act.
            val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

            // Assert.
            rule.runOnIdle {
                assertThat(movedFocusSuccessfully).isTrue()
                assertThat(focusedItem.value).isFalse()
                assertThat(other.value).isFalse()
                when (focusDirection) {
                    Left -> assertThat(left.value).isTrue()
                    Right -> assertThat(right.value).isTrue()
                    Up -> assertThat(top.value).isTrue()
                    Down -> assertThat(bottom.value).isTrue()
                    else -> error("Invalid FocusDirection")
                }
            }
    }

    /**
     *      __________________________      __________
     *     |  grandparent            |     |  other  |
     *     |   ____________________  |     |_________|
     *     |  |  parent           |  |
     *     |  |   ______________  |  |
     *     |  |  | focusedItem |  |  |
     *     |  |  |_____________|  |  |
     *     |  |___________________|  |
     *     |_________________________|
     */
    @Test
    fun implicitExit_deactivatedParentCanRedirectExit() {
        // Arrange.
        val (parent, grandparent, other) = List(3) { mutableStateOf(false) }
        val otherItem = FocusRequester()
        var receivedFocusDirection: FocusDirection? = null
        rule.setContentForTest {
            FocusableBox(grandparent, 0, 0, 50, 50) {
                val customExit = Modifier.focusProperties {
                    exit = {
                        receivedFocusDirection = it
                        otherItem
                    }
                }
                FocusableBox(parent, 10, 10, 30, 30, deactivated = true, modifier = customExit) {
                    FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus) }
            }
            FocusableBox(other, x = 0, y = 60, width = 10, height = 10, otherItem)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(receivedFocusDirection).isEqualTo(focusDirection)
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(grandparent.value).isFalse()
            assertThat(other.value).isTrue()
        }
    }

    /**
     *                    _________
     *                   |   Up   |
     *                   |________|
     *                 ________________
     *                |  parent       |
     *   _________    |   _________   |    _________
     *  |  Left  |    |  | child0 |   |   |  Right |
     *  |________|    |  |________|   |   |________|
     *                |_______________|
     *                    _________
     *                   |  Down  |
     *                   |________|
     */
    @Test
    fun moveFocusExit_blockFocusChange() {
        // Arrange.
        val (up, down, left, right, parent) = List(5) { mutableStateOf(false) }
        val customFocusExit = Modifier.focusProperties { exit = { Cancel } }
        rule.setContentForTest {
            FocusableBox(up, 30, 0, 10, 10)
            FocusableBox(left, 0, 30, 10, 10)
            FocusableBox(parent, 20, 20, 70, 50, deactivated = true, modifier = customFocusExit) {
                FocusableBox(focusedItem, 30, 30, 10, 10, initialFocus)
            }
            FocusableBox(right, 100, 35, 10, 10)
            FocusableBox(down, 30, 90, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(up.value).isFalse()
            assertThat(left.value).isFalse()
            assertThat(right.value).isFalse()
            assertThat(down.value).isFalse()
            assertThat(focusedItem.value).isTrue()
        }
    }

    /**
     *                    _________
     *                   |   Up   |
     *                   |________|
     *                 ________________
     *                |  parent       |
     *   _________    |   _________   |    _________
     *  |  Left  |    |  | source |   |   |  Right |
     *  |________|    |  |________|   |   |________|
     *                |_______________|
     *                    _________
     *                   |  Down  |
     *                   |________|
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun moveFocusExit_cancelExit() {
        // Arrange.
        val (up, down, left, right, parent) = List(5) { mutableStateOf(false) }
        var (upItem, downItem, leftItem, rightItem) = FocusRequester.createRefs()

        val customFocusExit = Modifier
            .focusProperties { exit = { Cancel } }
            .focusGroup()

        rule.setContentForTest {
            FocusableBox(up, 30, 0, 10, 10, upItem)
            FocusableBox(left, 0, 30, 10, 10, leftItem)
            FocusableBox(parent, 20, 20, 30, 30, modifier = customFocusExit) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
            }
            FocusableBox(right, 60, 30, 10, 10, rightItem)
            FocusableBox(down, 30, 60, 10, 10, downItem)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(up.value).isFalse()
            assertThat(left.value).isFalse()
            assertThat(right.value).isFalse()
            assertThat(down.value).isFalse()
            assertThat(focusedItem.value).isTrue()
        }
    }

    /**
     *   _________        _________
     *  |  dest  |       |   Up   |
     *  |________|       |________|
     *                 ________________
     *                |  parent       |
     *   _________    |   _________   |    _________
     *  |  Left  |    |  | source |   |   |  Right |
     *  |________|    |  |________|   |   |________|
     *                |_______________|
     *                    _________
     *                   |  Down  |
     *                   |________|
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun moveFocusExit_redirectExit() {
        // Arrange.
        val destItem = FocusRequester()
        val (dest, parent) = List(4) { mutableStateOf(false) }
        val (up, down, left, right) = List(4) { mutableStateOf(false) }
        var (upItem, downItem, leftItem, rightItem) = FocusRequester.createRefs()

        val customFocusExit = Modifier
            .focusProperties {
                exit = {
                    initialFocus.requestFocus()
                    Cancel
                }
            }
            .focusGroup()

        rule.setContentForTest {
            FocusableBox(dest, 0, 0, 10, 10, destItem)
            FocusableBox(up, 30, 0, 10, 10, upItem)
            FocusableBox(left, 0, 30, 10, 10, leftItem)
            FocusableBox(parent, 20, 20, 30, 30, modifier = customFocusExit) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
            }
            FocusableBox(right, 60, 30, 10, 10, rightItem)
            FocusableBox(down, 30, 60, 10, 10, downItem)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(up.value).isFalse()
            assertThat(left.value).isFalse()
            assertThat(right.value).isFalse()
            assertThat(down.value).isFalse()
            assertThat(focusedItem.value).isTrue()
        }
    }

    /**
     *                      _________
     *                     |   Up   |
     *                     |________|
     *               _________________________
     *              | grandparent            |
     *              |  _____________________ |
     *              | | parent             | |
     *   _________  | |     _________      | |  _________
     *  |  Left  |  | |    | source |      | | |  Right |
     *  |________|  | |    |________|      | | |________|
     *              | |____________________| |
     *              |________________________|
     *                      _________
     *                     |  Down  |
     *                     |________|
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun moveFocusExit_multipleParents_cancelExit() {
        // Arrange.
        val (grandparent, parent) = List(4) { mutableStateOf(false) }
        val (up, down, left, right) = List(4) { mutableStateOf(false) }
        var (upItem, downItem, leftItem, rightItem) = FocusRequester.createRefs()

        val customFocusExit = Modifier
            .focusProperties { exit = { Cancel } }
            .focusGroup()

        rule.setContentForTest {
            FocusableBox(up, 40, 0, 10, 10, upItem)
            FocusableBox(left, 0, 40, 10, 10, leftItem)
            FocusableBox(grandparent, 20, 20, 50, 50, modifier = Modifier.focusGroup()) {
                FocusableBox(parent, 10, 10, 30, 30, modifier = customFocusExit) {
                    FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                }
            }
            FocusableBox(right, 80, 40, 10, 10, rightItem)
            FocusableBox(down, 40, 80, 10, 10, downItem)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(up.value).isFalse()
            assertThat(left.value).isFalse()
            assertThat(right.value).isFalse()
            assertThat(down.value).isFalse()
            assertThat(focusedItem.value).isTrue()
        }
    }

    /**
     *   _________            _________
     *  |  dest  |           |   Up   |
     *  |________|           |________|
     *                  _____________________
     *                 | grandparent+parent |
     *   _________     |      _________     |    _________
     *  |  Left  |     |     | source |     |   |  Right |
     *  |________|     |     |________|     |   |________|
     *                 |____________________|
     *                        _________
     *                       |  Down  |
     *                       |________|
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun moveFocusExit_multipleParents_redirectExit() {
        // Arrange.
        val destItem = FocusRequester()
        val (dest, grandparent, parent) = List(4) { mutableStateOf(false) }
        val (up, down, left, right) = List(4) { mutableStateOf(false) }
        var (upItem, downItem, leftItem, rightItem) = FocusRequester.createRefs()

        val customFocusExit = Modifier
            .focusGroup()
            .focusProperties {
                exit = {
                    initialFocus.requestFocus()
                    Cancel
                }
            }
            .focusGroup()

        rule.setContentForTest {
            FocusableBox(dest, 0, 0, 10, 10, destItem)
            FocusableBox(up, 40, 0, 10, 10, upItem)
            FocusableBox(left, 0, 40, 10, 10, leftItem)
            FocusableBox(grandparent, 20, 20, 50, 50, modifier = Modifier.focusGroup()) {
                FocusableBox(parent, 10, 10, 30, 30, modifier = customFocusExit) {
                    FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                }
            }
            FocusableBox(right, 80, 40, 10, 10, rightItem)
            FocusableBox(down, 40, 80, 10, 10, downItem)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(up.value).isFalse()
            assertThat(left.value).isFalse()
            assertThat(right.value).isFalse()
            assertThat(down.value).isFalse()
            assertThat(focusedItem.value).isTrue()
        }
    }

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(val focusDirection: FocusDirection) {
        override fun toString() = focusDirection.toString()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(Left, Right, Up, Down).map { Param(it) }
    }

    private fun ComposeContentTestRule.setContentForTest(composable: @Composable () -> Unit) {
        setContent {
            focusManager = LocalFocusManager.current
            composable()
        }
        rule.runOnIdle { initialFocus.requestFocus() }
    }
}
