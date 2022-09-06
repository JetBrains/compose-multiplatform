
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

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier

// TODO(lmr): this interface needs a better name
@ExperimentalComposeUiApi
interface DelegatableNode {
    val node: Modifier.Node
}

// TREE TRAVERSAL APIS
// For now, traversing the node tree and layout node tree will be kept out of public API.
// Some internal modifiers, such as Focus, PointerInput, etc. will all need to utilize this
// a bit, but I think we want to avoid giving this power to public API just yet. We can
// introduce this as valid cases arise
@ExperimentalComposeUiApi
internal fun DelegatableNode.localChild(mask: Int): Modifier.Node? {
    val child = node.child ?: return null
    if (child.aggregateChildKindSet and mask == 0) return null
    var next: Modifier.Node? = child
    while (next != null) {
        if (next.kindSet and mask != 0) {
            return next
        }
        next = next.child
    }
    return null
}

@ExperimentalComposeUiApi
internal fun DelegatableNode.localParent(mask: Int): Modifier.Node? {
    var next = node.parent
    while (next != null) {
        if (next.kindSet and mask != 0) {
            return next
        }
        next = next.parent
    }
    return null
}

@ExperimentalComposeUiApi
internal inline fun DelegatableNode.visitAncestors(mask: Int, block: (Modifier.Node) -> Unit) {
    // TODO(lmr): we might want to add some safety wheels to prevent this from being called
    //  while one of the chains is being diffed / updated. Although that might only be
    //  necessary for visiting subtree.
    check(node.isAttached)
    var node: Modifier.Node? = node.parent
    var layout: LayoutNode? = requireLayoutNode()
    while (layout != null) {
        val head = layout.nodes.head
        if (head.aggregateChildKindSet and mask != 0) {
            while (node != null) {
                if (node.kindSet and mask != 0) {
                    block(node)
                }
                node = node.parent
            }
        }
        layout = layout.parent
        node = layout?.nodes?.tail
    }
}

@ExperimentalComposeUiApi
internal fun DelegatableNode.nearestAncestor(mask: Int): Modifier.Node? {
    check(node.isAttached)
    var node: Modifier.Node? = node.parent
    var layout: LayoutNode? = requireLayoutNode()
    while (layout != null) {
        val head = layout.nodes.head
        if (head.aggregateChildKindSet and mask != 0) {
            while (node != null) {
                if (node.kindSet and mask != 0) {
                    return node
                }
                node = node.parent
            }
        }
        layout = layout.parent
        node = layout?.nodes?.tail
    }
    return null
}

