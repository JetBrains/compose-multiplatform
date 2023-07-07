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

package androidx.compose.ui.node

import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutNode.LayoutState
import androidx.compose.ui.util.fastMap

internal interface MeasureScopeWithLayoutNode : MeasureScope {
    val layoutNode: LayoutNode
}

internal fun getChildrenOfVirtualChildren(scope: IntrinsicMeasureScope): List<List<Measurable>> {
    val layoutNode = (scope as MeasureScopeWithLayoutNode).layoutNode
    val lookahead = layoutNode.isInLookaheadPass()
    return layoutNode.foldedChildren.fastMap {
        if (lookahead) it.childLookaheadMeasurables else it.childMeasurables
    }
}

private fun LayoutNode.isInLookaheadPass(): Boolean {
    return when (layoutState) {
        LayoutState.LookaheadMeasuring, LayoutState.LookaheadLayingOut -> true
        LayoutState.Measuring, LayoutState.LayingOut -> false
        LayoutState.Idle -> {
            // idle means intrinsics are being asked, we need to check the parent
            requireNotNull(parent).isInLookaheadPass()
        }
    }
}
