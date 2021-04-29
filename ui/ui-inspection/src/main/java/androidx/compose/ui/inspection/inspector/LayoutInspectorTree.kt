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

package androidx.compose.ui.inspection.inspector

import android.view.View
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.inspection.framework.ancestors
import androidx.compose.ui.inspection.framework.isRoot
import androidx.compose.ui.layout.GraphicLayerInfo
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.NodeGroup
import androidx.compose.ui.tooling.data.ParameterInformation
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toSize
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

val systemPackages = setOf(
    -1,
    packageNameHash("androidx.compose.animation"),
    packageNameHash("androidx.compose.animation.core"),
    packageNameHash("androidx.compose.desktop"),
    packageNameHash("androidx.compose.foundation"),
    packageNameHash("androidx.compose.foundation.layout"),
    packageNameHash("androidx.compose.foundation.text"),
    packageNameHash("androidx.compose.material"),
    packageNameHash("androidx.compose.material.ripple"),
    packageNameHash("androidx.compose.runtime"),
    packageNameHash("androidx.compose.ui"),
    packageNameHash("androidx.compose.ui.graphics.vector"),
    packageNameHash("androidx.compose.ui.layout"),
    packageNameHash("androidx.compose.ui.platform"),
    packageNameHash("androidx.compose.ui.tooling"),
    packageNameHash("androidx.compose.ui.selection"),
    packageNameHash("androidx.compose.ui.semantics"),
    packageNameHash("androidx.compose.ui.viewinterop"),
    packageNameHash("androidx.compose.ui.window"),
)

private val unwantedCalls = setOf(
    "CompositionLocalProvider",
    "Content",
    "Inspectable",
    "ProvideAndroidCompositionLocals",
    "ProvideCommonCompositionLocals",
)

private fun packageNameHash(packageName: String) =
    packageName.fold(0) { hash, char -> hash * 31 + char.toInt() }.absoluteValue

/**
 * Generator of a tree for the Layout Inspector.
 */
class LayoutInspectorTree {
    @Suppress("MemberVisibilityCanBePrivate")
    var hideSystemNodes = true
    private val inlineClassConverter = InlineClassConverter()
    private val parameterFactory = ParameterFactory(inlineClassConverter)
    private val cache = ArrayDeque<MutableInspectorNode>()
    private var generatedId = -1L
    private val rootLocation = IntArray(2)
    /** Map from [LayoutInfo] to the nearest [InspectorNode] that contains it */
    private val claimedNodes = IdentityHashMap<LayoutInfo, InspectorNode>()
    /** Map from parent tree to child trees that are about to be stitched together */
    private val treeMap = IdentityHashMap<MutableInspectorNode, MutableList<MutableInspectorNode>>()
    /** Map from owner node to child trees that are about to be stitched to this owner */
    private val ownerMap = IdentityHashMap<InspectorNode, MutableList<MutableInspectorNode>>()
    /** Map from semantics id to a list of merged semantics information */
    private val semanticsMap = mutableMapOf<Int, List<RawParameter>>()
    /** Set of tree nodes that were stitched into another tree */
    private val stitched =
        Collections.newSetFromMap(IdentityHashMap<MutableInspectorNode, Boolean>())

    /**
     * Converts the [CompositionData] set held by [view] into a list of root nodes.
     */
    @OptIn(InternalComposeApi::class)
    fun convert(view: View): List<InspectorNode> {
        parameterFactory.density = Density(view.context)
        @Suppress("UNCHECKED_CAST")
        val tables = view.getTag(R.id.inspection_slot_table_set) as?
            Set<CompositionData>
            ?: return emptyList()
        clear()
        collectSemantics(view)
        val result = convert(tables, view)
        clear()
        return result
    }

    /**
     * Extract the merged semantics for this semantics owner such that they can be added
     * to compose nodes during the conversion of Group nodes.
     */
    private fun collectSemantics(view: View) {
        val root = view as? RootForTest ?: return
        val nodes = root.semanticsOwner.getAllSemanticsNodes(mergingEnabled = true)
        nodes.forEach { node ->
            semanticsMap[node.id] = node.config.map { RawParameter(it.key.name, it.value) }
        }
    }

