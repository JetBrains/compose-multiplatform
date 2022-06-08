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
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayoutState.PrecomposedSlotHandle
import androidx.compose.ui.UiComposable
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNode.LayoutState
import androidx.compose.ui.node.LayoutNode.UsageByParent
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
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
@UiComposable
fun SubcomposeLayout(
    state: SubcomposeLayoutState,
    modifier: Modifier = Modifier,
    measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult
) {
    val compositionContext = rememberCompositionContext()
    val materialized = currentComposer.materialize(modifier)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewConfiguration = LocalViewConfiguration.current
    ComposeNode<LayoutNode, Applier<Any>>(
        factory = LayoutNode.Constructor,
        update = {
            set(state, state.setRoot)
            set(compositionContext, state.setCompositionContext)
            set(materialized, ComposeUiNode.SetModifier)
            set(measurePolicy, state.setMeasurePolicy)
            set(density, ComposeUiNode.SetDensity)
            set(layoutDirection, ComposeUiNode.SetLayoutDirection)
            set(viewConfiguration, ComposeUiNode.SetViewConfiguration)
        }
    )
    if (!currentComposer.skipping) {
        SideEffect {
            state.forceRecomposeChildren()
        }
    }
    val stateHolder = rememberUpdatedState(state)
    DisposableEffect(Unit) {
        onDispose {
            stateHolder.value.disposeCurrentNodes()
        }
    }
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
 * [slotReusePolicy] the policy defining what slots should be retained to be reused later.
 */
class SubcomposeLayoutState(
    private val slotReusePolicy: SubcomposeSlotReusePolicy
) {
    /**
     * State used by [SubcomposeLayout].
     */
    constructor() : this(NoOpSubcomposeSlotReusePolicy)

    /**
     * State used by [SubcomposeLayout].
     *
     * @param maxSlotsToRetainForReuse when non-zero the layout will keep active up to this count
     * slots which we were used but not used anymore instead of disposing them. Later when you try to
     * compose a new slot instead of creating a completely new slot the layout would reuse the
     * previous slot which allows to do less work especially if the slot contents are similar.
     */
    @Deprecated(
        "This constructor is deprecated",
        ReplaceWith(
            "SubcomposeLayoutState(SubcomposeSlotReusePolicy(maxSlotsToRetainForReuse))",
            "androidx.compose.ui.layout.SubcomposeSlotReusePolicy"
        )
    )
    constructor(maxSlotsToRetainForReuse: Int) : this(
       SubcomposeSlotReusePolicy(maxSlotsToRetainForReuse)
    )

    private var _state: LayoutNodeSubcompositionsState? = null
    private val state: LayoutNodeSubcompositionsState
        get() = requireNotNull(_state) {
            "SubcomposeLayoutState is not attached to SubcomposeLayout"
        }

    // Pre-allocated lambdas to update LayoutNode
    internal val setRoot: LayoutNode.(SubcomposeLayoutState) -> Unit = {
        _state =
            subcompositionsState ?: LayoutNodeSubcompositionsState(this, slotReusePolicy).also {
                subcompositionsState = it
            }
        state.makeSureStateIsConsistent()
        state.slotReusePolicy = slotReusePolicy
    }
    internal val setCompositionContext:
        LayoutNode.(CompositionContext) -> Unit =
        { state.compositionContext = it }
    internal val setMeasurePolicy:
        LayoutNode.(SubcomposeMeasureScope.(Constraints) -> MeasureResult) -> Unit =
        { measurePolicy = state.createMeasurePolicy(it) }

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
    fun precompose(slotId: Any?, content: @Composable () -> Unit): PrecomposedSlotHandle =
        state.precompose(slotId, content)

    internal fun forceRecomposeChildren() = state.forceRecomposeChildren()

    internal fun disposeCurrentNodes() = state.disposeCurrentNodes()

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

        /**
         * The amount of placeables composed into this slot.
         */
        val placeablesCount: Int get() = 0

        /**
         * Performs synchronous measure of the placeable at the given [index].
         *
         * @param index the placeable index. Should be smaller than [placeablesCount].
         * @param constraints Constraints to measure this placeable with.
         */
        fun premeasure(index: Int, constraints: Constraints) {}
    }
}

/**
 * This policy allows [SubcomposeLayout] to retain some of slots which we were used but not
 * used anymore instead of disposing them. Next time when you try to compose a new slot instead of
 * creating a completely new slot the layout would reuse the kept slot. This allows to do less
 * work especially if the slot contents are similar.
 */
