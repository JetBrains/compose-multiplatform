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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.layout.RelocationRequester
import androidx.compose.ui.layout.relocationRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class FocusInScrollableRowTest(private val reverseScrolling: Boolean) {
    @get:Rule
    val rule = createComposeRule()
    val itemSize = with(rule.density) { 100.toDp() }
    lateinit var scrollState: ScrollState
    lateinit var focusManager: FocusManager

    @Test
    fun focusingOnVisibleItemDoesNotScroll() {
        // Arrange.
        val visibleItem = FocusRequester()
        rule.setContentForTest {
            ScrollableRow(Modifier.size(itemSize * 3, itemSize)) {
                FocusableBox(Modifier.size(itemSize))
                FocusableBox(Modifier.size(itemSize))
                FocusableBox(Modifier.size(itemSize).focusRequester(visibleItem))
                FocusableBox(Modifier.size(itemSize))
                FocusableBox(Modifier.size(itemSize))
            }
        }

        // Act.
        rule.runOnIdle { visibleItem.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(scrollState.value).isEqualTo(0) }
    }

    @Test
    fun focusingOutOfBoundsItem_bringsItIntoView() {
        // Arrange.
        val outOfBoundsItem = FocusRequester()
        rule.setContentForTest {
            ScrollableRow(Modifier.size(itemSize * 2, itemSize)) {
                FocusableBox(Modifier.size(itemSize))
                FocusableBox(Modifier.size(itemSize))
                FocusableBox(Modifier.size(itemSize).focusRequester(outOfBoundsItem))
                FocusableBox(Modifier.size(itemSize))
                FocusableBox(Modifier.size(itemSize))
            }
        }

        // Act.
        rule.runOnIdle { outOfBoundsItem.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(scrollState.value).isEqualTo(100) }
    }

    @Test
    fun moveOutFromBoundaryItem_bringsNextItemIntoView() {
        // Arrange.
        val itemOnBoundary = FocusRequester()
        rule.setContentForTest {
            ScrollableRow(Modifier.size(itemSize * 2, itemSize)) {
                FocusableBox(Modifier.size(itemSize))
                FocusableBox(Modifier.size(itemSize).focusRequester(itemOnBoundary))
                FocusableBox(Modifier.size(itemSize))
            }
        }
        rule.runOnIdle { itemOnBoundary.requestFocus() }

        // Act.
        rule.runOnIdle { focusManager.moveFocus(if (reverseScrolling) Left else Right) }

        // Assert.
        rule.runOnIdle { assertThat(scrollState.value).isEqualTo(100) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "reverseScrolling = {0}")
        fun initParameters() = listOf(true, false)
    }

    @Composable
    private fun ScrollableRow(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = modifier.horizontalScroll(
                state = scrollState,
                reverseScrolling = reverseScrolling
            ),
            content = content
        )
    }

    private fun ComposeContentTestRule.setContentForTest(
        composable: @Composable () -> Unit
    ) {
        setContent {
            scrollState = rememberScrollState()
            focusManager = LocalFocusManager.current
            composable()
        }
    }
}

@Composable
private fun FocusableBox(modifier: Modifier = Modifier) {
    Box(modifier.focusableWithRelocation())
}

// This is a hel function that users will have to use until bringIntoView is added to
// Modifier.focusable()
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.focusableWithRelocation() = composed {
    val relocationRequester = remember { RelocationRequester() }
    val coroutineScope = rememberCoroutineScope()
    Modifier
        .relocationRequester(relocationRequester)
        .onFocusChanged {
            if (it.isFocused) {
                coroutineScope.launch { relocationRequester.bringIntoView() }
            }
        }
        .focusable()
}
