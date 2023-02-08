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

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(Parameterized::class)
class TwoDimensionalFocusTraversalImplicitEnterTest(param: Param) {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var focusManager: FocusManager
    private val focusDirection = param.focusDirection
    private val initialFocus: FocusRequester = FocusRequester()

    /**
     *                                _________
     *                               |   Up   |
     *                               |________|
     *                 _______________________________________
     *                |  focusedItem                         |
     *                |   _________   _________   _________  |
     *   _________    |  | child0 |  | child1 |  | child2 |  |    _________
     *  |  Left  |    |  |________|  |________|  |________|  |   |  Right |
     *  |________|    |   _________   _________   _________  |   |________|
     *                |  | child3 |  | child4 |  | child5 |  |
     *                |  |________|  |________|  |________|  |
     *                |______________________________________|
     *                                 _________
     *                                |  Down  |
     *                                |________|
     */
    @Test
    fun moveFocusEnter_customChildIsFocused() {
        // Arrange.
        val (up, down, left, right, parent) = List(5) { mutableStateOf(false) }
        val children = List(6) { mutableStateOf(false) }
        var (upItem, downItem, leftItem, rightItem) = FocusRequester.createRefs()
        val (child1, child2, child3, child4) = FocusRequester.createRefs()
        val customFocusEnter = Modifier.focusProperties {
            enter = {
                when (it) {
                    Left -> child1
                    Up -> child2
                    Down -> child3
                    Right -> child4
                    else -> error("Invalid Direction")
                }
            }
        }
        when (focusDirection) {
            Left -> rightItem = initialFocus
            Right -> leftItem = initialFocus
            Up -> downItem = initialFocus
            Down -> upItem = initialFocus
        }
        rule.setContentForTest {
            FocusableBox(up, 50, 0, 10, 10, upItem)
            FocusableBox(left, 0, 35, 10, 10, leftItem)
            FocusableBox(parent, 20, 20, 70, 50, deactivated = true, modifier = customFocusEnter) {
                FocusableBox(children[0], 30, 30, 10, 10)
                FocusableBox(children[1], 50, 30, 10, 10, child1)
                FocusableBox(children[2], 70, 30, 10, 10, child2)
                FocusableBox(children[3], 30, 50, 10, 10, child3)
                FocusableBox(children[4], 50, 50, 10, 10, child4)
                FocusableBox(children[5], 70, 50, 10, 10)
            }
            FocusableBox(right, 100, 35, 10, 10, rightItem)
            FocusableBox(down, 50, 90, 10, 10, downItem)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            when (focusDirection) {
                Left -> assertThat(children.values).isExactly(
                    false, true, false,
                    false, false, false
                )
                Up -> assertThat(children.values).isExactly(
                    false, false, true,
                    false, false, false
                )
                Down -> assertThat(children.values).isExactly(
                    false, false, false,
                    true, false, false
                )
                Right -> assertThat(children.values).isExactly(
                    false, false, false,
                    false, true, false
                )
            }
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
    fun moveFocusEnter_blockFocusChange() {
        // Arrange.
        val (up, down, left, right, parent) = List(5) { mutableStateOf(false) }
        val child = mutableStateOf(false)
        var (upItem, downItem, leftItem, rightItem, childItem) = FocusRequester.createRefs()
        val customFocusEnter = Modifier.focusProperties { enter = { Cancel } }
        when (focusDirection) {
            Left -> rightItem = initialFocus
            Right -> leftItem = initialFocus
            Up -> downItem = initialFocus
            Down -> upItem = initialFocus
        }
        rule.setContentForTest {
            FocusableBox(up, 30, 0, 10, 10, upItem)
            FocusableBox(left, 0, 30, 10, 10, leftItem)
            FocusableBox(parent, 20, 20, 70, 50, deactivated = true, modifier = customFocusEnter) {
                FocusableBox(child, 30, 30, 10, 10, childItem)
            }
            FocusableBox(right, 100, 35, 10, 10, rightItem)
            FocusableBox(down, 30, 90, 10, 10, downItem)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(child.value).isFalse()
            when (focusDirection) {
                Left -> assertThat(right.value).isTrue()
                Right -> assertThat(left.value).isTrue()
                Up -> assertThat(down.value).isTrue()
                Down -> assertThat(up.value).isTrue()
            }
        }
    }

    /**
     *                    _________
     *                   |   Up   |
     *                   |________|
     *                 ________________
     *                |               |
     *   _________    |     empty     |    _________
     *  |  Left  |    |   lazylist    |   |  Right |
     *  |________|    |               |   |________|
     *                |_______________|
     *                    _________
     *                   |  Down  |
     *                   |________|
     */
    @Test
    fun moveFocusEnter_emptyLazyList() {
        // Arrange.
        val (up, down, left, right) = List(4) { mutableStateOf(false) }
        var (upItem, downItem, leftItem, rightItem) = FocusRequester.createRefs()
        when (focusDirection) {
            Left -> rightItem = initialFocus
            Right -> leftItem = initialFocus
            Up -> downItem = initialFocus
            Down -> upItem = initialFocus
        }
        rule.setContentForTest {
            FocusableBox(up, 30, 0, 10, 10, upItem)
            FocusableBox(left, 0, 30, 10, 10, leftItem)
            LazyRow(
                Modifier
                    .offset { IntOffset(30, 30) }
                    .width(10.dp)
                    .height(10.dp)
            ) {}
            FocusableBox(right, 100, 30, 10, 10, rightItem)
            FocusableBox(down, 30, 90, 10, 10, downItem)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            when (focusDirection) {
                Left, Up, Down -> assertThat(left.value).isTrue()
                Right -> assertThat(right.value).isTrue()
            }
        }
    }

    /**
     *                 _________
     *                |   Up   |
     *                |________|
     *
     *   _________     _________    _________
     *  |  Left  |    |  item  |   |  Right |
     *  |________|    |________|   |________|
     *
     *                 _________    _________
     *                |  Down  |   | Other  |
     *                |________|   |________|
     */
    @Test
    fun focusOnItem_doesNotTriggerEnter() {
        // Arrange.
        val (up, down, left, right) = List(4) { mutableStateOf(false) }
        val (item, other) = List(2) { mutableStateOf(false) }
        var (upItem, downItem, leftItem, rightItem) = FocusRequester.createRefs()
        val customFocusEnter = Modifier.focusProperties { enter = { Cancel } }
        when (focusDirection) {
            Left -> rightItem = initialFocus
            Right -> leftItem = initialFocus
            Up -> downItem = initialFocus
            Down -> upItem = initialFocus
        }
        rule.setContentForTest {
            FocusableBox(up, 20, 0, 10, 10, upItem)
            FocusableBox(left, 0, 20, 10, 10, leftItem)
            FocusableBox(item, 20, 20, 10, 10, modifier = customFocusEnter)
            FocusableBox(right, 40, 20, 10, 10, rightItem)
            FocusableBox(down, 20, 60, 10, 10, downItem)
            FocusableBox(other, 60, 60, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(item.value).isTrue()
            assertThat(other.value).isFalse()
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

private val List<MutableState<Boolean>>.values get() = this.map { it.value }
