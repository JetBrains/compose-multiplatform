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
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.containsAtLeast
import androidx.compose.foundation.containsExactly
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isEmpty
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
class FocusableBoundsTest {

    private lateinit var parentCoordinates: LayoutCoordinates
    private val focusedBounds = mutableListOf<Rect?>()
    private val size = 10f
    private val sizeDp = with(Density(1f)) { 10f.toDp() }

    @Test
    fun onFocusedBoundsPositioned_notified_whenChildGainsFocus() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        setContent {
            Box(
                Modifier
                    .onGloballyPositioned { parentCoordinates = it }
                    .onFocusedBoundsChanged { childCoordinates ->
                        focusedBounds +=
                            childCoordinates?.let { parentCoordinates.localBoundingBoxOf(it) }
                    }
                    .focusRequester(focusRequester)
                    .focusable()
                    // Needs a size to participate in layout.
                    .size(sizeDp)
            )
        }

        runOnIdle {
            assertThat(focusedBounds).isEmpty()
            focusRequester.requestFocus()
        }

        runOnIdle {
            assertThat(focusedBounds).containsExactly(Rect(0f, 0f, size, size))
        }
    }

    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusMovesBetweenChildren() = runSkikoComposeUiTest {
        val (focusRequester1, focusRequester2) = FocusRequester.createRefs()
        setContent {
            Column(
                Modifier
                    .onGloballyPositioned { parentCoordinates = it }
                    .onFocusedBoundsChanged { childCoordinates ->
                        focusedBounds +=
                            childCoordinates?.let { parentCoordinates.localBoundingBoxOf(it) }
                    }
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester1)
                        .focusable()
                        // Needs a size to participate in layout.
                        .size(sizeDp)
                )
                Box(
                    Modifier
                        .focusRequester(focusRequester2)
                        .focusable()
                        // Needs a size to participate in layout.
                        .size(sizeDp)
                )
            }
        }

        runOnIdle {
            focusRequester1.requestFocus()
        }
        runOnIdle {
            focusRequester2.requestFocus()
        }

        runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size),
                // First child sends null when it loses focus before the second child gains it.
                null,
                Rect(0f, size, size, size * 2)
            )
        }
    }

    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusedBoundsMoves() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var childOffset by mutableStateOf(IntOffset.Zero)

        setContent {
            Box(
                Modifier
                    .onGloballyPositioned { parentCoordinates = it }
                    .onFocusedBoundsChanged { childCoordinates ->
                        focusedBounds +=
                            childCoordinates?.let {
                                parentCoordinates.localBoundingBoxOf(it, clipBounds = false)
                            }
                    }
                    .size(sizeDp)
                    .wrapContentSize(unbounded = true)
            ) {
                Box(
                    Modifier
                        // Needs a size to participate in layout.
                        .offset { childOffset }
                        .focusRequester(focusRequester)
                        .focusable()
                        .size(sizeDp)
                )
            }
        }

        runOnIdle {
            focusRequester.requestFocus()
        }

        runOnIdle {
            childOffset += IntOffset(1, 2)
        }

        runOnIdle {
            assertThat(focusedBounds).containsAtLeast(
                Rect(Offset.Zero, Size(size, size)),
                Rect(Offset(1f, 2f), Size(size, size))
            )
        }
    }

    @Test
    fun onFocusedChildPositioned_notNotified_whenFocusableChildEntersComposition() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var includeFocusableModifier by mutableStateOf(false)
        setContent {
            Box(
                Modifier
                    .onGloballyPositioned { parentCoordinates = it }
                    .onFocusedBoundsChanged { childCoordinates ->
                        focusedBounds +=
                            childCoordinates?.let { parentCoordinates.localBoundingBoxOf(it) }
                    }
                    .focusRequester(focusRequester)
                    .then(if (includeFocusableModifier) Modifier.focusable() else Modifier)
                    // Needs a size to participate in layout.
                    .size(sizeDp)
            )
        }
        runOnIdle {
            focusRequester.requestFocus()
        }

        runOnIdle {
            includeFocusableModifier = true
        }

        runOnIdle {
            assertThat(focusedBounds).isEmpty()
        }
    }
    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusedBoundsLeavesComposition() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var includeFocusableModifier by mutableStateOf(true)
        setContent {
            Box(
                Modifier
                    .onGloballyPositioned { parentCoordinates = it }
                    .onFocusedBoundsChanged { childCoordinates ->
                        focusedBounds +=
                            childCoordinates?.let { parentCoordinates.localBoundingBoxOf(it) }
                    }
                    .focusRequester(focusRequester)
                    .then(if (includeFocusableModifier) Modifier.focusable() else Modifier)
                    // Needs a size to participate in layout.
                    .size(sizeDp)
            )
        }
        runOnIdle {
            focusRequester.requestFocus()
        }

        runOnIdle {
            includeFocusableModifier = false
        }

        runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size),
                null
            )
        }
    }

    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusedBoundsIsDisabled() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var focusableEnabled by mutableStateOf(true)
        setContent {
            Box(
                Modifier
                    .onGloballyPositioned { parentCoordinates = it }
                    .onFocusedBoundsChanged { childCoordinates ->
                        focusedBounds +=
                            childCoordinates?.let { parentCoordinates.localBoundingBoxOf(it) }
                    }
                    .focusRequester(focusRequester)
                    .focusable(enabled = focusableEnabled)
                    // Needs a size to participate in layout.
                    .size(sizeDp)
            )
        }

        runOnIdle {
            focusRequester.requestFocus()
        }

        runOnIdle {
            focusableEnabled = false
        }

        runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size),
                null
            )
        }
    }

    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusCleared() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        lateinit var focusManager: FocusManager
        setContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier
                    .onGloballyPositioned { parentCoordinates = it }
                    .onFocusedBoundsChanged { childCoordinates ->
                        focusedBounds +=
                            childCoordinates?.let { parentCoordinates.localBoundingBoxOf(it) }
                    }
                    .focusRequester(focusRequester)
                    .focusable()
                    // Needs a size to participate in layout.
                    .size(sizeDp)
            )
        }

        runOnIdle {
            focusRequester.requestFocus()
        }
        runOnIdle {
            focusManager.clearFocus()
        }

        runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size),
                null
            )
        }
    }

    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusMovesOutsideObserver() = runSkikoComposeUiTest {
        val (focusRequester1, focusRequester2) = FocusRequester.createRefs()
        setContent {
            Column {
                Box(
                    Modifier
                        .onGloballyPositioned { parentCoordinates = it }
                        .onFocusedBoundsChanged { childCoordinates ->
                            focusedBounds +=
                                childCoordinates?.let { parentCoordinates.localBoundingBoxOf(it) }
                        }
                        .focusRequester(focusRequester1)
                        .focusable()
                        // Needs a size to participate in layout.
                        .size(sizeDp)
                )
                Box(
                    Modifier
                        .focusRequester(focusRequester2)
                        .focusable()
                        // Needs a size to participate in layout.
                        .size(sizeDp)
                )
            }
        }

        runOnIdle {
            focusRequester1.requestFocus()
        }
        runOnIdle {
            focusRequester2.requestFocus()
        }

        runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size),
                null
            )
        }
    }

    @Test
    fun onFocusedBoundsPositioned_notified_whenMultipleObservers() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        val focusedBounds = mutableListOf<Pair<Int, Rect?>>()

        setContent {
            Box(
                Modifier
                    .onGloballyPositioned { parentCoordinates = it }
                    .onFocusedBoundsChanged { childCoordinates ->
                        focusedBounds += Pair(
                            0,
                            childCoordinates?.let { parentCoordinates.localBoundingBoxOf(it) }
                        )
                    }
                    .onFocusedBoundsChanged { childCoordinates ->
                        focusedBounds += Pair(
                            1,
                            childCoordinates?.let { parentCoordinates.localBoundingBoxOf(it) }
                        )
                    }
                    .focusRequester(focusRequester)
                    .focusable()
                    // Needs a size to participate in layout.
                    .size(sizeDp)
            )
        }

        runOnIdle {
            focusRequester.requestFocus()
        }

        runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Pair(1, Rect(0f, 0f, size, size)),
                Pair(0, Rect(0f, 0f, size, size)),
            )
        }
    }

    @Test
    fun onFocusedBoundsPositioned_notified_whenAddedToParentWithAlreadyFocusedBounds() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        var includeObserver by mutableStateOf(false)

        setContent {
            Box(
                Modifier
                    .onGloballyPositioned { parentCoordinates = it }
                    .then(
                        if (includeObserver) {
                            Modifier.onFocusedBoundsChanged { childCoordinates ->
                                focusedBounds +=
                                    childCoordinates?.let {
                                        parentCoordinates.localBoundingBoxOf(it)
                                    }
                            }
                        } else Modifier
                    )
                    .focusRequester(focusRequester)
                    .focusable()
                    // Needs a size to participate in layout.
                    .size(sizeDp)
            )
        }

        runOnIdle {
            focusRequester.requestFocus()
        }

        runOnIdle {
            assertThat(focusedBounds).isEmpty()
            includeObserver = true
        }

        runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size)
            )
        }
    }

    @Test
    fun onFocusedBoundsPositioned_notified_whenNewObserverAddedAboveExisting() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        val focusedBounds = mutableListOf<Pair<Int, Rect?>>()
        var includeSecondObserver by mutableStateOf(false)

        setContent {
            Box(
                Modifier
                    .onGloballyPositioned { parentCoordinates = it }
                    .then(
                        if (includeSecondObserver) {
                            Modifier.onFocusedBoundsChanged { childCoordinates ->
                                focusedBounds += Pair(
                                    0,
                                    childCoordinates?.let {
                                        parentCoordinates.localBoundingBoxOf(it)
                                    }
                                )
                            }
                        } else Modifier
                    )
                    .onFocusedBoundsChanged { childCoordinates ->
                        focusedBounds += Pair(
                            1,
                            childCoordinates?.let {
                                parentCoordinates.localBoundingBoxOf(it)
                            }
                        )
                    }
                    .focusRequester(focusRequester)
                    .focusable()
                    // Needs a size to participate in layout.
                    .size(sizeDp)
            )
        }

        runOnIdle {
            focusRequester.requestFocus()
        }

        runOnIdle {
            includeSecondObserver = true
        }

        runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Pair(1, Rect(0f, 0f, size, size)),
                Pair(1, Rect(0f, 0f, size, size)),
                Pair(0, Rect(0f, 0f, size, size)),
            )
        }
    }

    @Test
    fun onFocusedBoundsPositioned_notified_whenNewObserverAddedBelowExisting() = runSkikoComposeUiTest {
        val focusRequester = FocusRequester()
        val focusedBounds = mutableListOf<Pair<Int, Rect?>>()
        var includeSecondObserver by mutableStateOf(false)

        setContent {
            Box(
                Modifier
                    .onGloballyPositioned { parentCoordinates = it }
                    .onFocusedBoundsChanged { childCoordinates ->
                        focusedBounds += Pair(
                            0,
                            childCoordinates?.let {
                                parentCoordinates.localBoundingBoxOf(it)
                            }
                        )
                    }
                    .then(
                        if (includeSecondObserver) {
                            Modifier.onFocusedBoundsChanged { childCoordinates ->
                                focusedBounds += Pair(
                                    1,
                                    childCoordinates?.let {
                                        parentCoordinates.localBoundingBoxOf(it)
                                    }
                                )
                            }
                        } else Modifier
                    )
                    .focusRequester(focusRequester)
                    .focusable()
                    // Needs a size to participate in layout.
                    .size(sizeDp)
            )
        }

        runOnIdle {
            focusRequester.requestFocus()
        }

        runOnIdle {
            includeSecondObserver = true
        }

        runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Pair(0, Rect(0f, 0f, size, size)),
                Pair(1, Rect(0f, 0f, size, size)),
                Pair(0, Rect(0f, 0f, size, size)),
            )
        }
    }
}
