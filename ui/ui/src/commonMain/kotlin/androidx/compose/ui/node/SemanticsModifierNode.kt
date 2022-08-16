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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.getOrNull

@ExperimentalComposeUiApi
interface SemanticsModifierNode : DelegatableNode {
    val semanticsConfiguration: SemanticsConfiguration
}

@ExperimentalComposeUiApi
fun SemanticsModifierNode.invalidateSemantics() = requireOwner().onSemanticsChange()

@ExperimentalComposeUiApi
fun SemanticsModifierNode.collapsedSemanticsConfiguration(): SemanticsConfiguration {
    val next = localChild(Nodes.Semantics)
    if (next == null || semanticsConfiguration.isClearingSemantics) {
        return semanticsConfiguration
    }

    val config = semanticsConfiguration.copy()
    config.collapsePeer(next.collapsedSemanticsConfiguration())
    return config
}

@OptIn(ExperimentalComposeUiApi::class)
internal val SemanticsModifierNode.useMinimumTouchTarget: Boolean
    get() = semanticsConfiguration.getOrNull(SemanticsActions.OnClick) != null

@OptIn(ExperimentalComposeUiApi::class)
internal fun SemanticsModifierNode.touchBoundsInRoot(): Rect {
    if (!node.isAttached) {
        return Rect.Zero
    }
    if (!useMinimumTouchTarget) {
        return requireCoordinator(Nodes.Semantics).boundsInRoot()
    }

    return requireCoordinator(Nodes.Semantics).touchBoundsInRoot()
}
