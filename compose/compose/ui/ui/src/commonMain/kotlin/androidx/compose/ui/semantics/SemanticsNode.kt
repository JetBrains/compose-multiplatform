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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.collapsedSemanticsConfiguration
import androidx.compose.ui.node.requireCoordinator
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.touchBoundsInRoot
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach

/**
 * A list of key/value pairs associated with a layout node or its subtree.
 *
 * Each SemanticsNode takes its id and initial key/value list from the
 * outermost modifier on one layout node.  It also contains the "collapsed" configuration
 * of any other semantics modifiers on the same layout node, and if "mergeDescendants" is
 * specified and enabled, also the "merged" configuration of its subtree.
 */
@OptIn(ExperimentalComposeUiApi::class)
class SemanticsNode internal constructor(
    /*
     * This is expected to be the outermost semantics modifier on a layout node.
     */
    internal val outerSemanticsNode: SemanticsModifierNode,
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
    val mergingEnabled: Boolean,

    /**
     * The [LayoutNode] that this is associated with.
     */
    internal val layoutNode: LayoutNode = outerSemanticsNode.requireLayoutNode()
) {
    // We emit fake nodes for several cases. One is to prevent the content description clobbering
    // issue. Another case is  temporary workaround to retrieve default role ordering for Button
    // and other selection controls.
    internal var isFake = false
    private var fakeNodeParent: SemanticsNode? = null

    internal val unmergedConfig = outerSemanticsNode.collapsedSemanticsConfiguration()

    /**
     * The [LayoutInfo] that this is associated with.
     */
    val layoutInfo: LayoutInfo get() = layoutNode

    /**
     * The [root][RootForTest] this node is attached to.
     */
    val root: RootForTest? get() = layoutNode.owner?.rootForTest

    val id: Int = layoutNode.semanticsId

    // GEOMETRY

    /**
     * The rectangle of the touchable area.
     *
     * If this is a clickable region, this is the rectangle that accepts touch input. This can
     * be larger than [size] when the layout is less than
     * [ViewConfiguration.minimumTouchTargetSize]
     */
    val touchBoundsInRoot: Rect
        get() {
            val entity = if (unmergedConfig.isMergingSemanticsOfDescendants) {
                (layoutNode.outerMergingSemantics ?: outerSemanticsNode)
            } else {
                outerSemanticsNode
            }
            return entity.touchBoundsInRoot()
        }

    /**
     * The size of the bounding box for this node, with no clipping applied
     */
    val size: IntSize
        get() = findCoordinatorToGetBounds()?.size ?: IntSize.Zero

    /**
     * The bounding box for this node relative to the root of this Compose hierarchy, with
     * clipping applied. To get the bounds with no clipping applied, use
     * Rect([positionInRoot], [size].toSize())
     */
    val boundsInRoot: Rect
        get() = findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.boundsInRoot()
            ?: Rect.Zero

    /**
     * The position of this node relative to the root of this Compose hierarchy, with no clipping
     * applied
     */
    val positionInRoot: Offset
        get() = findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.positionInRoot()
            ?: Offset.Zero

    /**
     * The bounding box for this node relative to the screen, with clipping applied. To get the
     * bounds with no clipping applied, use PxBounds([positionInWindow], [size].toSize())
     */
    val boundsInWindow: Rect
        get() = findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.boundsInWindow()
            ?: Rect.Zero

    /**
     * The position of this node relative to the screen, with no clipping applied
     */
    val positionInWindow: Offset
        get() = findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.positionInWindow()
            ?: Offset.Zero

    /**
     * Whether this node is transparent.
     */
    internal val isTransparent: Boolean
        get() = findCoordinatorToGetBounds()?.isTransparent() ?: false

    /**
     * Returns the position of an [alignment line][AlignmentLine], or [AlignmentLine.Unspecified]
     * if the line is not provided.
     */
    fun getAlignmentLinePosition(alignmentLine: AlignmentLine): Int {
        return findCoordinatorToGetBounds()?.get(alignmentLine) ?: AlignmentLine.Unspecified
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
                if (!child.isMergingSemanticsOfDescendants) {
                    mergedConfig.mergeChild(child.unmergedConfig)
                    child.mergeConfig(mergedConfig)
                }
            }
        }
    }

    private val isMergingSemanticsOfDescendants: Boolean
        get() = mergingEnabled && unmergedConfig.isMergingSemanticsOfDescendants

    internal fun unmergedChildren(
        includeFakeNodes: Boolean = false
    ): List<SemanticsNode> {
        // TODO(lmr): we should be able to do this more efficiently using visitSubtree
        if (this.isFake) return listOf()
        val unmergedChildren: MutableList<SemanticsNode> = mutableListOf()

        val semanticsChildren = this.layoutNode.findOneLayerOfSemanticsWrappers()

        semanticsChildren.fastForEach { semanticsChild ->
            unmergedChildren.add(SemanticsNode(semanticsChild, mergingEnabled))
        }

        if (includeFakeNodes) {
            emitFakeNodes(unmergedChildren)
        }

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
        get() = getChildren(
            includeReplacedSemantics = !mergingEnabled,
            includeFakeNodes = false
        )

    /**
     * Contains the children in inverse hit test order (i.e. paint order).
     *
     * Unlike [children] property that includes replaced semantics nodes in unmerged tree, here
     * node marked as [clearAndSetSemantics] will not have children.
     * This property is primarily used in Accessibility delegate.
     */
    internal val replacedChildren: List<SemanticsNode>
        get() = getChildren(
            includeReplacedSemantics = false,
            includeFakeNodes = true
        )

    /**
     * @param includeReplacedSemantics if true, the result will contain children of nodes marked
     * as [clearAndSetSemantics]. For accessibility we always use false, but in testing and
     * debugging we should be able to investigate both
     * @param includeFakeNodes if true, the tree will include fake nodes. For accessibility we
     * set to true, but for testing purposes we don't want to expose the fake nodes and therefore
     * set to false. When Talkback can properly handle unmerged tree, fake nodes will be removed
     * and so will be this parameter.
     */
    private fun getChildren(
        includeReplacedSemantics: Boolean,
        includeFakeNodes: Boolean
    ): List<SemanticsNode> {
        if (!includeReplacedSemantics && unmergedConfig.isClearingSemantics) {
            return listOf()
        }

        if (isMergingSemanticsOfDescendants) {
            // In most common merging scenarios like Buttons, this will return nothing.
            // In cases like a clickable Row itself containing a Button, this will
            // return the Button as a child.
            return findOneLayerOfMergingSemanticsNodes()
        }

        return unmergedChildren(includeFakeNodes)
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
        list: MutableList<SemanticsNode> = mutableListOf()
    ): List<SemanticsNode> {
        unmergedChildren().fastForEach { child ->
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
    internal fun findCoordinatorToGetBounds(): NodeCoordinator? {
        if (isFake) return parent?.findCoordinatorToGetBounds()
        val semanticsModifierNode = layoutNode.outerMergingSemantics
            .takeIf { unmergedConfig.isMergingSemanticsOfDescendants } ?: outerSemanticsNode
        return semanticsModifierNode.requireCoordinator(Nodes.Semantics)
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
            outerSemanticsNode = object : SemanticsModifierNode, Modifier.Node() {
                override val semanticsConfiguration = SemanticsConfiguration().also {
                    it.isMergingSemanticsOfDescendants = false
                    it.isClearingSemantics = false
                    it.properties()
                }
            },
            mergingEnabled = false,
            layoutNode = LayoutNode(
                    isVirtual = true,
                    semanticsId =
                        if (role != null) roleFakeNodeId() else contentDescriptionFakeNodeId()
                ),
        )
        fakeNode.isFake = true
        fakeNode.fakeNodeParent = this
        return fakeNode
    }
}

/**
 * Returns the outermost semantics node on a LayoutNode.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal val LayoutNode.outerSemantics: SemanticsModifierNode?
    get() = nodes.head(Nodes.Semantics)

@OptIn(ExperimentalComposeUiApi::class)
internal val LayoutNode.outerMergingSemantics: SemanticsModifierNode?
    get() = nodes.firstFromHead(Nodes.Semantics) {
        it.semanticsConfiguration.isMergingSemanticsOfDescendants
    }

@OptIn(ExperimentalComposeUiApi::class)
private fun LayoutNode.findOneLayerOfSemanticsWrappers(
    list: MutableList<SemanticsModifierNode> = mutableListOf()
): List<SemanticsModifierNode> {
    // TODO(lmr): visitChildren would be great for this but we would lose the zSorted bit...
    //  i wonder if we can optimize this for the common case of no z-sortedness going on.
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
internal fun LayoutNode.findClosestParentNode(selector: (LayoutNode) -> Boolean): LayoutNode? {
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