    /**
     * Converts the [RawParameter]s of the [node] into displayable parameters.
     */
    fun convertParameters(
        rootId: Long,
        node: InspectorNode,
        kind: ParameterKind,
        maxRecursions: Int,
        maxInitialIterableSize: Int
    ): List<NodeParameter> {
        val parameters = node.parametersByKind(kind)
        return parameters.mapIndexed { index, parameter ->
            parameterFactory.create(
                rootId,
                node.id,
                parameter.name,
                parameter.value,
                kind,
                index,
                maxRecursions,
                maxInitialIterableSize
            )
        }
    }

    /**
     * Converts a part of the [RawParameter] identified by [reference] into a
     * displayable parameter. If the parameter is some sort of a collection
     * then [startIndex] and [maxElements] describes the scope of the data returned.
     */
    fun expandParameter(
        rootId: Long,
        node: InspectorNode,
        reference: NodeParameterReference,
        startIndex: Int,
        maxElements: Int,
        maxRecursions: Int,
        maxInitialIterableSize: Int
    ): NodeParameter? {
        val parameters = node.parametersByKind(reference.kind)
        if (reference.parameterIndex !in parameters.indices) {
            return null
        }
        val parameter = parameters[reference.parameterIndex]
        return parameterFactory.expand(
            rootId,
            node.id,
            parameter.name,
            parameter.value,
            reference,
            startIndex,
            maxElements,
            maxRecursions,
            maxInitialIterableSize
        )
    }

    /**
     * Reset the generated id. Nodes are assigned an id if there isn't a layout node id present.
     */
    @Suppress("unused")
    fun resetGeneratedId() {
        generatedId = -1L
    }

    private fun clear() {
        rootLocation[0] = 0
        rootLocation[1] = 0
        cache.clear()
        inlineClassConverter.clear()
        claimedNodes.clear()
        treeMap.clear()
        ownerMap.clear()
        semanticsMap.clear()
        stitched.clear()
    }

    @OptIn(InternalComposeApi::class)
    private fun convert(tables: Set<CompositionData>, view: View): List<InspectorNode> {
        val decorView = view.ancestors().first { it.isRoot() }
        decorView.getLocationOnScreen(rootLocation)
        val trees = tables.mapNotNull { convert(it, view) }
        return when (trees.size) {
            0 -> listOf()
            1 -> addTree(mutableListOf(), trees.single())
            else -> stitchTreesByLayoutInfo(trees)
        }
    }

