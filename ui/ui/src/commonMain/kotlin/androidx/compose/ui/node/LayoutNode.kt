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

import androidx.compose.runtime.collection.ExperimentalCollectionApi
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.AlignmentLine
import androidx.compose.ui.ContentDrawScope
import androidx.compose.ui.DrawLayerModifier
import androidx.compose.ui.DrawModifier
import androidx.compose.ui.FocusModifier
import androidx.compose.ui.FocusObserverModifier
import androidx.compose.ui.FocusRequesterModifier
import androidx.compose.ui.HorizontalAlignmentLine
import androidx.compose.ui.LayoutModifier
import androidx.compose.ui.Measurable
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnGloballyPositionedModifier
import androidx.compose.ui.OnRemeasuredModifier
import androidx.compose.ui.ParentDataModifier
import androidx.compose.ui.Placeable
import androidx.compose.ui.Remeasurement
import androidx.compose.ui.RemeasurementModifier
import androidx.compose.ui.ZIndexModifier
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.key.KeyInputModifier
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.merge
import androidx.compose.ui.node.LayoutNode.LayoutState.LayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.Measuring
import androidx.compose.ui.node.LayoutNode.LayoutState.NeedsRelayout
import androidx.compose.ui.node.LayoutNode.LayoutState.NeedsRemeasure
import androidx.compose.ui.node.LayoutNode.LayoutState.Ready
import androidx.compose.ui.platform.simpleIdentityToString
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsWrapper
import androidx.compose.ui.semantics.outerSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.deleteAt
import androidx.compose.ui.util.nativeClass
import kotlin.math.roundToInt

/**
 * Enable to log changes to the LayoutNode tree.  This logging is quite chatty.
 */
private const val DebugChanges = false

// Top level DrawScope instance shared across the LayoutNode hierarchy to re-use internal
// drawing objects
internal val sharedDrawScope = LayoutNodeDrawScope()

/**
 * An element in the layout hierarchy, built with compose UI.
 */
@ExperimentalLayoutNodeApi
@OptIn(
    ExperimentalCollectionApi::class,
    ExperimentalFocus::class,
    ExperimentalLayoutNodeApi::class
)
class LayoutNode : Measurable, Remeasurement, OwnerScope {

    constructor() : this(false)

    internal constructor(isVirtual: Boolean) {
        this.isVirtual = isVirtual
    }

    // Virtual LayoutNode is the temporary concept allows us to a node which is not a real node,
    // but just a holder for its children - allows us to combine some children into something we
    // can subcompose in(LayoutNode) without being required to define it as a real layout - we
    // don't want to define the layout strategy for such nodes, instead the children of the
    // virtual nodes will be threated as the direct children of the virtual node parent.
    // This whole concept will be replaced with a proper subcomposition logic which allows to
    // subcompose multiple times into the same LayoutNode and define offsets.

    private val isVirtual: Boolean

    private var virtualChildrenCount = 0

    // the list of nodes containing the virtual children as is
    private val _foldedChildren = mutableVectorOf<LayoutNode>()
    internal val foldedChildren: List<LayoutNode> get() = _foldedChildren.asMutableList()

    // the list of nodes where the virtual children are unfolded (their children are represented
    // as our direct children)
    private val _unfoldedChildren = mutableVectorOf<LayoutNode>()

    private fun recreateUnfoldedChildrenIfDirty() {
        if (unfoldedVirtualChildrenListDirty) {
            unfoldedVirtualChildrenListDirty = false
            _unfoldedChildren.clear()
            _foldedChildren.forEach {
                if (it.isVirtual) {
                    _unfoldedChildren.addAll(it._children)
                } else {
                    _unfoldedChildren.add(it)
                }
            }
        }
    }

    // when the list of our children is modified it will be set to true if we are a virtual node
    // or it will be set to true on a parent if the parent is a virtual node
    private var unfoldedVirtualChildrenListDirty = false
    private fun invalidateUnfoldedVirtualChildren() {
        if (virtualChildrenCount > 0) {
            unfoldedVirtualChildrenListDirty = true
        }
        if (isVirtual) {
            parent?.unfoldedVirtualChildrenListDirty = true
        }
    }

    internal val _children: MutableVector<LayoutNode>
        get() = if (virtualChildrenCount == 0) {
            _foldedChildren
        } else {
            recreateUnfoldedChildrenIfDirty()
            _unfoldedChildren
        }

    /**
     * The children of this LayoutNode, controlled by [insertAt], [move], and [removeAt].
     */
    val children: List<LayoutNode> get() = _children.asMutableList()

    /**
     * The parent node in the LayoutNode hierarchy. This is `null` when the `LayoutNode`
     * is attached (has an [owner]) and is the root of the tree or has not had [add] called for it.
     */
    var parent: LayoutNode? = null
        get() {
            val parent = field
            return if (parent != null && parent.isVirtual) parent.parent else parent
        }
        private set

    /**
     * The view system [Owner]. This `null` until [attach] is called
     */
    var owner: Owner? = null
        private set

    /**
     * The tree depth of the LayoutNode. This is valid only when [owner] is not `null`.
     */
    var depth: Int = 0

    /**
     * The layout state the node is currently in.
     */
    internal var layoutState = Ready

    internal val wasMeasuredDuringThisIteration: Boolean
        get() = requireOwner().measureIteration == outerMeasurablePlaceable.measureIteration

    /**
     * A cache of modifiers to be used when setting and reusing previous modifiers.
     */
    private var wrapperCache = mutableVectorOf<DelegatingLayoutNodeWrapper<*>>()

