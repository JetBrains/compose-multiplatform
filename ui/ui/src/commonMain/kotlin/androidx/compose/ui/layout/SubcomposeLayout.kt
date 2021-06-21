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

package androidx.compose.ui.layout

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNode.LayoutState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.createSubcomposition
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection

/**
 * Analogue of [Layout] which allows to subcompose the actual content during the measuring stage
 * for example to use the values calculated during the measurement as params for the composition
 * of the children.
 *
 * Possible use cases:
 * * You need to know the constraints passed by the parent during the composition and can't solve
 * your use case with just custom [Layout] or [LayoutModifier].
 * See [androidx.compose.foundation.layout.BoxWithConstraints].
 * * You want to use the size of one child during the composition of the second child.
 * * You want to compose your items lazily based on the available size. For example you have a
 * list of 100 items and instead of composing all of them you only compose the ones which are
 * currently visible(say 5 of them) and compose next items when the component is scrolled.
 *
 * @sample androidx.compose.ui.samples.SubcomposeLayoutSample
 *
 * @param modifier [Modifier] to apply for the layout.
 * @param measurePolicy Measure policy which provides ability to subcompose during the measuring.
 */
@Composable
fun SubcomposeLayout(
    modifier: Modifier = Modifier,
    measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult
) {
    SubcomposeLayout(
        state = remember { SubcomposeLayoutState() },
        modifier = modifier,
        measurePolicy = measurePolicy
    )
}

/**
 * Analogue of [Layout] which allows to subcompose the actual content during the measuring stage
 * for example to use the values calculated during the measurement as params for the composition
 * of the children.
 *
 * Possible use cases:
 * * You need to know the constraints passed by the parent during the composition and can't solve
 * your use case with just custom [Layout] or [LayoutModifier].
 * See [androidx.compose.foundation.layout.BoxWithConstraints].
 * * You want to use the size of one child during the composition of the second child.
 * * You want to compose your items lazily based on the available size. For example you have a
 * list of 100 items and instead of composing all of them you only compose the ones which are
 * currently visible(say 5 of them) and compose next items when the component is scrolled.
 *
 * @sample androidx.compose.ui.samples.SubcomposeLayoutSample
 *
 * @param state the state object to be used by the layout.
 * @param modifier [Modifier] to apply for the layout.
 * @param measurePolicy Measure policy which provides ability to subcompose during the measuring.
 */
@Composable
fun SubcomposeLayout(
    state: SubcomposeLayoutState,
    modifier: Modifier = Modifier,
    measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult
) {
    state.compositionContext = rememberCompositionContext()
    DisposableEffect(state) {
        onDispose {
            state.disposeCurrentNodes()
        }
    }

    val materialized = currentComposer.materialize(modifier)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    ComposeNode<LayoutNode, Applier<Any>>(
        factory = LayoutNode.Constructor,
        update = {
            init(state.setRoot)
            set(materialized, ComposeUiNode.SetModifier)
            set(measurePolicy, state.setMeasurePolicy)
            set(density, ComposeUiNode.SetDensity)
            set(layoutDirection, ComposeUiNode.SetLayoutDirection)
        }
    )
}

/**
 * The receiver scope of a [SubcomposeLayout]'s measure lambda which adds ability to dynamically
 * subcompose a content during the measuring on top of the features provided by [MeasureScope].
 */
interface SubcomposeMeasureScope : MeasureScope {
    /**
     * Performs subcomposition of the provided [content] with given [slotId].
     *
     * @param slotId unique id which represents the slot we are composing into. If you have fixed
     * amount or slots you can use enums as slot ids, or if you have a list of items maybe an
     * index in the list or some other unique key can work. To be able to correctly match the
     * content between remeasures you should provide the object which is equals to the one you
     * used during the previous measuring.
     * @param content the composable content which defines the slot. It could emit multiple
     * layouts, in this case the returned list of [Measurable]s will have multiple elements.
     */
    fun subcompose(slotId: Any?, content: @Composable () -> Unit): List<Measurable>
}