interface SubcomposeSlotReusePolicy {
    /**
     * This function will be called with [slotIds] set populated with the slot ids available to
     * reuse. In the implementation you can remove slots you don't want to retain.
     */
    fun getSlotsToRetain(slotIds: SlotIdsSet)

    /**
     * Returns true if the content previously composed with [reusableSlotId] is compatible with
     * the content which is going to be composed for [slotId].
     * Slots could be considered incompatible if they display completely different types of the UI.
     */
    fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean

    /**
     * Set containing slot ids currently available to reuse. Used by [getSlotsToRetain].
     *
     * This class works exactly as [MutableSet], but doesn't allow to add new items in it.
     */
    class SlotIdsSet internal constructor(
        private val set: MutableSet<Any?> = mutableSetOf()
    ) : Collection<Any?> by set {

        internal fun add(slotId: Any?) = set.add(slotId)

        override fun iterator(): MutableIterator<Any?> = set.iterator()

        /**
         * Removes a [slotId] from this set, if it is present.
         *
         * @return `true` if the slot id was removed, `false` if the set was not modified.
         */
        fun remove(slotId: Any?): Boolean = set.remove(slotId)

        /**
         * Removes all slot ids from [slotIds] that are also contained in this set.
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun removeAll(slotIds: Collection<Any?>): Boolean = set.remove(slotIds)

        /**
         * Removes all slot ids that match the given [predicate].
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun removeAll(predicate: (Any?) -> Boolean): Boolean = set.removeAll(predicate)

        /**
         * Retains only the slot ids that are contained in [slotIds].
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun retainAll(slotIds: Collection<Any?>): Boolean = set.retainAll(slotIds)

        /**
         * Retains only slotIds that match the given [predicate].
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun retainAll(predicate: (Any?) -> Boolean): Boolean = set.retainAll(predicate)

        /**
         * Removes all slot ids from this set.
         */
        fun clear() = set.clear()
    }
}

/**
 * Creates [SubcomposeSlotReusePolicy] which retains the fixed amount of slots.
 *
 * @param maxSlotsToRetainForReuse the [SubcomposeLayout] will retain up to this amount of slots.
 */
fun SubcomposeSlotReusePolicy(maxSlotsToRetainForReuse: Int): SubcomposeSlotReusePolicy =
    FixedCountSubcomposeSlotReusePolicy(maxSlotsToRetainForReuse)

/**
 * The inner state containing all the information about active slots and their compositions.
 * It is stored inside LayoutNode object as in fact we need to keep 1-1 mapping between this state
 * and the node: when we compose a slot we first create a virtual LayoutNode child to this node
 * and then save the extra information inside this state.
 * Keeping this state inside LayoutNode also helps us to retain the pool of reusable slots even
 * when a new SubcomposeLayoutState is applied to SubcomposeLayout and even when the
 * SubcomposeLayout's LayoutNode is reused via the ReusableComposeNode mechanism.
 */