    /**
     * Inserts a child [LayoutNode] at a particular index. If this LayoutNode [owner] is not `null`
     * then [instance] will become [attach]ed also. [instance] must have a `null` [parent].
     */
    fun insertAt(index: Int, instance: LayoutNode) {
        check(instance.parent == null) {
            "Cannot insert $instance because it already has a parent"
        }
        check(instance.owner == null) {
            "Cannot insert $instance because it already has an owner"
        }

        if (DebugChanges) {
            println("$instance added to $this at index $index")
        }

        instance.parent = this
        _foldedChildren.add(index, instance)

        if (instance.isVirtual) {
            require(!isVirtual) { "Virtual LayoutNode can't be added into a virtual parent" }
            virtualChildrenCount++
        }
        invalidateUnfoldedVirtualChildren()

        instance.outerLayoutNodeWrapper.wrappedBy = innerLayoutNodeWrapper

        val owner = this.owner
        if (owner != null) {
            instance.attach(owner)
        }
    }

    /**
     * Removes one or more children, starting at [index].
     */
    fun removeAt(index: Int, count: Int) {
        require(count >= 0) {
            "count ($count) must be greater than 0"
        }
        val attached = owner != null
        for (i in index + count - 1 downTo index) {
            val child = _foldedChildren.removeAt(i)
            if (DebugChanges) {
                println("$child removed from $this at index $i")
            }

            if (attached) {
                child.detach()
            }
            child.parent = null

            if (child.isVirtual) {
                virtualChildrenCount--
            }
            invalidateUnfoldedVirtualChildren()
        }
    }

    /**
     * Removes all children.
     */
    fun removeAll() {
        val attached = owner != null
        for (i in _foldedChildren.size - 1 downTo 0) {
            val child = _foldedChildren[i]
            if (attached) {
                child.detach()
            }
            child.parent = null
        }
        _foldedChildren.clear()

        virtualChildrenCount = 0
        invalidateUnfoldedVirtualChildren()
    }

    /**
     * Moves [count] elements starting at index [from] to index [to]. The [to] index is related to
     * the position before the change, so, for example, to move an element at position 1 to after
     * the element at position 2, [from] should be `1` and [to] should be `3`. If the elements
     * were LayoutNodes A B C D E, calling `move(1, 3, 1)` would result in the LayoutNodes
     * being reordered to A C B D E.
     */
    fun move(from: Int, to: Int, count: Int) {
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

        invalidateUnfoldedVirtualChildren()
        requestRemeasure()
    }

    /**
     * Set the [Owner] of this LayoutNode. This LayoutNode must not already be attached.
     * [owner] must match its [parent].[owner].
     */
    fun attach(owner: Owner) {
        check(this.owner == null) {
            "Cannot attach $this as it already is attached"
        }
        val parent = parent
        check(parent == null || parent.owner == owner) {
            "Attaching to a different owner($owner) than the parent's owner(${parent?.owner})"
        }
        if (parent == null) {
            // it is a root node and attached root nodes are always placed (as there is no parent
            // to place them explicitly)
            isPlaced = true
        }

        this.owner = owner
        this.depth = (parent?.depth ?: -1) + 1
        if (outerSemantics != null) {
            owner.onSemanticsChange()
        }
        owner.onAttach(this)
        _foldedChildren.forEach { child ->
            child.attach(owner)
        }

        requestRemeasure()
        parent?.requestRemeasure()
        forEachDelegate { it.attach() }
        onAttach?.invoke(owner)
    }

    /**
     * Remove the LayoutNode from the [Owner]. The [owner] must not be `null` before this call
     * and its [parent]'s [owner] must be `null` before calling this. This will also [detach] all
     * children. After executing, the [owner] will be `null`.
     */
    fun detach() {
        val owner = owner
        checkNotNull(owner) {
            "Cannot detach node that is already detached!"
        }
        val parentLayoutNode = parent
        if (parentLayoutNode != null) {
            parentLayoutNode.invalidateLayer()
            parentLayoutNode.requestRemeasure()
        }
        alignmentLinesQueryOwner = null
        alignmentUsageByParent = UsageByParent.NotUsed
        onDetach?.invoke(owner)
        forEachDelegate { it.detach() }

        if (outerSemantics != null) {
            owner.onSemanticsChange()
        }
        owner.onDetach(this)
        this.owner = null
        depth = 0
        _foldedChildren.forEach { child ->
            child.detach()
        }
        placeOrder = NotPlacedPlaceOrder
        isPlaced = false
    }

