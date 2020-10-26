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

package androidx.compose.ui.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

/**
 * A holder of the measured bounds for the layout (MeasureBox).
 */
// TODO(Andrey): Add Matrix transformation here when we would have this logic.
interface LayoutCoordinates {
    /**
     * The size of this layout in the local coordinates space.
     */
    val size: IntSize

    /**
     * The alignment lines provided for this layout, not including inherited lines.
     */
    val providedAlignmentLines: Set<AlignmentLine>

    /**
     * The coordinates of the parent layout. Null if there is no parent.
     */
    val parentCoordinates: LayoutCoordinates?

    /**
     * Returns false if the corresponding layout was detached from the hierarchy.
     */
    val isAttached: Boolean

    /**
     * Converts a global position into a local position within this layout.
     */
    fun globalToLocal(global: Offset): Offset

    /**
     * Converts a local position within this layout into a global one.
     */
    fun localToGlobal(local: Offset): Offset

    /**
     * Converts a local position within this layout into an offset from the root composable.
     */
    fun localToRoot(local: Offset): Offset

    /**
     * Converts a child layout position into a local position within this layout.
     */
    fun childToLocal(child: LayoutCoordinates, childLocal: Offset): Offset

    /**
     * Returns the child bounding box, discarding clipped rectangles, in local coordinates.
     */
    fun childBoundingBox(child: LayoutCoordinates): Rect

    /**
     * Returns the position of an [alignment line][AlignmentLine],
     * or [AlignmentLine.Unspecified] if the line is not provided.
     */
    operator fun get(line: AlignmentLine): Int
}

/**
 * The global position of this layout.
 */
inline val LayoutCoordinates.globalPosition: Offset get() = localToGlobal(Offset.Zero)

/**
 * The position of this layout inside the root composable.
 */
inline val LayoutCoordinates.positionInRoot: Offset get() = localToRoot(Offset.Zero)

/**
 * The boundaries of this layout inside the root composable.
 */
val LayoutCoordinates.boundsInRoot: Rect
    get() {
        return findRoot(this).childBoundingBox(this)
    }

/**
 * Returns the position of the top-left in the parent's content area or (0, 0)
 * for the root.
 */
val LayoutCoordinates.positionInParent: Offset
    get() = parentCoordinates?.childToLocal(this, Offset.Zero) ?: Offset.Zero

/**
 * Returns the bounding box of the child in the parent's content area, including any clipping
 * done with respect to the parent. For the root, the bounds is positioned at (0, 0) and sized
 * to the size of the root.
 */
val LayoutCoordinates.boundsInParent: Rect
    get() = parentCoordinates?.childBoundingBox(this)
        ?: Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())

/**
 * The global boundaries of this layout inside.
 */
val LayoutCoordinates.globalBounds: Rect
    get() {
        val root = findRoot(this)
        val rootPosition = root.localToGlobal(Offset.Zero)
        val bounds = root.childBoundingBox(this)
        return Rect(
            left = bounds.left + rootPosition.x,
            top = bounds.top + rootPosition.y,
            right = bounds.right + rootPosition.x,
            bottom = bounds.bottom + rootPosition.y
        )
    }

private fun findRoot(layoutCoordinates: LayoutCoordinates): LayoutCoordinates {
    var root = layoutCoordinates
    var parent = root.parentCoordinates
    while (parent != null) {
        root = parent
        parent = root.parentCoordinates
    }
    return root
}