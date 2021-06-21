/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.semantics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeWrapper
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach

/**
 * Signature for a function that is called for each [SemanticsNode].
 *
 * Return false to stop visiting nodes.
 *
 * Used by [SemanticsNode.visitChildren].
 */
internal typealias SemanticsNodeVisitor = (node: SemanticsNode) -> Boolean

/**
 * A list of key/value pairs associated with a layout node or its subtree.
 *
 * Each SemanticsNode takes its id and initial key/value list from the
 * outermost modifier on one layout node.  It also contains the "collapsed" configuration
 * of any other semantics modifiers on the same layout node, and if "mergeDescendants" is
 * specified and enabled, also the "merged" configuration of its subtree.
 */
class SemanticsNode internal constructor(
    /*
     * This is expected to be the outermost semantics modifier on a layout node.
     */
    internal val outerSemanticsNodeWrapper: SemanticsWrapper,
    /**
     * mergingEnabled specifies whether mergeDescendants config has any effect.
     *
     * If true, then mergeDescendants nodes will merge up all properties from child
     * semantics nodes and remove those children from "children", with the exception
     * of nodes that themselves have mergeDescendants.  If false, then mergeDescendants
     * has no effect.
     *
     * mergingEnabled is typically true or false consistently on every node of a SemanticsNode tree.
     */
    val mergingEnabled: Boolean
) {
    // We emit fake nodes for several cases. One is to prevent the content description clobbering
    // issue. Another case is  temporary workaround to retrieve default role ordering for Button
    // and other selection controls.
    internal var isFake = false
    private var fakeNodeParent: SemanticsNode? = null

    internal val unmergedConfig = outerSemanticsNodeWrapper.collapsedSemanticsConfiguration()
    val id: Int = outerSemanticsNodeWrapper.modifier.id

    /**
     * The [LayoutInfo] that this is associated with.
     */
    val layoutInfo: LayoutInfo get() = layoutNode

    /**
     * The [root][RootForTest] this node is attached to.
     */
    val root: RootForTest? get() = layoutNode.owner?.rootForTest

    /**
     * The [LayoutNode] that this is associated with.
     */
    internal val layoutNode: LayoutNode = outerSemanticsNodeWrapper.layoutNode

    // GEOMETRY

    /**
     * The size of the bounding box for this node, with no clipping applied
     */
    val size: IntSize get() = findWrapperToGetBounds().size

    /**
     * The bounding box for this node relative to the root of this Compose hierarchy, with
     * clipping applied. To get the bounds with no clipping applied, use
     * Rect([positionInRoot], [size].toSize())
     */
    val boundsInRoot: Rect
        get() {
            if (!layoutNode.isAttached) return Rect.Zero
            return this.findWrapperToGetBounds().boundsInRoot()
        }

    /**
     * The position of this node relative to the root of this Compose hierarchy, with no clipping
     * applied
     */
    val positionInRoot: Offset
        get() {
            if (!layoutNode.isAttached) return Offset.Zero
            return findWrapperToGetBounds().positionInRoot()
        }

    /**
     * The bounding box for this node relative to the screen, with clipping applied. To get the
     * bounds with no clipping applied, use PxBounds([positionInWindow], [size].toSize())
     */
    val boundsInWindow: Rect
        get() {
            if (!layoutNode.isAttached) return Rect.Zero
            return findWrapperToGetBounds().boundsInWindow()
        }

    /**
     * The position of this node relative to the screen, with no clipping applied
     */
    val positionInWindow: Offset
        get() {
            if (!layoutNode.isAttached) return Offset.Zero
            return findWrapperToGetBounds().positionInWindow()
        }

    /**
     * Returns the position of an [alignment line][AlignmentLine], or [AlignmentLine.Unspecified]
     * if the line is not provided.
     */
    fun getAlignmentLinePosition(alignmentLine: AlignmentLine): Int {
        return findWrapperToGetBounds()[alignmentLine]
    }

    // CHILDREN

    /**
     * The list of semantics properties of this node.
     *
     * This includes all properties attached as modifiers to the current layout node.
     * In addition, if mergeDescendants and mergingEnabled are both true, then it
     * also includes the semantics properties of descendant nodes.
     */
    // TODO(b/184376083): This is too expensive for a val (full subtree recreation every call);
    //               optimize this when the merging algorithm is improved.
    val config: SemanticsConfiguration
        get() {
            if (isMergingSemanticsOfDescendants) {
                val mergedConfig = unmergedConfig.copy()
                mergeConfig(mergedConfig)
                return mergedConfig
            } else {
                return unmergedConfig
            }
        }

    private fun mergeConfig(mergedConfig: SemanticsConfiguration) {
        if (!unmergedConfig.isClearingSemantics) {
            unmergedChildren().fastForEach { child ->
                // Don't merge children that themselves merge all their descendants (because that
                // indicates they're independently screen-reader-focusable).
                if (!child.isFake && !child.isMergingSemanticsOfDescendants) {
                    mergedConfig.mergeChild(child.unmergedConfig)
                    child.mergeConfig(mergedConfig)
                }
            }
        }
    }

    private val isMergingSemanticsOfDescendants: Boolean
        get() = mergingEnabled && unmergedConfig.isMergingSemanticsOfDescendants

    internal fun unmergedChildren(sortByBounds: Boolean = false): List<SemanticsNode> {
        if (this.isFake) return listOf()
        val unmergedChildren: MutableList<SemanticsNode> = mutableListOf()

        val semanticsChildren = if (sortByBounds) {
            this.layoutNode.findOneLayerOfSemanticsWrappersSortedByBounds()
        } else {
            this.layoutNode.findOneLayerOfSemanticsWrappers()
        }
        semanticsChildren.fastForEach { semanticsChild ->
            unmergedChildren.add(SemanticsNode(semanticsChild, mergingEnabled))
        }

        emitFakeNodes(unmergedChildren)

        return unmergedChildren
    }

    /**
     * Contains the children in inverse hit test order (i.e. paint order).
     *
     * Note that if mergingEnabled and mergeDescendants are both true, then there
     * are no children (except those that are themselves mergeDescendants).
     */
    // TODO(b/184376083): This is too expensive for a val (full subtree recreation every call);
    //               optimize this when the merging algorithm is improved.
    val children: List<SemanticsNode>
        get() = getChildren(sortByBounds = false)

    /**
     * Contains the children sorted by bounds: top to down, left to right(right to left in RTL
     * mode).
     *
     * Note that if mergingEnabled and mergeDescendants are both true, then there
     * are no children (except those that are themselves mergeDescendants).
     */
    // TODO(b/184376083): This is too expensive for a val (full subtree recreation every call);
    //               optimize this when the merging algorithm is improved.
    internal val childrenSortedByBounds: List<SemanticsNode>
        get() = getChildren(sortByBounds = true)

    private fun getChildren(sortByBounds: Boolean): List<SemanticsNode> {
        // Replacing semantics never appear to have any children in the merged tree.
        if (mergingEnabled && unmergedConfig.isClearingSemantics) {
            return listOf()
        }

        if (isMergingSemanticsOfDescendants) {
            // In most common merging scenarios like Buttons, this will return nothing.
            // In cases like a clickable Row itself containing a Button, this will
            // return the Button as a child.
            return findOneLayerOfMergingSemanticsNodes(sortByBounds = sortByBounds)
        }

        return unmergedChildren(sortByBounds)
    }

    /**
     * Visits the immediate children of this node.
     *
     * This function calls visitor for each immediate child until visitor returns
     * false.
     */
    private fun visitChildren(visitor: SemanticsNodeVisitor) {
        children.fastForEach {
            if (!visitor(it)) {
                return
            }
        }
    }

    /**
     * Visit all the descendants of this node.  *
     * This function calls visitor for each descendant in a pre-order traversal
     * until visitor returns false. Returns true if all the visitor calls
     * returned true, otherwise returns false.
     */
    internal fun visitDescendants(visitor: SemanticsNodeVisitor): Boolean {
        children.fastForEach {
            if (!visitor(it) || !it.visitDescendants(visitor))
                return false
        }
        return true
    }

    /**
     * Whether this SemanticNode is the root of a tree or not
     */
    val isRoot: Boolean
        get() = parent == null

    /** The parent of this node in the tree. */
    val parent: SemanticsNode?
        get() {
            if (fakeNodeParent != null) return fakeNodeParent
            var node: LayoutNode? = null
            if (mergingEnabled) {
                node = this.layoutNode.findClosestParentNode {
                    it.outerSemantics
                        ?.collapsedSemanticsConfiguration()
                        ?.isMergingSemanticsOfDescendants == true
                }
            }

            if (node == null) {
                node = this.layoutNode.findClosestParentNode { it.outerSemantics != null }
            }

            val outerSemantics = node?.outerSemantics
            if (outerSemantics == null)
                return null

            return SemanticsNode(outerSemantics, mergingEnabled)
        }

    private fun findOneLayerOfMergingSemanticsNodes(
        list: MutableList<SemanticsNode> = mutableListOf(),
        sortByBounds: Boolean = false
    ): List<SemanticsNode> {
        unmergedChildren(sortByBounds).fastForEach { child ->
            if (child.isMergingSemanticsOfDescendants) {
                list.add(child)
            } else {
                if (!child.unmergedConfig.isClearingSemantics) {
                    child.findOneLayerOfMergingSemanticsNodes(list)
                }
            }
        }
        return list
    }

    /**
     * If the node is merging the descendants, we'll use the outermost semantics modifier that has
     * mergeDescendants == true to report the bounds, size and position of the node. For majority
     * of use cases it means that accessibility bounds will be equal to the clickable area.
     * Otherwise the outermost semantics will be used to report bounds, size and position.
     */
    private fun findWrapperToGetBounds(): LayoutNodeWrapper {
        return if (unmergedConfig.isMergingSemanticsOfDescendants) {
            layoutNode.outerMergingSemantics ?: outerSemanticsNodeWrapper
        } else {
            outerSemanticsNodeWrapper
        }
    }

    // Fake nodes
    private fun emitFakeNodes(unmergedChildren: MutableList<SemanticsNode>) {
        val nodeRole = this.role
        if (nodeRole != null && unmergedConfig.isMergingSemanticsOfDescendants &&
            unmergedChildren.isNotEmpty()
        ) {
            val fakeNode = fakeSemanticsNode(nodeRole) {
                this.role = nodeRole
            }
            unmergedChildren.add(fakeNode)
        }

        // Fake node for contentDescription clobbering issue
        if (unmergedConfig.contains(SemanticsProperties.ContentDescription) &&
            unmergedChildren.isNotEmpty() && unmergedConfig.isMergingSemanticsOfDescendants
        ) {
            val contentDescription =
                this.unmergedConfig.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
            if (contentDescription != null) {
                val fakeNode = fakeSemanticsNode(null) {
                    this.contentDescription = contentDescription
                }
                unmergedChildren.add(0, fakeNode)
            }
        }
    }

    private fun fakeSemanticsNode(
        role: Role?,
        properties: SemanticsPropertyReceiver.() -> Unit
    ): SemanticsNode {
        val fakeNode = SemanticsNode(
            outerSemanticsNodeWrapper = SemanticsWrapper(
                wrapped = LayoutNode(isVirtual = true).innerLayoutNodeWrapper,
                semanticsModifier = SemanticsModifierCore(
                    if (role != null) this.roleFakeNodeId() else contentDescriptionFakeNodeId(),
                    mergeDescendants = false,
                    clearAndSetSemantics = false,
                    properties = properties
                )
            ),
            mergingEnabled = false
        )
        fakeNode.isFake = true
        fakeNode.fakeNodeParent = this
        return fakeNode
    }
}