    private val _zSortedChildren = mutableVectorOf<LayoutNode>()

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
            _zSortedChildren.clear()
            _zSortedChildren.addAll(_children)
            _zSortedChildren.sortWith(ZComparator)
            return _zSortedChildren
        }

    override val isValid: Boolean
        get() = isAttached()

    override fun toString(): String {
        return "${simpleIdentityToString(this, null)} children: ${children.size} " +
            "measureBlocks: $measureBlocks"
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

        _children.forEach { child ->
            tree.append(child.debugTreeToString(depth + 1))
        }

        if (depth == 0) {
            // Delete trailing newline
            tree.deleteAt(tree.length - 1)
        }
        return tree.toString()
    }

    interface MeasureBlocks {
        /**
         * The function used to measure the child. It must call [MeasureScope.layout] before
         * completing.
         */
        fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureScope.MeasureResult

        /**
         * The function used to calculate [IntrinsicMeasurable.minIntrinsicWidth].
         */
        fun minIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: Int
        ): Int

        /**
         * The lambda used to calculate [IntrinsicMeasurable.minIntrinsicHeight].
         */
        fun minIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: Int
        ): Int

        /**
         * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth].
         */
        fun maxIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: Int
        ): Int

        /**
         * The lambda used to calculate [IntrinsicMeasurable.maxIntrinsicHeight].
         */
        fun maxIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: Int
        ): Int
    }

    abstract class NoIntrinsicsMeasureBlocks(private val error: String) : MeasureBlocks {
        override fun minIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: Int
        ) = error(error)

        override fun minIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: Int
        ) = error(error)

        override fun maxIntrinsicWidth(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            h: Int
        ) = error(error)

        override fun maxIntrinsicHeight(
            intrinsicMeasureScope: IntrinsicMeasureScope,
            measurables: List<IntrinsicMeasurable>,
            w: Int
        ) = error(error)
    }

    /**
     * Blocks that define the measurement and intrinsic measurement of the layout.
     */
    var measureBlocks: MeasureBlocks = ErrorMeasureBlocks
        set(value) {
            if (field != value) {
                field = value
                requestRemeasure()
            }
        }

    /**
     * The screen density to be used by this layout.
     */
    var density: Density = Density(1f)

    /**
     * The scope used to run the [MeasureBlocks.measure]
     * [MeasureBlock][androidx.compose.ui.MeasureBlock].
     */
    val measureScope: MeasureScope = object : MeasureScope, Density {
        override val density: Float get() = this@LayoutNode.density.density
        override val fontScale: Float get() = this@LayoutNode.density.fontScale
        override val layoutDirection: LayoutDirection get() = this@LayoutNode.layoutDirection
    }

    /**
     * The layout direction of the layout node.
     */
    internal var layoutDirection: LayoutDirection = LayoutDirection.Ltr
        set(value) {
            if (field != value) {
                field = value
                requestRemeasure()
            }
        }

    /**
     * The measured width of this layout and all of its [modifier]s. Shortcut for `size.width`.
     */
    val width: Int get() = outerMeasurablePlaceable.width

    /**
     * The measured height of this layout and all of its [modifier]s. Shortcut for `size.height`.
     */
    val height: Int get() = outerMeasurablePlaceable.height

    /**
     * The alignment lines of this layout, inherited + intrinsic
     */
    internal val alignmentLines: MutableMap<AlignmentLine, Int> = hashMapOf()

    /**
     * The alignment lines provided by this layout at the last measurement
     */
    internal val providedAlignmentLines: MutableMap<AlignmentLine, Int> = hashMapOf()

    internal val mDrawScope: LayoutNodeDrawScope = sharedDrawScope

    /**
     * Whether or not this LayoutNode and all of its parents have been placed in the hierarchy.
     */
    var isPlaced = false
        private set

    /**
     * The order in which this node was placed by its parent during the previous [layoutChildren].
     * Before the placement the order is set to [NotPlacedPlaceOrder] to all the children. Then
     * every placed node assigns this variable to [parent]s [nextChildPlaceOrder] and increments
     * this counter. Not placed items will still have [NotPlacedPlaceOrder] set.
     */
    private var placeOrder: Int = NotPlacedPlaceOrder

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
     * `true` while doing [calculateAlignmentLines]
     */
    private var isCalculatingAlignmentLines = false

    /**
     * `true` when the parent reads our alignment lines
     */
    private var alignmentLinesRead = false

    private var alignmentLinesCalculatedDuringLastLayout = false

    /**
     * `true` when an ancestor relies on our alignment lines
     */
    internal val alignmentLinesRequired
        get() = alignmentLinesQueryOwner != null && alignmentLinesQueryOwner!!.alignmentLinesRead

    /**
     * Used by the parent to identify if the child has been queried for alignment lines since
     * last measurement.
     */
    private var alignmentLinesQueriedSinceLastLayout = false

    /**
     * The closest layout node above in the hierarchy which asked for alignment lines.
     */
    internal var alignmentLinesQueryOwner: LayoutNode? = null
        private set

    internal var alignmentUsageByParent = UsageByParent.NotUsed

    private val previousAlignmentLines = mutableMapOf<AlignmentLine, Int>()

    @Deprecated("Temporary API to support ConstraintLayout prototyping.")
    var canMultiMeasure: Boolean = false

    internal val innerLayoutNodeWrapper: LayoutNodeWrapper = InnerPlaceable(this)
    private val outerMeasurablePlaceable = OuterMeasurablePlaceable(this, innerLayoutNodeWrapper)
    internal val outerLayoutNodeWrapper: LayoutNodeWrapper
        get() = outerMeasurablePlaceable.outerWrapper

    /**
     * zIndex defines the drawing order of the LayoutNode. Children with larger zIndex are drawn
     * after others (the original order is used for the nodes with the same zIndex).
     * Default zIndex is 0. We use sum of the values of all [ZIndexModifier] as a zIndex.
     */
    private val zIndex: Float
        get() = if (zIndexModifiers.isEmpty()) {
            0f
        } else {
            zIndexModifiers.fold(0f) { acc, item ->
                acc + item.zIndex
            }
        }

    /**
     * All [ZIndexModifier]s added to the node.
     */
    private val zIndexModifiers = mutableVectorOf<ZIndexModifier>()

    /**
     * The inner-most layer wrapper. Used for performance for LayoutNodeWrapper.findLayer().
     */
    internal var innerLayerWrapper: LayerWrapper? = null

    /**
     * Invalidates the inner-most layer as part of this LayoutNode or from the containing
     * LayoutNode. This is added for performance so that LayoutNodeWrapper.invalidateLayer() can be
     * faster.
     */
    internal fun invalidateLayer() {
        val innerLayerWrapper = innerLayerWrapper
        if (innerLayerWrapper != null) {
            innerLayerWrapper.invalidateLayer()
        } else {
            val parent = parent
            parent?.invalidateLayer()
        }
    }

    /**
     * The [Modifier] currently applied to this node.
     */
    var modifier: Modifier = Modifier
        set(value) {
            if (value == field) return
            if (modifier != Modifier) {
                require(!isVirtual) { "Modifiers are not supported on virtual LayoutNodes" }
            }
            field = value

            val invalidateParentLayer = shouldInvalidateParentLayer()
            val startZIndex = zIndex

            copyWrappersToCache()

            // Rebuild layoutNodeWrapper
            val oldOuterWrapper = outerMeasurablePlaceable.outerWrapper
            if (outerSemantics != null && isAttached()) {
                owner!!.onSemanticsChange()
            }
            val addedCallback = hasNewPositioningCallback()
            onPositionedCallbacks.clear()
            onRemeasuredCallbacks.clear()
            zIndexModifiers.clear()
            innerLayerWrapper = null

            // Create a new chain of LayoutNodeWrappers, reusing existing ones from wrappers
            // when possible.
            val outerWrapper = modifier.foldOut(innerLayoutNodeWrapper) { mod, toWrap ->
                var wrapper = toWrap
                if (mod is OnGloballyPositionedModifier) {
                    onPositionedCallbacks += mod
                }
                if (mod is OnRemeasuredModifier) {
                    onRemeasuredCallbacks += mod
                }
                if (mod is ZIndexModifier) {
                    zIndexModifiers += mod
                }
                if (mod is RemeasurementModifier) {
                    mod.onRemeasurementAvailable(this)
                }

                val delegate = reuseLayoutNodeWrapper(mod, toWrap)
                if (delegate != null) {
                    wrapper = delegate
                } else {
                    // The order in which the following blocks occur matters. For example, the
                    // DrawModifier block should be before the LayoutModifier block so that a
                    // Modifier that implements both DrawModifier and LayoutModifier will have
                    // it's draw bounds reflect the dimensions defined by the LayoutModifier.
                    if (mod is DrawModifier) {
                        wrapper = ModifiedDrawNode(wrapper, mod)
                    }
                    if (mod is DrawLayerModifier) {
                        val layerWrapper = LayerWrapper(wrapper, mod).assignChained(toWrap)
                        wrapper = layerWrapper
                        if (innerLayerWrapper == null) {
                            innerLayerWrapper = layerWrapper
                        }
                    }
                    if (mod is FocusModifier) {
                        wrapper = ModifiedFocusNode(wrapper, mod).assignChained(toWrap)
                    }
                    if (mod is FocusObserverModifier) {
                        wrapper = ModifiedFocusObserverNode(wrapper, mod).assignChained(toWrap)
                    }
                    if (mod is FocusRequesterModifier) {
                        wrapper = ModifiedFocusRequesterNode(wrapper, mod).assignChained(toWrap)
                    }
                    if (mod is KeyInputModifier) {
                        wrapper = ModifiedKeyInputNode(wrapper, mod).assignChained(toWrap)
                    }
                    if (mod is PointerInputModifier) {
                        wrapper = PointerInputDelegatingWrapper(wrapper, mod).assignChained(toWrap)
                    }
                    if (mod is LayoutModifier) {
                        wrapper = ModifiedLayoutNode(wrapper, mod).assignChained(toWrap)
                    }
                    if (mod is ParentDataModifier) {
                        wrapper = ModifiedParentDataNode(wrapper, mod).assignChained(toWrap)
                    }
                    if (mod is SemanticsModifier) {
                        wrapper = SemanticsWrapper(wrapper, mod).assignChained(toWrap)
                    }
                }
                wrapper
            }

            outerWrapper.wrappedBy = parent?.innerLayoutNodeWrapper
            outerMeasurablePlaceable.outerWrapper = outerWrapper

            if (isAttached()) {
                // call detach() on all removed LayoutNodeWrappers
                wrapperCache.forEach { it.detach() }

                // attach() all new LayoutNodeWrappers
                forEachDelegate {
                    if (!it.isAttached) {
                        it.attach()
                    }
                }
            }
            wrapperCache.clear()

            // call onModifierChanged() on all LayoutNodeWrappers
            forEachDelegate { it.onModifierChanged() }

            // Optimize the case where the layout itself is not modified. A common reason for
            // this is if no wrapping actually occurs above because no LayoutModifiers are
            // present in the modifier chain.
            if (oldOuterWrapper != innerLayoutNodeWrapper ||
                outerWrapper != innerLayoutNodeWrapper
            ) {
                requestRemeasure()
                parent?.requestRelayout()
            } else if (layoutState == Ready && addedCallback) {
                // We need to notify the callbacks of a change in position since there's
                // a new one.
                requestRemeasure()
            }
            // If the parent data has changed, the parent needs remeasurement.
            val oldParentData = parentData
            outerMeasurablePlaceable.recalculateParentData()
            if (oldParentData != parentData) {
                parent?.requestRemeasure()
            }
            if (invalidateParentLayer || startZIndex != zIndex ||
                shouldInvalidateParentLayer()
            ) {
                parent?.invalidateLayer()
            }
        }

    /**
     * Coordinates of just the contents of the LayoutNode, after being affected by all modifiers.
     */
    // TODO(mount): remove this
    val coordinates: LayoutCoordinates
        get() = innerLayoutNodeWrapper

    /**
     * Callback to be executed whenever the [LayoutNode] is attached to a new [Owner].
     */
    var onAttach: ((Owner) -> Unit)? = null

    /**
     * Callback to be executed whenever the [LayoutNode] is detached from an [Owner].
     */
    var onDetach: ((Owner) -> Unit)? = null

    /**
     * List of all OnPositioned callbacks in the modifier chain.
     */
    private val onPositionedCallbacks = mutableVectorOf<OnGloballyPositionedModifier>()

    /**
     * List of all OnSizeChangedModifiers in the modifier chain.
     */
    private val onRemeasuredCallbacks = mutableVectorOf<OnRemeasuredModifier>()

    /**
     * Flag used by [OnPositionedDispatcher] to identify LayoutNodes that have already
     * had their [OnGloballyPositionedModifier]'s dispatch called so that they aren't called
     * multiple times.
     */
    internal var needsOnPositionedDispatch = false

    fun place(x: Int, y: Int) {
        Placeable.PlacementScope.executeWithRtlMirroringValues(
            outerMeasurablePlaceable.measuredWidth,
            layoutDirection
        ) {
            outerMeasurablePlaceable.placeRelative(x, y)
        }
    }

    /**
     * Place this layout node again on the same position it was placed last time
     */
    internal fun replace() {
        outerMeasurablePlaceable.replace()
    }

    fun draw(canvas: Canvas) = outerLayoutNodeWrapper.draw(canvas)

    /**
     * Carries out a hit test on the [PointerInputModifier]s associated with this [LayoutNode] and
     * all [PointerInputModifier]s on all descendant [LayoutNode]s.
     *
     * If [pointerPositionRelativeToScreen] is within the bounds of any tested
     * [PointerInputModifier]s, the [PointerInputModifier] is added to [hitPointerInputFilters]
     * and true is returned.
     *
     * @param pointerPositionRelativeToScreen The tested pointer position, which is relative to
     * the device screen.
     * @param hitPointerInputFilters The collection that the hit [PointerInputFilter]s will be
     * added to if hit.
     */
    fun hitTest(
        pointerPositionRelativeToScreen: Offset,
        hitPointerInputFilters: MutableList<PointerInputFilter>
    ) {
        outerLayoutNodeWrapper.hitTest(pointerPositionRelativeToScreen, hitPointerInputFilters)
    }

    /**
     * Returns the alignment line value for a given alignment line without affecting whether
     * the flag for whether the alignment line was read.
     */
    fun getAlignmentLine(line: AlignmentLine): Int? {
        val linePos = alignmentLines[line] ?: return null
        var pos = Offset(linePos.toFloat(), linePos.toFloat())
        var wrapper = innerLayoutNodeWrapper
        while (wrapper != outerLayoutNodeWrapper) {
            pos = wrapper.toParentPosition(pos)
            wrapper = wrapper.wrappedBy!!
        }
        pos = wrapper.toParentPosition(pos)
        return if (line is HorizontalAlignmentLine) {
            pos.y.roundToInt()
        } else {
            pos.x.roundToInt()
        }
    }

    /**
     * Return true if there is a new [OnGloballyPositionedModifier] assigned to this Layout.
     */
    private fun hasNewPositioningCallback(): Boolean {
        return modifier.foldOut(false) { mod, hasNewCallback ->
            hasNewCallback ||
                (mod is OnGloballyPositionedModifier && mod !in onPositionedCallbacks)
        }
    }

    /**
     * Invoked when the parent placed the node. It will trigger the layout.
     */
    internal fun onNodePlaced() {
        val parent = parent

        if (!isPlaced) {
            isPlaced = true
            // when the visibility of a child has been changed we need to invalidate
            // parents inner layer - the layer in which this child will be drawn
            parent?.invalidateLayer()
            // plus all the inner layers that were invalidated while the node was not placed
            forEachDelegate {
                if (it is LayerWrapper && it.lastDrawingWasSkipped) {
                    it.layer.invalidate()
                }
            }
            markSubtreeAsPlaced()
        }

        if (parent != null) {
            if (parent.layoutState == LayingOut) {
                // the parent is currently placing its children
                check(placeOrder == NotPlacedPlaceOrder) {
                    "Place was called on a node which was placed already"
                }
                placeOrder = parent.nextChildPlaceOrder
                parent.nextChildPlaceOrder++
            }
            // if parent is not laying out we were asked to be relaid out without affecting the
            // parent. this means our placeOrder didn't change since the last time parent placed us
        } else {
            // parent is null for the root node
            placeOrder = 0
        }

        layoutChildren()
    }

    private fun layoutChildren() {
        if (layoutState == NeedsRelayout) {
            onBeforeLayoutChildren()
        }
        // as a result of the previous operation we can figure out a child has been resized
        // and we need to be remeasured, not relaid out
        if (layoutState == NeedsRelayout) {
            layoutState = LayoutState.LayingOut
            val owner = requireOwner()
            owner.observeLayoutModelReads(this) {
                // reset the place order counter which will be used by the children
                nextChildPlaceOrder = 0
                _children.forEach { child ->
                    // and reset the place order for all the children before placing them
                    child.placeOrder = NotPlacedPlaceOrder
                    if (alignmentLinesRequired && child.layoutState == Ready &&
                        !child.alignmentLinesCalculatedDuringLastLayout
                    ) {
                        child.layoutState = NeedsRelayout
                    }
                    if (!child.alignmentLinesRequired) {
                        child.alignmentLinesQueryOwner = alignmentLinesQueryOwner
                    }
                    child.alignmentLinesQueriedSinceLastLayout = false
                }
                innerLayoutNodeWrapper.measureResult.placeChildren()
                _children.forEach { child ->
                    // we set `placeOrder` to NotPlacedPlaceOrder for all the children, then
                    // during the placeChildren() invocation the real order will be assigned for
                    // all the placed children.
                    if (child.placeOrder == NotPlacedPlaceOrder) {
                        child.markSubtreeAsNotPlaced()
                    }
                    child.alignmentLinesRead = child.alignmentLinesQueriedSinceLastLayout
                }
            }

            alignmentLinesCalculatedDuringLastLayout = false
            if (alignmentLinesRequired) {
                alignmentLinesCalculatedDuringLastLayout = true
                previousAlignmentLines.clear()
                previousAlignmentLines.putAll(alignmentLines)
                alignmentLines.clear()
                _children.forEach { child ->
                    if (!child.isPlaced) return@forEach
                    child.alignmentLines.keys.forEach { childLine ->
                        val linePositionInContainer = child.getAlignmentLine(childLine)!!
                        // If the line was already provided by a previous child, merge the values.
                        alignmentLines[childLine] = if (childLine in alignmentLines) {
                            childLine.merge(
                                alignmentLines.getValue(childLine),
                                linePositionInContainer
                            )
                        } else {
                            linePositionInContainer
                        }
                    }
                }
                alignmentLines += providedAlignmentLines
                if (previousAlignmentLines != alignmentLines) {
                    onAlignmentsChanged()
                }
            }
            layoutState = Ready
        }
    }

    private fun markSubtreeAsPlaced() {
        _children.forEach {
            // if the layout state is not Ready then isPlaced will be set during the layout
            if (it.layoutState == Ready && it.placeOrder != NotPlacedPlaceOrder) {
                it.isPlaced = true
                it.markSubtreeAsPlaced()
            }
        }
    }

    private fun markSubtreeAsNotPlaced() {
        if (isPlaced) {
            isPlaced = false
            _children.forEach {
                markSubtreeAsNotPlaced()
            }
        }
    }

    /**
     * The callback to be executed before running layoutChildren.
     *
     * There are possible cases when we run layoutChildren() on the parent node, but some of its
     * children are not yet measured even if they are supposed to be measured in the measure
     * block of our parent.
     *
     * Example:
     * val child = Layout(...)
     * Layout(child) { measuruables, constraints ->
     *    val placeable = measurables.first().measure(constraints)
     *    layout(placeable.width, placeable.height) {
     *       placeable.place(0, 0)
     *    }
     * }
     * And now some set of changes scheduled remeasure for child and relayout for parent.
     *
     * During the [MeasureAndLayoutDelegate.measureAndLayout] we will start with the parent as it
     * has lower depth. Inside the layout block we will call placeable.width which is currently
     * dirty as the child was scheduled to remeasure. This callback will ensure it never happens
     * and pre-remeasure everything required for this layoutChildren().
     */
    private fun onBeforeLayoutChildren() {
        _children.forEach {
            if (it.layoutState == NeedsRemeasure &&
                it.measuredByParent == UsageByParent.InMeasureBlock
            ) {
                if (it.remeasure()) {
                    requestRemeasure()
                }
            }
        }
    }

    internal fun onAlignmentsChanged() {
        val parent = parent
        if (parent != null) {
            if (alignmentUsageByParent == UsageByParent.InMeasureBlock &&
                parent.layoutState != LayoutState.LayingOut
            ) {
                parent.requestRemeasure()
            } else if (alignmentUsageByParent == UsageByParent.InLayoutBlock) {
                parent.requestRelayout()
            }
        }
    }

    internal fun calculateAlignmentLines(): Map<AlignmentLine, Int> {
        isCalculatingAlignmentLines = true
        alignmentLinesRead = true
        alignmentLinesQueryOwner = this
        alignmentLinesQueriedSinceLastLayout = true
        val newUsageByParent = when (parent?.layoutState) {
            Measuring -> UsageByParent.InMeasureBlock
            LayoutState.LayingOut -> UsageByParent.InLayoutBlock
            else -> UsageByParent.NotUsed
        }
        val newUsageHasLowerPriority = newUsageByParent == UsageByParent.InLayoutBlock &&
            alignmentUsageByParent == UsageByParent.InMeasureBlock
        if (!newUsageHasLowerPriority) {
            alignmentUsageByParent = newUsageByParent
        }
        if (layoutState == NeedsRelayout || !alignmentLinesCalculatedDuringLastLayout) {
            // layoutChildren() is a state transformation from NeedsRelayout to Ready.
            // when we are already in NeedsRelayout we need to end up with Ready, but if we are
            // currently measuring or need remeasure this extra layoutChildren is just a side effect
            // and we will need to restore the current state.
            val endState = if (layoutState == Measuring || layoutState == NeedsRemeasure) {
                layoutState
            } else {
                Ready
            }
            if (!alignmentLinesCalculatedDuringLastLayout) {
                layoutState = NeedsRelayout
            }
            layoutChildren()
            layoutState = endState
        }
        isCalculatingAlignmentLines = false
        return alignmentLines
    }

    internal fun handleMeasureResult(measureResult: MeasureScope.MeasureResult) {
        innerLayoutNodeWrapper.measureResult = measureResult
        this.providedAlignmentLines.clear()
        this.providedAlignmentLines += measureResult.alignmentLines

        if (onRemeasuredCallbacks.isNotEmpty()) {
            owner?.pauseModelReadObserveration {
                val content = innerLayoutNodeWrapper
                val size = IntSize(content.measuredWidth, content.measuredHeight)
                onRemeasuredCallbacks.forEach { it.onRemeasured(size) }
            }
        }
    }

    /**
     * Used to request a new measurement + layout pass from the owner.
     */
    fun requestRemeasure() {
        owner?.onRequestMeasure(this)
    }

    /**
     * Used to request a new layout pass from the owner.
     */
    fun requestRelayout() {
        owner?.onRequestRelayout(this)
    }

    /**
     * Execute your code within the [block] if you want some code to not be observed for the
     * model reads even if you are currently inside some observed scope like measuring.
     */
    fun ignoreModelReads(block: () -> Unit) {
        requireOwner().pauseModelReadObserveration(block)
    }

    internal fun dispatchOnPositionedCallbacks() {
        if (layoutState != Ready) {
            return // it hasn't yet been properly positioned, so don't make a call
        }
        if (!isPlaced) {
            return // it hasn't been placed, so don't make a call
        }
        onPositionedCallbacks.forEach { it.onGloballyPositioned(coordinates) }
    }

    /**
     * This returns a new List of Modifiers and the coordinates and any extra information
     * that may be useful. This is used for tooling to retrieve layout modifier and layer
     * information.
     */
    fun getModifierInfo(): List<ModifierInfo> {
        val infoList = mutableVectorOf<ModifierInfo>()
        forEachDelegate { wrapper ->
            val info = if (wrapper is LayerWrapper) {
                ModifierInfo(wrapper.modifier, wrapper, wrapper.layer)
            } else {
                wrapper as DelegatingLayoutNodeWrapper<*>
                ModifierInfo(wrapper.modifier, wrapper)
            }
            infoList += info
        }
        return infoList.asMutableList()
    }

    /**
     * Invalidates layers defined on this LayoutNode.
     */
    internal fun invalidateLayers() {
        forEachDelegate { wrapper ->
            (wrapper as? LayerWrapper)?.invalidateLayer()
        }
    }

    /**
     * Reuses a [DelegatingLayoutNodeWrapper] from [wrapperCache] if one matches the class
     * type of [modifier]. This walks backward through the [wrapperCache] and
     * extracts all [DelegatingLayoutNodeWrapper]s that are
     * [chained][DelegatingLayoutNodeWrapper.isChained] together.
     * If none can be reused, `null` is returned.
     */
    private fun reuseLayoutNodeWrapper(
        modifier: Modifier.Element,
        wrapper: LayoutNodeWrapper
    ): DelegatingLayoutNodeWrapper<*>? {
        if (wrapperCache.isEmpty()) {
            return null
        }
        val index = wrapperCache.indexOfLast {
            it.modifier === modifier || it.modifier.nativeClass() == modifier.nativeClass()
        }

        if (index < 0) {
            return null
        }

        val endWrapper = wrapperCache[index]
        var startWrapper = endWrapper
        var chainedIndex = index
        startWrapper.setModifierTo(modifier)
        if (innerLayerWrapper == null && startWrapper is LayerWrapper) {
            innerLayerWrapper = startWrapper
        }

        while (startWrapper.isChained) {
            chainedIndex--
            startWrapper = wrapperCache[chainedIndex]
            startWrapper.setModifierTo(modifier)
            if (innerLayerWrapper == null && startWrapper is LayerWrapper) {
                innerLayerWrapper = startWrapper
            }
        }

        wrapperCache.removeRange(chainedIndex, index + 1)

        endWrapper.wrapped = wrapper
        wrapper.wrappedBy = endWrapper
        return startWrapper
    }

    /**
     * Copies all [DelegatingLayoutNodeWrapper]s currently in use and returns them in a new
     * Array.
     */
    private fun copyWrappersToCache() {
        forEachDelegate {
            wrapperCache += it as DelegatingLayoutNodeWrapper<*>
        }
    }

    // Delegation from Measurable to measurableAndPlaceable
    override fun measure(constraints: Constraints) =
        outerMeasurablePlaceable.measure(constraints)

    /**
     * Return true if the measured size has been changed
     */
    internal fun remeasure(
        constraints: Constraints = outerMeasurablePlaceable.lastConstraints!!
    ) = outerMeasurablePlaceable.remeasure(constraints)

    override val parentData: Any? get() = outerMeasurablePlaceable.parentData

    override fun minIntrinsicWidth(height: Int): Int =
        outerMeasurablePlaceable.minIntrinsicWidth(height)

    override fun maxIntrinsicWidth(height: Int): Int =
        outerMeasurablePlaceable.maxIntrinsicWidth(height)

    override fun minIntrinsicHeight(width: Int): Int =
        outerMeasurablePlaceable.minIntrinsicHeight(width)

    override fun maxIntrinsicHeight(width: Int): Int =
        outerMeasurablePlaceable.maxIntrinsicHeight(width)

    override fun forceRemeasure() {
        requestRemeasure()
        owner?.measureAndLayout()
    }

    /**
     * Calls [block] on all [DelegatingLayoutNodeWrapper]s in the LayoutNodeWrapper chain.
     */
    private inline fun forEachDelegate(block: (LayoutNodeWrapper) -> Unit) {
        var delegate = outerLayoutNodeWrapper
        val inner = innerLayoutNodeWrapper
        while (delegate != inner) {
            block(delegate)
            delegate = delegate.wrapped!!
        }
    }

    private fun shouldInvalidateParentLayer(): Boolean {
        if (innerLayerWrapper == null) {
            return true
        }
        forEachDelegate {
            if (it is ModifiedDrawNode) {
                return true
            } else if (it is LayerWrapper) {
                return false
            }
        }
        error("innerLayerWrapper should have been reached.")
    }

    /**
     * Comparator allowing to sort nodes by zIndex and placement order.
     */
    private val ZComparator = Comparator<LayoutNode> { node1, node2 ->
        if (node1.zIndex == node2.zIndex) {
            // if zIndex is the same we use the placement order
            node1.placeOrder.compareTo(node2.placeOrder)
        } else {
            node1.zIndex.compareTo(node2.zIndex)
        }
    }

    internal companion object {
        private val ErrorMeasureBlocks: NoIntrinsicsMeasureBlocks =
            object : NoIntrinsicsMeasureBlocks(
                error = "Undefined intrinsics block and it is required"
            ) {
                override fun measure(
                    measureScope: MeasureScope,
                    measurables: List<Measurable>,
                    constraints: Constraints
                ) = error("Undefined measure and it is required")
            }

        /**
         * Constant used by [placeOrder].
         */
        private const val NotPlacedPlaceOrder = Int.MAX_VALUE
    }

    /**
     * Describes the current state the [LayoutNode] is in.
     */
    internal enum class LayoutState {
        /**
         * Request remeasure was called on the node.
         */
        NeedsRemeasure,
        /**
         * Node is currently being measured.
         */
        Measuring,
        /**
         * Request relayout was called on the node or the node was just measured and is going to
         * layout soon (measure stage is always being followed by the layout stage).
         */
        NeedsRelayout,
        /**
         * Node is currently being laid out.
         */
        LayingOut,
        /**
         * Node is measured and laid out or not yet attached to the [Owner] (see [LayoutNode.owner]).
         */
        Ready
    }

    internal enum class UsageByParent {
        InMeasureBlock,
        InLayoutBlock,
        NotUsed,
    }
}

