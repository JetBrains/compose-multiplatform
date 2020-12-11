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
    @Deprecated(
        "Use windowToLocal instead",
        replaceWith = ReplaceWith("windowToLocal(global)")
    )
    fun globalToLocal(global: Offset): Offset

    /**
     * Converts [relativeToWindow] relative to the window's origin into an [Offset] relative to
     * this layout.
     */
    fun windowToLocal(relativeToWindow: Offset): Offset

    /**
     * Converts a local position within this layout into a global one.
     */
    @Deprecated("Use localToWindow instead", ReplaceWith("localToWindow(local)"))
    fun localToGlobal(local: Offset): Offset

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
     * Converts a child layout position into a local position within this layout.
     */
    @Deprecated("Use localPositionOf instead", ReplaceWith("localPositionOf(child, childLocal)"))
    fun childToLocal(child: LayoutCoordinates, childLocal: Offset): Offset

    /**
     * Returns the child bounding box, in local coordinates. A child that is rotated or scaled
     * will have the bounding box of rotated or scaled content in local coordinates. If a child
     * is clipped, the clipped rectangle will be returned.
     */
    @Deprecated(
        message = "Use localBoundingBoxOf instead",
        replaceWith = ReplaceWith("localBoundingBoxOf(child)")
    )
    fun childBoundingBox(child: LayoutCoordinates): Rect

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
    operator fun get(line: AlignmentLine): Int
}

/**
 * The global position of this layout.
 */
@Suppress("DEPRECATION")
@Deprecated("Use positionInWindow() instead", ReplaceWith("positionInWindow()"))
inline val LayoutCoordinates.globalPosition: Offset get() = localToGlobal(Offset.Zero)

/**
 * The position of this layout inside the root composable.
 */
fun LayoutCoordinates.positionInRoot(): Offset = localToRoot(Offset.Zero)

@Deprecated("Use positionInRoot() instead", ReplaceWith("positionInRoot()"))
inline val LayoutCoordinates.positionInRoot: Offset get() = localToRoot(Offset.Zero)

/**
 * The position of this layout relative to the window.
 */
fun LayoutCoordinates.positionInWindow(): Offset = localToWindow(Offset.Zero)

/**
 * The boundaries of this layout inside the root composable.
 */
fun LayoutCoordinates.boundsInRoot(): Rect =
    findRoot().localBoundingBoxOf(this)

@Deprecated("Use boundsInRoot()", ReplaceWith("boundsInRoot()"))
val LayoutCoordinates.boundsInRoot: Rect get() = boundsInRoot()

/**
 * The boundaries of this layout relative to the window's origin.
 */
fun LayoutCoordinates.boundsInWindow(): Rect {
    val root = findRoot()
    val bounds = boundsInRoot()
    val windowPosition = root.positionInWindow()
    return Rect(
        left = bounds.left + windowPosition.x,
        top = bounds.top + windowPosition.y,
        right = bounds.right + windowPosition.x,
        bottom = bounds.bottom + windowPosition.y
    )
}

/**
 * Returns the position of the top-left in the parent's content area or (0, 0)
 * for the root.
 */
val LayoutCoordinates.positionInParent: Offset
    get() = parentCoordinates?.localPositionOf(this, Offset.Zero) ?: Offset.Zero

/**
 * Returns the bounding box of the child in the parent's content area, including any clipping
 * done with respect to the parent. For the root, the bounds is positioned at (0, 0) and sized
 * to the size of the root.
 */
val LayoutCoordinates.boundsInParent: Rect
    get() = parentCoordinates?.localBoundingBoxOf(this)
        ?: Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())

/**
 * The global boundaries of this layout inside.
 */
@Deprecated("Use boundsInWindow instead", ReplaceWith("boundsInWindow"))
val LayoutCoordinates.globalBounds: Rect
    get() {
        val root = findRoot()
        @Suppress("DEPRECATION")
        val rootPosition = root.localToGlobal(Offset.Zero)
        val bounds = root.localBoundingBoxOf(this)
        return Rect(
            left = bounds.left + rootPosition.x,
            top = bounds.top + rootPosition.y,
            right = bounds.right + rootPosition.x,
            bottom = bounds.bottom + rootPosition.y
        )
    }

/**
 * Returns the [LayoutCoordinates] of the root layout element in the hierarchy. This will have
 * the size of the entire compose UI.
 */
internal fun LayoutCoordinates.findRoot(): LayoutCoordinates {
    var root = this
    var parent = root.parentCoordinates
    while (parent != null) {
        root = parent
        parent = root.parentCoordinates
    }
    var rootLayoutNodeWrapper = root as? LayoutNodeWrapper ?: return root
    var parentLayoutNodeWrapper = rootLayoutNodeWrapper.wrappedBy
    while (parentLayoutNodeWrapper != null) {
        rootLayoutNodeWrapper = parentLayoutNodeWrapper
        parentLayoutNodeWrapper = parentLayoutNodeWrapper.wrappedBy
    }
    return rootLayoutNodeWrapper
}
