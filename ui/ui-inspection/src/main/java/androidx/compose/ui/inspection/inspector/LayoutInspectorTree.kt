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
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.ui.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.inspection.data.ContextCache
import androidx.compose.ui.inspection.data.ParameterInformation
import androidx.compose.ui.inspection.data.SourceContext
import androidx.compose.ui.inspection.data.SourceLocation
import androidx.compose.ui.inspection.data.asLazyTree
import androidx.compose.ui.layout.GraphicLayerInfo
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.node.Ref
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
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
    packageNameHash("androidx.compose.runtime.saveable"),
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

/**
 * The [InspectorNode.id] will be populated with:
 * - the layerId from a LayoutNode if this exists
 * - an id generated from an Anchor instance from the SlotTree if this exists
 * - a generated id if none of the above ids are available
 *
 * The interval -10000..-2 is reserved for the generated ids.
 */
@VisibleForTesting
const val RESERVED_FOR_GENERATED_IDS = -10000L
const val PLACEHOLDER_ID = Long.MAX_VALUE

private val emptySize = IntSize(0, 0)

private val unwantedCalls = setOf(
    "CompositionLocalProvider",
    "Content",
    "Inspectable",
    "ProvideAndroidCompositionLocals",
    "ProvideCommonCompositionLocals",
)

@VisibleForTesting
fun packageNameHash(packageName: String) =
    packageName.fold(0) { hash, char -> hash * 31 + char.code }.absoluteValue

/**
 * Generator of a tree for the Layout Inspector.
 */
class LayoutInspectorTree {
    @Suppress("MemberVisibilityCanBePrivate")
    var hideSystemNodes = true
    var includeNodesOutsizeOfWindow = true
    var includeAllParameters = true
    private var searchingForAnchorId = false
    private var includeParametersForAnchorHash = 0
    private var foundNode: InspectorNode? = null
    private var windowSize = emptySize
    private val inlineClassConverter = InlineClassConverter()
    private val parameterFactory = ParameterFactory(inlineClassConverter)
    private val cache = ArrayDeque<MutableInspectorNode>()
    private var generatedId = -1L
    private val subCompositions = SubCompositionRoots()
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
    private val contextCache = ContextCache()

