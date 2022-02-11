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

package androidx.compose.foundation

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.In
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Out
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusGroupTest {
    @get:Rule
    val rule = createComposeRule()

    private val initialFocus = FocusRequester()
    private lateinit var focusManager: FocusManager

    @Test
    fun focusGroup_withNonFocusableContent_isNotFocusable() {
        // Arrange.
        val focusRequester = FocusRequester()
        var isFocused = false
        rule.setContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusGroup()
            ) {
                BasicText("Non Focusable Item")
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(isFocused).isFalse() }
    }

    @Test
    fun focusGroup_withFocusableContent_isNotFocusable() {
        // Arrange.
        val focusRequester = FocusRequester()
        var isFocused = false
        rule.setContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusGroup()
            ) {
                Box(Modifier.focusable())
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(isFocused).isFalse() }
    }

    @Test
    fun focusGroup_canBeMadeFocusableUsingFocusProperties() {
        // Arrange.
        val focusRequester = FocusRequester()
        var isFocused = false
        rule.setContent {
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusProperties { canFocus = true }
                    .focusGroup()
            ) {
                BasicText("Non Focusable Item")
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(isFocused).isTrue() }
    }

    @Test
    fun itemWithfocusGroup_canBeMadeFocusable() {
        // Arrange.
        val focusRequester = FocusRequester()
        var isFocused = false
        var internalIsFocused = false
        rule.setContent {
            BoxWithFocusGroup(
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .onFocusChanged { internalIsFocused = it.isFocused }
            ) {
                BasicText("Non Focusable Item")
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(isFocused).isTrue()
            assertThat(internalIsFocused).isFalse()
        }
    }

    @Test
    fun oneDimensionalFocusSearch_traversesCurrentFocusGroupBeforeNextGroup() {
        // Arrange.
        lateinit var expectedFocus: FocusState
        rule.setContentWithInitialFocus {
            Row {
                Column(Modifier.focusGroup()) {
                    Box(Modifier.focusRequester(initialFocus).focusable())
                    Box(Modifier.onFocusChanged { expectedFocus = it }.focusable())
                }
                Column(Modifier.focusGroup()) {
                    Box(Modifier.focusable())
                    Box(Modifier.focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(Next) }

        // Assert.
        rule.runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun oneDimensionalFocusSearch_flowsOverToNextGroup() {
        // Arrange.
        lateinit var expectedFocus: FocusState
        rule.setContentWithInitialFocus {
            Row {
                Column(Modifier.focusGroup()) {
                    Box(Modifier.focusable())
                    Box(Modifier.focusRequester(initialFocus).focusable())
                }
                Column(Modifier.focusGroup()) {
                    Box(Modifier.onFocusChanged { expectedFocus = it }.focusable())
                    Box(Modifier.focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(Next) }

        // Assert.
        rule.runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun oneDimensionalFocusSearch_flowsOverToNextFocusGroupParent() {
        // Arrange.
        lateinit var expectedFocus: FocusState
        rule.setContentWithInitialFocus {
            Row {
                Column(Modifier.focusGroup()) {
                    Box(Modifier.focusable())
                    Box(Modifier.focusRequester(initialFocus).focusable())
                }
                Column(
                    Modifier
                        .onFocusChanged { expectedFocus = it }
                        .focusProperties { canFocus = true }
                        .focusGroup()
                ) {
                    Box(Modifier.focusable())
                    Box(Modifier.focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(Next) }

        // Assert.
        rule.runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun oneDimensionalFocusSearch_wrapsAroundToFirstGroup() {
        // Arrange.
        lateinit var expectedFocus: FocusState
        rule.setContentWithInitialFocus {
            Row {
                Column(Modifier.focusGroup()) {
                    Box(Modifier.onFocusChanged { expectedFocus = it }.focusable())
                    Box(Modifier.focusable())
                }
                Column(Modifier.focusGroup()) {
                    Box(Modifier.focusable())
                    Box(Modifier.focusRequester(initialFocus).focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(Next) }

        // Assert.
        rule.runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun twoDimensionalFocusSearch_In() {
        // Arrange.
        lateinit var expectedFocus: FocusState
        rule.setContentWithInitialFocus {
            Box(
                Modifier
                    .focusRequester(initialFocus)
                    .focusProperties { canFocus = true }
                    .focusGroup()
            ) {
                    Box(
                        Modifier
                            .onFocusChanged { expectedFocus = it }
                            .focusable()
                    )
            }
        }

        // Act.
        @OptIn(ExperimentalComposeUiApi::class)
        rule.runOnIdle { focusManager.moveFocus(In) }

        // Assert.
        rule.runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun twoDimensionalFocusSearch_Out() {
        // Arrange.
        lateinit var expectedFocus: FocusState
        rule.setContentWithInitialFocus {
            Box(
                Modifier
                    .onFocusChanged { expectedFocus = it }
                    .focusProperties { canFocus = true }
                    .focusGroup()
            ) {
                Box(
                    Modifier
                        .focusRequester(initialFocus)
                        .focusable()
                )
            }
        }

        // Act.
        @OptIn(ExperimentalComposeUiApi::class)
        rule.runOnIdle { focusManager.moveFocus(Out) }

        // Assert.
        rule.runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun twoDimensionalFocusSearch_skipsChildrenIfTheyAreNotInDirectionOfSearch() {
        // Arrange.
        lateinit var expectedFocus: FocusState
        rule.setContentWithInitialFocus {
            Row {
                Column(Modifier.focusGroup()) {
                    Box(Modifier.focusRequester(initialFocus).focusable())
                    Box(Modifier.focusable())
                }
                Column(Modifier.focusGroup()) {
                    Box(Modifier.onFocusChanged { expectedFocus = it }.focusable())
                    Box(Modifier.focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        rule.runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun twoDimensionalFocusSearch_returnsNextFocusGroupParentIfItIsFocusable() {
        // Arrange.
        lateinit var expectedFocus: FocusState
        rule.setContentWithInitialFocus {
            Row {
                Column(Modifier.focusGroup()) {
                    Box(Modifier.focusRequester(initialFocus).focusable())
                    Box(Modifier.focusable())
                }
                Column(
                    Modifier
                        .onFocusChanged { expectedFocus = it }
                        .focusProperties { canFocus = true }
                        .focusGroup()
                ) {
                    Box(Modifier.focusable())
                    Box(Modifier.focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        rule.runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun twoDimensionalFocusSearch_acrossFocusableFocusGroups_skipsChildren() {
        // Arrange.
        lateinit var expectedFocus: FocusState
        rule.setContentWithInitialFocus {
            Row {
                Column(
                    Modifier
                        .focusRequester(initialFocus)
                        .focusProperties { canFocus = true }
                        .focusGroup()
                ) {
                    Box(Modifier.focusable())
                }
                Column(
                    Modifier
                        .onFocusChanged { expectedFocus = it }
                        .focusProperties { canFocus = true }
                        .focusGroup()
                ) {
                    Box(Modifier.focusable())
                }
            }
        }

        // Act.
        val success = rule.runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            assertThat(expectedFocus.isFocused).isTrue()
        }
    }

    @Test
    fun twoDimensionalFocusSearch_acrossFocusableFocusGroups_whenChildIsInitiallyFocused() {
        // Arrange.
        lateinit var expectedFocus: FocusState
        rule.setContentWithInitialFocus {
            Row {
                Column(
                    Modifier
                        .focusProperties { canFocus = true }
                        .focusGroup()
                ) {
                    Box(Modifier.focusRequester(initialFocus).focusable())
                    Box(Modifier.focusable())
                }
                Column(
                    Modifier
                        .onFocusChanged { expectedFocus = it }
                        .focusProperties { canFocus = true }
                        .focusGroup()
                ) {
                    Box(Modifier.focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        rule.runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    private fun ComposeContentTestRule.setContentWithInitialFocus(content: @Composable () -> Unit) {
        setContent {
            focusManager = LocalFocusManager.current
            content()
        }
        runOnIdle { initialFocus.requestFocus() }
    }

    // Using this helper function because both initial focus (From Android) and focus search need
    // the items to have a non-zero size.
    @Composable
    private fun Box(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit = {}
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = modifier.size(10.dp),
            content = content
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    @Composable
    private inline fun BoxWithFocusGroup(
        modifier: Modifier = Modifier,
        noinline content: @Composable BoxScope.() -> Unit = {}
    ) {
        @OptIn(ExperimentalFoundationApi::class)
        Box(modifier.focusGroup(), content)
    }
}