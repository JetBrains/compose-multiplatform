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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isTrue
import androidx.compose.foundation.isFalse
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
class FocusGroupTest {

    private val initialFocus = FocusRequester()
    private lateinit var focusManager: FocusManager

    @Test
    fun focusGroup_withNonFocusableContent_isNotFocusable() = runSkikoComposeUiTest {
        // Arrange.
        val focusRequester = FocusRequester()
        var isFocused = false
        setContent {
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
        runOnIdle { focusRequester.requestFocus() }

        // Assert.
        runOnIdle { assertThat(isFocused).isFalse() }
    }

    @Test
    fun focusGroup_withFocusableContent_isNotFocusable() = runSkikoComposeUiTest {
        // Arrange.
        val focusRequester = FocusRequester()
        var isFocused = false
        setContent {
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
        runOnIdle { focusRequester.requestFocus() }

        // Assert.
        runOnIdle { assertThat(isFocused).isFalse() }
    }

    @Test
    fun focusGroup_canBeMadeFocusableUsingFocusProperties() = runSkikoComposeUiTest {
        // Arrange.
        val focusRequester = FocusRequester()
        var isFocused = false
        setContent {
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
        runOnIdle { focusRequester.requestFocus() }

        // Assert.
        runOnIdle { assertThat(isFocused).isTrue() }
    }

    @Test
    fun itemWithFocusGroup_canBeMadeFocusable() = runSkikoComposeUiTest {
        // Arrange.
        val focusRequester = FocusRequester()
        var isFocused = false
        var internalIsFocused = false
        setContent {
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
        runOnIdle { focusRequester.requestFocus() }

        // Assert.
        runOnIdle {
            assertThat(isFocused).isTrue()
            assertThat(internalIsFocused).isFalse()
        }
    }

    @Test
    fun oneDimensionalFocusSearch_traversesCurrentFocusGroupBeforeNextGroup() = runSkikoComposeUiTest {
        // Arrange.
        lateinit var expectedFocus: FocusState
        setContentWithInitialFocus {
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
        runOnIdle { focusManager.moveFocus(Next) }

        // Assert.
        runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun oneDimensionalFocusSearch_flowsOverToNextGroup() = runSkikoComposeUiTest {
        // Arrange.
        lateinit var expectedFocus: FocusState
        setContentWithInitialFocus {
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
        runOnIdle { focusManager.moveFocus(Next) }

        // Assert.
        runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun oneDimensionalFocusSearch_flowsOverToNextFocusGroupParent() = runSkikoComposeUiTest {
        // Arrange.
        lateinit var expectedFocus: FocusState
        setContentWithInitialFocus {
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
        runOnIdle { focusManager.moveFocus(Next) }

        // Assert.
        runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun oneDimensionalFocusSearch_wrapsAroundToFirstGroup() = runSkikoComposeUiTest {
        // Arrange.
        lateinit var expectedFocus: FocusState
        setContentWithInitialFocus {
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
        runOnIdle { focusManager.moveFocus(Next) }

        // Assert.
        runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun twoDimensionalFocusSearch_Enter() = runSkikoComposeUiTest {
        // Arrange.
        lateinit var expectedFocus: FocusState
        setContentWithInitialFocus {
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
        runOnIdle { focusManager.moveFocus(Enter) }

        // Assert.
        runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun twoDimensionalFocusSearch_Exit() = runSkikoComposeUiTest {
        // Arrange.
        lateinit var expectedFocus: FocusState
        setContentWithInitialFocus {
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
        runOnIdle { focusManager.moveFocus(Exit) }

        // Assert.
        runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun twoDimensionalFocusSearch_skipsChildrenIfTheyAreNotInDirectionOfSearch() = runSkikoComposeUiTest {
        // Arrange.
        lateinit var expectedFocus: FocusState
        setContentWithInitialFocus {
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
        runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun twoDimensionalFocusSearch_returnsNextFocusGroupParentIfItIsFocusable() = runSkikoComposeUiTest {
        // Arrange.
        lateinit var expectedFocus: FocusState
        setContentWithInitialFocus {
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
        runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    @Test
    fun twoDimensionalFocusSearch_acrossFocusableFocusGroups_skipsChildren() = runSkikoComposeUiTest {
        // Arrange.
        lateinit var expectedFocus: FocusState
        setContentWithInitialFocus {
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
        val success = runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        runOnIdle {
            assertThat(success).isTrue()
            assertThat(expectedFocus.isFocused).isTrue()
        }
    }

    @Test
    fun twoDimensionalFocusSearch_acrossFocusableFocusGroups_whenChildIsInitiallyFocused() = runSkikoComposeUiTest {
        // Arrange.
        lateinit var expectedFocus: FocusState
        setContentWithInitialFocus {
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
        runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        runOnIdle { assertThat(expectedFocus.isFocused).isTrue() }
    }

    private fun SkikoComposeUiTest.setContentWithInitialFocus(content: @Composable () -> Unit) {
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