    /**
     * Converts the [CompositionData] set held by [view] into a list of root nodes.
     */
    fun convert(view: View): List<InspectorNode> {
        windowSize = IntSize(view.width, view.height)
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

    fun findParameters(view: View, anchorHash: Int): InspectorNode? {
        windowSize = IntSize(view.width, view.height)
        parameterFactory.density = Density(view.context)
        @Suppress("UNCHECKED_CAST")
        val tables = view.getTag(R.id.inspection_slot_table_set) as?
            Set<CompositionData>
            ?: return null
        clear()
        searchingForAnchorId = true
        includeParametersForAnchorHash = anchorHash
        val node = tables.firstNotNullOfOrNull { findParameters(it) }
        clear()
        return node
    }

    /**
     * Add the roots to sub compositions that may have been collected from a different SlotTree.
     *
     * See [SubCompositionRoots] for details.
     */
    fun addSubCompositionRoots(view: View, nodes: List<InspectorNode>): List<InspectorNode> =
        subCompositions.addRoot(view, nodes)

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
                node.anchorHash,
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
            node.anchorHash,
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
     * Reset any state accumulated between windows.
     */
    @Suppress("unused")
    fun resetAccumulativeState() {
        subCompositions.resetAccumulativeState()
        parameterFactory.clearReferenceCache()
        // Reset the generated id. Nodes are assigned an id if there isn't a layout node id present.
        generatedId = -1L
    }

    private fun clear() {
        cache.clear()
        inlineClassConverter.clear()
        claimedNodes.clear()
        treeMap.clear()
        ownerMap.clear()
        semanticsMap.clear()
        stitched.clear()
        subCompositions.clear()
        searchingForAnchorId = false
        includeParametersForAnchorHash = 0
        foundNode = null
    }

    private fun convert(tables: Set<CompositionData>, view: View): List<InspectorNode> {
        val trees = tables.mapNotNull { convert(view, it) }
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

    private fun convert(view: View, table: CompositionData): MutableInspectorNode? {
        val fakeParent = newNode()
        val group = table.asLazyTree(::convert, contextCache) ?: return null
        addToParent(fakeParent, listOf(group), buildFakeChildNodes = true)
        return if (belongsToView(fakeParent.layoutNodes, view)) fakeParent else null
    }

    private fun findParameters(table: CompositionData): InspectorNode? {
        table.asLazyTree(::convert, contextCache) ?: return null
        return foundNode
    }

    private fun convert(
        group: CompositionGroup,
        context: SourceContext,
        children: List<MutableInspectorNode>
    ): MutableInspectorNode {
        val parent = parse(group, context, children)
        subCompositions.captureNode(parent, context)
        addToParent(parent, children)
        return parent
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
        if (parentNode.name == "AndroidView") {
            // Special case:
            // We may have captured the View id from an AndroidView Composable.
            // Add the viewId to the child ComposeNode that should be present.
            input.singleOrNull { it.name == "ComposeNode" }?.viewId = subCompositions.latestViewId()
        }

        var id: Long? = null
        input.forEach { node ->
            if (node.name.isEmpty() && !(buildFakeChildNodes && node.layoutNodes.isNotEmpty())) {
                parentNode.children.addAll(node.children)
                if (node.id > UNDEFINED_ID) {
                    // If multiple siblings with a render ids are dropped:
                    // Ignore them all. And delegate the drawing to a parent in the inspector.
                    id = if (id == null) node.id else UNDEFINED_ID
                }
            } else {
                node.id = if (node.id != UNDEFINED_ID) node.id else --generatedId
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
            if (node.bounds != null && parentNode.box == node.box) {
                parentNode.bounds = node.bounds
            }
            parentNode.layoutNodes.addAll(node.layoutNodes)
            parentNode.mergedSemantics.addAll(node.mergedSemantics)
            parentNode.unmergedSemantics.addAll(node.unmergedSemantics)
            release(node)
        }
        val nodeId = id
        parentNode.id =
            if (parentNode.id <= UNDEFINED_ID && nodeId != null) nodeId else parentNode.id
    }

    private fun parse(
        group: CompositionGroup,
        context: SourceContext,
        children: List<MutableInspectorNode>
    ): MutableInspectorNode {
        val node = newNode()
        node.name = context.name ?: ""
        node.key = group.key as? Int ?: 0
        val layoutInfo = group.node as? LayoutInfo
        if (layoutInfo != null) {
            return parseLayoutInfo(layoutInfo, context, node)
        }
        if (unwantedOutsideWindow(node, children)) {
            return markUnwanted(group, context, node)
        }
        node.box = context.bounds.emptyCheck()
        if (unwantedName(node.name) || (node.box == emptyBox && !subCompositions.capturing)) {
            return markUnwanted(group, context, node)
        }
        if (!searchingForAnchorId) {
            parseCallLocation(node, context.location)
            if (isHiddenSystemNode(node)) {
                return markUnwanted(group, context, node)
            }
        }
        val hash = group.identity?.hashCode()
        node.anchorHash = hash ?: 0
        node.id = syntheticId(hash)
        if (includeAllParameters ||
            (searchingForAnchorId && includeParametersForAnchorHash == hash)
        ) {
            addParameters(context, node)
            if (searchingForAnchorId && includeParametersForAnchorHash == hash) {
                foundNode = buildAndRelease(node)
                return newNode()
            }
        }
        return node
    }

    private fun IntRect.emptyCheck(): IntRect =
        if (left >= right && top >= bottom) emptyBox else this

    private fun IntRect.inWindow(): Boolean =
        !(left > windowSize.width || right < 0 || top > windowSize.height || bottom < 0)

    private fun IntRect.union(other: IntRect): IntRect {
        if (this == outsideBox) return other else if (other == outsideBox) return this

        return IntRect(
            left = min(left, other.left),
            top = min(top, other.top),
            bottom = max(bottom, other.bottom),
            right = max(right, other.right)
        )
    }

    private fun parseLayoutInfo(
        layoutInfo: LayoutInfo,
        context: SourceContext,
        node: MutableInspectorNode
    ): MutableInspectorNode {
        val box = context.bounds
        val size = box.size.toSize()
        val coordinates = layoutInfo.coordinates
        val topLeft = toIntOffset(coordinates.localToWindow(Offset.Zero))
        val topRight = toIntOffset(coordinates.localToWindow(Offset(size.width, 0f)))
        val bottomRight = toIntOffset(coordinates.localToWindow(Offset(size.width, size.height)))
        val bottomLeft = toIntOffset(coordinates.localToWindow(Offset(0f, size.height)))
        var bounds: QuadBounds? = null

        if (topLeft.x != box.left || topLeft.y != box.top ||
            topRight.x != box.right || topRight.y != box.top ||
            bottomRight.x != box.right || bottomRight.y != box.bottom ||
            bottomLeft.x != box.left || bottomLeft.y != box.bottom
        ) {
            bounds = QuadBounds(
                topLeft.x, topLeft.y,
                topRight.x, topRight.y,
                bottomRight.x, bottomRight.y,
                bottomLeft.x, bottomLeft.y,
            )
        }
        if (!includeNodesOutsizeOfWindow) {
            // Ignore this node if the bounds are completely outside the window
            node.outerBox = bounds?.outerBox ?: box
            if (!node.outerBox.inWindow()) {
                return node
            }
        }

        node.box = box.emptyCheck()
        node.bounds = bounds
        if (searchingForAnchorId) {
            return node
        }

        node.layoutNodes.add(layoutInfo)
        val modifierInfo = layoutInfo.getModifierInfo()
        node.unmergedSemantics.addAll(
            modifierInfo.asSequence()
                .map { it.modifier }
                .filterIsInstance<SemanticsModifier>()
                .map { it.semanticsConfiguration }
                .flatMap { config -> config.map { RawParameter(it.key.name, it.value) } }
        )

        node.mergedSemantics.addAll(
            modifierInfo.asSequence()
                .map { it.modifier }
                .filterIsInstance<SemanticsModifier>()
                .map { it.id }
                .flatMap { semanticsMap[it].orEmpty() }
        )

        node.id = modifierInfo.asSequence()
            .map { it.extra }
            .filterIsInstance<GraphicLayerInfo>()
            .map { it.layerId }
            .firstOrNull() ?: UNDEFINED_ID

        return node
    }

    private fun syntheticId(identityHash: Int?): Long {
        val id = identityHash ?: return UNDEFINED_ID
        // The hashCode is an Int
        return id.toLong() - Int.MAX_VALUE.toLong() + RESERVED_FOR_GENERATED_IDS
    }

    private fun belongsToView(layoutNodes: List<LayoutInfo>, view: View): Boolean =
        layoutNodes.asSequence().flatMap { node ->
            node.getModifierInfo().asSequence()
                .map { it.extra }
                .filterIsInstance<GraphicLayerInfo>()
                .map { it.ownerViewId }
        }.contains(view.uniqueDrawingId)

    private fun addParameters(context: SourceContext, node: MutableInspectorNode) {
        context.parameters.forEach {
            val castedValue = castValue(it)
            node.parameters.add(RawParameter(it.name, castedValue))
        }
    }

    private fun castValue(parameter: ParameterInformation): Any? {
        val value = parameter.value ?: return null
        if (parameter.inlineClass == null || !isPrimitive(value.javaClass)) return value
        return inlineClassConverter.castParameterValue(parameter.inlineClass, value)
    }

    private fun isPrimitive(cls: Class<*>): Boolean =
        cls.kotlin.javaPrimitiveType != null

    private fun toIntOffset(offset: Offset): IntOffset =
        IntOffset(offset.x.roundToInt(), offset.y.roundToInt())

    private fun markUnwanted(
        group: CompositionGroup,
        context: SourceContext,
        node: MutableInspectorNode
    ): MutableInspectorNode =
        when (node.name) {
            "rememberCompositionContext" ->
                subCompositions.rememberCompositionContext(node, context)
            "remember" ->
                subCompositions.remember(node, group)
            else ->
                node.apply { markUnwanted() }
        }

    private fun parseCallLocation(node: MutableInspectorNode, location: SourceLocation?) {
        val fileName = location?.sourceFile ?: return
        node.fileName = fileName
        node.packageHash = location.packageHash
        node.lineNumber = location.lineNumber
        node.offset = location.offset
        node.length = location.length
    }

    private fun isHiddenSystemNode(node: MutableInspectorNode): Boolean =
        node.packageHash in systemPackages && hideSystemNodes

    private fun unwantedName(name: String): Boolean =
        name.isEmpty() ||
            name.startsWith("remember") ||
            name in unwantedCalls

    private fun unwantedOutsideWindow(
        node: MutableInspectorNode,
        children: List<MutableInspectorNode>
    ): Boolean {
        if (includeNodesOutsizeOfWindow) {
            return false
        }
        node.outerBox = if (children.isEmpty()) outsideBox else
            children.map { g -> g.outerBox }.reduce { acc, box -> box.union(acc) }
        return !node.outerBox.inWindow()
    }

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

    /**
     * Keep track of sub-composition roots.
     *
     * Examples:
     * - Popup, Dialog: When one of these is open an extra Android Window is created with
     *   its own AndroidComposeView. The contents of the Composable is a sub-composition that
     *   will be computed by calling convert.
     *
     *   The Popup/Dialog composable itself, and a few helping composables (the root) will
     *   not be included in the SlotTree with the contents, instead these composables
     *   will be found in the SlotTree for the main app and they all have empty sizes.
     *   The aim is to collect these sub-composition roots such that they can be added to
     *   the [InspectorNode]s of the contents.
     *
     * - AndroidView: When this is used in a compose app we will see a similar pattern in
     *   the SlotTree except there isn't a sub-composition to stitch in. But we need to
     *   collect the view id separately from the "AndroidView" node itself.
     */
    private inner class SubCompositionRoots {
        /** Set to true when the nodes found should be added to a sub-composition root */
        var capturing = false
            private set

        /** The `uniqueDrawingId` of the `AndroidComposeView` that owns the root being captured */
        private var ownerView = UNDEFINED_ID

        /** The node that represent the root of the sub-composition */
        private var rootNode: MutableInspectorNode? = null

        /** The depth of the parse tree the [rootNode] was found at */
        private var rootNodeDepth = 0

        /** Last captured view that is believed to be an embbed View under an AndroidView node */
        private var androidView = UNDEFINED_ID

        /**
         * The sub-composition roots found.
         *
         * Map from View owner to a pair of [InspectorNode] indicating the actual root,
         * and the node where the content should be stitched in.
         */
        private val found = mutableMapOf<Long, InspectorNode>()

        /** Call this before converting a SlotTree for an AndroidComposeView */
        fun clear() {
            capturing = false
            ownerView = UNDEFINED_ID
            rootNode?.markUnwanted()
            rootNode?.id = UNDEFINED_ID
            rootNode = null
            rootNodeDepth = 0
        }

        /** Call this when starting converting a new set of windows */
        fun resetAccumulativeState() {
            found.clear()
        }

        /**
         * When a "rememberCompositionContext" is found in the slot tree, it indicates
         * that a sub-composition was started. We should capture all parent nodes with
         * an empty size as the "root" of the sub-composition.
         */
        fun rememberCompositionContext(
            node: MutableInspectorNode,
            context: SourceContext
        ): MutableInspectorNode {
            if (capturing) {
                save()
            }
            capturing = true
            rootNode = node
            rootNodeDepth = context.depth
            node.id = PLACEHOLDER_ID
            return node
        }

        /**
         * When "remember" is found in the slot tree and we are currently capturing,
         * the data of the [group] may contain the owner of the sub-composition.
         */
        fun remember(node: MutableInspectorNode, group: CompositionGroup): MutableInspectorNode {
            node.markUnwanted()
            if (!capturing) {
                return node
            }
            val root = group.data.filterIsInstance<ViewRootForInspector>().singleOrNull()
                ?: group.data.filterIsInstance<Ref<ViewRootForInspector>>().singleOrNull()?.value
                ?: return node

            val view = root.subCompositionView
            if (view != null) {
                val composeOwner = if (view.childCount == 1) view.getChildAt(0) else return node
                ownerView = composeOwner.uniqueDrawingId
            } else {
                androidView = root.viewRoot?.uniqueDrawingId ?: UNDEFINED_ID
                // Store the viewRoot such that we can move the View under the compose node
                // in Studio. We do not need to capture the Groups found for this case, so
                // we call "reset" here to stop capturing.
                clear()
            }
            return node
        }

        /**
         * Capture the top node of the sub-composition root until a non empty node is found.
         */
        fun captureNode(node: MutableInspectorNode, context: SourceContext) {
            if (!capturing) {
                return
            }
            if (node.box != emptyBox) {
                save()
                return
            }
            val depth = context.depth
            if (depth < rootNodeDepth) {
                rootNode = node
                rootNodeDepth = depth
            }
        }

        fun latestViewId(): Long {
            val id = androidView
            androidView = UNDEFINED_ID
            return id
        }

        /**
         * If a sub-composition root has been captured, save it now.
         */
        private fun save() {
            val node = rootNode
            if (node != null && ownerView != UNDEFINED_ID) {
                found[ownerView] = node.build()
            }
            node?.markUnwanted()
            node?.id = UNDEFINED_ID
            node?.children?.clear()
            clear()
        }

        /**
         * Add the root of the sub-composition to the found tree.
         *
         * If a root is not found for this [owner] or if the stitching fails just return [nodes].
         */
        fun addRoot(owner: View, nodes: List<InspectorNode>): List<InspectorNode> {
            val root = found[owner.uniqueDrawingId] ?: return nodes
            val box = IntRect(0, 0, owner.width, owner.height)
            val info = StitchInfo(nodes, box)
            val result = listOf(stitch(root, info))
            return if (info.added) result else nodes
        }

        private fun stitch(node: InspectorNode, info: StitchInfo): InspectorNode {
            val children = node.children.map { stitch(it, info) }
            val index = children.indexOfFirst { it.id == PLACEHOLDER_ID }
            val newNode = newNode()
            newNode.shallowCopy(node)
            newNode.children.clear()
            if (index < 0) {
                newNode.children.addAll(children)
            } else {
                newNode.children.addAll(children.subList(0, index))
                newNode.children.addAll(info.nodes)
                newNode.children.addAll(children.subList(index + 1, children.size))
                info.added = true
            }
            newNode.box = info.bounds
            return buildAndRelease(newNode)
        }
    }

    private class StitchInfo(
        /** The nodes found that should be stitched into a sub-composition root. */
        val nodes: List<InspectorNode>,

        /** The bounds of the View containing the sub-composition */
        val bounds: IntRect
    ) {
        /** Set this to true when the [nodes] have been added to a sub-composition root */
        var added: Boolean = false
    }
}
