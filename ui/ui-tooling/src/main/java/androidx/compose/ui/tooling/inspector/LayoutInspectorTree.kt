/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.tooling.inspector

import android.view.View
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.SlotTable
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.tooling.Group
import androidx.compose.ui.tooling.NodeGroup
import androidx.compose.ui.tooling.ParameterInformation
import androidx.compose.ui.tooling.R
import androidx.compose.ui.tooling.asTree
import androidx.compose.ui.unit.Density
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.absoluteValue

private val unwantedPackages = setOf(
    -1,
    packageNameHash("androidx.compose.ui"),
    packageNameHash("androidx.compose.runtime"),
    packageNameHash("androidx.compose.ui.tooling"),
    packageNameHash("androidx.compose.ui.selection"),
    packageNameHash("androidx.compose.ui.semantics")
)

private val unwantedCalls = setOf(
    "emit",
    "remember",
    "Inspectable",
    "Layout",
    "Providers",
    "SelectionContainer",
    "SelectionLayout"
)

private fun packageNameHash(packageName: String) =
    packageName.fold(0) { hash, char -> hash * 31 + char.toInt() }.absoluteValue

/**
 * Generator of a tree for the Layout Inspector.
 */
class LayoutInspectorTree {
    private val inlineClassConverter = InlineClassConverter()
    private val parameterFactory = ParameterFactory(inlineClassConverter)
    private val cache = ArrayDeque<MutableInspectorNode>()
    private var generatedId = -1L
    /** Map from [LayoutInfo] to the nearest [InspectorNode] that contains it */
    private val claimedNodes = IdentityHashMap<LayoutInfo, InspectorNode>()
    /** Map from parent tree to child trees that are about to be stitched together */
    private val treeMap = IdentityHashMap<MutableInspectorNode, MutableList<MutableInspectorNode>>()
    /** Map from owner node to child trees that are about to be stitched to this owner */
    private val ownerMap = IdentityHashMap<InspectorNode, MutableList<MutableInspectorNode>>()
    /** Set of tree nodes that were stitched into another tree */
    private val stitched =
        Collections.newSetFromMap(IdentityHashMap<MutableInspectorNode, Boolean>())

    /**
     * Converts the [SlotTable] set held by [view] into a list of root nodes.
     */
    @OptIn(InternalComposeApi::class)
    fun convert(view: View): List<InspectorNode> {
        parameterFactory.density = Density(view.context)
        @Suppress("UNCHECKED_CAST")
        val tables = view.getTag(R.id.inspection_slot_table_set) as? Set<SlotTable>
            ?: return emptyList()
        clear()
        val result = convert(tables)
        clear()
        return result
    }

    /**
     * Converts the [RawParameter]s of the [node] into displayable parameters.
     */
    fun convertParameters(node: InspectorNode): List<NodeParameter> {
        return node.parameters.mapNotNull { parameterFactory.create(node, it.name, it.value) }
    }

    /**
     * Reset the generated id. Nodes are assigned an id if there isn't a layout node id present.
     */
    fun resetGeneratedId() {
        generatedId = -1L
    }

    private fun clear() {
        cache.clear()
        inlineClassConverter.clear()
        claimedNodes.clear()
        treeMap.clear()
        ownerMap.clear()
        stitched.clear()
    }

    @OptIn(InternalComposeApi::class)
    private fun convert(tables: Set<SlotTable>): List<InspectorNode> {
        val trees = tables.map { convert(it) }
        return when (trees.size) {
            0 -> listOf()
            1 -> trees.first().children
            else -> stitchTreesByLayoutNode(trees)
        }
    }

