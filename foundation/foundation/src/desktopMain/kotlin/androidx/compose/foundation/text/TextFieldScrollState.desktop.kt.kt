/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


/**
 * Creates a [TextFieldScrollState] that is remembered across compositions.
 *
 * Changes to the provided initial value will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param orientation the direction of scrolling. For single-line, non-wrapping text fields this
 * must be [Orientation.Horizontal]. For all others, it must be [Orientation.Vertical].
 * @param initial the initial value for [TextFieldScrollState.offset]
 */
@ExperimentalFoundationApi
@Composable
fun rememberTextFieldScrollState(
    orientation: Orientation,
    initial: Int = 0,
): TextFieldScrollState {
    return remember(orientation) {
        TextFieldScrollState(orientation, initial)
    }
}

/**
 * Creates a [TextFieldScrollState] that is remembered across compositions, for a vertically
 * scrolling text field.
 *
 * This cannot be used with text fields that are single-line and non-wrapping.
 *
 * Changes to the provided initial value will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initial the initial value for [TextFieldScrollState.offset]
 */
@ExperimentalFoundationApi
@Composable
fun rememberTextFieldVerticalScrollState(initial: Int = 0): TextFieldScrollState {
    return rememberTextFieldScrollState(Orientation.Vertical, initial)
}

/**
 * Creates a [TextFieldScrollState] that is remembered across compositions, for a horizontally
 * scrolling text field.
 *
 * This can only be used with text fields that are single-line and non-wrapping.
 *
 * Changes to the provided initial value will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initial the initial value for [TextFieldScrollState.offset]
 */
@ExperimentalFoundationApi
@Composable
fun rememberTextFieldHorizontalScrollState(initial: Int = 0): TextFieldScrollState {
    return rememberTextFieldScrollState(Orientation.Horizontal, initial)
}

/**
 * A state object that can be hoisted to control and observe scrolling of a [BasicTextField].
 *
 * In most cases, this will be created via [rememberTextFieldScrollState].
 *
 * @param orientation the direction of scrolling. For single-line, non-wrapping text fields this
 * must be [Orientation.Horizontal]. For all others, it must be [Orientation.Vertical].
 * @param initial the initial value for [TextFieldScrollState.offset]
 */
@ExperimentalFoundationApi
class TextFieldScrollState(
    orientation: Orientation,
    initial: Int = 0,
): ScrollableState{

    /**
     * The underlying [TextFieldScrollerPosition] that is the scroll state of text fields.
     */
    internal val scrollerPosition = TextFieldScrollerPosition(orientation, initial.toFloat())

    /**
     * The scroll offset, in pixels.
     */
    var offset: Float by scrollerPosition::offset

    /**
     * The maximum scroll offset, in pixels.
     */
    val maxOffset: Float by scrollerPosition::maximum

    /**
     * The size of the visible part of the text field, in the direction of scrolling, in pixels.
     */
    val viewportSize: Int by scrollerPosition::viewportSize

    private val scrollableState = ScrollableState {
        val raw = offset + it
        val prevOffset = offset
        offset = raw.coerceIn(0f, maxOffset)

        // Avoid floating-point rounding error
        if (offset != prevOffset) offset - prevOffset else it
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ): Unit = scrollableState.scroll(scrollPriority, block)

    override fun dispatchRawDelta(delta: Float): Float =
        scrollableState.dispatchRawDelta(delta)

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

}