/**
 * Object of pre-allocated lambdas used to make emits to LayoutNodes allocation-less.
 */
@OptIn(ExperimentalLayoutNodeApi::class)
@PublishedApi
internal object LayoutEmitHelper {
    val constructor: () -> LayoutNode = { LayoutNode() }
    val setModifier: LayoutNode.(Modifier) -> Unit = { this.modifier = it }
    val setDensity: LayoutNode.(Density) -> Unit = { this.density = it }
    val setMeasureBlocks: LayoutNode.(LayoutNode.MeasureBlocks) -> Unit =
        { this.measureBlocks = it }
    val setRef: LayoutNode.(Ref<LayoutNode>) -> Unit = { it.value = this }
    val setLayoutDirection: LayoutNode.(LayoutDirection) -> Unit = { this.layoutDirection = it }
}

/**
 * Returns true if this [LayoutNode] currently has an [LayoutNode.owner].  Semantically,
 * this means that the LayoutNode is currently a part of a component tree.
 */
@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalLayoutNodeApi::class)
internal inline fun LayoutNode.isAttached() = owner != null

/**
 * Used by tooling to examine the modifiers on a [LayoutNode].
 */
class ModifierInfo(
    val modifier: Modifier,
    val coordinates: LayoutCoordinates,
    val extra: Any? = null
)