    /**
     * Stitch separate trees together using the [LayoutInfo]s found in the [SlotTable]s.
     *
     * Some constructs in Compose (e.g. ModalDrawerLayout) will result is multiple [SlotTable]s.
     * This code will attempt to stitch the resulting [InspectorNode] trees together by looking
     * at the parent of each [LayoutInfo].
     * If this algorithm is successful the result of this function will be a list with a single
     * tree.
     */
    private fun stitchTreesByLayoutNode(trees: List<MutableInspectorNode>): List<InspectorNode> {
        val layoutToTreeMap = IdentityHashMap<LayoutInfo, MutableInspectorNode>()
        trees.forEach { tree -> tree.layoutNodes.forEach { layoutToTreeMap[it] = tree } }
        trees.forEach { tree ->
            val layout = tree.layoutNodes.lastOrNull()
            val parentLayout = generateSequence(layout) { it.parentInfo }.firstOrNull {
                val otherTree = layoutToTreeMap[it]
                otherTree != null && otherTree != tree
            }
            if (parentLayout != null) {
                val ownerNode = claimedNodes[parentLayout]
                val ownerTree = layoutToTreeMap[parentLayout]
                if (ownerNode != null && ownerTree != null) {
                    ownerMap.getOrPut(ownerNode) { mutableListOf() }.add(tree)
                    treeMap.getOrPut(ownerTree) { mutableListOf() }.add(tree)
                }
            }
        }
        var parentTree = findDeepParentTree()
        while (parentTree != null) {
            addSubTrees(parentTree)
            treeMap.remove(parentTree)
            parentTree = findDeepParentTree()
        }
        return trees.asSequence().filter { !stitched.contains(it) }.flatMap { it.children }.toList()
    }

    /**
     * Return a parent tree where the children trees (to be stitched under the parent) are not
     * a parent themselves. Do this to avoid rebuilding the same tree more than once.
     */
    private fun findDeepParentTree(): MutableInspectorNode? =
        treeMap.entries.asSequence()
            .filter { (_, children) -> children.none { treeMap.containsKey(it) } }
            .firstOrNull()?.key

    private fun addSubTrees(tree: MutableInspectorNode) {
        for ((index, child) in tree.children.withIndex()) {
            tree.children[index] = addSubTrees(child) ?: child
        }
    }

    /**
     * Rebuild [node] with any possible sub trees added (stitched in).
     * Return the rebuild node, or null if no changes were found in this node or its children.
     * Lazily allocate the new node to avoid unnecessary allocations.
     */
    private fun addSubTrees(node: InspectorNode): InspectorNode? {
        var newNode: MutableInspectorNode? = null
        for ((index, child) in node.children.withIndex()) {
            val newChild = addSubTrees(child)
            if (newChild != null) {
                val newCopy = newNode ?: newNode(node)
                newCopy.children[index] = newChild
                newNode = newCopy
            }
        }
        val trees = ownerMap[node]
        if (trees == null && newNode == null) {
            return null
        }
        val newCopy = newNode ?: newNode(node)
        if (trees != null) {
            trees.flatMapTo(newCopy.children) { it.children }
            stitched.addAll(trees)
        }
        return buildAndRelease(newCopy)
    }

    @OptIn(InternalComposeApi::class)
    private fun convert(table: SlotTable): MutableInspectorNode {
        val fakeParent = newNode()
        addToParent(fakeParent, listOf(convert(table.asTree())))
        return fakeParent
    }

    private fun convert(group: Group): MutableInspectorNode {
        val children = convertChildren(group)
        val parent = parse(group)
        addToParent(parent, children)
        return parent
    }

    private fun convertChildren(group: Group): List<MutableInspectorNode> {
        if (group.children.isEmpty()) {
            return emptyList()
        }
        val result = mutableListOf<MutableInspectorNode>()
        for (child in group.children) {
            val node = convert(child)
            if (node.name.isNotEmpty() || node.children.isNotEmpty() ||
                node.id != 0L || node.layoutNodes.isNotEmpty()
            ) {
                result.add(node)
            } else {
                release(node)
            }
        }
        return result
    }

