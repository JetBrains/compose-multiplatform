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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.In
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Out
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(Parameterized::class)
class LazyListFocusMoveTest(param: Param) {
    @get:Rule
    val rule = createComposeRule()

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(
        val focusDirection: FocusDirection,
        val reverseLayout: Boolean,
        val layoutDirection: LayoutDirection
    ) {
        override fun toString() = "focusDirection=$focusDirection " +
            "reverseLayout=$reverseLayout " +
            "layoutDirection=$layoutDirection"
    }

    private val focusDirection = param.focusDirection
    private val reverseLayout = param.reverseLayout
    private val layoutDirection = param.layoutDirection
    private val initiallyFocused: FocusRequester = FocusRequester()
    private var isLazyListFocused by mutableStateOf(false)
    private val isFocused = mutableMapOf<Int, Boolean>()
    private lateinit var lazyListState: LazyListState
    private lateinit var focusManager: FocusManager

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = buildList {
            for (focusDirection in listOf(Left, Right, Up, Down)) {
                for (reverseLayout in listOf(true, false)) {
                    for (layoutDirection in listOf(Ltr, Rtl)) {
                        add(Param(focusDirection, reverseLayout, layoutDirection))
                    }
                }
            }
        }
    }

    @Test
    fun moveFocusAmongVisibleItems() {
        // Arrange.
        rule.setTestContent {
            lazyList(50.dp, lazyListState) {
                item { FocusableBox(0) }
                item { FocusableBox(1, initiallyFocused) }
                item { FocusableBox(2) }
            }
        }
        rule.runOnIdle { initiallyFocused.requestFocus() }

        // Act.
        val success = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            when (focusDirection) {
                Left -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 2 else 0]).isTrue()
                    Rtl -> {
                        // Disabling this case due to b/230758535
                        // assertThat(isFocused[if (reverseLayout) 0 else 2]).isTrue()
                    }
                }
                Right -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 0 else 2]).isTrue()
                    Rtl -> {
                        // Disabling this case due to b/230758535
                        // assertThat(isFocused[if (reverseLayout) 2 else 0]).isTrue()
                    }
                }
                Up -> assertThat(isFocused[if (reverseLayout) 2 else 0]).isTrue()
                Down -> assertThat(isFocused[if (reverseLayout) 0 else 2]).isTrue()
                In -> assertThat(isFocused[1]).isTrue()
                Out -> assertThat(isLazyListFocused).isTrue()
                else -> unsupportedDirection()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsJustBeyondBounds() {
        // Arrange.
        rule.setTestContent {
            lazyList(30.dp, lazyListState) {
                items(5) { FocusableBox(it) }
                item { FocusableBox(5, initiallyFocused) }
                items(5) { FocusableBox(it + 6) }
            }
        }
        rule.runOnIdle {
            // Scroll so that the focused item is in the middle,
            // then move focus to the last visible item.
            runBlocking { lazyListState.scrollToItem(4) }
            initiallyFocused.requestFocus()
            focusManager.moveFocus(focusDirection)
        }

        // Act.
        val success = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            when (focusDirection) {
                Left -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 7 else 3]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 3 else 7]).isTrue()
                }
                Right -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 3 else 7]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 7 else 3]).isTrue()
                }
                Up -> assertThat(isFocused[if (reverseLayout) 7 else 3]).isTrue()
                Down -> assertThat(isFocused[if (reverseLayout) 3 else 7]).isTrue()
                In -> assertThat(isFocused[5]).isTrue()
                Out -> assertThat(isLazyListFocused).isTrue()
                else -> unsupportedDirection()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsFarBeyondBounds() {
        // Arrange.
        rule.setTestContent {
            lazyList(30.dp, lazyListState) {
                items(5) { FocusableBox(it) }
                items(100) { Box(Modifier.size(10.dp)) }
                item { FocusableBox(105) }
                item { FocusableBox(106, initiallyFocused) }
                item { FocusableBox(107) }
                items(100) { Box(Modifier.size(10.dp)) }
                items(5) { FocusableBox(it + 208) }
            }
        }
        rule.runOnIdle {
            // Scroll so that the focused item is in the middle,
            // then move focus to the last visible item.
            runBlocking { lazyListState.scrollToItem(105) }
            initiallyFocused.requestFocus()
            focusManager.moveFocus(focusDirection)
        }

        // Act.
        val success = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            when (focusDirection) {
                Left -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 208 else 4]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 4 else 208]).isTrue()
                }
                Right -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 4 else 208]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 208 else 4]).isTrue()
                }
                Up -> assertThat(isFocused[if (reverseLayout) 208 else 4]).isTrue()
                Down -> assertThat(isFocused[if (reverseLayout) 4 else 208]).isTrue()
                In -> assertThat(isFocused[106]).isTrue()
                Out -> assertThat(isLazyListFocused).isTrue()
                else -> unsupportedDirection()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsBeyondBoundsAndInANestedLazyList() {
        // Arrange.
        rule.setTestContent {
            lazyList(30.dp, lazyListState) {
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 0) } } }
                item { FocusableBox(3) }
                item { FocusableBox(4, initiallyFocused) }
                item { FocusableBox(5) }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 6) } } }
            }
        }
        rule.runOnIdle {
            // Scroll so that the focused item is in the middle,
            // then move focus to the last visible item.
            runBlocking { lazyListState.scrollToItem(1) }
            initiallyFocused.requestFocus()
            focusManager.moveFocus(focusDirection)
        }

        // Act.
        val success = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            when (focusDirection) {
                Left -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 8 else 0]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 2 else 6]).isTrue()
                }
                Right -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 2 else 6]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 8 else 0]).isTrue()
                }
                Up -> assertThat(isFocused[if (reverseLayout) 8 else 0]).isTrue()
                Down -> assertThat(isFocused[if (reverseLayout) 2 else 6]).isTrue()
                In -> assertThat(isFocused[4]).isTrue()
                Out -> assertThat(isLazyListFocused).isTrue()
                else -> unsupportedDirection()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsBeyondBoundsAndOutsideTheCurrentLazyList() {
        // Arrange.
        rule.setTestContent {
            lazyList(30.dp, lazyListState) {
                item { FocusableBox(0) }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 1) } } }
                item { FocusableBox(4, initiallyFocused) }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 5) } } }
                item { FocusableBox(8) }
            }
        }
        rule.runOnIdle {
            // Scroll so that the focused item is in the middle,
            // then move focus to the last visible item.
            runBlocking { lazyListState.scrollToItem(1) }
            initiallyFocused.requestFocus()
            focusManager.moveFocus(focusDirection)
        }

        // Act.
        val success = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            when (focusDirection) {
                Left -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 8 else 0]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 0 else 8]).isTrue()
                }
                Right -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 0 else 8]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 8 else 0]).isTrue()
                }
                Up -> assertThat(isFocused[if (reverseLayout) 8 else 0]).isTrue()
                Down -> assertThat(isFocused[if (reverseLayout) 0 else 8]).isTrue()
                In -> assertThat(isFocused[6]).isTrue()
                Out -> assertThat(isLazyListFocused).isTrue()
                else -> unsupportedDirection()
            }
        }
    }

    @Test
    fun moveFocusToAmongItemsInNestedLazyLists() {
        // Arrange.
        rule.setTestContent {
            lazyList(30.dp, lazyListState) {
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 0) } } }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 3) } } }
                item { FocusableBox(6, initiallyFocused) }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 7) } } }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 10) } } } }
        }
        rule.runOnIdle {
            // Scroll so that the focused item is in the middle,
            // then move focus to the last visible item.
            runBlocking { lazyListState.scrollToItem(1) }
            initiallyFocused.requestFocus()
            focusManager.moveFocus(focusDirection)
        }

        // Act.
        val success = rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(success).isTrue()
            when (focusDirection) {
                Left -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 12 else 0]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 2 else 10]).isTrue()
                }
                Right -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 2 else 10]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 12 else 0]).isTrue()
                }
                Up -> assertThat(isFocused[if (reverseLayout) 12 else 0]).isTrue()
                Down -> assertThat(isFocused[if (reverseLayout) 2 else 10]).isTrue()
                In -> assertThat(isFocused[6]).isTrue()
                Out -> assertThat(isLazyListFocused).isTrue()
                else -> unsupportedDirection()
            }
        }
    }

    @Composable
    private fun FocusableBox(index: Int, focusRequester: FocusRequester = FocusRequester()) {
        Box(
            Modifier
                .size(10.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused[index] = it.isFocused }
                .focusable()
        )
    }

    private fun ComposeContentTestRule.setTestContent(composable: @Composable () -> Unit) {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                focusManager = LocalFocusManager.current
                lazyListState = rememberLazyListState()
                composable()
            }
        }
    }

    @Composable
    private fun lazyList(
        size: Dp,
        state: LazyListState = rememberLazyListState(),
        content: LazyListScope.() -> Unit
    ) {
        when (focusDirection) {
            Left, Right, In, Out -> LazyRow(
                modifier = Modifier
                    .size(size)
                    .onFocusChanged { isLazyListFocused = it.isFocused }
                    .focusable(),
                state = state,
                reverseLayout = reverseLayout,
                content = content
            )
            Up, Down -> LazyColumn(
                modifier = Modifier
                    .size(size)
                    .onFocusChanged { isLazyListFocused = it.isFocused }
                    .focusable(),
                state = state,
                reverseLayout = reverseLayout,
                content = content
            )
            else -> unsupportedDirection()
        }
    }

    @Composable
    private fun lazyListCrossAxis(
        size: Dp,
        state: LazyListState = rememberLazyListState(),
        content: LazyListScope.() -> Unit
    ) {
        when (focusDirection) {
            Left, Right, In, Out -> LazyColumn(
                modifier = Modifier.size(size),
                state = state,
                reverseLayout = reverseLayout,
                content = content
            )
            Up, Down -> LazyRow(
                modifier = Modifier.size(size),
                state = state,
                reverseLayout = reverseLayout,
                content = content
            )
            else -> unsupportedDirection()
        }
    }

    private fun unsupportedDirection(): Nothing = error("Unsupported Focus Direction.")
}