/**
 * Returns [LayoutNode.owner] or throws if it is null.
 */
@OptIn(ExperimentalLayoutNodeApi::class)
internal fun LayoutNode.requireOwner(): Owner {
    val owner = owner
    checkNotNull(owner) {
        "LayoutNode should be attached to an owner"
    }
    return owner
}

/**
 * Inserts a child [LayoutNode] at a last index. If this LayoutNode [isAttached]
 * then [child] will become [isAttached]ed also. [child] must have a `null` [LayoutNode.parent].
 */
@OptIn(ExperimentalLayoutNodeApi::class)
internal fun LayoutNode.add(child: LayoutNode) {
    insertAt(children.size, child)
}

/**
 * Executes [selector] on every parent of this [LayoutNode] and returns the closest
 * [LayoutNode] to return `true` from [selector] or null if [selector] returns false
 * for all ancestors.
 */
@OptIn(ExperimentalLayoutNodeApi::class)
fun LayoutNode.findClosestParentNode(selector: (LayoutNode) -> Boolean): LayoutNode? {
    var currentParent = parent
    while (currentParent != null) {
        if (selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parent
        }
    }

    return null
}

/**
 * [ContentDrawScope] implementation that extracts density and layout direction information
 * from the given LayoutNodeWrapper
 */