/**
 * State used by [SubcomposeLayout].
 *
 * @param maxSlotsToRetainForReuse when non-zero the layout will keep active up to this count
 * slots which we were used but not used anymore instead of disposing them. Later when you try to
 * compose a new slot instead of creating a completely new slot the layout would reuse the
 * previous slot which allows to do less work especially if the slot contents are similar.
 */
class SubcomposeLayoutState(
    private val maxSlotsToRetainForReuse: Int
) {
    /**
     * State used by [SubcomposeLayout].
     */
    constructor() : this(0)

    internal var compositionContext: CompositionContext? = null

    // Pre-allocated lambdas to update LayoutNode
    internal val setRoot: LayoutNode.() -> Unit = { _root = this }
    internal val setMeasurePolicy:
        LayoutNode.(SubcomposeMeasureScope.(Constraints) -> MeasureResult) -> Unit =
            { measurePolicy = createMeasurePolicy(it) }

    // inner state
    private var _root: LayoutNode? = null
    private val root: LayoutNode get() = requireNotNull(_root)
    private var currentIndex = 0
    private val nodeToNodeState = mutableMapOf<LayoutNode, NodeState>()
    // this map contains active slotIds (without precomposed or reusable nodes)
    private val slotIdToNode = mutableMapOf<Any?, LayoutNode>()
    private val scope = Scope()
    private val precomposeMap = mutableMapOf<Any?, LayoutNode>()

    /**
     * `root.foldedChildren` list consist of:
     * 1) all the active children (used during the last measure pass)
     * 2) `reusableCount` nodes in the middle of the list which were active and stopped being
     * used. now we keep them (up to `maxCountOfSlotsToReuse`) in order to reuse next time we
     * will need to compose a new item
     * 4) `precomposedCount` nodes in the end of the list which were precomposed and
     * are waiting to be used during the next measure passes.
     */
    private var reusableCount = 0
    private var precomposedCount = 0

    internal fun subcompose(slotId: Any?, content: @Composable () -> Unit): List<Measurable> {
        makeSureStateIsConsistent()
        val layoutState = root.layoutState
        check(layoutState == LayoutState.Measuring || layoutState == LayoutState.LayingOut) {
            "subcompose can only be used inside the measure or layout blocks"
        }

        val node = slotIdToNode.getOrPut(slotId) {
            val precomposed = precomposeMap.remove(slotId)
            if (precomposed != null) {
                check(precomposedCount > 0)
                precomposedCount--
                precomposed
            } else if (reusableCount > 0) {
                takeNodeFromReusables(slotId)
            } else {
                createNodeAt(currentIndex)
            }
        }

        val itemIndex = root.foldedChildren.indexOf(node)
        if (itemIndex < currentIndex) {
            throw IllegalArgumentException(
                "Key $slotId was already used. If you are using LazyColumn/Row please make sure " +
                    "you provide a unique key for each item."
            )
        }
        if (currentIndex != itemIndex) {
            move(itemIndex, currentIndex)
        }
        currentIndex++

        subcompose(node, slotId, content)
        return node.children
    }

    private fun subcompose(node: LayoutNode, slotId: Any?, content: @Composable () -> Unit) {
        val nodeState = nodeToNodeState.getOrPut(node) {
            NodeState(slotId, {})
        }
        val hasPendingChanges = nodeState.composition?.hasInvalidations ?: true
        if (nodeState.content !== content || hasPendingChanges) {
            nodeState.content = content
            subcompose(node, nodeState)
        }
    }

    private fun subcompose(node: LayoutNode, nodeState: NodeState) {
        node.withNoSnapshotReadObservation {
            ignoreRemeasureRequests {
                val content = nodeState.content
                nodeState.composition = subcomposeInto(
                    existing = nodeState.composition,
                    container = node,
                    parent = compositionContext ?: error("parent composition reference not set"),
                    // Do not optimize this by passing nodeState.content directly; the additional
                    // composable function call from the lambda expression affects the scope of
                    // recomposition and recomposition of siblings.
                    composable = { content() }
                )
            }
        }
    }

    private fun subcomposeInto(
        existing: Composition?,
        container: LayoutNode,
        parent: CompositionContext,
        composable: @Composable () -> Unit
    ): Composition {
        return if (existing == null || existing.isDisposed) {
            createSubcomposition(container, parent)
        } else {
            existing
        }
            .apply {
                setContent(composable)
            }
    }

    private fun disposeAfterIndex(currentIndex: Int) {
        val precomposedNodesSectionStart = root.foldedChildren.size - precomposedCount
        val reusableNodesSectionStart = maxOf(
            currentIndex,
            precomposedNodesSectionStart - maxSlotsToRetainForReuse
        )

        // keep up to maxCountOfSlotsToReuse last nodes to be reused later
        reusableCount = precomposedNodesSectionStart - reusableNodesSectionStart
        for (i in reusableNodesSectionStart until reusableNodesSectionStart + reusableCount) {
            val node = root.foldedChildren[i]
            val state = nodeToNodeState[node]!!
            // remove them from slotIdToNode so they are not considered active
            slotIdToNode.remove(state.slotId)
        }

        // dispose the rest of the nodes
        val nodesToDispose = reusableNodesSectionStart - currentIndex
        if (nodesToDispose > 0) {
            ignoreRemeasureRequests {
                for (i in currentIndex until currentIndex + nodesToDispose) {
                    disposeNode(root.foldedChildren[i])
                }
                root.removeAt(currentIndex, nodesToDispose)
            }
        }

        makeSureStateIsConsistent()
    }

    private fun makeSureStateIsConsistent() {
        require(nodeToNodeState.size == root.foldedChildren.size) {
            "Inconsistency between the count of nodes tracked by the state (${nodeToNodeState
                .size}) and the children count on the SubcomposeLayout (${root.foldedChildren
                .size}). Are you trying to use the state of the disposed SubcomposeLayout?"
        }
    }

    private fun takeNodeFromReusables(slotId: Any?): LayoutNode {
        check(reusableCount > 0)
        val reusableNodesSectionEnd = root.foldedChildren.size - precomposedCount
        val reusableNodesSectionStart = reusableNodesSectionEnd - reusableCount
        var index = reusableNodesSectionStart
        while (true) {
            val node = root.foldedChildren[index]
            val nodeState = nodeToNodeState.getValue(node)
            if (nodeState.slotId == slotId) {
                // we have a node with the same slotId
                break
            } else if (index == reusableNodesSectionEnd - 1) {
                // it is the last available reusable node
                nodeState.slotId = slotId
                break
            } else {
                index++
            }
        }
        if (index != reusableNodesSectionStart) {
            // we need to rearrange the items
            move(index, reusableNodesSectionStart, 1)
        }
        reusableCount--
        return root.foldedChildren[reusableNodesSectionStart]
    }

    private fun disposeNode(node: LayoutNode) {
        val nodeState = nodeToNodeState.remove(node)!!
        nodeState.composition!!.dispose()
        slotIdToNode.remove(nodeState.slotId)
    }

    private fun createMeasurePolicy(
        block: SubcomposeMeasureScope.(Constraints) -> MeasureResult
    ): MeasurePolicy = object : LayoutNode.NoIntrinsicsMeasurePolicy(error = NoIntrinsicsMessage) {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            scope.layoutDirection = layoutDirection
            scope.density = density
            scope.fontScale = fontScale
            currentIndex = 0
            val result = scope.block(constraints)
            val indexAfterMeasure = currentIndex
            return object : MeasureResult {
                override val width: Int
                    get() = result.width
                override val height: Int
                    get() = result.height
                override val alignmentLines: Map<AlignmentLine, Int>
                    get() = result.alignmentLines

                override fun placeChildren() {
                    currentIndex = indexAfterMeasure
                    result.placeChildren()
                    disposeAfterIndex(currentIndex)
                }
            }
        }
    }

    private val NoIntrinsicsMessage = "Asking for intrinsic measurements of SubcomposeLayout " +
        "layouts is not supported. This includes components that are built on top of " +
        "SubcomposeLayout, such as lazy lists, BoxWithConstraints, TabRow, etc. To mitigate " +
        "this:\n" +
        "- if intrinsic measurements are used to achieve 'match parent' sizing,, consider " +
        "replacing the parent of the component with a custom layout which controls the order in " +
        "which children are measured, making intrinsic measurement not needed\n" +
        "- adding a size modifier to the component, in order to fast return the queried " +
        "intrinsic measurement."

    internal fun disposeCurrentNodes() {
        nodeToNodeState.values.forEach {
            it.composition!!.dispose()
        }
        nodeToNodeState.clear()
        slotIdToNode.clear()
    }

    /**
     * Composes the content for the given [slotId]. This makes the next scope.subcompose(slotId)
     * call during the measure pass faster as the content is already composed.
     *
     * If the [slotId] was precomposed already but after the future calculations ended up to not be
     * needed anymore (meaning this slotId is not going to be used during the measure pass
     * anytime soon) you can use [PrecomposedSlotHandle.dispose] on a returned object to dispose the
     * content.
     *
     * @param slotId unique id which represents the slot we are composing into.
     * @param content the composable content which defines the slot.
     * @return [PrecomposedSlotHandle] instance which allows you to dispose the content.
     */
    fun precompose(slotId: Any?, content: @Composable () -> Unit): PrecomposedSlotHandle {
        makeSureStateIsConsistent()
        if (!slotIdToNode.containsKey(slotId)) {
            val node = precomposeMap.getOrPut(slotId) {
                if (reusableCount > 0) {
                    val node = takeNodeFromReusables(slotId)
                    // now move this node to the end where we keep precomposed items
                    val nodeIndex = root.foldedChildren.indexOf(node)
                    move(nodeIndex, root.foldedChildren.size, 1)
                    precomposedCount++
                    node
                } else {
                    createNodeAt(root.foldedChildren.size).also {
                        precomposedCount++
                    }
                }
            }
            subcompose(node, slotId, content)
        }
        return object : PrecomposedSlotHandle {
            override fun dispose() {
                val node = precomposeMap.remove(slotId)
                if (node != null) {
                    val itemIndex = root.foldedChildren.indexOf(node)
                    check(itemIndex != -1)
                    if (reusableCount < maxSlotsToRetainForReuse) {
                        val reusableNodesSectionStart =
                            root.foldedChildren.size - precomposedCount - reusableCount
                        move(itemIndex, reusableNodesSectionStart, 1)
                        reusableCount++
                    } else {
                        ignoreRemeasureRequests {
                            disposeNode(node)
                            root.removeAt(itemIndex, 1)
                        }
                    }
                    check(precomposedCount > 0)
                    precomposedCount--
                }
            }
        }
    }

    private fun createNodeAt(index: Int) = LayoutNode(isVirtual = true).also {
        ignoreRemeasureRequests {
            root.insertAt(index, it)
        }
    }

    private fun move(from: Int, to: Int, count: Int = 1) {
        ignoreRemeasureRequests {
            root.move(from, to, count)
        }
    }

    private inline fun ignoreRemeasureRequests(block: () -> Unit) =
        root.ignoreRemeasureRequests(block)

    private class NodeState(
        var slotId: Any?,
        var content: @Composable () -> Unit,
        var composition: Composition? = null
    )

    private inner class Scope : SubcomposeMeasureScope {
        // MeasureScope delegation
        override var layoutDirection: LayoutDirection = LayoutDirection.Rtl
        override var density: Float = 0f
        override var fontScale: Float = 0f

        override fun subcompose(slotId: Any?, content: @Composable () -> Unit) =
            this@SubcomposeLayoutState.subcompose(slotId, content)
    }

    /**
     * Instance of this interface is returned by [precompose] function.
     */
    interface PrecomposedSlotHandle {

        /**
         * This function allows to dispose the content for the slot which was precomposed
         * previously via [precompose].
         *
         * If this slot was already used during the regular measure pass via
         * [SubcomposeMeasureScope.subcompose] this function will do nothing.
         *
         * This could be useful if after the future calculations this item is not anymore expected to
         * be used during the measure pass anytime soon.
         */
        fun dispose()
    }
}
