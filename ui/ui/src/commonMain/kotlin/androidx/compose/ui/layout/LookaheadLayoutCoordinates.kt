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
import androidx.compose.ui.node.LayoutNodeWrapper
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
    val wrapper: LayoutNodeWrapper
        get() = lookaheadDelegate.wrapper

    override fun localLookaheadPositionOf(
        sourceCoordinates: LookaheadLayoutCoordinates,
        relativeToSource: Offset
    ): Offset {
        val source = (sourceCoordinates as LookaheadLayoutCoordinatesImpl).lookaheadDelegate
        val commonAncestor = wrapper.findCommonAncestor(source.wrapper)

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

            lookaheadDelegate.rootLookaheadDelegate.wrapper.wrappedBy!!.localPositionOf(
                sourceRoot.wrapper.wrappedBy!!, relativePosition.toOffset()
            )
        }
    }

    override val size: IntSize
        get() = wrapper.size
    override val providedAlignmentLines: Set<AlignmentLine>
        get() = wrapper.providedAlignmentLines

    override val parentLayoutCoordinates: LayoutCoordinates?
        get() = wrapper.parentLayoutCoordinates
    override val parentCoordinates: LayoutCoordinates?
        get() = wrapper.parentCoordinates
    override val isAttached: Boolean
        get() = wrapper.isAttached

    override fun windowToLocal(relativeToWindow: Offset): Offset =
        wrapper.windowToLocal(relativeToWindow)

    override fun localToWindow(relativeToLocal: Offset): Offset =
        wrapper.localToWindow(relativeToLocal)

    override fun localToRoot(relativeToLocal: Offset): Offset =
        wrapper.localToRoot(relativeToLocal)

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset
    ): Offset = wrapper.localPositionOf(sourceCoordinates, relativeToSource)

    override fun localBoundingBoxOf(
        sourceCoordinates: LayoutCoordinates,
        clipBounds: Boolean
    ): Rect = wrapper.localBoundingBoxOf(sourceCoordinates, clipBounds)

    override fun get(alignmentLine: AlignmentLine): Int = wrapper.get(alignmentLine)
}

private val LookaheadDelegate.rootLookaheadDelegate: LookaheadDelegate
    get() = lookaheadScope.root.outerLayoutNodeWrapper.lookaheadDelegate!!