internal class LayoutNodeSubcompositionsState(
    private val root: LayoutNode,
    slotReusePolicy: SubcomposeSlotReusePolicy
) {
    var compositionContext: CompositionContext? = null

    var slotReusePolicy: SubcomposeSlotReusePolicy = slotReusePolicy
        set(value) {
            if (field !== value) {
                field = value
                // apply the new policy
                disposeOrReuseStartingFromIndex(0)
            }
        }

    private var currentIndex = 0
    private val nodeToNodeState = mutableMapOf<LayoutNode, NodeState>()
    // this map contains active slotIds (without precomposed or reusable nodes)
    private val slotIdToNode = mutableMapOf<Any?, LayoutNode>()
    private val scope = Scope()
    private val precomposeMap = mutableMapOf<Any?, LayoutNode>()
    private val reusableSlotIdsSet = SubcomposeSlotReusePolicy.SlotIdsSet()

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

    fun subcompose(slotId: Any?, content: @Composable () -> Unit): List<Measurable> {
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
            } else {
                takeNodeFromReusables(slotId) ?: createNodeAt(currentIndex)
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
        return node.childMeasurables
    }

    private fun subcompose(node: LayoutNode, slotId: Any?, content: @Composable () -> Unit) {
        val nodeState = nodeToNodeState.getOrPut(node) {
            NodeState(slotId, {})
        }
        val hasPendingChanges = nodeState.composition?.hasInvalidations ?: true
        if (nodeState.content !== content || hasPendingChanges || nodeState.forceRecompose) {
            nodeState.content = content
            subcompose(node, nodeState)
            nodeState.forceRecompose = false
        }
    }

    private fun subcompose(node: LayoutNode, nodeState: NodeState) {
        Snapshot.withoutReadObservation {
            ignoreRemeasureRequests {
                val content = nodeState.content
                nodeState.composition = subcomposeInto(
                    existing = nodeState.composition,
                    container = node,
                    parent = compositionContext ?: error("parent composition reference not set"),
                    // Do not optimize this by passing nodeState.content directly; the additional
                    // composable function call from the lambda expression affects the scope of
                    // recomposition and recomposition of siblings.
                    composable = {
                        ReusableContentHost(nodeState.active, content)
                    }
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

    private fun getSlotIdAtIndex(index: Int): Any? {
        val node = root.foldedChildren[index]
        return nodeToNodeState[node]!!.slotId
    }

    fun disposeOrReuseStartingFromIndex(startIndex: Int) {
        reusableCount = 0
        val lastReusableIndex = root.foldedChildren.size - precomposedCount - 1
        if (startIndex <= lastReusableIndex) {
            // construct the set of available slot ids
            reusableSlotIdsSet.clear()
            for (i in startIndex..lastReusableIndex) {
                reusableSlotIdsSet.add(getSlotIdAtIndex(i))
            }

            slotReusePolicy.getSlotsToRetain(reusableSlotIdsSet)
            // iterating backwards so it is easier to remove items
            var i = lastReusableIndex
            while (i >= startIndex) {
                val node = root.foldedChildren[i]
                val nodeState = nodeToNodeState[node]!!
                val slotId = nodeState.slotId
                if (reusableSlotIdsSet.contains(slotId)) {
                    node.measuredByParent = UsageByParent.NotUsed
                    reusableCount++
                    nodeState.active = false
                } else {
                    ignoreRemeasureRequests {
                        nodeToNodeState.remove(node)
                        nodeState.composition?.dispose()
                        root.removeAt(i, 1)
                    }
                }
                // remove it from slotIdToNode so it is not considered active
                slotIdToNode.remove(slotId)
                i--
            }
        }

        makeSureStateIsConsistent()
    }

    fun makeSureStateIsConsistent() {
        require(nodeToNodeState.size == root.foldedChildren.size) {
            "Inconsistency between the count of nodes tracked by the state (${nodeToNodeState
                .size}) and the children count on the SubcomposeLayout (${root.foldedChildren
                .size}). Are you trying to use the state of the disposed SubcomposeLayout?"
        }
        require(root.foldedChildren.size - reusableCount - precomposedCount >= 0) {
            "Incorrect state. Total children ${root.foldedChildren.size}. Reusable children " +
                "$reusableCount. Precomposed children $precomposedCount"
        }
        require(precomposeMap.size == precomposedCount) {
            "Incorrect state. Precomposed children $precomposedCount. Map size " +
                "${precomposeMap.size}"
        }
    }

    private fun takeNodeFromReusables(slotId: Any?): LayoutNode? {
        if (reusableCount == 0) {
            return null
        }
        val reusableNodesSectionEnd = root.foldedChildren.size - precomposedCount
        val reusableNodesSectionStart = reusableNodesSectionEnd - reusableCount
        var index = reusableNodesSectionEnd - 1
        var chosenIndex = -1
        // first try to find a node with exactly the same slotId
        while (index >= reusableNodesSectionStart) {
            if (getSlotIdAtIndex(index) == slotId) {
                // we have a node with the same slotId
                chosenIndex = index
                break
            } else {
                index--
            }
        }
        if (chosenIndex == -1) {
            // try to find a first compatible slotId from the end of the section
            index = reusableNodesSectionEnd - 1
            while (index >= reusableNodesSectionStart) {
                val node = root.foldedChildren[index]
                val nodeState = nodeToNodeState[node]!!
                if (slotReusePolicy.areCompatible(slotId, nodeState.slotId)) {
                    nodeState.slotId = slotId
                    chosenIndex = index
                    break
                }
                index--
            }
        }
        return if (chosenIndex == -1) {
            // no compatible nodes found
            null
        } else {
            if (index != reusableNodesSectionStart) {
                // we need to rearrange the items
                move(index, reusableNodesSectionStart, 1)
            }
            reusableCount--
            val node = root.foldedChildren[reusableNodesSectionStart]
            val nodeState = nodeToNodeState[node]!!
            nodeState.active = true
            Snapshot.sendApplyNotifications()
            node
        }
    }

    fun createMeasurePolicy(
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
                    disposeOrReuseStartingFromIndex(currentIndex)
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

    fun precompose(slotId: Any?, content: @Composable () -> Unit): PrecomposedSlotHandle {
        makeSureStateIsConsistent()
        if (!slotIdToNode.containsKey(slotId)) {
            val node = precomposeMap.getOrPut(slotId) {
                val reusedNode = takeNodeFromReusables(slotId)
                if (reusedNode != null) {
                    // now move this node to the end where we keep precomposed items
                    val nodeIndex = root.foldedChildren.indexOf(reusedNode)
                    move(nodeIndex, root.foldedChildren.size, 1)
                    precomposedCount++
                    reusedNode
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
                makeSureStateIsConsistent()
                val node = precomposeMap.remove(slotId)
                if (node != null) {
                    check(precomposedCount > 0)
                    val itemIndex = root.foldedChildren.indexOf(node)
                    // make sure this item is in the precomposed items range
                    check(itemIndex >= root.foldedChildren.size - precomposedCount)
                    // move this item into the reusable section
                    reusableCount++
                    precomposedCount--
                    val reusableStart = root.foldedChildren.size - precomposedCount - reusableCount
                    move(itemIndex, reusableStart, 1)
                    disposeOrReuseStartingFromIndex(reusableStart)
                }
            }

            override val placeablesCount: Int
                get() = precomposeMap[slotId]?.children?.size ?: 0

            override fun premeasure(index: Int, constraints: Constraints) {
                val node = precomposeMap[slotId]
                if (node != null && node.isAttached) {
                    val size = node.children.size
                    if (index < 0 || index >= size) {
                        throw IndexOutOfBoundsException(
                            "Index ($index) is out of bound of [0, $size)"
                        )
                    }
                    require(!node.isPlaced)
                    root.ignoreRemeasureRequests {
                        node.requireOwner().measureAndLayout(node.children[index], constraints)
                    }
                }
            }
        }
    }

    fun forceRecomposeChildren() {
        nodeToNodeState.forEach { (_, nodeState) ->
            nodeState.forceRecompose = true
        }
        if (!root.measurePending) {
            root.requestRemeasure()
        }
    }

    private fun createNodeAt(index: Int) = LayoutNode(isVirtual = true).also { node ->
        ignoreRemeasureRequests {
            root.insertAt(index, node)
        }
    }

    private fun move(from: Int, to: Int, count: Int = 1) {
        ignoreRemeasureRequests {
            root.move(from, to, count)
        }
    }

    private inline fun ignoreRemeasureRequests(block: () -> Unit) =
        root.ignoreRemeasureRequests(block)

    fun disposeCurrentNodes() {
        root.ignoreRemeasureRequests {
            nodeToNodeState.values.forEach {
                it.composition?.dispose()
            }
            root.removeAll()
        }
        nodeToNodeState.clear()
        slotIdToNode.clear()
        precomposedCount = 0
        reusableCount = 0
        precomposeMap.clear()
        makeSureStateIsConsistent()
    }

    private class NodeState(
        var slotId: Any?,
        var content: @Composable () -> Unit,
        var composition: Composition? = null
    ) {
        var forceRecompose = false
        var active by mutableStateOf(true)
    }

    private inner class Scope : SubcomposeMeasureScope {
        // MeasureScope delegation
        override var layoutDirection: LayoutDirection = LayoutDirection.Rtl
        override var density: Float = 0f
        override var fontScale: Float = 0f

        override fun subcompose(slotId: Any?, content: @Composable () -> Unit) =
            this@LayoutNodeSubcompositionsState.subcompose(slotId, content)
    }
}

private class FixedCountSubcomposeSlotReusePolicy(
    private val maxSlotsToRetainForReuse: Int
) : SubcomposeSlotReusePolicy {

    override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
        if (slotIds.size > maxSlotsToRetainForReuse) {
            var count = 0
            with(slotIds.iterator()) {
                // keep first maxSlotsToRetainForReuse items
                while (hasNext()) {
                    next()
                    count++
                    if (count > maxSlotsToRetainForReuse) {
                        remove()
                    }
                }
            }
        }
    }

    override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean = true
}

private object NoOpSubcomposeSlotReusePolicy : SubcomposeSlotReusePolicy {
    override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
        slotIds.clear()
    }

    override fun areCompatible(slotId: Any?, reusableSlotId: Any?) = false
}
