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
import androidx.compose.ui.focus.FocusEventModifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusOrderModifier
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.rotary.RotaryInputModifierNode
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
internal val NodeKind<*>.includeSelfInTraversal: Boolean
    get() = mask and Nodes.LayoutAware.mask != 0

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
    @JvmStatic
    inline val FocusTarget get() = NodeKind<FocusTargetModifierNode>(0b1 shl 10)
    @JvmStatic
    inline val FocusProperties get() = NodeKind<FocusPropertiesModifierNode>(0b1 shl 11)
    @JvmStatic
    inline val FocusEvent get() = NodeKind<FocusEventModifierNode>(0b1 shl 12)
    @JvmStatic
    inline val KeyInput get() = NodeKind<KeyInputModifierNode>(0b1 shl 13)
    @JvmStatic
    inline val RotaryInput get() = NodeKind<RotaryInputModifierNode>(0b1 shl 14)
    // ...
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun calculateNodeKindSetFrom(element: Modifier.Element): Int {
    var mask = Nodes.Any.mask
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
        element is ModifierLocalProvider<*>
    ) {
        mask = mask or Nodes.Locals
    }
    if (element is FocusEventModifier) {
        mask = mask or Nodes.FocusEvent
    }
    if (element is FocusOrderModifier) {
        mask = mask or Nodes.FocusProperties
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
    if (node is FocusTargetModifierNode) {
        mask = mask or Nodes.FocusTarget
    }
    if (node is FocusPropertiesModifierNode) {
        mask = mask or Nodes.FocusProperties
    }
    if (node is FocusEventModifierNode) {
        mask = mask or Nodes.FocusEvent
    }
    if (node is KeyInputModifierNode) {
        mask = mask or Nodes.KeyInput
    }
    if (node is RotaryInputModifierNode) {
        mask = mask or Nodes.RotaryInput
    }
    return mask
}

private const val Updated = 0
private const val Inserted = 1
private const val Removed = 2

@OptIn(ExperimentalComposeUiApi::class)
internal fun autoInvalidateRemovedNode(node: Modifier.Node) = autoInvalidateNode(node, Removed)

@OptIn(ExperimentalComposeUiApi::class)
internal fun autoInvalidateInsertedNode(node: Modifier.Node) = autoInvalidateNode(node, Inserted)

@OptIn(ExperimentalComposeUiApi::class)
internal fun autoInvalidateUpdatedNode(node: Modifier.Node) = autoInvalidateNode(node, Updated)

@OptIn(ExperimentalComposeUiApi::class)
private fun autoInvalidateNode(node: Modifier.Node, phase: Int) {
    check(node.isAttached)
    if (node.isKind(Nodes.Layout) && node is LayoutModifierNode) {
        node.invalidateMeasurements()
        if (phase == Removed) {
            val coordinator = node.requireCoordinator(Nodes.Layout)
            coordinator.onRelease()
        }
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
    if (node.isKind(Nodes.FocusTarget) && node is FocusTargetModifierNode) {
        when (phase) {
            // when we previously had focus target modifier on a node and then this modifier
            // is removed we need to notify the focus tree about so the focus state is reset.
            Removed -> node.onReset()
            else -> node.requireOwner().focusOwner.scheduleInvalidation(node)
        }
    }
    if (
        node.isKind(Nodes.FocusProperties) &&
        node is FocusPropertiesModifierNode &&
        node.specifiesCanFocusProperty()
    ) {
        when (phase) {
            Removed -> node.scheduleInvalidationOfAssociatedFocusTargets()
            else -> node.requireOwner().focusOwner.scheduleInvalidation(node)
        }
    }
    if (node.isKind(Nodes.FocusEvent) && node is FocusEventModifierNode && phase != Removed) {
        node.requireOwner().focusOwner.scheduleInvalidation(node)
    }
}

@ExperimentalComposeUiApi
private fun FocusPropertiesModifierNode.scheduleInvalidationOfAssociatedFocusTargets() {
    visitChildren(Nodes.FocusTarget) {
        // Schedule invalidation for the focus target,
        // which will cause it to recalculate focus properties.
        requireOwner().focusOwner.scheduleInvalidation(it)
    }
}

/**
 * This function checks if the FocusProperties node has set the canFocus [FocusProperties.canFocus]
 * property.
 *
 * We use a singleton CanFocusChecker to prevent extra allocations, and in doing so, we assume that
 * there won't be multiple concurrent calls of this function. This is not an issue since this is
 * called from the main thread, but if this changes in the future, replace the
 * [CanFocusChecker.reset] call with a new [FocusProperties] object for every invocation.
 */
@ExperimentalComposeUiApi
private fun FocusPropertiesModifierNode.specifiesCanFocusProperty(): Boolean {
    CanFocusChecker.reset()
    modifyFocusProperties(CanFocusChecker)
    return CanFocusChecker.isCanFocusSet()
}

private object CanFocusChecker : FocusProperties {
    private var canFocusValue: Boolean? = null
    override var canFocus: Boolean
        get() = checkNotNull(canFocusValue)
        set(value) { canFocusValue = value }
    fun isCanFocusSet(): Boolean = canFocusValue != null
    fun reset() { canFocusValue = null }
}