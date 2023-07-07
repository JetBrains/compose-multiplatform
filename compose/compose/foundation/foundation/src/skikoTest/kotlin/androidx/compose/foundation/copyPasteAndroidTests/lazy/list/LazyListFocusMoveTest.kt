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

package androidx.compose.foundation.copyPasteAndroidTests.lazy.list

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isFalse
import androidx.compose.foundation.isTrue
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.unit.dp
import kotlin.test.Ignore
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
@Ignore // TODO: the tests fail or get stuck time from time - flaky
// Fails with:
// Compose Runtime internal error.
// Unexpected or incorrect use of the Compose internal runtime API ... (flaky too, tested on desktop)
class LazyListFocusMoveTest {

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
    
    private var param: Param? = null

    private val focusDirection 
        get() = param!!.focusDirection
    
    private val reverseLayout 
        get() = param!!.reverseLayout
    
    private val layoutDirection 
        get() = param!!.layoutDirection

    private val initiallyFocused: FocusRequester = FocusRequester()
    private var isLazyListFocused by mutableStateOf(false)
    private val isFocused = mutableMapOf<Int, Boolean>()
    private lateinit var lazyListState: LazyListState
    private lateinit var focusManager: FocusManager

    private fun runParametrizedTest(test: SkikoComposeUiTest.() -> Unit) {
        initParameters().forEach {
            param = it
            runSkikoComposeUiTest {
                test()
            }
            param = null
        }
    }

    companion object {
        fun initParameters() = buildList {
            for (direction in listOf(Previous, Next, Left, Right, Up, Down, Enter, Exit)) {
                for (reverseLayout in listOf(true, false)) {
                    for (layoutDirection in listOf(Ltr, Rtl)) {
                        add(Param(direction, reverseLayout, layoutDirection))
                    }
                }
            }
        }
    }