@OptIn(ExperimentalLayoutNodeApi::class)
internal class LayoutNodeDrawScope(
    private val canvasDrawScope: CanvasDrawScope = CanvasDrawScope()
) : DrawScope by canvasDrawScope, ContentDrawScope {

    // NOTE, currently a single ComponentDrawScope is shared across composables
    // which done to allocate a single set of Paint objects and re-use them across
    // draw calls for all composables.
    // As a result there could be thread safety concerns here for multi-threaded drawing
    // scenarios, generally a single ComponentDrawScope should be shared for a particular thread
    private var wrapped: LayoutNodeWrapper? = null

    override fun drawContent() {
        drawIntoCanvas { canvas -> wrapped?.draw(canvas) }
    }

    internal inline fun draw(
        canvas: Canvas,
        size: Size,
        layoutNodeWrapper: LayoutNodeWrapper,
        block: DrawScope.() -> Unit
    ) {
        val previousWrapper = wrapped
        wrapped = layoutNodeWrapper
        canvasDrawScope.draw(
            layoutNodeWrapper.measureScope,
            layoutNodeWrapper.measureScope.layoutDirection,
            canvas,
            size,
            block
        )
        wrapped = previousWrapper
    }
}

/**
 * Sets [DelegatingLayoutNodeWrapper#isChained] to `true` of the [wrapped][this.wrapped] when it
 * is part of a chain of LayoutNodes for the same modifier.
 *
 * @param originalWrapper The LayoutNodeWrapper that the modifier chain should be wrapping.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun <T : DelegatingLayoutNodeWrapper<*>> T.assignChained(
    originalWrapper: LayoutNodeWrapper
): T {
    if (originalWrapper !== wrapped) {
        val wrapper = wrapped as DelegatingLayoutNodeWrapper<*>
        wrapper.isChained = true
    }
    return this
}
