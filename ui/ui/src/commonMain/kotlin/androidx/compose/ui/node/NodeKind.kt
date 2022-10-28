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

@file:Suppress("DEPRECATION", "NOTHING_TO_INLINE")

package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.focus.FocusOrderModifier
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.layout.IntermediateLayoutModifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.LookaheadOnPlacedModifier
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.layout.OnPlacedModifier
import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalNode
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.semantics.SemanticsModifier

@JvmInline
internal value class NodeKind<T>(val mask: Int) {
    inline infix fun or(other: NodeKind<*>): Int = mask or other.mask
    inline infix fun or(other: Int): Int = mask or other
}
internal inline infix fun Int.or(other: NodeKind<*>): Int = this or other.mask

// For a given NodeCoordinator, the "LayoutAware" nodes that it is concerned with should include
// its own measureNode if the measureNode happens to implement LayoutAware. If the measureNode
// implements any other node interfaces, such as draw, those should be visited by the coordinator
// below them.
@OptIn(ExperimentalComposeUiApi::class)
internal val NodeKind<*>.includeSelfInTraversal: Boolean get() {
    return mask and Nodes.LayoutAware.mask != 0
}

// Note that these don't inherit from Modifier.Node to allow for a single Modifier.Node
// instance to implement multiple Node interfaces

@OptIn(ExperimentalComposeUiApi::class)
internal object Nodes {
    @JvmStatic
    inline val Any get() = NodeKind<Modifier.Node>(0b1 shl 0)
    @JvmStatic
    inline val Layout get() = NodeKind<LayoutModifierNode>(0b1 shl 1)
    @JvmStatic
    inline val Draw get() = NodeKind<DrawModifierNode>(0b1 shl 2)
    @JvmStatic
    inline val Semantics get() = NodeKind<SemanticsModifierNode>(0b1 shl 3)
    @JvmStatic
    inline val PointerInput get() = NodeKind<PointerInputModifierNode>(0b1 shl 4)
    @JvmStatic
    inline val Locals get() = NodeKind<ModifierLocalNode>(0b1 shl 5)
    @JvmStatic
    inline val ParentData get() = NodeKind<ParentDataModifierNode>(0b1 shl 6)
    @JvmStatic
    inline val LayoutAware get() = NodeKind<LayoutAwareModifierNode>(0b1 shl 7)
    @JvmStatic
    inline val GlobalPositionAware get() = NodeKind<GlobalPositionAwareModifierNode>(0b1 shl 8)
    @JvmStatic
    inline val IntermediateMeasure get() = NodeKind<IntermediateLayoutModifierNode>(0b1 shl 9)
    // ...
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun calculateNodeKindSetFrom(element: Modifier.Element): Int {
    var mask = Nodes.Any.mask
    if (element is LayoutModifier) {
        mask = mask or Nodes.Layout
    }
    @OptIn(ExperimentalComposeUiApi::class)
    if (element is IntermediateLayoutModifier) {
        mask = mask or Nodes.IntermediateMeasure
    }
    if (element is DrawModifier) {
        mask = mask or Nodes.Draw
    }
    if (element is SemanticsModifier) {
        mask = mask or Nodes.Semantics
    }
    if (element is PointerInputModifier) {
        mask = mask or Nodes.PointerInput
    }
    if (
        element is ModifierLocalConsumer ||
        element is ModifierLocalProvider<*> ||
        // Special handling for FocusOrderModifier -- we have to use modifier local
        // consumers and providers for it.
        element is FocusOrderModifier
    ) {
        mask = mask or Nodes.Locals
    }
    if (element is OnGloballyPositionedModifier) {
        mask = mask or Nodes.GlobalPositionAware
    }
    if (element is ParentDataModifier) {
        mask = mask or Nodes.ParentData
    }
    if (
        element is OnPlacedModifier ||
        element is OnRemeasuredModifier ||
        element is LookaheadOnPlacedModifier
    ) {
        mask = mask or Nodes.LayoutAware
    }
    return mask
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun calculateNodeKindSetFrom(node: Modifier.Node): Int {
    var mask = Nodes.Any.mask
    if (node is LayoutModifierNode) {
        mask = mask or Nodes.Layout
    }
    if (node is DrawModifierNode) {
        mask = mask or Nodes.Draw
    }
    if (node is SemanticsModifierNode) {
        mask = mask or Nodes.Semantics
    }
    if (node is PointerInputModifierNode) {
        mask = mask or Nodes.PointerInput
    }
    if (node is ModifierLocalNode) {
        mask = mask or Nodes.Locals
    }
    if (node is ParentDataModifierNode) {
        mask = mask or Nodes.ParentData
    }
    if (node is LayoutAwareModifierNode) {
        mask = mask or Nodes.LayoutAware
    }
    if (node is GlobalPositionAwareModifierNode) {
        mask = mask or Nodes.GlobalPositionAware
    }
    if (node is IntermediateLayoutModifierNode) {
        mask = mask or Nodes.IntermediateMeasure
    }
    return mask
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun autoInvalidateNode(node: Modifier.Node) {
    if (node.isKind(Nodes.Layout) && node is LayoutModifierNode) {
        node.invalidateMeasurements()
    }
    if (node.isKind(Nodes.GlobalPositionAware) && node is GlobalPositionAwareModifierNode) {
        node.requireLayoutNode().invalidateMeasurements()
    }
    if (node.isKind(Nodes.Draw) && node is DrawModifierNode) {
        node.invalidateDraw()
    }
    if (node.isKind(Nodes.Semantics) && node is SemanticsModifierNode) {
        node.invalidateSemantics()
    }
    if (node.isKind(Nodes.ParentData) && node is ParentDataModifierNode) {
        node.invalidateParentData()
    }
}