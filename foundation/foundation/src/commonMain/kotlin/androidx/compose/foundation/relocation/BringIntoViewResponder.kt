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

package androidx.compose.foundation.relocation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf

/**
 * A parent that can respond to [bringIntoView] requests from its children, and scroll so that the
 * item is visible on screen.
 *
 * When a component calls [BringIntoViewRequester.bringIntoView], the
 * [BringIntoView ModifierLocal][ModifierLocalBringIntoViewResponder] is read to gain access to the
 * [BringIntoViewResponder], which is responsible for performing a scroll to bring the requesting
 * component into view and then send a [bringIntoView] request to its parent.
 *
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 *
 * @see BringIntoViewRequester
 */
@ExperimentalFoundationApi
interface BringIntoViewResponder {
    /**
     * Bring this specified rectangle into bounds by making all the scrollable parents scroll
     * appropriately.
     *
     * @param rect The rectangle that should be brought into view. If you
     * don't specify the coordinates, the entire component is brought into view.
     *
     *
     * Here is a sample where a composable is brought into view:
     * @sample androidx.compose.foundation.samples.BringIntoViewSample
     *
     * Here is a sample where a part of a composable is brought into view:
     * @sample androidx.compose.foundation.samples.BringPartOfComposableIntoViewSample
     */
    suspend fun bringIntoView(rect: Rect)

    /**
     * Convert a Rect into the layoutCoordinates of this [BringIntoViewResponder].
     */
    fun toLocalRect(rect: Rect, layoutCoordinates: LayoutCoordinates): Rect

    @ExperimentalFoundationApi
    companion object {
        /**
         * The Key for the ModifierLocal that can be used to access the [BringIntoViewResponder].
         */
        val ModifierLocalBringIntoViewResponder: ProvidableModifierLocal<BringIntoViewResponder> =
            modifierLocalOf { RootBringIntoViewResponder }

        /**
         * The Root [BringIntoViewResponder]. If you read this value for the
         * [ModifierLocalBringIntoViewResponder], it means that this is the topmost
         * [BringIntoViewResponder] in this hierarchy.
         */
        val RootBringIntoViewResponder = object : BringIntoViewResponder {
            override suspend fun bringIntoView(rect: Rect) {}
            override fun toLocalRect(rect: Rect, layoutCoordinates: LayoutCoordinates) =
                Rect(layoutCoordinates.localToRoot(rect.topLeft), rect.size)
        }
    }
}
