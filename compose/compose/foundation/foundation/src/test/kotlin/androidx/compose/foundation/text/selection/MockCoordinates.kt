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

import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize

class MockCoordinates(
    override var size: IntSize = IntSize.Zero,
    var localOffset: Offset = Offset.Zero,
    var globalOffset: Offset = Offset.Zero,
    var windowOffset: Offset = Offset.Zero,
    var rootOffset: Offset = Offset.Zero,
    var childToLocalOffset: Offset = Offset.Zero,
    override var isAttached: Boolean = true
) : LayoutCoordinates {
    val globalToLocalParams = mutableListOf<Offset>()
    val windowToLocalParams = mutableListOf<Offset>()
    val localToGlobalParams = mutableListOf<Offset>()
    val localToWindowParams = mutableListOf<Offset>()
    val localToRootParams = mutableListOf<Offset>()
    val childToLocalParams = mutableListOf<Pair<LayoutCoordinates, Offset>>()
    val localPositionOfParams = mutableListOf<Pair<LayoutCoordinates, Offset>>()

    override val providedAlignmentLines: Set<AlignmentLine>
        get() = emptySet()

    override val parentLayoutCoordinates: LayoutCoordinates?
        get() = null

    override val parentCoordinates: LayoutCoordinates?
        get() = null

    override fun windowToLocal(relativeToWindow: Offset): Offset {
        windowToLocalParams += relativeToWindow
        return localOffset
    }

    override fun localToWindow(relativeToLocal: Offset): Offset {
        localToWindowParams += relativeToLocal
        return windowOffset
    }

    override fun localToRoot(relativeToLocal: Offset): Offset {
        localToRootParams += relativeToLocal
        return rootOffset
    }

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset
    ): Offset {
        localPositionOfParams += sourceCoordinates to relativeToSource
        return childToLocalOffset
    }

    override fun localBoundingBoxOf(
        sourceCoordinates: LayoutCoordinates,
        clipBounds: Boolean
    ): Rect = Rect.Zero

    override fun get(alignmentLine: AlignmentLine): Int = 0
}