    /**
     * Adds the nodes in [input] to the children of [parentNode].
     * Nodes without a reference to a Composable are skipped.
     * A single skipped render id and layoutNode will be added to [parentNode].
     */
    private fun addToParent(parentNode: MutableInspectorNode, input: List<MutableInspectorNode>) {
        var id: Long? = null
        input.forEach { node ->
            if (node.name.isEmpty()) {
                parentNode.children.addAll(node.children)
                if (node.id != 0L) {
                    // If multiple siblings with a render ids are dropped:
                    // Ignore them all. And delegate the drawing to a parent in the inspector.
                    id = if (id == null) node.id else 0L
                }
            } else {
                node.id = if (node.id != 0L) node.id else --generatedId
                val resultNode = node.build()
                // TODO: replace getOrPut with putIfAbsent which requires API level 24
                node.layoutNodes.forEach { claimedNodes.getOrPut(it) { resultNode } }
                parentNode.children.add(resultNode)
            }
            parentNode.layoutNodes.addAll(node.layoutNodes)
            release(node)
        }
        val nodeId = id
        parentNode.id = if (parentNode.id == 0L && nodeId != null) nodeId else parentNode.id
    }

    private fun parse(group: Group): MutableInspectorNode {
        val node = newNode()
        node.id = getRenderNode(group)
        ((group as? NodeGroup)?.node as? LayoutInfo)?.let { node.layoutNodes.add(it) }
        if (!parseCallLocation(group, node) && group.name.isNullOrEmpty()) {
            return markUnwanted(node)
        }
        group.name?.let { node.name = it }
        if (unwantedGroup(node)) {
            return markUnwanted(node)
        }
        val box = group.box
        node.top = box.top
        node.left = box.left
        node.height = box.bottom - box.top
        node.width = box.right - box.left
        if (node.height <= 0 && node.width <= 0) {
            return markUnwanted(node)
        }
        addParameters(group.parameters, node)
        return node
    }

    private fun markUnwanted(node: MutableInspectorNode): MutableInspectorNode {
        node.resetExceptIdLayoutNodesAndChildren()
        return node
    }

    private fun parseCallLocation(group: Group, node: MutableInspectorNode): Boolean {
        val location = group.location ?: return false
        val fileName = location.sourceFile ?: return false
        node.fileName = fileName
        node.packageHash = location.packageHash
        node.lineNumber = location.lineNumber
        node.offset = location.offset
        node.length = location.length
        return true
    }

    private fun getRenderNode(group: Group): Long =
        group.modifierInfo.asSequence()
            .map { it.extra }
            .filterIsInstance<OwnedLayer>()
            .map { it.layerId }
            .firstOrNull() ?: 0

    private fun addParameters(parameters: List<ParameterInformation>, node: MutableInspectorNode) =
        parameters.forEach { addParameter(it, node) }

    private fun addParameter(parameter: ParameterInformation, node: MutableInspectorNode) {
        val castedValue = castValue(parameter)
        node.parameters.add(RawParameter(parameter.name, castedValue))
    }

    private fun castValue(parameter: ParameterInformation): Any? {
        val value = parameter.value ?: return null
        if (parameter.inlineClass == null) return value
        return inlineClassConverter.castParameterValue(parameter.inlineClass, value)
    }

    private fun unwantedGroup(node: MutableInspectorNode): Boolean =
        (node.packageHash in unwantedPackages && node.name in unwantedCalls)

    private fun newNode(): MutableInspectorNode =
        if (cache.isNotEmpty()) cache.pop() else MutableInspectorNode()

    private fun newNode(copyFrom: InspectorNode): MutableInspectorNode =
        newNode().shallowCopy(copyFrom)

    private fun release(node: MutableInspectorNode) {
        node.reset()
        cache.add(node)
    }

    private fun buildAndRelease(node: MutableInspectorNode): InspectorNode {
        val result = node.build()
        release(node)
        return result
    }
}
