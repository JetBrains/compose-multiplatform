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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.node

import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.minus
import androidx.compose.ui.unit.plus
import androidx.compose.ui.unit.toOffset

/**
 * Measurable and Placeable type that has a position.
 */
@OptIn(ExperimentalLayoutNodeApi::class)
internal abstract class LayoutNodeWrapper(
    internal val layoutNode: LayoutNode
) : Placeable(), Measurable, LayoutCoordinates {
    internal open val wrapped: LayoutNodeWrapper? = null
    internal var wrappedBy: LayoutNodeWrapper? = null

    /**
     * The scope used to measure the wrapped. InnerPlaceables are using the MeasureScope
     * of the LayoutNode. For fewer allocations, everything else is reusing the measure scope of
     * their wrapped.
     */
    abstract val measureScope: MeasureScope

    // Size exposed to LayoutCoordinates.
    final override val size: IntSize get() = measuredSize

    open val invalidateLayerOnBoundsChange = true

    private var _measureResult: MeasureScope.MeasureResult? = null
    open var measureResult: MeasureScope.MeasureResult
        get() = _measureResult ?: error(UnmeasuredError)
        internal set(value) {
            if (invalidateLayerOnBoundsChange &&
                (value.width != _measureResult?.width || value.height != _measureResult?.height)
            ) {
                invalidateLayer()
            }
            _measureResult = value
            measuredSize = IntSize(measureResult.width, measureResult.height)
        }

    var position: IntOffset = IntOffset.Zero
        internal set(value) {
            if (invalidateLayerOnBoundsChange && value != field) {
                invalidateLayer()
            }
            field = value
        }

    override val parentCoordinates: LayoutCoordinates?
        get() {
            check(isAttached) { ExpectAttachedLayoutCoordinates }
            return layoutNode.outerLayoutNodeWrapper.wrappedBy
        }

    // True when the wrapper is running its own placing block to obtain the position of the
    // wrapped, but is not interested in the position of the wrapped of the wrapped.
    var isShallowPlacing = false

    private var _rectCache: MutableRect? = null
    private val rectCache: MutableRect get() = _rectCache ?: MutableRect(0f, 0f, 0f, 0f).also {
        _rectCache = it
    }

    /**
     * Whether a pointer that is relative to the device screen is in the bounds of this
     * LayoutNodeWrapper.
     */
    fun isGlobalPointerInBounds(globalPointerPosition: Offset): Boolean {
        // TODO(shepshapard): Right now globalToLocal has to traverse the tree all the way back up
        //  so calling this is expensive.  Would be nice to cache data such that this is cheap.
        val localPointerPosition = globalToLocal(globalPointerPosition)
        return localPointerPosition.x >= 0 &&
            localPointerPosition.x < measuredSize.width &&
            localPointerPosition.y >= 0 &&
            localPointerPosition.y < measuredSize.height
    }

    /**
     * Measures the modified child.
     */
    abstract fun performMeasure(constraints: Constraints): Placeable

    /**
     * Measures the modified child.
     */
    final override fun measure(constraints: Constraints): Placeable {
        measurementConstraints = constraints
        return performMeasure(constraints)
    }

    /**
     * Places the modified child.
     */
    abstract override fun placeAt(position: IntOffset)

    /**
     * Draws the content of the LayoutNode
     */
    abstract fun draw(canvas: Canvas)

    /**
     * Executes a hit test on any appropriate type associated with this [LayoutNodeWrapper].
     *
     * Override appropriately to either add a [PointerInputFilter] to [hitPointerInputFilters] or
     * to pass the execution on.
     *
     * @param pointerPositionRelativeToScreen The tested pointer position, which is relative to
     * the device screen.
     * @param hitPointerInputFilters The collection that the hit [PointerInputFilter]s will be
     * added to if hit.
     */
    abstract fun hitTest(
        pointerPositionRelativeToScreen: Offset,
        hitPointerInputFilters: MutableList<PointerInputFilter>
    )

    override fun childToLocal(child: LayoutCoordinates, childLocal: Offset): Offset {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        check(child.isAttached) { "Child $child is not attached!" }
        var wrapper = child as LayoutNodeWrapper
        var position = childLocal
        while (wrapper !== this) {
            position = wrapper.toParentPosition(position)

            val parent = wrapper.wrappedBy
            check(parent != null) {
                "childToLocal: child parameter is not a child of the LayoutCoordinates"
            }
            wrapper = parent
        }
        return position
    }

    override fun globalToLocal(global: Offset): Offset {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        val wrapper = wrappedBy ?: return fromParentPosition(
            global - layoutNode.requireOwner().calculatePosition().toOffset()
        )
        return fromParentPosition(wrapper.globalToLocal(global))
    }

    override fun localToGlobal(local: Offset): Offset {
        return localToRoot(local) + layoutNode.requireOwner().calculatePosition()
    }

    override fun localToRoot(local: Offset): Offset {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        var wrapper: LayoutNodeWrapper? = this
        var position = local
        while (wrapper != null) {
            position = wrapper.toParentPosition(position)
            wrapper = wrapper.wrappedBy
        }
        return position
    }

    protected inline fun withPositionTranslation(canvas: Canvas, block: (Canvas) -> Unit) {
        val x = position.x.toFloat()
        val y = position.y.toFloat()
        canvas.translate(x, y)
        block(canvas)
        canvas.translate(-x, -y)
    }

    /**
     * Converts [position] in the local coordinate system to a [Offset] in the
     * [parentCoordinates] coordinate system.
     */
    open fun toParentPosition(position: Offset): Offset = position + this.position

    /**
     * Converts [position] in the [parentCoordinates] coordinate system to a [Offset] in the
     * local coordinate system.
     */
    open fun fromParentPosition(position: Offset): Offset = position - this.position

    protected fun drawBorder(canvas: Canvas, paint: Paint) {
        val rect = Rect(
            left = 0.5f,
            top = 0.5f,
            right = measuredSize.width.toFloat() - 0.5f,
            bottom = measuredSize.height.toFloat() - 0.5f
        )
        canvas.drawRect(rect, paint)
    }

    /**
     * Attaches the [LayoutNodeWrapper] and its wrapped [LayoutNodeWrapper] to an active
     * LayoutNode.
     *
     * This will be called when the [LayoutNode] associated with this [LayoutNodeWrapper] is
     * attached to the [Owner].
     *
     * It is also called whenever the modifier chain is replaced and the [LayoutNodeWrapper]s are
     * recreated.
     */
    abstract fun attach()

    /**
     * Detaches the [LayoutNodeWrapper] and its wrapped [LayoutNodeWrapper] from an active
     * LayoutNode.
     *
     * This will be called when the [LayoutNode] associated with this [LayoutNodeWrapper] is
     * detached from the [Owner].
     *
     * It is also called whenever the modifier chain is replaced and the [LayoutNodeWrapper]s are
     * recreated.
     */
    abstract fun detach()

    /**
     * Modifies bounds to be in the parent LayoutNodeWrapper's coordinates, including clipping,
     * scaling, etc.
     */
    protected open fun rectInParent(bounds: MutableRect) {
        val x = position.x
        bounds.left += x
        bounds.right += x

        val y = position.y
        bounds.top += y
        bounds.bottom += y
    }

    override fun childBoundingBox(child: LayoutCoordinates): Rect {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        check(child.isAttached) { "Child $child is not attached!" }
        val bounds = rectCache
        bounds.left = 0f
        bounds.top = 0f
        bounds.right = child.size.width.toFloat()
        bounds.bottom = child.size.height.toFloat()
        var wrapper = child as LayoutNodeWrapper
        while (wrapper !== this) {
            wrapper.rectInParent(bounds)
            if (bounds.isEmpty) {
                return Rect.Zero
            }

            val parent = wrapper.wrappedBy
            check(parent != null) {
                "childToLocal: child parameter is not a child of the LayoutCoordinates"
            }
            wrapper = parent
        }
        return bounds.toRect()
    }

    /**
     * Invalidates the layer that this wrapper will draw into.
     */
    open fun invalidateLayer() {
        if (layoutNode.innerLayerWrapper != null) {
            wrappedBy?.invalidateLayer()
        } else {
            layoutNode.invalidateLayer()
        }
    }

    /**
     * Returns the first [focus node][ModifiedFocusNode] in the wrapper list that wraps this
     * [LayoutNodeWrapper].
     */
    abstract fun findPreviousFocusWrapper(): ModifiedFocusNode?

    /**
     * Returns the next [focus node][ModifiedFocusNode] in the wrapper list that is wrapped by
     * this [LayoutNodeWrapper].
     */
    abstract fun findNextFocusWrapper(): ModifiedFocusNode?

    /**
     * Returns the last [focus node][ModifiedFocusNode] found following this [LayoutNodeWrapper].
     * It searches the wrapper list associated with this [LayoutNodeWrapper].
     */
    abstract fun findLastFocusWrapper(): ModifiedFocusNode?

    /**
     * When the focus state changes, a [LayoutNodeWrapper] calls this function on the wrapper
     * that wraps it. The focus state change must be propagated to the parents until we reach
     * another [focus node][ModifiedFocusNode].
     */
    @OptIn(ExperimentalFocus::class)
    abstract fun propagateFocusStateChange(focusState: FocusState)

    /**
     * Find the first ancestor that is a [ModifiedFocusNode].
     */
    internal fun findParentFocusNode(): ModifiedFocusNode? {
        // TODO(b/152066829): We shouldn't need to search through the parentLayoutNode, as the
        // wrappedBy property should automatically point to the last layoutWrapper of the parent.
        // Find out why this doesn't work.
        var focusParent = wrappedBy?.findPreviousFocusWrapper()
        if (focusParent != null) {
            return focusParent
        }

        var parentLayoutNode = layoutNode.parent
        while (parentLayoutNode != null) {
            focusParent = parentLayoutNode.outerLayoutNodeWrapper.findLastFocusWrapper()
            if (focusParent != null) {
                return focusParent
            }
            parentLayoutNode = parentLayoutNode.parent
        }
        return null
    }

    /**
     *  Find the first ancestor that is a [ModifiedKeyInputNode].
     */
    internal fun findParentKeyInputNode(): ModifiedKeyInputNode? {
        // TODO(b/152066829): We shouldn't need to search through the parentLayoutNode, as the
        // wrappedBy property should automatically point to the last layoutWrapper of the parent.
        // Find out why this doesn't work.
        var keyInputParent = wrappedBy?.findPreviousKeyInputWrapper()
        if (keyInputParent != null) {
            return keyInputParent
        }

        var parentLayoutNode = layoutNode.parent
        while (parentLayoutNode != null) {
            keyInputParent = parentLayoutNode.outerLayoutNodeWrapper.findLastKeyInputWrapper()
            if (keyInputParent != null) {
                return keyInputParent
            }
            parentLayoutNode = parentLayoutNode.parent
        }
        return null
    }

    /**
     * Returns the first [ModifiedKeyInputNode] in the wrapper list that wraps this
     * [LayoutNodeWrapper].
     */
    abstract fun findPreviousKeyInputWrapper(): ModifiedKeyInputNode?

    /**
     * Returns the next [ModifiedKeyInputNode] in the wrapper list that is wrapped by this
     * [LayoutNodeWrapper].
     */
    abstract fun findNextKeyInputWrapper(): ModifiedKeyInputNode?

    /**
     * Returns the last [focus node][ModifiedFocusNode] found following this [LayoutNodeWrapper].
     * It searches the wrapper list associated with this [LayoutNodeWrapper]
     */
    abstract fun findLastKeyInputWrapper(): ModifiedKeyInputNode?

    /**
     * Called when [LayoutNode.modifier] has changed and all the LayoutNodeWrappers have been
     * configured.
     */
    open fun onModifierChanged() {}

    internal companion object {
        const val ExpectAttachedLayoutCoordinates = "LayoutCoordinate operations are only valid " +
            "when isAttached is true"
        const val UnmeasuredError = "Asking for measurement result of unmeasured layout modifier"
    }
}
