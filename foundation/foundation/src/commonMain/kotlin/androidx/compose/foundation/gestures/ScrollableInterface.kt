/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.gestures

/**
 * Scope used for suspending scroll blocks
 */
interface ScrollScope {
    /**
     * Attempts to scroll forward by [pixels] px.
     *
     * @return the amount of the requested scroll that was consumed (that is, how far it scrolled)
     */
    fun scrollBy(pixels: Float): Float
}

/**
 * A an object representing something that can be scrolled. This interface is implemented by states
 * of scrollable containers such as [androidx.compose.foundation.lazy.LazyListState] or
 * [androidx.compose.foundation.ScrollState] in order to provide low-level scrolling control via
 * [scroll], as well as allowing for higher-level scrolling functions like
 * [androidx.compose.foundation.animation.smoothScrollBy] to be implemented as extension
 * functions on [Scrollable].
 *
 * Subclasses may also have their own methods that are specific to their interaction paradigm, such
 * as [androidx.compose.foundation.lazy.LazyListState.snapToItemIndex].
 *
 * @see ScrollableController
 * @see androidx.compose.foundation.animation.smoothScrollBy
 */
interface Scrollable {
    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [ScrollScope.scrollBy]. All actions that change the logical scroll position must be
     * performed within a [scroll] block (even if they don't call any other methods on this
     * object) in order to guarantee that mutual exclusion is enforced.
     *
     * Cancels the currently running scroll, if any, and suspends until the cancellation is
     * complete.
     *
     * If [scroll] is called from elsewhere, this will be canceled.
     */
    suspend fun scroll(
        block: suspend ScrollScope.() -> Unit
    )
}
