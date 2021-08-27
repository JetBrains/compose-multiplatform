/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * The enum class allows user to decide the selection mode.
 */
internal enum class SelectionMode {
    /**
     * When selection handles are dragged across composables, selection extends by row, for example,
     * when the end selection handle is dragged down, upper rows will be selected first, and the
     * lower rows.
     */
    Vertical {
        override fun compare(position: Offset, bounds: Rect): Int {
            if (bounds.contains(position)) return 0

            // When the position of the selection handle is on the top of the composable, and the
            // not on the right of the composable, it's considered as start.
            if (position.y < bounds.top) return -1

            // When the position of the selection handle is on the left of the composable, and not
            // below the bottom of composable, it's considered as start.
            if (position.x < bounds.left && position.y < bounds.bottom) return -1

            // In all other cases, the selection handle is considered as the end.
            return 1
        }
    },

    /**
     * When selection handles are dragged across composables, selection extends by column, for example,
     * when the end selection handle is dragged to the right, left columns will be selected first,
     * and the right rows.
     */
    Horizontal {
        override fun compare(position: Offset, bounds: Rect): Int {
            if (bounds.contains(position)) return 0

            // When the end of the selection is on the left of the composable, the composable is
            // outside of the selection range.
            if (position.x < bounds.left) return -1

            // When the end of the selection is on the top of the composable, and the not on the
            // right of the composable, the composable is outside of the selection range.
            if (position.y < bounds.top && position.x < bounds.right) return -1

            // In all other cases, the selection handle is considered as the end.
            return 1
        }
    };

    /**
     * A compare a selection handle with a  [Selectable] boundary. This defines whether an out of
     * boundary selection handle is treated as the start or the end of the Selectable. If the
     * [Selectable] is a text selectable, then the start is the index 0, and end corresponds to
     * the text length.
     *
     * @param position the position of the selection handle.
     * @param bounds the boundary of the [Selectable].
     * @return 0 if the selection handle [position] is within the [bounds]; a negative value if
     * the selection handle is considered as "start" of the [Selectable]; a positive value if the
     * selection handle is considered as the "end" of the [Selectable].
     */
    internal abstract fun compare(position: Offset, bounds: Rect): Int

    /**
     * Decides if Composable which has [bounds], should be accepted by the selection and
     * change its selected state for a selection that starts at [start] and ends at [end].
     *
     * @param bounds Composable bounds of the widget to be checked.
     * @param start The start coordinates of the selection, in SelectionContainer range.
     * @param end The end coordinates of the selection, in SelectionContainer range.
     */
    internal fun isSelected(
        bounds: Rect,
        start: Offset,
        end: Offset
    ): Boolean {
        // If either of the start or end is contained by bounds, the composable is selected.
        if (bounds.contains(start) || bounds.contains(end)) {
            return true
        }
        // Compare the location of start and end to the bound. If both are on the same side, return
        // false, otherwise return ture.
        val compareStart = compare(start, bounds)
        val compareEnd = compare(end, bounds)
        return (compareStart > 0) xor (compareEnd > 0)
    }
}
