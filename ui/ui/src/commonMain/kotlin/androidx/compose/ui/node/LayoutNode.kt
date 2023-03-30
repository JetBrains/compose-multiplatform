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
package androidx.compose.ui.node

import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.LayoutNodeSubcompositionsState
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.node.LayoutNode.LayoutState.Idle
import androidx.compose.ui.node.LayoutNode.LayoutState.LayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.LookaheadLayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.LookaheadMeasuring
import androidx.compose.ui.node.LayoutNode.LayoutState.Measuring
import androidx.compose.ui.node.Nodes.FocusEvent
import androidx.compose.ui.node.Nodes.FocusProperties
import androidx.compose.ui.node.Nodes.FocusTarget
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.simpleIdentityToString
import androidx.compose.ui.semantics.SemanticsModifierCore.Companion.generateSemanticsId
import androidx.compose.ui.semantics.outerSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.InteropViewFactoryHolder

/**
 * Enable to log changes to the LayoutNode tree.  This logging is quite chatty.
 */
private const val DebugChanges = false

/**
 * An element in the layout hierarchy, built with compose UI.
 */
@OptIn(InternalComposeUiApi::class)
internal class LayoutNode(
    // Virtual LayoutNode is the temporary concept allows us to a node which is not a real node,
    // but just a holder for its children - allows us to combine some children into something we
    // can subcompose in(LayoutNode) without being required to define it as a real layout - we
    // don't want to define the layout strategy for such nodes, instead the children of the
    // virtual nodes will be treated as the direct children of the virtual node parent.
    // This whole concept will be replaced with a proper subcomposition logic which allows to
    // subcompose multiple times into the same LayoutNode and define offsets.
    private val isVirtual: Boolean = false,
    // The unique semantics ID that is used by all semantics modifiers attached to this LayoutNode.
    override val semanticsId: Int = generateSemanticsId()
) : ComposeNodeLifecycleCallback,
    Remeasurement,
    OwnerScope,
    LayoutInfo,
    ComposeUiNode,
    InteroperableComposeUiNode,
    Owner.OnLayoutCompletedListener {

    val isPlacedInLookahead: Boolean?
        get() = lookaheadPassDelegate?.isPlaced

    private var virtualChildrenCount = 0

    // the list of nodes containing the virtual children as is
    private val _foldedChildren = MutableVectorWithMutationTracking(mutableVectorOf<LayoutNode>()) {
        layoutDelegate.markChildrenDirty()
    }
    internal val foldedChildren: List<LayoutNode> get() = _foldedChildren.asList()

    // the list of nodes where the virtual children are unfolded (their children are represented
    // as our direct children)
    private var _unfoldedChildren: MutableVector<LayoutNode>? = null

    private fun recreateUnfoldedChildrenIfDirty() {
        if (unfoldedVirtualChildrenListDirty) {
            unfoldedVirtualChildrenListDirty = false
            val unfoldedChildren = _unfoldedChildren ?: mutableVectorOf<LayoutNode>().also {
                _unfoldedChildren = it
            }
            unfoldedChildren.clear()
            _foldedChildren.forEach {
                if (it.isVirtual) {
                    unfoldedChildren.addAll(it._children)
                } else {
                    unfoldedChildren.add(it)
                }
            }
            layoutDelegate.markChildrenDirty()
        }
    }

    internal val childMeasurables: List<Measurable>
        get() = measurePassDelegate.childMeasurables

    internal val childLookaheadMeasurables: List<Measurable>
        get() = lookaheadPassDelegate!!.childMeasurables

    // when the list of our children is modified it will be set to true if we are a virtual node
    // or it will be set to true on a parent if the parent is a virtual node
    private var unfoldedVirtualChildrenListDirty = false
    private fun invalidateUnfoldedVirtualChildren() {
        if (virtualChildrenCount > 0) {
            unfoldedVirtualChildrenListDirty = true
        }
        if (isVirtual) {
            this.parent?.unfoldedVirtualChildrenListDirty = true
        }
    }

    /**
     * This should **not** be mutated or even accessed directly from outside of [LayoutNode]. Use
     * [forEachChild]/[forEachChildIndexed] when there's a need to iterate through the vector.
     */
    internal val _children: MutableVector<LayoutNode>
        get() {
            updateChildrenIfDirty()
            return if (virtualChildrenCount == 0) {
                _foldedChildren.vector
            } else {
                _unfoldedChildren!!
            }
        }

    /**
     * Update children if the list is not up to date.
     */
    internal fun updateChildrenIfDirty() {
        if (virtualChildrenCount > 0) {
            recreateUnfoldedChildrenIfDirty()
        }
    }

    inline fun forEachChild(block: (LayoutNode) -> Unit) = _children.forEach(block)
    inline fun forEachChildIndexed(block: (Int, LayoutNode) -> Unit) =
        _children.forEachIndexed(block)

    /**
     * The children of this LayoutNode, controlled by [insertAt], [move], and [removeAt].
     */
    internal val children: List<LayoutNode> get() = _children.asMutableList()

    /**
     * The parent node in the LayoutNode hierarchy. This is `null` when the [LayoutNode]
     * is not attached to a hierarchy or is the root of the hierarchy.
     */
    private var _foldedParent: LayoutNode? = null

    /*
     * The parent node in the LayoutNode hierarchy, skipping over virtual nodes.
     */
    internal val parent: LayoutNode?
        get() {
            return if (_foldedParent?.isVirtual == true) _foldedParent?.parent else _foldedParent
        }

    /**
     * The view system [Owner]. This `null` until [attach] is called
     */
    internal var owner: Owner? = null
        private set

    /**
     * The [InteropViewFactoryHolder] associated with this node, which is used to instantiate and
     * manage platform View instances that are hosted in Compose.
     */
    internal var interopViewFactoryHolder: InteropViewFactoryHolder? = null

    @InternalComposeUiApi
    override fun getInteropView(): InteropView? = interopViewFactoryHolder?.getInteropView()

    /**
     * Returns true if this [LayoutNode] currently has an [LayoutNode.owner].  Semantically,
     * this means that the LayoutNode is currently a part of a component tree.
     */
    override val isAttached: Boolean get() = owner != null

    /**
     * The tree depth of the [LayoutNode]. This is valid only when it is attached to a hierarchy.
     */
    internal var depth: Int = 0

    /**
     * The layout state the node is currently in.
     *
     * The mutation of [layoutState] is confined to [LayoutNode], and is therefore read-only
     * outside LayoutNode. This makes the state machine easier to reason about.
     */
    internal val layoutState
        get() = layoutDelegate.layoutState

    /**
     * The lookahead pass delegate for the [LayoutNode]. This should only be used for measure
     * and layout related impl during *lookahead*. For the actual measure & layout, use
     * [measurePassDelegate].
     */
    private val lookaheadPassDelegate
        get() = layoutDelegate.lookaheadPassDelegate

    /**
     * The measure pass delegate for the [LayoutNode]. This delegate is responsible for the actual
     * measure & layout, after lookahead if any.
     */
    private val measurePassDelegate
        get() = layoutDelegate.measurePassDelegate

    /**
     * [requestRemeasure] calls will be ignored while this flag is true.
     */
    private var ignoreRemeasureRequests = false

    /**
     * Inserts a child [LayoutNode] at a particular index. If this LayoutNode [owner] is not `null`
     * then [instance] will become [attach]ed also. [instance] must have a `null` [parent].
     */
    internal fun insertAt(index: Int, instance: LayoutNode) {
        check(instance._foldedParent == null) {
            "Cannot insert $instance because it already has a parent." +
                " This tree: " + debugTreeToString() +
                " Other tree: " + instance._foldedParent?.debugTreeToString()
        }
        check(instance.owner == null) {
            "Cannot insert $instance because it already has an owner." +
                " This tree: " + debugTreeToString() +
                " Other tree: " + instance.debugTreeToString()
        }

        if (DebugChanges) {
            println("$instance added to $this at index $index")
        }

        instance._foldedParent = this
        _foldedChildren.add(index, instance)
        onZSortedChildrenInvalidated()

        if (instance.isVirtual) {
            require(!isVirtual) { "Virtual LayoutNode can't be added into a virtual parent" }
            virtualChildrenCount++
        }
        invalidateUnfoldedVirtualChildren()

        instance.outerCoordinator.wrappedBy = if (isVirtual) {
            // if this node is virtual we use the inner coordinator of our parent
            _foldedParent?.innerCoordinator
        } else {
            innerCoordinator
        }
        // and if the child is virtual we set our inner coordinator for the grandchildren
        if (instance.isVirtual) {
            instance._foldedChildren.forEach {
                it.outerCoordinator.wrappedBy = innerCoordinator
            }
        }

        val owner = this.owner
        if (owner != null) {
            instance.attach(owner)
        }

        if (instance.layoutDelegate.childrenAccessingCoordinatesDuringPlacement > 0) {
            layoutDelegate.childrenAccessingCoordinatesDuringPlacement++
        }
    }

    internal fun onZSortedChildrenInvalidated() {
        if (isVirtual) {
            parent?.onZSortedChildrenInvalidated()
        } else {
            zSortedChildrenInvalidated = true
        }
    }

    /**
     * Removes one or more children, starting at [index].
     */
    internal fun removeAt(index: Int, count: Int) {
        require(count >= 0) {
            "count ($count) must be greater than 0"
        }
        for (i in index + count - 1 downTo index) {
            val child = _foldedChildren.removeAt(i)
            onChildRemoved(child)
            if (DebugChanges) {
                println("$child removed from $this at index $i")
            }
        }
    }

    /**
     * Removes all children.
     */
    internal fun removeAll() {
        for (i in _foldedChildren.size - 1 downTo 0) {
            onChildRemoved(_foldedChildren[i])
        }
        _foldedChildren.clear()

        if (DebugChanges) {
            println("Removed all children from $this")
        }
    }

    private fun onChildRemoved(child: LayoutNode) {
        if (child.layoutDelegate.childrenAccessingCoordinatesDuringPlacement > 0) {
            layoutDelegate.childrenAccessingCoordinatesDuringPlacement--
        }
        if (owner != null) {
            child.detach()
        }
        child._foldedParent = null
        child.outerCoordinator.wrappedBy = null

        if (child.isVirtual) {
            virtualChildrenCount--
            child._foldedChildren.forEach {
                it.outerCoordinator.wrappedBy = null
            }
        }
        invalidateUnfoldedVirtualChildren()
        onZSortedChildrenInvalidated()
    }

    /**
     * Moves [count] elements starting at index [from] to index [to]. The [to] index is related to
     * the position before the change, so, for example, to move an element at position 1 to after
     * the element at position 2, [from] should be `1` and [to] should be `3`. If the elements
     * were LayoutNodes A B C D E, calling `move(1, 3, 1)` would result in the LayoutNodes
     * being reordered to A C B D E.
     */
    internal fun move(from: Int, to: Int, count: Int) {
        if (from == to) {
            return // nothing to do
        }

        for (i in 0 until count) {
            // if "from" is after "to," the from index moves because we're inserting before it
            val fromIndex = if (from > to) from + i else from
            val toIndex = if (from > to) to + i else to + count - 2
            val child = _foldedChildren.removeAt(fromIndex)

            if (DebugChanges) {
                println("$child moved in $this from index $fromIndex to $toIndex")
            }

            _foldedChildren.add(toIndex, child)
        }
        onZSortedChildrenInvalidated()

        invalidateUnfoldedVirtualChildren()
        invalidateMeasurements()
    }

    /**
     * Set the [Owner] of this LayoutNode. This LayoutNode must not already be attached.
     * [owner] must match its [parent].[owner].
     */
    internal fun attach(owner: Owner) {
        check(this.owner == null) {
            "Cannot attach $this as it already is attached.  Tree: " + debugTreeToString()
        }
        check(_foldedParent == null || _foldedParent?.owner == owner) {
            "Attaching to a different owner($owner) than the parent's owner(${parent?.owner})." +
                " This tree: " + debugTreeToString() +
                " Parent tree: " + _foldedParent?.debugTreeToString()
        }
        val parent = this.parent
        if (parent == null) {
            // it is a root node and attached root nodes are always placed (as there is no parent
            // to place them explicitly)
            isPlaced = true
        }

        this.owner = owner
        this.depth = (parent?.depth ?: -1) + 1
        @OptIn(ExperimentalComposeUiApi::class)
        if (outerSemantics != null) {
            owner.onSemanticsChange()
        }
        owner.onAttach(this)
        // Update lookahead scope when attached. For nested cases, we'll always use the
        // lookahead scope from the out-most LookaheadRoot.
        mLookaheadScope =
            parent?.mLookaheadScope ?: if (isLookaheadRoot) LookaheadScope(this) else null

        nodes.attach(performInvalidations = false)
        _foldedChildren.forEach { child ->
            child.attach(owner)
        }

        invalidateMeasurements()
        parent?.invalidateMeasurements()

        forEachCoordinatorIncludingInner { it.onLayoutNodeAttach() }
        onAttach?.invoke(owner)

        invalidateFocusOnAttach()
    }

    /**
     * Remove the LayoutNode from the [Owner]. The [owner] must not be `null` before this call
     * and its [parent]'s [owner] must be `null` before calling this. This will also [detach] all
     * children. After executing, the [owner] will be `null`.
     */
    internal fun detach() {
        val owner = owner
        checkNotNull(owner) {
            "Cannot detach node that is already detached!  Tree: " + parent?.debugTreeToString()
        }
        invalidateFocusOnDetach()
        val parent = this.parent
        if (parent != null) {
            parent.invalidateLayer()
            parent.invalidateMeasurements()
            measuredByParent = UsageByParent.NotUsed
        }
        layoutDelegate.resetAlignmentLines()
        onDetach?.invoke(owner)

        @OptIn(ExperimentalComposeUiApi::class)
        if (outerSemantics != null) {
            owner.onSemanticsChange()
        }
        nodes.detach()
        owner.onDetach(this)
        this.owner = null
        depth = 0
        _foldedChildren.forEach { child ->
            child.detach()
        }
        placeOrder = NotPlacedPlaceOrder
        previousPlaceOrder = NotPlacedPlaceOrder
        isPlaced = false
    }

    private val _zSortedChildren = mutableVectorOf<LayoutNode>()
    private var zSortedChildrenInvalidated = true

    /**
     * Returns the children list sorted by their [LayoutNode.zIndex] first (smaller first) and the
     * order they were placed via [Placeable.placeAt] by parent (smaller first).
     * Please note that this list contains not placed items as well, so you have to manually
     * filter them.
     *
     * Note that the object is reused so you shouldn't save it for later.
     */
    @PublishedApi
    internal val zSortedChildren: MutableVector<LayoutNode>
        get() {
            if (zSortedChildrenInvalidated) {
                _zSortedChildren.clear()
                _zSortedChildren.addAll(_children)
                _zSortedChildren.sortWith(ZComparator)
                zSortedChildrenInvalidated = false
            }
            return _zSortedChildren
        }

    override val isValidOwnerScope: Boolean
        get() = isAttached

    override fun toString(): String {
        return "${simpleIdentityToString(this, null)} children: ${children.size} " +
            "measurePolicy: $measurePolicy"
    }

    internal val hasFixedInnerContentConstraints: Boolean
        get() {
            // it is the constraints we have after all the modifiers applied on this node,
            // the one to be passed into user provided [measurePolicy.measure]. if those
            // constraints are fixed this means the children size changes can't affect
            // this LayoutNode size.
            val innerContentConstraints = innerCoordinator.lastMeasurementConstraints
            return innerContentConstraints.hasFixedWidth && innerContentConstraints.hasFixedHeight
        }

    /**
     * Call this method from the debugger to see a dump of the LayoutNode tree structure
     */
    @Suppress("unused")
    private fun debugTreeToString(depth: Int = 0): String {
        val tree = StringBuilder()
        for (i in 0 until depth) {
            tree.append("  ")
        }
        tree.append("|-")
        tree.append(toString())
        tree.append('\n')

        forEachChild { child ->
            tree.append(child.debugTreeToString(depth + 1))
        }

        var treeString = tree.toString()
        if (depth == 0) {
            // Delete trailing newline
            treeString = treeString.substring(0, treeString.length - 1)
        }

        return treeString
    }

    internal abstract class NoIntrinsicsMeasurePolicy(private val error: String) : MeasurePolicy {
        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = error(error)

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = error(error)

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = error(error)

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = error(error)
    }

    /**
     * Blocks that define the measurement and intrinsic measurement of the layout.
     */
    override var measurePolicy: MeasurePolicy = ErrorMeasurePolicy
        set(value) {
            if (field != value) {
                field = value
                intrinsicsPolicy.updateFrom(measurePolicy)
                invalidateMeasurements()
            }
        }

    /**
     * The intrinsic measurements of this layout, backed up by states to trigger
     * correct remeasurement for layouts using the intrinsics of this layout
     * when the [measurePolicy] is changing.
     */
    internal val intrinsicsPolicy = IntrinsicsPolicy(this)

    /**
     * The screen density to be used by this layout.
     */
    override var density: Density = Density(1f)
        set(value) {
            if (field != value) {
                field = value
                onDensityOrLayoutDirectionChanged()
            }
        }

    internal var mLookaheadScope: LookaheadScope? = null
        private set(newScope) {
            if (newScope != field) {
                field = newScope
                layoutDelegate.onLookaheadScopeChanged(newScope)
                forEachCoordinatorIncludingInner { coordinator ->
                    coordinator.updateLookaheadScope(newScope)
                }
            }
        }

    /**
     * The layout direction of the layout node.
     */
    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr
        set(value) {
            if (field != value) {
                field = value
                onDensityOrLayoutDirectionChanged()
            }
        }

    override var viewConfiguration: ViewConfiguration = DummyViewConfiguration

    private fun onDensityOrLayoutDirectionChanged() {
        // TODO(b/242120396): it seems like we need to update some densities in the node coordinators here
        // measure/layout modifiers on the node
        invalidateMeasurements()
        // draw modifiers on the node
        parent?.invalidateLayer()
        // and draw modifiers after graphics layers on the node
        invalidateLayers()
    }

    /**
     * The measured width of this layout and all of its [modifier]s. Shortcut for `size.width`.
     */
    override val width: Int
        get() = layoutDelegate.width

    /**
     * The measured height of this layout and all of its [modifier]s. Shortcut for `size.height`.
     */
    override val height: Int
        get() = layoutDelegate.height

    internal val alignmentLinesRequired: Boolean
        get() = layoutDelegate.run {
            alignmentLinesOwner.alignmentLines.required ||
                lookaheadAlignmentLinesOwner?.alignmentLines?.required == true
        }

    internal val mDrawScope: LayoutNodeDrawScope
        get() = requireOwner().sharedDrawScope

    /**
     * Whether or not this [LayoutNode] and all of its parents have been placed in the hierarchy.
     */
    override var isPlaced: Boolean = false
        private set

    /**
     * The order in which this node was placed by its parent during the previous `layoutChildren`.
     * Before the placement the order is set to [NotPlacedPlaceOrder] to all the children. Then
     * every placed node assigns this variable to [parent]s [nextChildPlaceOrder] and increments
     * this counter. Not placed items will still have [NotPlacedPlaceOrder] set.
     */
    internal var placeOrder: Int = NotPlacedPlaceOrder
        private set

    /**
     * The value [placeOrder] had during the previous parent `layoutChildren`. Helps us to
     * understand if the order did change.
     */
    internal var previousPlaceOrder: Int = NotPlacedPlaceOrder
        private set

    /**
     * The counter on a parent node which is used by its children to understand the order in which
     * they were placed.
     * @see placeOrder
     */
    private var nextChildPlaceOrder: Int = 0

    /**
     * Remembers how the node was measured by the parent.
     */
    internal var measuredByParent: UsageByParent = UsageByParent.NotUsed

    /**
     * Remembers how the node was measured by the parent in lookahead.
     */
    internal var measuredByParentInLookahead: UsageByParent = UsageByParent.NotUsed

    /**
     * Remembers how the node was measured using intrinsics by an ancestor.
     */
    internal var intrinsicsUsageByParent: UsageByParent = UsageByParent.NotUsed

    /**
     * We must cache a previous value of [intrinsicsUsageByParent] because measurement
     * is sometimes skipped. When it is skipped, the subtree must be restored to this value.
     */
    private var previousIntrinsicsUsageByParent: UsageByParent = UsageByParent.NotUsed

    @Deprecated("Temporary API to support ConstraintLayout prototyping.")
    internal var canMultiMeasure: Boolean = false

    var isLookaheadRoot: Boolean = false
        set(value) {
            if (value != field) {
                if (!value) {
                    mLookaheadScope = null
                } else {
                    mLookaheadScope = LookaheadScope(this)
                }
                field = value
            }
        }

    internal val nodes = NodeChain(this)
    internal val innerCoordinator: NodeCoordinator
        get() = nodes.innerCoordinator
    internal val layoutDelegate = LayoutNodeLayoutDelegate(this)
    internal val outerCoordinator: NodeCoordinator
        get() = nodes.outerCoordinator

    /**
     * zIndex defines the drawing order of the LayoutNode. Children with larger zIndex are drawn
     * on top of others (the original order is used for the nodes with the same zIndex).
     * Default zIndex is 0. We use sum of the values passed as zIndex to place() by the
     * parent layout and all the applied modifiers.
     */
    private var zIndex: Float = 0f

    /**
     * The inner state associated with [androidx.compose.ui.layout.SubcomposeLayout].
     */
    internal var subcompositionsState: LayoutNodeSubcompositionsState? = null

    /**
     * The inner-most layer coordinator. Used for performance for NodeCoordinator.findLayer().
     */
    private var _innerLayerCoordinator: NodeCoordinator? = null
    internal var innerLayerCoordinatorIsDirty = true
    private val innerLayerCoordinator: NodeCoordinator?
        get() {
            if (innerLayerCoordinatorIsDirty) {
                var coordinator: NodeCoordinator? = innerCoordinator
                val final = outerCoordinator.wrappedBy
                _innerLayerCoordinator = null
                while (coordinator != final) {
                    if (coordinator?.layer != null) {
                        _innerLayerCoordinator = coordinator
                        break
                    }
                    coordinator = coordinator?.wrappedBy
                }
            }
            val layerCoordinator = _innerLayerCoordinator
            if (layerCoordinator != null) {
                requireNotNull(layerCoordinator.layer)
            }
            return layerCoordinator
        }

    /**
     * Invalidates the inner-most layer as part of this LayoutNode or from the containing
     * LayoutNode. This is added for performance so that NodeCoordinator.invalidateLayer() can be
     * faster.
     */
    internal fun invalidateLayer() {
        val innerLayerCoordinator = innerLayerCoordinator
        if (innerLayerCoordinator != null) {
            innerLayerCoordinator.invalidateLayer()
        } else {
            val parent = this.parent
            parent?.invalidateLayer()
        }
    }

    /**
     * The [Modifier] currently applied to this node.
     */
    override var modifier: Modifier = Modifier
        set(value) {
            require(!isVirtual || modifier === Modifier) {
                "Modifiers are not supported on virtual LayoutNodes"
            }
            field = value
            nodes.updateFrom(value)

            // TODO(lmr): we don't need to do this every time and should attempt to avoid it
            //  whenever possible!
            forEachCoordinatorIncludingInner {
                it.updateLookaheadScope(mLookaheadScope)
            }

            layoutDelegate.updateParentData()
        }

    private fun resetModifierState() {
        nodes.resetState()
    }

    internal fun invalidateParentData() {
        layoutDelegate.invalidateParentData()
    }

    /**
     * Coordinates of just the contents of the [LayoutNode], after being affected by all modifiers.
     */
    override val coordinates: LayoutCoordinates
        get() = innerCoordinator

    /**
     * Callback to be executed whenever the [LayoutNode] is attached to a new [Owner].
     */
    internal var onAttach: ((Owner) -> Unit)? = null

    /**
     * Callback to be executed whenever the [LayoutNode] is detached from an [Owner].
     */
    internal var onDetach: ((Owner) -> Unit)? = null

    /**
     * Flag used by [OnPositionedDispatcher] to identify LayoutNodes that have already
     * had their [OnGloballyPositionedModifier]'s dispatch called so that they aren't called
     * multiple times.
     */
    internal var needsOnPositionedDispatch = false

    internal fun place(x: Int, y: Int) {
        if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
            // This LayoutNode may have asked children for intrinsics. If so, we should
            // clear the intrinsics usage for everything that was requested previously.
            clearSubtreePlacementIntrinsicsUsage()
        }
        with(measurePassDelegate) {
            Placeable.PlacementScope.executeWithRtlMirroringValues(
                measuredWidth,
                layoutDirection,
                parent?.innerCoordinator
            ) {
                placeRelative(x, y)
            }
        }
    }

    /**
     * Place this layout node again on the same position it was placed last time
     */
    internal fun replace() {
        if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
            // This LayoutNode may have asked children for intrinsics. If so, we should
            // clear the intrinsics usage for everything that was requested previously.
            clearSubtreePlacementIntrinsicsUsage()
        }
        try {
            relayoutWithoutParentInProgress = true
            measurePassDelegate.replace()
        } finally {
            relayoutWithoutParentInProgress = false
        }
    }

    internal fun lookaheadReplace() {
        if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
            // This LayoutNode may have asked children for intrinsics. If so, we should
            // clear the intrinsics usage for everything that was requested previously.
            clearSubtreePlacementIntrinsicsUsage()
        }
        lookaheadPassDelegate!!.replace()
    }

    /**
     * Is true during [replace] invocation. Helps to differentiate between the cases when our
     * parent is measuring us during the measure block, and when we are remeasured individually
     * because of some change. This could be useful to know if we need to record the placing order.
     */
    private var relayoutWithoutParentInProgress = false

    internal fun draw(canvas: Canvas) = outerCoordinator.draw(canvas)

    /**
     * Carries out a hit test on the [PointerInputModifier]s associated with this [LayoutNode] and
     * all [PointerInputModifier]s on all descendant [LayoutNode]s.
     *
     * If [pointerPosition] is within the bounds of any tested
     * [PointerInputModifier]s, the [PointerInputModifier] is added to [hitTestResult]
     * and true is returned.
     *
     * @param pointerPosition The tested pointer position, which is relative to
     * the LayoutNode.
     * @param hitTestResult The collection that the hit [PointerInputFilter]s will be
     * added to if hit.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    internal fun hitTest(
        pointerPosition: Offset,
        hitTestResult: HitTestResult<PointerInputModifierNode>,
        isTouchEvent: Boolean = false,
        isInLayer: Boolean = true
    ) {
        val positionInWrapped = outerCoordinator.fromParentPosition(pointerPosition)
        outerCoordinator.hitTest(
            NodeCoordinator.PointerInputSource,
            positionInWrapped,
            hitTestResult,
            isTouchEvent,
            isInLayer
        )
    }

    @Suppress("UNUSED_PARAMETER")
    @OptIn(ExperimentalComposeUiApi::class)
    internal fun hitTestSemantics(
        pointerPosition: Offset,
        hitSemanticsEntities: HitTestResult<SemanticsModifierNode>,
        isTouchEvent: Boolean = true,
        isInLayer: Boolean = true
    ) {
        val positionInWrapped = outerCoordinator.fromParentPosition(pointerPosition)
        outerCoordinator.hitTest(
            NodeCoordinator.SemanticsSource,
            positionInWrapped,
            hitSemanticsEntities,
            isTouchEvent = true,
            isInLayer = isInLayer
        )
    }

    /**
     * Invoked when the parent placed the node. It will trigger the layout.
     */
    internal fun onNodePlaced() {
        val parent = parent

        var newZIndex = innerCoordinator.zIndex
        forEachCoordinator {
            newZIndex += it.zIndex
        }
        if (newZIndex != zIndex) {
            zIndex = newZIndex
            parent?.onZSortedChildrenInvalidated()
            parent?.invalidateLayer()
        }

        if (!isPlaced) {
            // when the visibility of a child has been changed we need to invalidate
            // parents inner layer - the layer in which this child will be drawn
            parent?.invalidateLayer()
            markNodeAndSubtreeAsPlaced()
        }

        if (parent != null) {
            if (!relayoutWithoutParentInProgress && parent.layoutState == LayingOut) {
                // the parent is currently placing its children
                check(placeOrder == NotPlacedPlaceOrder) {
                    "Place was called on a node which was placed already"
                }
                placeOrder = parent.nextChildPlaceOrder
                parent.nextChildPlaceOrder++
            }
            // if relayoutWithoutParentInProgress is true we were asked to be relaid out without
            // affecting the parent. this means our placeOrder didn't change since the last time
            // parent placed us.
        } else {
            // parent is null for the root node
            placeOrder = 0
        }

        layoutDelegate.alignmentLinesOwner.layoutChildren()
    }

    internal fun clearPlaceOrder() {
        // reset the place order counter which will be used by the children
        nextChildPlaceOrder = 0
        forEachChild { child ->
            // and reset the place order for all the children before placing them
            child.previousPlaceOrder = child.placeOrder
            child.placeOrder = LayoutNode.NotPlacedPlaceOrder
            // before rerunning the user's layout block reset previous measuredByParent
            // for children which we measured in the layout block during the last run.
            if (child.measuredByParent == LayoutNode.UsageByParent.InLayoutBlock) {
                child.measuredByParent = LayoutNode.UsageByParent.NotUsed
            }
        }
    }

    internal fun checkChildrenPlaceOrderForUpdates() {
        forEachChild { child ->
            // we set `placeOrder` to NotPlacedPlaceOrder for all the children, then
            // during the placeChildren() invocation the real order will be assigned for
            // all the placed children.
            if (child.previousPlaceOrder != child.placeOrder) {
                onZSortedChildrenInvalidated()
                invalidateLayer()
                if (child.placeOrder == LayoutNode.NotPlacedPlaceOrder) {
                    child.markSubtreeAsNotPlaced()
                }
            }
        }
    }

    private fun markNodeAndSubtreeAsPlaced() {
        val wasPlaced = isPlaced
        isPlaced = true
        if (!wasPlaced) {
            // if the node was not placed previous remeasure request could have been ignored
            if (measurePending) {
                requestRemeasure(forceRequest = true)
            } else if (lookaheadMeasurePending) {
                requestLookaheadRemeasure(forceRequest = true)
            }
        }
        // invalidate all the nodes layers that were invalidated while the node was not placed
        forEachCoordinatorIncludingInner {
            if (it.lastLayerDrawingWasSkipped) {
                it.invalidateLayer()
            }
        }
        forEachChild {
            // this child was placed during the previous parent's layoutChildren(). this means that
            // before the parent became not placed this child was placed. we need to restore that
            if (it.placeOrder != NotPlacedPlaceOrder) {
                it.markNodeAndSubtreeAsPlaced()
                rescheduleRemeasureOrRelayout(it)
            }
        }
    }

    internal fun rescheduleRemeasureOrRelayout(it: LayoutNode) {
        when (it.layoutState) {
            Idle -> {
                // this node was scheduled for remeasure or relayout while it was not
                // placed. such requests are ignored for non-placed nodes so we have to
                // re-schedule remeasure or relayout.
                if (it.measurePending) {
                    it.requestRemeasure(forceRequest = true)
                } else if (it.layoutPending) {
                    it.requestRelayout(forceRequest = true)
                } else if (it.lookaheadMeasurePending) {
                    it.requestLookaheadRemeasure(forceRequest = true)
                } else if (it.lookaheadLayoutPending) {
                    it.requestLookaheadRelayout(forceRequest = true)
                } else {
                    // no extra work required and node is ready to be displayed
                }
            }
            else -> throw IllegalStateException("Unexpected state ${it.layoutState}")
        }
    }

    private fun markSubtreeAsNotPlaced() {
        if (isPlaced) {
            isPlaced = false
            forEachChild {
                it.markSubtreeAsNotPlaced()
            }
        }
    }

    /**
     * Used to request a new measurement + layout pass from the owner.
     */
    internal fun requestRemeasure(forceRequest: Boolean = false) {
        if (!ignoreRemeasureRequests && !isVirtual) {
            val owner = owner ?: return
            owner.onRequestMeasure(this, forceRequest = forceRequest)
            measurePassDelegate.invalidateIntrinsicsParent(forceRequest)
        }
    }

    /**
     * Used to request a new lookahead measurement, lookahead layout, and subsequently
     * measure and layout from the owner.
     */
    internal fun requestLookaheadRemeasure(forceRequest: Boolean = false) {
        check(mLookaheadScope != null) {
            "Lookahead measure cannot be requested on a node that is not a part of the" +
                "LookaheadLayout"
        }
        val owner = owner ?: return
        if (!ignoreRemeasureRequests && !isVirtual) {
            owner.onRequestMeasure(this, affectsLookahead = true, forceRequest = forceRequest)
            lookaheadPassDelegate!!.invalidateIntrinsicsParent(forceRequest)
        }
    }

    /**
     * This gets called when both lookahead measurement (if in a LookaheadLayout) and actual
     * measurement need to be re-done. Such events include modifier change, attach/detach, etc.
     */
    internal fun invalidateMeasurements() {
        if (mLookaheadScope != null) {
            requestLookaheadRemeasure()
        } else {
            requestRemeasure()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun invalidateFocusOnAttach() {
        if (nodes.has(FocusTarget or FocusProperties or FocusEvent)) {
            nodes.headToTail {
                if (it.isKind(FocusTarget) or it.isKind(FocusProperties) or it.isKind(FocusEvent)) {
                    autoInvalidateInsertedNode(it)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun invalidateFocusOnDetach() {
        if (nodes.has(FocusTarget)) {
            nodes.tailToHead {
                if (
                    it.isKind(FocusTarget) &&
                    it is FocusTargetModifierNode &&
                    it.focusState.isFocused
                ) {
                    requireOwner().focusOwner.clearFocus(force = true, refreshFocusEvents = false)
                    it.scheduleInvalidationForFocusEvents()
                }
            }
        }
    }

    internal inline fun ignoreRemeasureRequests(block: () -> Unit) {
        ignoreRemeasureRequests = true
        block()
        ignoreRemeasureRequests = false
    }

    /**
     * Used to request a new layout pass from the owner.
     */
    internal fun requestRelayout(forceRequest: Boolean = false) {
        if (!isVirtual) {
            owner?.onRequestRelayout(this, forceRequest = forceRequest)
        }
    }

    internal fun requestLookaheadRelayout(forceRequest: Boolean = false) {
        if (!isVirtual) {
            owner?.onRequestRelayout(this, affectsLookahead = true, forceRequest)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    internal fun dispatchOnPositionedCallbacks() {
        if (layoutState != Idle || layoutPending || measurePending) {
            return // it hasn't yet been properly positioned, so don't make a call
        }
        if (!isPlaced) {
            return // it hasn't been placed, so don't make a call
        }
        nodes.headToTail(Nodes.GlobalPositionAware) {
            it.onGloballyPositioned(it.requireCoordinator(Nodes.GlobalPositionAware))
        }
    }

    /**
     * This returns a new List of Modifiers and the coordinates and any extra information
     * that may be useful. This is used for tooling to retrieve layout modifier and layer
     * information.
     */
    override fun getModifierInfo(): List<ModifierInfo> = nodes.getModifierInfo()

    /**
     * Invalidates layers defined on this LayoutNode.
     */
    internal fun invalidateLayers() {
        forEachCoordinator { coordinator ->
            coordinator.layer?.invalidate()
        }
        innerCoordinator.layer?.invalidate()
    }

    internal fun lookaheadRemeasure(
        constraints: Constraints? = layoutDelegate.lastLookaheadConstraints
    ): Boolean {
        // Only lookahead remeasure when the constraints are valid and the node is in
        // a LookaheadLayout (by checking whether the lookaheadScope is set)
        return if (constraints != null && mLookaheadScope != null) {
            lookaheadPassDelegate!!.remeasure(constraints)
        } else {
            false
        }
    }

    /**
     * Return true if the measured size has been changed
     */
    internal fun remeasure(
        constraints: Constraints? = layoutDelegate.lastConstraints
    ): Boolean {
        return if (constraints != null) {
            if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
                // This LayoutNode may have asked children for intrinsics. If so, we should
                // clear the intrinsics usage for everything that was requested previously.
                clearSubtreeIntrinsicsUsage()
            }
            measurePassDelegate.remeasure(constraints)
        } else {
            false
        }
    }

    /**
     * Tracks whether another measure pass is needed for the LayoutNode.
     * Mutation to [measurePending] is confined to LayoutNodeLayoutDelegate.
     * It can only be set true from outside of LayoutNode via [markMeasurePending].
     * It is cleared (i.e. set false) during the measure pass (
     * i.e. in [LayoutNodeLayoutDelegate.performMeasure]).
     */
    internal val measurePending: Boolean
        get() = layoutDelegate.measurePending

    /**
     * Tracks whether another layout pass is needed for the LayoutNode.
     * Mutation to [layoutPending] is confined to LayoutNode. It can only be set true from outside
     * of LayoutNode via [markLayoutPending]. It is cleared (i.e. set false) during the layout pass
     * (i.e. in layoutChildren).
     */
    internal val layoutPending: Boolean
        get() = layoutDelegate.layoutPending

    internal val lookaheadMeasurePending: Boolean
        get() = layoutDelegate.lookaheadMeasurePending

    internal val lookaheadLayoutPending: Boolean
        get() = layoutDelegate.lookaheadLayoutPending

    /**
     * Marks the layoutNode dirty for another layout pass.
     */
    internal fun markLayoutPending() = layoutDelegate.markLayoutPending()

    /**
     * Marks the layoutNode dirty for another measure pass.
     */
    internal fun markMeasurePending() = layoutDelegate.markMeasurePending()

    /**
     * Marks the layoutNode dirty for another lookahead layout pass.
     */
    internal fun markLookaheadLayoutPending() = layoutDelegate.markLookaheadLayoutPending()

    @OptIn(ExperimentalComposeUiApi::class)
    fun invalidateSubtree(isRootOfInvalidation: Boolean = true) {
        if (isRootOfInvalidation) {
            parent?.invalidateLayer()
            // Invalidate semantics. We can do this once because there isn't a node-by-node
            // invalidation mechanism.
            requireOwner().onSemanticsChange()
        }
        requestRemeasure()
        nodes.headToTail(Nodes.Layout) {
            it.requireCoordinator(Nodes.Layout).layer?.invalidate()
        }
        // TODO: invalidate parent data
        _children.forEach { it.invalidateSubtree(false) }
    }

    /**
     * Marks the layoutNode dirty for another lookahead measure pass.
     */
    internal fun markLookaheadMeasurePending() =
        layoutDelegate.markLookaheadMeasurePending()

    override fun forceRemeasure() {
        requestRemeasure()
        val lastConstraints = layoutDelegate.lastConstraints
        if (lastConstraints != null) {
            owner?.measureAndLayout(this, lastConstraints)
        } else {
            owner?.measureAndLayout()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onLayoutComplete() {
        innerCoordinator.visitNodes(Nodes.LayoutAware) {
            it.onPlaced(innerCoordinator)
        }
    }

    /**
     * Calls [block] on all [LayoutModifierNodeCoordinator]s in the NodeCoordinator chain.
     */
    private inline fun forEachCoordinator(block: (LayoutModifierNodeCoordinator) -> Unit) {
        var coordinator: NodeCoordinator? = outerCoordinator
        val inner = innerCoordinator
        while (coordinator !== inner) {
            block(coordinator as LayoutModifierNodeCoordinator)
            coordinator = coordinator.wrapped
        }
    }

    /**
     * Calls [block] on all [NodeCoordinator]s in the NodeCoordinator chain.
     */
    private inline fun forEachCoordinatorIncludingInner(block: (NodeCoordinator) -> Unit) {
        var delegate: NodeCoordinator? = outerCoordinator
        val final = innerCoordinator.wrapped
        while (delegate != final && delegate != null) {
            block(delegate)
            delegate = delegate.wrapped
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun shouldInvalidateParentLayer(): Boolean {
        if (nodes.has(Nodes.Draw) && !nodes.has(Nodes.Layout)) return true
        nodes.headToTail {
            if (it.isKind(Nodes.Layout) && it is LayoutModifierNode) {
                if (it.requireCoordinator(Nodes.Layout).layer != null) {
                    return false
                }
            }
            if (it.isKind(Nodes.Draw)) return true
        }
        return true
    }

    /**
     * Walks the subtree and clears all [intrinsicsUsageByParent] that this
     * LayoutNode's measurement used intrinsics on.
     *
     * The layout that asks for intrinsics of its children is the node to call this to request
     * all of its subtree to be cleared.
     *
     * We can't do clearing as part of measure() because the child's measure()
     * call is normally done after the intrinsics is requested and we don't want
     * to clear the usage at that point.
     */
    internal fun clearSubtreeIntrinsicsUsage() {
        // save the usage in case we short-circuit the measure call
        previousIntrinsicsUsageByParent = intrinsicsUsageByParent
        intrinsicsUsageByParent = UsageByParent.NotUsed
        forEachChild {
            if (it.intrinsicsUsageByParent != UsageByParent.NotUsed) {
                it.clearSubtreeIntrinsicsUsage()
            }
        }
    }

    /**
     * Walks the subtree and clears all [intrinsicsUsageByParent] that this
     * LayoutNode's layout block used intrinsics on.
     *
     * The layout that asks for intrinsics of its children is the node to call this to request
     * all of its subtree to be cleared.
     *
     * We can't do clearing as part of measure() because the child's measure()
     * call is normally done after the intrinsics is requested and we don't want
     * to clear the usage at that point.
     */
    private fun clearSubtreePlacementIntrinsicsUsage() {
        // save the usage in case we short-circuit the measure call
        previousIntrinsicsUsageByParent = intrinsicsUsageByParent
        intrinsicsUsageByParent = UsageByParent.NotUsed
        forEachChild {
            if (it.intrinsicsUsageByParent == UsageByParent.InLayoutBlock) {
                it.clearSubtreePlacementIntrinsicsUsage()
            }
        }
    }

    /**
     * For a subtree that skips measurement, this resets the [intrinsicsUsageByParent]
     * to what it was prior to [clearSubtreeIntrinsicsUsage].
     */
    internal fun resetSubtreeIntrinsicsUsage() {
        forEachChild {
            it.intrinsicsUsageByParent = it.previousIntrinsicsUsageByParent
            if (it.intrinsicsUsageByParent != UsageByParent.NotUsed) {
                it.resetSubtreeIntrinsicsUsage()
            }
        }
    }

    override val parentInfo: LayoutInfo?
        get() = parent

    private var deactivated = false

    override fun onReuse() {
        interopViewFactoryHolder?.onReuse()
        if (deactivated) {
            deactivated = false
            // we don't need to reset state as it was done when deactivated
        } else {
            resetModifierState()
        }
    }

    override fun onDeactivate() {
        interopViewFactoryHolder?.onDeactivate()
        deactivated = true
        resetModifierState()
    }

    override fun onRelease() {
        interopViewFactoryHolder?.onRelease()
        forEachCoordinatorIncludingInner { it.onRelease() }
    }

    internal companion object {
        private val ErrorMeasurePolicy: NoIntrinsicsMeasurePolicy =
            object : NoIntrinsicsMeasurePolicy(
                error = "Undefined intrinsics block and it is required"
            ) {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints
                ) = error("Undefined measure and it is required")
            }

        /**
         * Constant used by [placeOrder].
         */
        internal const val NotPlacedPlaceOrder = Int.MAX_VALUE

        /**
         * Pre-allocated constructor to be used with ComposeNode
         */
        internal val Constructor: () -> LayoutNode = { LayoutNode() }

        /**
         * All of these values are only used in tests. The real ViewConfiguration should
         * be set in Layout()
         */
        internal val DummyViewConfiguration = object : ViewConfiguration {
            override val longPressTimeoutMillis: Long
                get() = 400L
            override val doubleTapTimeoutMillis: Long
                get() = 300L
            override val doubleTapMinTimeMillis: Long
                get() = 40L
            override val touchSlop: Float
                get() = 16f
            override val minimumTouchTargetSize: DpSize
                get() = DpSize.Zero
        }

        /**
         * Comparator allowing to sort nodes by zIndex and placement order.
         */
        internal val ZComparator = Comparator<LayoutNode> { node1, node2 ->
            if (node1.zIndex == node2.zIndex) {
                // if zIndex is the same we use the placement order
                node1.placeOrder.compareTo(node2.placeOrder)
            } else {
                node1.zIndex.compareTo(node2.zIndex)
            }
        }
    }

    /**
     * Describes the current state the [LayoutNode] is in. A [LayoutNode] is expected to be in
     * [LookaheadMeasuring] first, followed by [LookaheadLayingOut] if it is in a
     * LookaheadLayout. After the lookahead is finished, [Measuring] and then [LayingOut] will
     * happen as needed.
     */
    internal enum class LayoutState {
        /**
         * Node is currently being measured.
         */
        Measuring,

        /**
         * Node is being measured in lookahead.
         */
        LookaheadMeasuring,

        /**
         * Node is currently being laid out.
         */
        LayingOut,

        /**
         * Node is being laid out in lookahead.
         */
        LookaheadLayingOut,

        /**
         * Node is not currently measuring or laying out. It could be pending measure or pending
         * layout depending on the [measurePending] and [layoutPending] flags.
         */
        Idle,
    }

    internal enum class UsageByParent {
        InMeasureBlock,
        InLayoutBlock,
        NotUsed,
    }
}

/**
 * Returns [LayoutNode.owner] or throws if it is null.
 */
internal fun LayoutNode.requireOwner(): Owner {
    val owner = owner
    checkNotNull(owner) {
        "LayoutNode should be attached to an owner"
    }
    return owner
}

/**
 * Inserts a child [LayoutNode] at a last index. If this LayoutNode [LayoutNode.isAttached]
 * then [child] will become [LayoutNode.isAttached] also. [child] must have a `null`
 * [LayoutNode.parent].
 */
internal fun LayoutNode.add(child: LayoutNode) {
    insertAt(children.size, child)
}
