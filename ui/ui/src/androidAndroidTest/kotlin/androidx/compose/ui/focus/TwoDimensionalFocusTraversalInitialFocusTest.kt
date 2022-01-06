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
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
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
    lateinit var view: View
    private lateinit var focusManager: FocusManager

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(Param(Left), Param(Right), Param(Up), Param(Down))
    }

    @Test
    fun initialFocus() {
        // Arrange.
        val isFocused = MutableList(4) { mutableStateOf(false) }
        rule.setContentForTest {
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

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            when (focusDirection) {
                Up -> assertThat(isFocused.values).isExactly(false, false, false, true)
                Down -> assertThat(isFocused.values).isExactly(true, false, false, false)
                Left -> assertThat(isFocused.values).isExactly(false, false, false, true)
                Right -> assertThat(isFocused.values).isExactly(true, false, false, false)
                else -> error(invalid)
            }
        }
    }

    @Test
    fun initialFocus_DeactivatedItemIsSkipped() {
        // Arrange.
        val isFocused = MutableList(9) { mutableStateOf(false) }
        rule.setContentForTest {
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

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            when (focusDirection) {
                Up -> assertThat(isFocused.values).isExactly(
                    false, false, false,
                    false, false, false,
                    false, false, true
                )
                Down -> assertThat(isFocused.values).isExactly(
                    false, true, false,
                    false, false, false,
                    false, false, false
                )
                Left -> assertThat(isFocused.values).isExactly(
                    false, false, false,
                    false, false, false,
                    false, false, true
                )
                Right -> assertThat(isFocused.values).isExactly(
                    false, false, false,
                    true, false, false,
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
        rule.setContentForTest {
            FocusableBox(isFocused)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(isFocused.value).isTrue()
        }
    }

    @Test
    fun doesNotCrash_whenThereIsNoFocusable() {
        // Arrange.
        rule.setContentForTest {
            BasicText("Hello")
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
        }
    }

    @Test
    fun doesNotCrash_whenThereIsOneDeactivatedItem() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(deactivated = true)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
        }
    }

    private fun ComposeContentTestRule.setContentForTest(composable: @Composable () -> Unit) {
        setContent {
            view = LocalView.current
            focusManager = LocalFocusManager.current
            composable()
        }
        rule.runOnIdle { view.requestFocus() }
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
