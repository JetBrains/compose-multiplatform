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

package androidx.compose.ui.focus

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val invalid = "Not applicable to a 2D focus search."

@MediumTest
@RunWith(Parameterized::class)
class TwoDimensionalFocusTraversalInitialFocusTest(param: Param) {
    @get:Rule
    val rule = createComposeRule()

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(val focusDirection: FocusDirection) {
        override fun toString() = focusDirection.toString()
    }

    private val focusDirection = param.focusDirection

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(Param(Left), Param(Right), Param(Up), Param(Down))
    }

    @Test
    fun initialFocus() {
        // Arrange.
        lateinit var view: View
        lateinit var focusManager: FocusManager
        val isFocused = MutableList(4) { mutableStateOf(false) }
        rule.setContent {
            view = LocalView.current
            focusManager = LocalFocusManager.current
            Column {
                Row {
                    FocusableBox(isFocused[0])
                    FocusableBox(isFocused[1])
                }
                Row {
                    FocusableBox(isFocused[2])
                    FocusableBox(isFocused[3])
                }
            }
        }
        rule.runOnIdle { view.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> assertThat(isFocused.values).containsExactly(false, false, true, false)
                Down -> assertThat(isFocused.values).containsExactly(true, false, false, false)
                Left -> assertThat(isFocused.values).containsExactly(false, false, false, true)
                Right -> assertThat(isFocused.values).containsExactly(true, false, false, false)
                else -> error(invalid)
            }
        }
    }

    @Test
    fun initialFocus_DeactivatedItemIsSkipped() {
        // Arrange.
        lateinit var view: View
        lateinit var focusManager: FocusManager
        val isFocused = MutableList(9) { mutableStateOf(false) }
        rule.setContent {
            view = LocalView.current
            focusManager = LocalFocusManager.current
            Column {
                Row {
                    FocusableBox(isFocused[0], deactivated = true)
                    FocusableBox(isFocused[1])
                    FocusableBox(isFocused[2])
                }
                Row {
                    FocusableBox(isFocused[3])
                    FocusableBox(isFocused[4])
                    FocusableBox(isFocused[5])
                }
                Row {
                    FocusableBox(isFocused[6], deactivated = true)
                    FocusableBox(isFocused[7])
                    FocusableBox(isFocused[8])
                }
            }
        }
        rule.runOnIdle { view.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> assertThat(isFocused.values).containsExactly(
                    false, false, false,
                    false, false, false,
                    false, true, false
                )
                Down -> assertThat(isFocused.values).containsExactly(
                    false, true, false,
                    false, false, false,
                    false, false, false
                )
                Left -> assertThat(isFocused.values).containsExactly(
                    false, true, false,
                    false, false, false,
                    false, false, false
                )
                Right -> assertThat(isFocused.values).containsExactly(
                    false, true, false,
                    false, false, false,
                    false, false, false
                )
                else -> error(invalid)
            }
        }
    }

    @Test
    fun initialFocus_whenThereIsOnlyOneFocusable() {
        // Arrange.
        val isFocused = mutableStateOf(false)
        lateinit var view: View
        lateinit var focusManager: FocusManager
        rule.setContent {
            view = LocalView.current
            focusManager = LocalFocusManager.current
            FocusableBox(isFocused)
        }
        rule.runOnIdle { view.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle { assertThat(isFocused.value).isTrue() }
    }

    @Test
    fun doesNotCrash_whenThereIsNoFocusable() {
        // Arrange.
        lateinit var view: View
        lateinit var focusManager: FocusManager
        rule.setContent {
            view = LocalView.current
            focusManager = LocalFocusManager.current
            BasicText("Hello")
        }
        rule.runOnIdle { view.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(focusDirection) }
    }

    @Test
    fun doesNotCrash_whenThereIsOneDeactivatedItem() {
        // Arrange.
        lateinit var view: View
        lateinit var focusManager: FocusManager
        rule.setContent {
            view = LocalView.current
            focusManager = LocalFocusManager.current
            FocusableBox(deactivated = true)
        }
        rule.runOnIdle { view.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(focusDirection) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun movesFocusAmongSiblingsDeepInTheFocusHierarchy() {
        // Arrange.
        lateinit var focusManager: FocusManager
        val isFocused = MutableList(2) { mutableStateOf(false) }
        val (item1, item2) = FocusRequester.createRefs()
        val siblings = @Composable {
            FocusableBox(isFocused[0], item1)
            FocusableBox(isFocused[1], item2)
        }
        val initialFocusedItem = when (focusDirection) {
            Up, Left -> item2
            Down, Right -> item1
            else -> error(invalid)
        }
        rule.setContent {
            focusManager = LocalFocusManager.current
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
        rule.runOnIdle { initialFocusedItem.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up, Left -> assertThat(isFocused.values).containsExactly(true, false)
                Down, Right -> assertThat(isFocused.values).containsExactly(false, true)
                else -> error(invalid)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun movesFocusAmongSiblingsDeepInTheFocusHierarchy_skipsDeactivatedSibling() {
        // Arrange.
        lateinit var focusManager: FocusManager
        val isFocused = MutableList(3) { mutableStateOf(false) }
        val (item1, item3) = FocusRequester.createRefs()
        val siblings = @Composable {
            FocusableBox(isFocused[0], item1)
            FocusableBox(isFocused[1])
            FocusableBox(isFocused[2], item3)
        }
        val initialFocusedItem = when (focusDirection) {
            Up, Left -> item3
            Down, Right -> item1
            else -> error(invalid)
        }
        rule.setContent {
            focusManager = LocalFocusManager.current
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
        rule.runOnIdle { initialFocusedItem.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(focusDirection) }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up, Left -> assertThat(isFocused.values).containsExactly(true, false, false)
                Down, Right -> assertThat(isFocused.values).containsExactly(false, false, true)
                else -> error(invalid)
            }
        }
    }
}

@Composable
private fun FocusableBox(
    isFocused: MutableState<Boolean> = mutableStateOf(false),
    focusRequester: FocusRequester? = null,
    deactivated: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    FocusableBox(isFocused, 0, 0, 10, 10, focusRequester, deactivated, content)
}

private val MutableList<MutableState<Boolean>>.values get() = this.map { it.value }
