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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.Handle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp

internal val HandleWidth = 25.dp
internal val HandleHeight = 25.dp

/**
 * [SelectionHandleInfo]s for the nodes representing selection handles. These nodes are in popup
 * windows, and will respond to drag gestures.
 */
internal val SelectionHandleInfoKey =
    SemanticsPropertyKey<SelectionHandleInfo>("SelectionHandleInfo")

/**
 * Information about a single selection handle popup.
 *
 * @param handle Which selection [Handle] this is about.
 * @param position The position that the handle is anchored to relative to the selectable content.
 * This position is not necessarily the position of the popup itself, it's the position that the
 * handle "points" to (so e.g. top-middle for [Handle.Cursor]).
 */
internal data class SelectionHandleInfo(
    val handle: Handle,
    val position: Offset
)

@Composable
internal expect fun SelectionHandle(
    position: Offset,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    modifier: Modifier,
    content: @Composable (() -> Unit)?
)

/**
 * Adjust coordinates for given text offset.
 *
 * Currently [android.text.Layout.getLineBottom] returns y coordinates of the next
 * line's top offset, which is not included in current line's hit area. To be able to
 * hit current line, move up this y coordinates by 1 pixel.
 */
internal fun getAdjustedCoordinates(position: Offset): Offset {
    return Offset(position.x, position.y - 1f)
}
