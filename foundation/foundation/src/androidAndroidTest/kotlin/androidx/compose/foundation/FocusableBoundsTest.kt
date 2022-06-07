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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusableBoundsTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var parentCoordinates: LayoutCoordinates
    private val focusedBounds = mutableListOf<Rect?>()
    private val size = 10f
    private val sizeDp = with(rule.density) { 10f.toDp() }

    @Ignore // b/222529358
    @Test
    fun onFocusedBoundsPositioned_notified_whenChildGainsFocus() {
        val focusRequester = FocusRequester()
        rule.setContent {
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

        rule.runOnIdle {
            assertThat(focusedBounds).isEmpty()
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(focusedBounds).containsExactly(Rect(0f, 0f, size, size))
        }
    }

    @Ignore // b/222529358
    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusMovesBetweenChildren() {
        val (focusRequester1, focusRequester2) = FocusRequester.createRefs()
        rule.setContent {
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

        rule.runOnIdle {
            focusRequester1.requestFocus()
        }
        rule.runOnIdle {
            focusRequester2.requestFocus()
        }

        rule.runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size),
                // First child sends null when it loses focus before the second child gains it.
                null,
                Rect(0f, size, size, size * 2)
            ).inOrder()
        }
    }

    @Ignore // b/222529358
    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusedBoundsMoves() {
        val focusRequester = FocusRequester()
        var childOffset by mutableStateOf(IntOffset.Zero)

        rule.setContent {
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

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            childOffset += IntOffset(1, 2)
        }

        rule.runOnIdle {
            assertThat(focusedBounds).containsAtLeast(
                Rect(Offset.Zero, Size(size, size)),
                Rect(Offset(1f, 2f), Size(size, size))
            ).inOrder()
        }
    }

    @Ignore // b/222529358
    @Test
    fun onFocusedChildPositioned_notNotified_whenFocusableChildEntersComposition() {
        val focusRequester = FocusRequester()
        var includeFocusableModifier by mutableStateOf(false)
        rule.setContent {
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
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            includeFocusableModifier = true
        }

        rule.runOnIdle {
            assertThat(focusedBounds).isEmpty()
        }
    }

    @Ignore // b/222529358
    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusedBoundsLeavesComposition() {
        val focusRequester = FocusRequester()
        var includeFocusableModifier by mutableStateOf(true)
        rule.setContent {
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
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            includeFocusableModifier = false
        }

        rule.runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size),
                null
            ).inOrder()
        }
    }

    @Ignore // b/222529358
    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusedBoundsIsDisabled() {
        val focusRequester = FocusRequester()
        var focusableEnabled by mutableStateOf(true)
        rule.setContent {
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

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            focusableEnabled = false
        }

        rule.runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size),
                null
            ).inOrder()
        }
    }

    @Ignore // b/222529358
    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusCleared() {
        val focusRequester = FocusRequester()
        lateinit var focusManager: FocusManager
        rule.setContent {
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

        rule.runOnIdle {
            focusRequester.requestFocus()
        }
        rule.runOnIdle {
            focusManager.clearFocus()
        }

        rule.runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size),
                null
            ).inOrder()
        }
    }

    @Ignore // b/222529358
    @Test
    fun onFocusedBoundsPositioned_notified_whenFocusMovesOutsideObserver() {
        val (focusRequester1, focusRequester2) = FocusRequester.createRefs()
        rule.setContent {
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

        rule.runOnIdle {
            focusRequester1.requestFocus()
        }
        rule.runOnIdle {
            focusRequester2.requestFocus()
        }

        rule.runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size),
                null
            ).inOrder()
        }
    }

    @Ignore // b/222529358
    @Test
    fun onFocusedBoundsPositioned_notified_whenMultipleObservers() {
        val focusRequester = FocusRequester()
        val focusedBounds = mutableListOf<Pair<Int, Rect?>>()

        rule.setContent {
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

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Pair(1, Rect(0f, 0f, size, size)),
                Pair(0, Rect(0f, 0f, size, size)),
            ).inOrder()
        }
    }

    @Ignore // b/222529358
    @Test
    fun onFocusedBoundsPositioned_notified_whenAddedToParentWithAlreadyFocusedBounds() {
        val focusRequester = FocusRequester()
        var includeObserver by mutableStateOf(false)

        rule.setContent {
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

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(focusedBounds).isEmpty()
            includeObserver = true
        }

        rule.runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Rect(0f, 0f, size, size)
            ).inOrder()
        }
    }

    @Test
    @FlakyTest(bugId = 225229487)
    fun onFocusedBoundsPositioned_notified_whenNewObserverAddedAboveExisting() {
        val focusRequester = FocusRequester()
        val focusedBounds = mutableListOf<Pair<Int, Rect?>>()
        var includeSecondObserver by mutableStateOf(false)

        rule.setContent {
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

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            includeSecondObserver = true
        }

        rule.runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Pair(1, Rect(0f, 0f, size, size)),
                Pair(1, Rect(0f, 0f, size, size)),
                Pair(0, Rect(0f, 0f, size, size)),
            ).inOrder()
        }
    }

    @Ignore // b/222529358
    @Test
    fun onFocusedBoundsPositioned_notified_whenNewObserverAddedBelowExisting() {
        val focusRequester = FocusRequester()
        val focusedBounds = mutableListOf<Pair<Int, Rect?>>()
        var includeSecondObserver by mutableStateOf(false)

        rule.setContent {
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

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            includeSecondObserver = true
        }

        rule.runOnIdle {
            assertThat(focusedBounds).containsExactly(
                Pair(0, Rect(0f, 0f, size, size)),
                Pair(1, Rect(0f, 0f, size, size)),
                Pair(0, Rect(0f, 0f, size, size)),
            ).inOrder()
        }
    }
}