    @Test
    fun moveFocusAmongVisibleItems() = runParametrizedTest {
        // Arrange.
        setTestContent {
            lazyList(50.dp, lazyListState) {
                item { FocusableBox(0) }
                item { FocusableBox(1, initiallyFocused) }
                item { FocusableBox(2) }
            }
        }
        runOnIdle { initiallyFocused.requestFocus() }

        // Act.
        val success = runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        runOnIdle {
            assertThat(success).apply { if (focusDirection == Enter) isFalse() else isTrue() }
            when (focusDirection) {
                Left -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 2 else 0]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 0 else 2]).isTrue()
                }
                Right -> when (layoutDirection) {
                    Ltr -> assertThat(isFocused[if (reverseLayout) 0 else 2]).isTrue()
                    Rtl -> assertThat(isFocused[if (reverseLayout) 2 else 0]).isTrue()
                }
                Up -> assertThat(isFocused[if (reverseLayout) 2 else 0]).isTrue()
                Down -> assertThat(isFocused[if (reverseLayout) 0 else 2]).isTrue()
                Previous -> assertThat(isFocused[0]).isTrue()
                Next -> assertThat(isFocused[2]).isTrue()
                Enter -> assertThat(isFocused[1]).isTrue()
                Exit -> assertThat(isLazyListFocused).isTrue()
                else -> unsupportedDirection()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsJustBeyondBounds() = runParametrizedTest {
        // Arrange.
        setTestContent {
            lazyList(30.dp, lazyListState) {
                items(5) { FocusableBox(it) }
                item { FocusableBox(5, initiallyFocused) }
                items(5) { FocusableBox(it + 6) }
            }
        }
        runOnIdle {
            // Scroll so that the focused item is in the middle.
            scope.launch { lazyListState.scrollToItem(4) }

            // Move focus to the last visible item.
            initiallyFocused.requestFocus()
            when (focusDirection) {
                Left, Right, Up, Down, Previous, Next -> focusManager.moveFocus(focusDirection)
                Enter, Exit -> { /* Do nothing */ }
                else -> unsupportedDirection()
            }
        }

        // Act.
        val success = runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        runOnIdle {
            assertThat(success).apply { if (focusDirection == Enter) isFalse() else isTrue() }
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
                Previous -> assertThat(isFocused[3]).isTrue()
                Next -> assertThat(isFocused[7]).isTrue()
                Enter -> assertThat(isFocused[5]).isTrue()
                Exit -> assertThat(isLazyListFocused).isTrue()
                else -> unsupportedDirection()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsFarBeyondBounds() = runParametrizedTest {
        // Arrange.
        lateinit var scope: CoroutineScope
        setTestContent {
            scope = rememberCoroutineScope()
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
        waitForIdle()
        // Scroll so that the focused item is in the middle.
        scope.launch { lazyListState.scrollToItem(105) }
        runOnIdle {
            initiallyFocused.requestFocus()

            // Move focus to the last visible item.
            when (focusDirection) {
                Left, Right, Up, Down, Previous, Next -> focusManager.moveFocus(focusDirection)
                Enter, Exit -> { /* Do nothing */ }
                else -> unsupportedDirection()
            }
        }
        // Act.
        val success = runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        runOnIdle {
            assertThat(success).apply { if (focusDirection == Enter) isFalse() else isTrue() }
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
                Previous -> assertThat(isFocused[4]).isTrue()
                Next -> assertThat(isFocused[208]).isTrue()
                Enter -> assertThat(isFocused[106]).isTrue()
                Exit -> assertThat(isLazyListFocused).isTrue()
                else -> unsupportedDirection()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsBeyondBoundsAndInANestedLazyList() = runParametrizedTest {
        // Arrange.
        setTestContent {
            lazyList(30.dp, lazyListState) {
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 0) } } }
                item { FocusableBox(3) }
                item { FocusableBox(4, initiallyFocused) }
                item { FocusableBox(5) }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 6) } } }
            }
        }
        runOnIdle {
            // Scroll so that the focused item is in the middle.
            scope.launch { lazyListState.scrollToItem(1) }
            initiallyFocused.requestFocus()

            // Move focus to the last visible item.
            when (focusDirection) {
                Left, Right, Up, Down, Previous, Next -> focusManager.moveFocus(focusDirection)
                Enter, Exit -> { /* Do nothing */ }
                else -> unsupportedDirection()
            }
        }

        // Act.
        val success = runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        runOnIdle {
            assertThat(success).apply { if (focusDirection == Enter) isFalse() else isTrue() }
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
                Previous -> assertThat(isFocused[2]).isTrue()
                Next -> assertThat(isFocused[6]).isTrue()
                Enter -> assertThat(isFocused[4]).isTrue()
                Exit -> assertThat(isLazyListFocused).isTrue()
                else -> unsupportedDirection()
            }
        }
    }

    @Test
    fun moveFocusToItemThatIsBeyondBoundsAndOutsideTheCurrentLazyList() = runParametrizedTest {
        // Arrange.
        setTestContent {
            lazyList(30.dp, lazyListState) {
                item { FocusableBox(0) }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 1) } } }
                item { FocusableBox(4, initiallyFocused) }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 5) } } }
                item { FocusableBox(8) }
            }
        }
        runOnIdle {
            // Scroll so that the focused item is in the middle.
            scope.launch { lazyListState.scrollToItem(1) }
            initiallyFocused.requestFocus()

            // Move focus to the last visible item.
            when (focusDirection) {
                Left, Right, Up, Down -> focusManager.moveFocus(focusDirection)
                Previous, Next -> repeat(3) { focusManager.moveFocus(focusDirection) }
                Enter, Exit -> { /* Do nothing */ }
                else -> unsupportedDirection()
            }
        }

        // Act.
        val success = runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        runOnIdle {
            assertThat(success).apply { if (focusDirection == Enter) isFalse() else isTrue() }
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
                Previous -> assertThat(isFocused[0]).isTrue()
                Next -> assertThat(isFocused[8]).isTrue()
                Enter -> assertThat(isFocused[4]).isTrue()
                Exit -> assertThat(isLazyListFocused).isTrue()
                else -> unsupportedDirection()
            }
        }
    }

    @Test
    fun moveFocusAmongNestedLazyLists() = runParametrizedTest {
        // Arrange.
        setTestContent {
            lazyList(30.dp, lazyListState) {
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 0) } } }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 3) } } }
                item { FocusableBox(6, initiallyFocused) }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 7) } } }
                item { lazyListCrossAxis(30.dp) { items(3) { FocusableBox(it + 10) } } }
            }
        }
        runOnIdle {
            // Scroll so that the focused item is in the middle.
            scope.launch { lazyListState.scrollToItem(1) }
            initiallyFocused.requestFocus()

            // Move focus to the last visible item.
            when (focusDirection) {
                Left, Right, Up, Down -> focusManager.moveFocus(focusDirection)
                Previous, Next -> repeat(3) { focusManager.moveFocus(focusDirection) }
                Enter, Exit -> { /* Do nothing */ }
                else -> unsupportedDirection()
            }
        }

        // Act.
        val success = runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        runOnIdle {
            assertThat(success).apply { if (focusDirection == Enter) isFalse() else isTrue() }
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
                Previous -> assertThat(isFocused[2]).isTrue()
                Next -> assertThat(isFocused[10]).isTrue()
                Enter -> assertThat(isFocused[6]).isTrue()
                Exit -> assertThat(isLazyListFocused).isTrue()
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

    private lateinit var scope: CoroutineScope

    private fun SkikoComposeUiTest.setTestContent (composable: @Composable () -> Unit) {
        setContent {
            scope = rememberCoroutineScope()
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
            Left, Right, Enter, Exit, Next, Previous -> LazyRow(
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
            Left, Right, Enter, Exit, Next, Previous -> LazyColumn(
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
