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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val invalid = "Not applicable to a 2D focus search."

@MediumTest
@RunWith(Parameterized::class)
class TwoDimensionalFocusTraversalTest(param: Param) {
    @get:Rule
    val rule = createComposeRule()

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(val focusDirection: FocusDirection) {
        override fun toString() = focusDirection.toString()
    }

    private val focusDirection = param.focusDirection
    private lateinit var focusManager: FocusManager
    private var initialFocus: FocusRequester = FocusRequester()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(Param(Left), Param(Right), Param(Up), Param(Down))
    }

    @FlakyTest(bugId = 233373546)
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun movesFocusAmongSiblingsDeepInTheFocusHierarchy() {
        // Arrange.
        val isFocused = MutableList(2) { mutableStateOf(false) }
        val (item1, item2) = FocusRequester.createRefs()
        initialFocus = when (focusDirection) {
            Up, Left -> item2
            Down, Right -> item1
            else -> error(invalid)
        }
        val siblings = @Composable {
            FocusableBox(isFocused[0], item1)
            FocusableBox(isFocused[1], item2)
        }
        rule.setContentForTest {
            FocusableBox {
                FocusableBox {
                    FocusableBox {
                        FocusableBox {
                            FocusableBox {
                                FocusableBox {
                                    when (focusDirection) {
                                        Up, Down -> Column { siblings() }
                                        Left, Right -> Row { siblings() }
                                        else -> error(invalid)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            when (focusDirection) {
                Up, Left -> assertThat(isFocused.values).isExactly(true, false)
                Down, Right -> assertThat(isFocused.values).isExactly(false, true)
                else -> error(invalid)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun movesFocusOutsideCurrentParent() {
        // Arrange.
        val isFocused = MutableList(2) { mutableStateOf(false) }
        val content = @Composable {
            FocusableBox(isFocused[0])
            FocusableBox {
                FocusableBox(focusRequester = initialFocus)
            }
            FocusableBox(isFocused[1])
        }
        rule.setContentForTest {
            when (focusDirection) {
                Up, Down -> Column { content() }
                Left, Right -> Row { content() }
                else -> error(invalid)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            when (focusDirection) {
                Up, Left -> assertThat(isFocused.values).isExactly(true, false)
                Down, Right -> assertThat(isFocused.values).isExactly(false, true)
                else -> error(invalid)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun movesOutsideDeactivatedParent() {
        // Arrange.
        val isFocused = MutableList(2) { mutableStateOf(false) }
        val initialFocusValue = mutableStateOf(false)
        val content = @Composable {
            FocusableBox(isFocused[0])
            FocusableBox(deactivated = true) {
                FocusableBox(isFocused = initialFocusValue, focusRequester = initialFocus)
            }
            FocusableBox(isFocused[1])
        }
        rule.setContentForTest {
            when (focusDirection) {
                Up, Down -> Column { content() }
                Left, Right -> Row { content() }
                else -> error(invalid)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            when (focusDirection) {
                Up, Left -> assertThat(isFocused.values)
                    .isExactly(true, false)
                Down, Right -> assertThat(isFocused.values)
                    .isExactly(false, true)
                else -> error(invalid)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun skipsChild() {
        // Arrange.
        val isFocused = MutableList(3) { mutableStateOf(false) }
        val content = @Composable {
            FocusableBox(isFocused[0])
            FocusableBox(isFocused[1], focusRequester = initialFocus) {
                FocusableBox()
            }
            FocusableBox(isFocused[2])
        }
        rule.setContentForTest {
            when (focusDirection) {
                Up, Down -> Column { content() }
                Left, Right -> Row { content() }
                else -> error(invalid)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            when (focusDirection) {
                Up, Left -> assertThat(isFocused.values).isExactly(true, false, false)
                Down, Right -> assertThat(isFocused.values).isExactly(false, false, true)
                else -> error(invalid)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun DoesNotSkipChildOfDeactivatedItem() {
        // Arrange.
        val isFocused = MutableList(3) { mutableStateOf(false) }
        val childIsFocused = mutableStateOf(false)
        val (item1, item2) = FocusRequester.createRefs()
        initialFocus = when (focusDirection) {
            Up, Left -> item2
            Down, Right -> item1
            else -> error(invalid)
        }
        val content = @Composable {
            FocusableBox(isFocused[0], item1)
            FocusableBox(isFocused[1], deactivated = true) {
                FocusableBox(childIsFocused)
            }
            FocusableBox(isFocused[2], item2)
        }

        rule.setContentForTest {
            when (focusDirection) {
                Up, Down -> Column { content() }
                Left, Right -> Row { content() }
                else -> error(invalid)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(childIsFocused.value).isTrue()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun movesFocusAmongSiblingsDeepInTheFocusHierarchy_skipsDeactivatedSibling() {
        // Arrange.
        val isFocused = MutableList(3) { mutableStateOf(false) }
        val (item1, item3) = FocusRequester.createRefs()
        initialFocus = when (focusDirection) {
            Up, Left -> item3
            Down, Right -> item1
            else -> error(invalid)
        }
        val siblings = @Composable {
            FocusableBox(isFocused[0], item1)
            FocusableBox(isFocused[1], deactivated = true)
            FocusableBox(isFocused[2], item3)
        }

        rule.setContentForTest {
            FocusableBox {
                FocusableBox {
                    FocusableBox {
                        FocusableBox {
                            FocusableBox {
                                FocusableBox {
                                    when (focusDirection) {
                                        Up, Down -> Column { siblings() }
                                        Left, Right -> Row { siblings() }
                                        else -> error(invalid)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            when (focusDirection) {
                Up, Left -> assertThat(isFocused.values).isExactly(true, false, false)
                Down, Right -> assertThat(isFocused.values).isExactly(false, false, true)
                else -> error(invalid)
            }
        }
    }

    @Test
    fun movesFocusAmongSiblings_prefersSiblingToAnAncestorThatIsCloser() {
        // Arrange.
        val siblings = MutableList(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Up, Down -> Box {
                    FocusableBox(x = 0, y = 10)
                    FocusableBox(x = 0, y = 0, height = 50) {
                        FocusableBox(siblings[0], x = 0, y = 0)
                        FocusableBox(siblings[1], initialFocus, x = 0, y = 20)
                        FocusableBox(siblings[2], x = 0, y = 40)
                    }
                    FocusableBox(x = 0, y = 30)
                }
                Left, Right -> Box {
                    FocusableBox(x = 10, y = 0)
                    FocusableBox(x = 0, y = 0, width = 50) {
                        FocusableBox(siblings[0], x = 0, y = 0)
                        FocusableBox(siblings[1], initialFocus, x = 20, y = 0)
                        FocusableBox(siblings[2], x = 40, y = 0)
                    }
                    FocusableBox(x = 30, y = 0)
                }
                else -> error(invalid)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            when (focusDirection) {
                Up, Left -> assertThat(siblings.values).isExactly(true, false, false)
                Down, Right -> assertThat(siblings.values).isExactly(false, false, true)
                else -> error(invalid)
            }
        }
    }

    @Test
    fun movesFocusAmongSiblings_prefersSiblingToAnAncestorThatIsCloser_whenParentIsDeactivated() {
        // Arrange.
        val siblings = MutableList(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Up, Down -> Box {
                    FocusableBox(x = 0, y = 10)
                    FocusableBox(x = 0, y = 0, height = 50, deactivated = true) {
                        FocusableBox(siblings[0], x = 0, y = 0)
                        FocusableBox(siblings[1], initialFocus, x = 0, y = 20)
                        FocusableBox(siblings[2], x = 0, y = 40)
                    }
                    FocusableBox(x = 0, y = 30)
                }
                Left, Right -> Box {
                    FocusableBox(x = 10, y = 0)
                    FocusableBox(x = 0, y = 0, width = 50, deactivated = true) {
                        FocusableBox(siblings[0], x = 0, y = 0)
                        FocusableBox(siblings[1], initialFocus, x = 20, y = 0)
                        FocusableBox(siblings[2], x = 40, y = 0)
                    }
                    FocusableBox(x = 30, y = 0)
                }
                else -> error(invalid)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            when (focusDirection) {
                Up, Left -> assertThat(siblings.values).isExactly(true, false, false)
                Down, Right -> assertThat(siblings.values).isExactly(false, false, true)
                else -> error(invalid)
            }
        }
    }

    private fun ComposeContentTestRule.setContentForTest(composable: @Composable () -> Unit) {
        setContent {
            focusManager = LocalFocusManager.current
            composable()
        }
        rule.runOnIdle { initialFocus.requestFocus() }
    }
}

@Composable
private fun FocusableBox(
    isFocused: MutableState<Boolean> = mutableStateOf(false),
    focusRequester: FocusRequester? = null,
    x: Int = 0,
    y: Int = 0,
    width: Int = 10,
    height: Int = 10,
    deactivated: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    FocusableBox(isFocused, x, y, width, height, focusRequester, deactivated, content)
}

private val MutableList<MutableState<Boolean>>.values get() = this.map { it.value }