@ExperimentalComposeUiApi
internal inline fun DelegatableNode.visitSubtree(mask: Int, block: (Modifier.Node) -> Unit) {
    // TODO(lmr): we might want to add some safety wheels to prevent this from being called
    //  while one of the chains is being diffed / updated.
    check(node.isAttached)
    var node: Modifier.Node? = node.child
    var layout: LayoutNode? = requireLayoutNode()
    // we use this bespoke data structure here specifically for traversing children. In the
    // depth first traversal you would typically do a `stack.addAll(node.children)` type
    // call, but to avoid enumerating the vector and moving into our stack, we simply keep
    // a stack of vectors and keep track of where we are in each
    val nodes = NestedVectorStack<LayoutNode>()
    while (layout != null) {
        // NOTE: the ?: is important here for the starting condition, since we are starting
        // at THIS node, and not the head of this node chain.
        node = node ?: layout.nodes.head
        if (node.aggregateChildKindSet and mask != 0) {
            while (node != null) {
                if (node.kindSet and mask != 0) {
                    block(node)
                }
                node = node.child
            }
            node = null
        }
        nodes.push(layout._children)
        layout = if (nodes.isNotEmpty()) nodes.pop() else null
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun MutableVector<Modifier.Node>.addLayoutNodeChildren(node: Modifier.Node) {
    node.requireLayoutNode()._children.forEach {
        add(it.nodes.head)
    }
}

@ExperimentalComposeUiApi
internal inline fun DelegatableNode.visitChildren(mask: Int, block: (Modifier.Node) -> Unit) {
    check(node.isAttached)
    val branches = mutableVectorOf<Modifier.Node>()
    val child = node.child
    if (child == null)
        branches.addLayoutNodeChildren(node)
    else
        branches.add(child)
    while (branches.isNotEmpty()) {
        val branch = branches.removeAt(branches.size)
        if (branch.aggregateChildKindSet and mask == 0) {
            branches.addLayoutNodeChildren(branch)
            // none of these nodes match the mask, so don't bother traversing them
            continue
        }
        var node: Modifier.Node? = branch
        while (node != null) {
            if (node.kindSet and mask != 0) {
                block(node)
                break
            }
            node = node.child
        }
    }
}

/**
 * visit the shallow tree of children of a given mask, but if block returns true, we will continue
 * traversing below it
 */
@ExperimentalComposeUiApi
internal inline fun DelegatableNode.visitSubtreeIf(mask: Int, block: (Modifier.Node) -> Boolean) {
    check(node.isAttached)
    val branches = mutableVectorOf<Modifier.Node>()
    val child = node.child
    if (child == null)
        branches.addLayoutNodeChildren(node)
    else
        branches.add(child)
    outer@ while (branches.isNotEmpty()) {
        val branch = branches.removeAt(branches.size - 1)
        if (branch.aggregateChildKindSet and mask != 0) {
            var node: Modifier.Node? = branch
            while (node != null) {
                if (node.kindSet and mask != 0) {
                    val diveDeeper = block(node)
                    if (!diveDeeper) continue@outer
                }
                node = node.child
            }
        }
        branches.addLayoutNodeChildren(branch)
    }
}

@ExperimentalComposeUiApi
internal inline fun DelegatableNode.visitLocalChildren(mask: Int, block: (Modifier.Node) -> Unit) {
    check(node.isAttached)
    val self = node
    if (self.aggregateChildKindSet and mask == 0) return
    var next = self.child
    while (next != null) {
        if (next.kindSet and mask != 0) {
            block(next)
        }
        next = next.child
    }
}

@ExperimentalComposeUiApi
internal inline fun DelegatableNode.visitLocalParents(mask: Int, block: (Modifier.Node) -> Unit) {
    check(node.isAttached)
    var next = node.parent
    while (next != null) {
        if (next.kindSet and mask != 0) {
            block(next)
        }
        next = next.parent
    }
}

@ExperimentalComposeUiApi
internal inline fun <reified T> DelegatableNode.visitLocalChildren(
    type: NodeKind<T>,
    block: (T) -> Unit
) = visitLocalChildren(type.mask) {
    if (it is T) block(it)
}

@ExperimentalComposeUiApi
internal inline fun <reified T> DelegatableNode.visitLocalParents(
    type: NodeKind<T>,
    block: (T) -> Unit
) = visitLocalParents(type.mask) {
    if (it is T) block(it)
}

@ExperimentalComposeUiApi
internal inline fun <reified T> DelegatableNode.localParent(type: NodeKind<T>): T? =
    localParent(type.mask) as? T

@ExperimentalComposeUiApi
internal inline fun <reified T> DelegatableNode.localChild(type: NodeKind<T>): T? =
    localChild(type.mask) as? T

@ExperimentalComposeUiApi
internal inline fun <reified T> DelegatableNode.visitAncestors(
    type: NodeKind<T>,
    block: (T) -> Unit
) = visitAncestors(type.mask) { if (it is T) block(it) }

@ExperimentalComposeUiApi
internal inline fun <reified T : Any> DelegatableNode.nearestAncestor(type: NodeKind<T>): T? =
    nearestAncestor(type.mask) as? T

@ExperimentalComposeUiApi
internal inline fun <reified T> DelegatableNode.visitSubtree(
    type: NodeKind<T>,
    block: (T) -> Unit
) = visitSubtree(type.mask) { if (it is T) block(it) }

@ExperimentalComposeUiApi
internal inline fun <reified T> DelegatableNode.visitChildren(
    type: NodeKind<T>,
    block: (T) -> Unit
) = visitChildren(type.mask) { if (it is T) block(it) }

@ExperimentalComposeUiApi
internal inline fun <reified T> DelegatableNode.visitSubtreeIf(
    type: NodeKind<T>,
    block: (T) -> Boolean
) = visitSubtreeIf(type.mask) { if (it is T) block(it) else true }

@ExperimentalComposeUiApi
internal fun DelegatableNode.has(type: NodeKind<*>): Boolean =
    node.aggregateChildKindSet and type.mask != 0

@ExperimentalComposeUiApi
internal fun DelegatableNode.requireCoordinator(kind: NodeKind<*>): NodeCoordinator {
    val coordinator = node.coordinator!!
    return if (coordinator.tail !== this)
        coordinator
    else if (kind.includeSelfInTraversal)
        coordinator.wrapped!!
    else
        coordinator
}

@ExperimentalComposeUiApi
internal fun DelegatableNode.requireLayoutNode(): LayoutNode = node.coordinator!!.layoutNode

@ExperimentalComposeUiApi
internal fun DelegatableNode.requireOwner(): Owner = requireLayoutNode().owner!!
