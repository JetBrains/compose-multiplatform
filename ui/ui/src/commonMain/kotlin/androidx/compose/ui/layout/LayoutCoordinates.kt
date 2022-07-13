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
import androidx.compose.ui.node.LayoutNodeWrapper
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * A holder of the measured bounds for the layout (MeasureBox).
 */
@JvmDefaultWithCompatibility
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
    val parentLayoutCoordinates: LayoutCoordinates?

    /**
     * The coordinates of the parent layout modifier or parent layout if there is no
     * parent layout modifier, or `null` if there is no parent.
     */
    val parentCoordinates: LayoutCoordinates?

    /**
     * Returns false if the corresponding layout was detached from the hierarchy.
     */
    val isAttached: Boolean

    /**
     * Converts [relativeToWindow] relative to the window's origin into an [Offset] relative to
     * this layout.
     */
    fun windowToLocal(relativeToWindow: Offset): Offset

    /**
     * Converts [relativeToLocal] position within this layout into an [Offset] relative to the
     * window's origin.
     */
    fun localToWindow(relativeToLocal: Offset): Offset

    /**
     * Converts a local position within this layout into an offset from the root composable.
     */
    fun localToRoot(relativeToLocal: Offset): Offset

    /**
     * Converts an [relativeToSource] in [sourceCoordinates] space into local coordinates.
     * [sourceCoordinates] may be any [LayoutCoordinates] that belong to the same
     * compose layout hierarchy.
     */
    fun localPositionOf(sourceCoordinates: LayoutCoordinates, relativeToSource: Offset): Offset

    /**
     * Returns the bounding box of [sourceCoordinates] in the local coordinates.
     * If [clipBounds] is `true`, any clipping that occurs between [sourceCoordinates] and
     * this layout will affect the returned bounds, and can even result in an empty rectangle
     * if clipped regions do not overlap. If [clipBounds] is false, the bounding box of
     * [sourceCoordinates] will be converted to local coordinates irrespective of any clipping
     * applied between the layouts.
     *
     * When rotation or scaling is applied, the bounding box of the rotated or scaled value
     * will be computed in the local coordinates. For example, if a 40 pixels x 20 pixel layout
     * is rotated 90 degrees, the bounding box will be 20 pixels x 40 pixels in its parent's
     * coordinates.
     */
    fun localBoundingBoxOf(sourceCoordinates: LayoutCoordinates, clipBounds: Boolean = true): Rect

    /**
     * Returns the position in pixels of an [alignment line][AlignmentLine],
     * or [AlignmentLine.Unspecified] if the line is not provided.
     */
    operator fun get(alignmentLine: AlignmentLine): Int
}

/**
 * The position of this layout inside the root composable.
 */
fun LayoutCoordinates.positionInRoot(): Offset = localToRoot(Offset.Zero)

/**
 * The position of this layout relative to the window.
 */
fun LayoutCoordinates.positionInWindow(): Offset = localToWindow(Offset.Zero)

/**
 * The boundaries of this layout inside the root composable.
 */
fun LayoutCoordinates.boundsInRoot(): Rect =
    findRoot().localBoundingBoxOf(this)

/**
 * The boundaries of this layout relative to the window's origin.
 */
fun LayoutCoordinates.boundsInWindow(): Rect {
    val root = findRoot()
    val bounds = boundsInRoot()
    val topLeft = root.localToWindow(Offset(bounds.left, bounds.top))
    val topRight = root.localToWindow(Offset(bounds.right, bounds.top))
    val bottomRight = root.localToWindow(Offset(bounds.right, bounds.bottom))
    val bottomLeft = root.localToWindow(Offset(bounds.left, bounds.bottom))
    val left = minOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x)
    val top = minOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y)
    val right = maxOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x)
    val bottom = maxOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y)
    return Rect(left, top, right, bottom)
}

/**
 * Returns the position of the top-left in the parent's content area or (0, 0)
 * for the root.
 */
fun LayoutCoordinates.positionInParent(): Offset =
    parentLayoutCoordinates?.localPositionOf(this, Offset.Zero) ?: Offset.Zero

/**
 * Returns the bounding box of the child in the parent's content area, including any clipping
 * done with respect to the parent. For the root, the bounds is positioned at (0, 0) and sized
 * to the size of the root.
 */
fun LayoutCoordinates.boundsInParent(): Rect =
    parentLayoutCoordinates?.localBoundingBoxOf(this)
        ?: Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())

/**
 * Returns the [LayoutCoordinates] of the root layout element in the hierarchy. This will have
 * the size of the entire compose UI.
 */
internal fun LayoutCoordinates.findRoot(): LayoutCoordinates {
    var root = this
    var parent = root.parentLayoutCoordinates
    while (parent != null) {
        root = parent
        parent = root.parentLayoutCoordinates
    }
    var rootLayoutNodeWrapper = root as? LayoutNodeWrapper ?: return root
    var parentLayoutNodeWrapper = rootLayoutNodeWrapper.wrappedBy
    while (parentLayoutNodeWrapper != null) {
        rootLayoutNodeWrapper = parentLayoutNodeWrapper
        parentLayoutNodeWrapper = parentLayoutNodeWrapper.wrappedBy
    }
    return rootLayoutNodeWrapper
}