    /**
     * Stitch separate trees together using the [LayoutInfo]s found in the [CompositionData]s.
     *
     * Some constructs in Compose (e.g. ModalDrawer) will result is multiple
     * [CompositionData]s. This code will attempt to stitch the resulting [InspectorNode] trees
     * together by looking at the parent of each [LayoutInfo].
     *
     * If this algorithm is successful the result of this function will be a list with a single
     * tree.
     */
    private fun stitchTreesByLayoutInfo(trees: List<MutableInspectorNode>): List<InspectorNode> {
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
        val result = mutableListOf<InspectorNode>()
        trees.asSequence().filter { !stitched.contains(it) }.forEach { addTree(result, it) }
        return result
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
            trees.forEach { addTree(newCopy.children, it) }
            stitched.addAll(trees)
        }
        return buildAndRelease(newCopy)
    }

    /**
     * Add [tree] to the end of the [out] list.
     * The root nodes of [tree] may be a fake node that hold a list of [LayoutInfo].
     */
    private fun addTree(
        out: MutableList<InspectorNode>,
        tree: MutableInspectorNode
    ): List<InspectorNode> {
        tree.children.forEach {
            if (it.name.isNotEmpty()) {
                out.add(it)
            } else {
                out.addAll(it.children)
            }
        }
        return out
    }

    @OptIn(InternalComposeApi::class, UiToolingDataApi::class)
    private fun convert(table: CompositionData, view: View): MutableInspectorNode? {
        val fakeParent = newNode()
        addToParent(fakeParent, listOf(convert(table.asTree())), buildFakeChildNodes = true)
        return if (belongsToView(fakeParent.layoutNodes, view)) fakeParent else null
    }

    @OptIn(UiToolingDataApi::class)
    private fun convert(group: Group): MutableInspectorNode {
        val children = convertChildren(group)
        val parent = parse(group)
        addToParent(parent, children)
        return parent
    }

    @OptIn(UiToolingDataApi::class)
    private fun convertChildren(group: Group): List<MutableInspectorNode> {
        if (group.children.isEmpty()) {
            return emptyList()
        }
        val result = mutableListOf<MutableInspectorNode>()
        for (child in group.children) {
            val node = convert(child)
            if (node.name.isNotEmpty() ||
                node.id != 0L ||
                node.children.isNotEmpty() ||
                node.layoutNodes.isNotEmpty() ||
                node.mergedSemantics.isNotEmpty() ||
                node.unmergedSemantics.isNotEmpty()
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
     * Nodes without a reference to a wanted Composable are skipped unless [buildFakeChildNodes].
     * A single skipped render id and layoutNode will be added to [parentNode].
     */
    private fun addToParent(
        parentNode: MutableInspectorNode,
        input: List<MutableInspectorNode>,
        buildFakeChildNodes: Boolean = false
    ) {
        var id: Long? = null
        input.forEach { node ->
            if (node.name.isEmpty() && !(buildFakeChildNodes && node.layoutNodes.isNotEmpty())) {
                parentNode.children.addAll(node.children)
                if (node.id != 0L) {
                    // If multiple siblings with a render ids are dropped:
                    // Ignore them all. And delegate the drawing to a parent in the inspector.
                    id = if (id == null) node.id else 0L
                }
            } else {
                node.id = if (node.id != 0L) node.id else --generatedId
                val withSemantics = node.packageHash !in systemPackages
                val resultNode = node.build(withSemantics)
                // TODO: replace getOrPut with putIfAbsent which requires API level 24
                node.layoutNodes.forEach { claimedNodes.getOrPut(it) { resultNode } }
                parentNode.children.add(resultNode)
                if (withSemantics) {
                    node.mergedSemantics.clear()
                    node.unmergedSemantics.clear()
                }
            }
            if (node.bounds != null && sameBoundingRectangle(parentNode, node)) {
                parentNode.bounds = node.bounds
            }
            parentNode.layoutNodes.addAll(node.layoutNodes)
            parentNode.mergedSemantics.addAll(node.mergedSemantics)
            parentNode.unmergedSemantics.addAll(node.unmergedSemantics)
            release(node)
        }
        val nodeId = id
        parentNode.id = if (parentNode.id == 0L && nodeId != null) nodeId else parentNode.id
    }

    @OptIn(UiToolingDataApi::class)
    private fun parse(group: Group): MutableInspectorNode {
        val node = newNode()
        node.id = getRenderNode(group)
        parsePosition(group, node)
        parseLayoutInfo(group, node)
        if (node.height <= 0 && node.width <= 0) {
            return markUnwanted(node)
        }
        if (!parseCallLocation(group, node) && group.name.isNullOrEmpty()) {
            return markUnwanted(node)
        }
        group.name?.let { node.name = it }
        if (unwantedGroup(node)) {
            return markUnwanted(node)
        }
        addParameters(group.parameters, node)
        return node
    }

    @OptIn(UiToolingDataApi::class)
    private fun parsePosition(group: Group, node: MutableInspectorNode) {
        val box = group.box
        node.top = box.top + rootLocation[1]
        node.left = box.left + rootLocation[0]
        node.height = box.bottom - box.top
        node.width = box.right - box.left
    }

    @OptIn(UiToolingDataApi::class)
    private fun parseLayoutInfo(group: Group, node: MutableInspectorNode) {
        node.unmergedSemantics.addAll(
            group.modifierInfo.asSequence()
                .map { it.modifier }
                .filterIsInstance<SemanticsModifier>()
                .map { it.semanticsConfiguration }
                .flatMap { config -> config.map { RawParameter(it.key.name, it.value) } }
        )

        node.mergedSemantics.addAll(
            group.modifierInfo.asSequence()
                .map { it.modifier }
                .filterIsInstance<SemanticsModifier>()
                .map { it.id }
                .flatMap { semanticsMap[it].orEmpty() }
        )

        val layoutInfo = (group as? NodeGroup)?.node as? LayoutInfo ?: return
        node.layoutNodes.add(layoutInfo)
        val box = group.box
        val size = box.size.toSize()
        val coordinates = layoutInfo.coordinates
        val topLeft = toIntOffset(coordinates.localToWindow(Offset.Zero))
        val topRight = toIntOffset(coordinates.localToWindow(Offset(size.width, 0f)))
        val bottomRight = toIntOffset(coordinates.localToWindow(Offset(size.width, size.height)))
        val bottomLeft = toIntOffset(coordinates.localToWindow(Offset(0f, size.height)))
        if (
            topLeft.x == box.left && topLeft.y == box.top &&
            topRight.x == box.right && topRight.y == box.top &&
            bottomRight.x == box.right && bottomRight.y == box.bottom &&
            bottomLeft.x == box.left && bottomLeft.y == box.bottom
        ) {
            return
        }
        node.bounds = QuadBounds(
            topLeft.x, topLeft.y,
            topRight.x, topRight.y,
            bottomRight.x, bottomRight.y,
            bottomLeft.x, bottomLeft.y,
        )
    }

    private fun toIntOffset(offset: Offset): IntOffset =
        IntOffset(offset.x.roundToInt(), offset.y.roundToInt())

    private fun markUnwanted(node: MutableInspectorNode): MutableInspectorNode =
        node.apply { markUnwanted() }

    @OptIn(UiToolingDataApi::class)
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

    @OptIn(UiToolingDataApi::class)
    private fun getRenderNode(group: Group): Long =
        group.modifierInfo.asSequence()
            .map { it.extra }
            .filterIsInstance<GraphicLayerInfo>()
            .map { it.layerId }
            .firstOrNull() ?: 0

    @OptIn(ExperimentalComposeUiApi::class)
    private fun belongsToView(layoutNodes: List<LayoutInfo>, view: View): Boolean =
        layoutNodes.asSequence().flatMap { node ->
            node.getModifierInfo().asSequence()
                .map { it.extra }
                .filterIsInstance<GraphicLayerInfo>()
                .mapNotNull { it.ownerViewId }
        }.contains(view.uniqueDrawingId)

    @OptIn(UiToolingDataApi::class)
    private fun addParameters(parameters: List<ParameterInformation>, node: MutableInspectorNode) =
        parameters.forEach { addParameter(it, node) }

    @OptIn(UiToolingDataApi::class)
    private fun addParameter(parameter: ParameterInformation, node: MutableInspectorNode) {
        val castedValue = castValue(parameter)
        node.parameters.add(RawParameter(parameter.name, castedValue))
    }

    @OptIn(UiToolingDataApi::class)
    private fun castValue(parameter: ParameterInformation): Any? {
        val value = parameter.value ?: return null
        if (parameter.inlineClass == null) return value
        return inlineClassConverter.castParameterValue(parameter.inlineClass, value)
    }

    private fun unwantedGroup(node: MutableInspectorNode): Boolean =
        (node.packageHash in systemPackages && hideSystemNodes) ||
            node.name.isEmpty() ||
            node.name.startsWith("remember") ||
            node.name in unwantedCalls

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

    private fun sameBoundingRectangle(
        node1: MutableInspectorNode,
        node2: MutableInspectorNode
    ): Boolean =
        node1.left == node2.left &&
            node1.top == node2.top &&
            node1.width == node2.width &&
            node1.height == node2.height
}
