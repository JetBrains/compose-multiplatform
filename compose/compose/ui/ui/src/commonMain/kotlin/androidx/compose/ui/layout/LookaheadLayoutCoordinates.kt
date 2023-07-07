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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.layout

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.LookaheadDelegate
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset

/**
 * [LookaheadLayoutCoordinates] interface holds layout coordinates from both the lookahead
 * calculation and the post-lookahead layout pass.
 */
@ExperimentalComposeUiApi
sealed interface LookaheadLayoutCoordinates : LayoutCoordinates {
    /**
     * Converts an [relativeToSource] in [sourceCoordinates] space into local coordinates.
     * [sourceCoordinates] may be any [LookaheadLayoutCoordinates] that belong to the same
     * compose layout hierarchy. Unlike [localPositionOf], [localLookaheadPositionOf] uses
     * the lookahead positions for coordinates calculation.
     */
    fun localLookaheadPositionOf(
        sourceCoordinates: LookaheadLayoutCoordinates,
        relativeToSource: Offset = Offset.Zero
    ): Offset
}

internal class LookaheadLayoutCoordinatesImpl(val lookaheadDelegate: LookaheadDelegate) :
    LookaheadLayoutCoordinates {
    val coordinator: NodeCoordinator
        get() = lookaheadDelegate.coordinator

    override fun localLookaheadPositionOf(
        sourceCoordinates: LookaheadLayoutCoordinates,
        relativeToSource: Offset
    ): Offset {
        val source = (sourceCoordinates as LookaheadLayoutCoordinatesImpl).lookaheadDelegate
        val commonAncestor = coordinator.findCommonAncestor(source.coordinator)

        return commonAncestor.lookaheadDelegate?.let { ancestor ->
            // Common ancestor is in lookahead
            (source.positionIn(ancestor) + relativeToSource.round() -
                lookaheadDelegate.positionIn(ancestor)).toOffset()
        } ?: commonAncestor.let {
            // The two coordinates are in two separate LookaheadLayouts
            val sourceRoot = source.rootLookaheadDelegate
            val relativePosition = source.positionIn(sourceRoot) +
                sourceRoot.position + relativeToSource.round() -
                with(lookaheadDelegate) {
                    (positionIn(rootLookaheadDelegate) + rootLookaheadDelegate.position)
                }

            lookaheadDelegate.rootLookaheadDelegate.coordinator.wrappedBy!!.localPositionOf(
                sourceRoot.coordinator.wrappedBy!!, relativePosition.toOffset()
            )
        }
    }

    override val size: IntSize
        get() = coordinator.size
    override val providedAlignmentLines: Set<AlignmentLine>
        get() = coordinator.providedAlignmentLines

    override val parentLayoutCoordinates: LayoutCoordinates?
        get() = coordinator.parentLayoutCoordinates
    override val parentCoordinates: LayoutCoordinates?
        get() = coordinator.parentCoordinates
    override val isAttached: Boolean
        get() = coordinator.isAttached

    override fun windowToLocal(relativeToWindow: Offset): Offset =
        coordinator.windowToLocal(relativeToWindow)

    override fun localToWindow(relativeToLocal: Offset): Offset =
        coordinator.localToWindow(relativeToLocal)

    override fun localToRoot(relativeToLocal: Offset): Offset =
        coordinator.localToRoot(relativeToLocal)

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset
    ): Offset = coordinator.localPositionOf(sourceCoordinates, relativeToSource)

    override fun localBoundingBoxOf(
        sourceCoordinates: LayoutCoordinates,
        clipBounds: Boolean
    ): Rect = coordinator.localBoundingBoxOf(sourceCoordinates, clipBounds)

    override fun transformFrom(sourceCoordinates: LayoutCoordinates, matrix: Matrix) {
        coordinator.transformFrom(sourceCoordinates, matrix)
    }

    override fun get(alignmentLine: AlignmentLine): Int = coordinator.get(alignmentLine)
}

private val LookaheadDelegate.rootLookaheadDelegate: LookaheadDelegate
    get() = lookaheadScope.root.outerCoordinator.lookaheadDelegate!!