/**
 * Returns the outermost semantics node on a LayoutNode.
 */
internal val LayoutNode.outerSemantics: SemanticsWrapper?
    get() = outerLayoutNodeWrapper.nearestSemantics { true }

internal val LayoutNode.outerMergingSemantics
    get() = outerLayoutNodeWrapper.nearestSemantics {
        it.modifier.semanticsConfiguration.isMergingSemanticsOfDescendants
    }

/**
 * Returns the nearest semantics wrapper starting from a LayoutNodeWrapper.
 */
internal inline fun LayoutNodeWrapper.nearestSemantics(
    predicate: (SemanticsWrapper) -> Boolean
): SemanticsWrapper? {
    var wrapper: LayoutNodeWrapper? = this
    while (wrapper != null) {
        if (wrapper is SemanticsWrapper && predicate(wrapper)) return wrapper
        wrapper = wrapper.wrapped
    }
    return null
}

internal fun SemanticsNode.findChildById(id: Int): SemanticsNode? {
    if (this.id == id) return this
    children.fastForEach {
        val result = it.findChildById(id)
        if (result != null) return result
    }
    return null
}

private fun LayoutNode.findOneLayerOfSemanticsWrappers(
    list: MutableList<SemanticsWrapper> = mutableListOf()
): List<SemanticsWrapper> {
    zSortedChildren.forEach { child ->
        val outerSemantics = child.outerSemantics
        if (outerSemantics != null) {
            list.add(outerSemantics)
        } else {
            child.findOneLayerOfSemanticsWrappers(list)
        }
    }
    return list
}

/**
 * Executes [selector] on every parent of this [LayoutNode] and returns the closest
 * [LayoutNode] to return `true` from [selector] or null if [selector] returns false
 * for all ancestors.
 */
private fun LayoutNode.findClosestParentNode(selector: (LayoutNode) -> Boolean): LayoutNode? {
    var currentParent = this.parent
    while (currentParent != null) {
        if (selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parent
        }
    }

    return null
}

private val SemanticsNode.role get() = this.unmergedConfig.getOrNull(SemanticsProperties.Role)
private fun SemanticsNode.contentDescriptionFakeNodeId() = this.id + 2_000_000_000
private fun SemanticsNode.roleFakeNodeId() = this.id + 1_000_000_000
