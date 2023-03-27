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

package androidx.compose.foundation.lazy.list

import androidx.compose.animation.core.snap
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.BaseLazyLayoutTestWithOrientation
import androidx.compose.foundation.composeViewSwipeDown
import androidx.compose.foundation.composeViewSwipeLeft
import androidx.compose.foundation.composeViewSwipeRight
import androidx.compose.foundation.composeViewSwipeUp
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyList
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

open class BaseLazyListTestWithOrientation(
    private val orientation: Orientation
) : BaseLazyLayoutTestWithOrientation(orientation) {

    fun Modifier.fillMaxCrossAxis() =
        if (vertical) {
            this.fillMaxWidth()
        } else {
            this.fillMaxHeight()
        }

    fun LazyItemScope.fillParentMaxMainAxis() =
        if (vertical) {
            Modifier.fillParentMaxHeight()
        } else {
            Modifier.fillParentMaxWidth()
        }

    fun LazyItemScope.fillParentMaxCrossAxis() =
        if (vertical) {
            Modifier.fillParentMaxWidth()
        } else {
            Modifier.fillParentMaxHeight()
        }

    fun LazyListState.scrollBy(offset: Dp) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            animateScrollBy(with(rule.density) { offset.roundToPx().toFloat() }, snap())
        }
    }

    fun LazyListState.scrollTo(index: Int) {
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) {
            scrollToItem(index)
        }
    }

    fun SemanticsNodeInteraction.scrollBy(offset: Dp) = scrollBy(
        x = if (vertical) 0.dp else offset,
        y = if (!vertical) 0.dp else offset,
        density = rule.density
    )

    fun composeViewSwipeForward() {
        if (orientation == Orientation.Vertical) {
            composeViewSwipeUp()
        } else {
            composeViewSwipeLeft()
        }
    }

    fun composeViewSwipeBackward() {
        if (orientation == Orientation.Vertical) {
            composeViewSwipeDown()
        } else {
            composeViewSwipeRight()
        }
    }

    fun Velocity.toFloat(): Float {
        return if (orientation == Orientation.Vertical) y else x
    }

    @Composable
    fun LazyColumnOrRow(
        modifier: Modifier = Modifier,
        state: LazyListState = rememberLazyListState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        reverseArrangement: Boolean = false,
        flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
        userScrollEnabled: Boolean = true,
        spacedBy: Dp = 0.dp,
        content: LazyListScope.() -> Unit
    ) {
        if (vertical) {
            val verticalArrangement = when {
                spacedBy != 0.dp -> Arrangement.spacedBy(spacedBy)
                reverseLayout xor reverseArrangement -> Arrangement.Bottom
                else -> Arrangement.Top
            }
            LazyColumn(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                verticalArrangement = verticalArrangement,
                content = content
            )
        } else {
            val horizontalArrangement = when {
                spacedBy != 0.dp -> Arrangement.spacedBy(spacedBy)
                reverseLayout xor reverseArrangement -> Arrangement.End
                else -> Arrangement.Start
            }
            LazyRow(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                horizontalArrangement = horizontalArrangement,
                content = content
            )
        }
    }

    @Composable
    fun LazyColumnOrRow(
        modifier: Modifier = Modifier,
        state: LazyListState = rememberLazyListState(),
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        reverseArrangement: Boolean = false,
        flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
        userScrollEnabled: Boolean = true,
        spacedBy: Dp = 0.dp,
        beyondBoundsItemCount: Int,
        content: LazyListScope.() -> Unit
    ) {
        if (vertical) {
            val verticalArrangement = when {
                spacedBy != 0.dp -> Arrangement.spacedBy(spacedBy)
                reverseLayout xor reverseArrangement -> Arrangement.Bottom
                else -> Arrangement.Top
            }
            LazyColumn(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                verticalArrangement = verticalArrangement,
                beyondBoundsItemCount = beyondBoundsItemCount,
                content = content
            )
        } else {
            val horizontalArrangement = when {
                spacedBy != 0.dp -> Arrangement.spacedBy(spacedBy)
                reverseLayout xor reverseArrangement -> Arrangement.End
                else -> Arrangement.Start
            }
            LazyRow(
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                horizontalArrangement = horizontalArrangement,
                beyondBoundsItemCount = beyondBoundsItemCount,
                content = content
            )
        }
    }
}

@Composable
private fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    beyondBoundsItemCount: Int,
    content: LazyListScope.() -> Unit
) {
    LazyList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        isVertical = true,
        reverseLayout = reverseLayout,
        userScrollEnabled = userScrollEnabled,
        beyondBoundsItemCount = beyondBoundsItemCount,
        content = content
    )
}

@Composable
private fun LazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    beyondBoundsItemCount: Int,
    content: LazyListScope.() -> Unit
) {
    LazyList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
        isVertical = false,
        flingBehavior = flingBehavior,
        reverseLayout = reverseLayout,
        userScrollEnabled = userScrollEnabled,
        beyondBoundsItemCount = beyondBoundsItemCount,
        content = content
    )
}