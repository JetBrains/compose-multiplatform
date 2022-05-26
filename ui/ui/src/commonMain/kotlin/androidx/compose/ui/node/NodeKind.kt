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

@file:Suppress("DEPRECATION")

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
internal value class NodeKind<T>(val mask: Long) {
    infix fun or(other: NodeKind<*>): Long = mask or other.mask
    infix fun or(other: Long): Long = mask or other
}
internal infix fun Long.or(other: NodeKind<*>): Long = this or other.mask

// For a given NodeCoordinator, the "LayoutAware" nodes that it is concerned with should include
// its own measureNode if the measureNode happens to implement LayoutAware. If the measureNode
// implements any other node interfaces, such as draw, those should be visited by the coordinator
// below them.
@OptIn(ExperimentalComposeUiApi::class)
internal val NodeKind<*>.includeSelfInTraversal: Boolean get() {
    return mask and Nodes.LayoutAware.mask != 0L
}

// Note that these don't inherit from Modifier.Node to allow for a single Modifier.Node
// instance to implement multiple Node interfaces

@OptIn(ExperimentalComposeUiApi::class)
internal object Nodes {
    val Layout = NodeKind<LayoutModifierNode>(0b1 shl 0)
    val Draw = NodeKind<DrawModifierNode>(0b1 shl 1)
    val Semantics = NodeKind<SemanticsModifierNode>(0b1 shl 2)
    val PointerInput = NodeKind<PointerInputModifierNode>(0b1 shl 3)
    val Locals = NodeKind<ModifierLocalNode>(0b1 shl 6)
    val ParentData = NodeKind<ParentDataModifierNode>(0b1 shl 7)
    val LayoutAware = NodeKind<LayoutAwareModifierNode>(0b1 shl 8)
    val GlobalPositionAware = NodeKind<GlobalPositionAwareModifierNode>(0b1 shl 9)
    val IntermediateMeasure = NodeKind<IntermediateLayoutModifierNode>(0b1 shl 10)
    // ...
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun calculateNodeKindSetFrom(element: Modifier.Element): Long {
    var mask = 0L
    if (element is LayoutModifier) {
        mask = mask or Nodes.Layout
    }
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
internal fun calculateNodeKindSetFrom(node: Modifier.Node): Long {
    var mask = 0L
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