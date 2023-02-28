/*
 * Copyright 2022 The Android Open Source Project
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
@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.node

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.DefaultCameraDistance
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadLayoutCoordinatesImpl
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.outerSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.minus
import androidx.compose.ui.unit.plus
import androidx.compose.ui.unit.toSize

/**
 * Measurable and Placeable type that has a position.
 */
internal abstract class NodeCoordinator(
    override val layoutNode: LayoutNode,
) :
    LookaheadCapablePlaceable(),
    Measurable,
    LayoutCoordinates,
    OwnerScope,
        (Canvas) -> Unit {

    abstract val tail: Modifier.Node

    internal var wrapped: NodeCoordinator? = null
    internal var wrappedBy: NodeCoordinator? = null

    override val layoutDirection: LayoutDirection
        get() = layoutNode.layoutDirection

    override val density: Float
        get() = layoutNode.density.density

    override val fontScale: Float
        get() = layoutNode.density.fontScale

    override val parent: LookaheadCapablePlaceable?
        get() = wrappedBy

    override val coordinates: LayoutCoordinates
        get() = this

    private var released = false

    private fun headNode(includeTail: Boolean): Modifier.Node? {
        return if (layoutNode.outerCoordinator === this) {
            layoutNode.nodes.head
        } else if (includeTail) {
            wrappedBy?.tail?.child
        } else {
            wrappedBy?.tail
        }
    }

    inline fun visitNodes(mask: Int, includeTail: Boolean, block: (Modifier.Node) -> Unit) {
        val stopNode = if (includeTail) tail else (tail.parent ?: return)
        var node: Modifier.Node? = headNode(includeTail)
        while (node != null) {
            if (node.aggregateChildKindSet and mask == 0) return
            if (node.kindSet and mask != 0) block(node)
            if (node === stopNode) break
            node = node.child
        }
    }

    inline fun <reified T> visitNodes(type: NodeKind<T>, block: (T) -> Unit) {
        visitNodes(type.mask, type.includeSelfInTraversal) {
            if (it is T) block(it)
        }
    }

    fun hasNode(type: NodeKind<*>): Boolean {
        return headNode(type.includeSelfInTraversal)?.has(type) == true
    }

    inline fun <reified T> head(type: NodeKind<T>): T? {
        visitNodes(type.mask, type.includeSelfInTraversal) { return it as? T }
        return null
    }

    fun <T> headUnchecked(type: NodeKind<T>): T? {
        visitNodes(type.mask, type.includeSelfInTraversal) {
            @Suppress("UNCHECKED_CAST")
            return it as T
        }
        return null
    }

    // Size exposed to LayoutCoordinates.
    final override val size: IntSize get() = measuredSize

    private var isClipping: Boolean = false

    protected var layerBlock: (GraphicsLayerScope.() -> Unit)? = null
        private set
    private var layerDensity: Density = layoutNode.density
    private var layerLayoutDirection: LayoutDirection = layoutNode.layoutDirection

    private var lastLayerAlpha: Float = 0.8f
    fun isTransparent(): Boolean {
        if (layer != null && lastLayerAlpha <= 0f) return true
        return this.wrappedBy?.isTransparent() ?: return false
    }

    override val alignmentLinesOwner: AlignmentLinesOwner
        get() = layoutNode.layoutDelegate.alignmentLinesOwner

    override val child: LookaheadCapablePlaceable?
        get() = wrapped

    override fun replace() {
        placeAt(position, zIndex, layerBlock)
    }

    override val hasMeasureResult: Boolean
        get() = _measureResult != null

    override val isAttached: Boolean
        get() = !released && layoutNode.isAttached

    private var _measureResult: MeasureResult? = null
    override var measureResult: MeasureResult
        get() = _measureResult ?: error(UnmeasuredError)
        internal set(value) {
            val old = _measureResult
            if (value !== old) {
                _measureResult = value
                if (old == null || value.width != old.width || value.height != old.height) {
                    onMeasureResultChanged(value.width, value.height)
                }
                // We do not simply compare against old.alignmentLines in case this is a
                // MutableStateMap and the same instance might be passed.
                if ((!oldAlignmentLines.isNullOrEmpty() || value.alignmentLines.isNotEmpty()) &&
                    value.alignmentLines != oldAlignmentLines
                ) {
                    alignmentLinesOwner.alignmentLines.onAlignmentsChanged()

                    val oldLines = oldAlignmentLines
                        ?: (mutableMapOf<AlignmentLine, Int>().also { oldAlignmentLines = it })
                    oldLines.clear()
                    oldLines.putAll(value.alignmentLines)
                }
            }
        }

    internal var lookaheadDelegate: LookaheadDelegate? = null
        private set

    private var oldAlignmentLines: MutableMap<AlignmentLine, Int>? = null

    /**
     * Creates a new lookaheadDelegate instance when the scope changes. If the provided scope is
     * null, it means the lookahead root does not exit (or no longer exists), set
     * the [lookaheadDelegate] to null.
     */
    internal fun updateLookaheadScope(scope: LookaheadScope?) {
        lookaheadDelegate = scope?.let {
            if (it != lookaheadDelegate?.lookaheadScope) {
                createLookaheadDelegate(it)
            } else {
                lookaheadDelegate
            }
        }
    }

    protected fun updateLookaheadDelegate(lookaheadDelegate: LookaheadDelegate) {
        this.lookaheadDelegate = lookaheadDelegate
    }

    abstract fun createLookaheadDelegate(scope: LookaheadScope): LookaheadDelegate

    override val providedAlignmentLines: Set<AlignmentLine>
        get() {
            var set: MutableSet<AlignmentLine>? = null
            var coordinator: NodeCoordinator? = this
            while (coordinator != null) {
                val alignmentLines = coordinator._measureResult?.alignmentLines
                if (alignmentLines?.isNotEmpty() == true) {
                    if (set == null) {
                        set = mutableSetOf()
                    }
                    set.addAll(alignmentLines.keys)
                }
                coordinator = coordinator.wrapped
            }
            return set ?: emptySet()
        }

    /**
     * Called when the width or height of [measureResult] change. The object instance pointed to
     * by [measureResult] may or may not have changed.
     */
    protected open fun onMeasureResultChanged(width: Int, height: Int) {
        val layer = layer
        if (layer != null) {
            layer.resize(IntSize(width, height))
        } else {
            wrappedBy?.invalidateLayer()
        }
        layoutNode.owner?.onLayoutChange(layoutNode)
        measuredSize = IntSize(width, height)
        graphicsLayerScope.size = measuredSize.toSize()
        visitNodes(Nodes.Draw) {
            it.onMeasureResultChanged()
        }
    }

    override var position: IntOffset = IntOffset.Zero
        protected set

    var zIndex: Float = 0f
        protected set

    override val parentData: Any?
        get() {
            var data: Any? = null
            val thisNode = tail
            if (layoutNode.nodes.has(Nodes.ParentData)) {
                with(layoutNode.density) {
                    layoutNode.nodes.tailToHead {
                        if (it === thisNode) return@tailToHead
                        if (it.isKind(Nodes.ParentData) && it is ParentDataModifierNode) {
                            data = with(it) { modifyParentData(data) }
                        }
                    }
                }
            }
            return data
        }

    final override val parentLayoutCoordinates: LayoutCoordinates?
        get() {
            check(isAttached) { ExpectAttachedLayoutCoordinates }
            return layoutNode.outerCoordinator.wrappedBy
        }

    final override val parentCoordinates: LayoutCoordinates?
        get() {
            check(isAttached) { ExpectAttachedLayoutCoordinates }
            return wrappedBy
        }

    private var _rectCache: MutableRect? = null
    protected val rectCache: MutableRect
        get() = _rectCache ?: MutableRect(0f, 0f, 0f, 0f).also {
            _rectCache = it
        }

    private val snapshotObserver get() = layoutNode.requireOwner().snapshotObserver

    /**
     * The current layer's positional attributes.
     */
    private var layerPositionalProperties: LayerPositionalProperties? = null

    internal val lastMeasurementConstraints: Constraints get() = measurementConstraints

    protected inline fun performingMeasure(
        constraints: Constraints,
        block: () -> Placeable
    ): Placeable {
        measurementConstraints = constraints
        val result = block()
        layer?.resize(measuredSize)
        return result
    }

    fun onMeasured() {
        if (hasNode(Nodes.LayoutAware)) {
            Snapshot.withoutReadObservation {
                visitNodes(Nodes.LayoutAware) {
                    it.onRemeasured(measuredSize)
                }
            }
        }
    }

    /**
     * Places the modified child.
     */
    /*@CallSuper*/
    override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        onLayerBlockUpdated(layerBlock)
        if (this.position != position) {
            this.position = position
            layoutNode.layoutDelegate.measurePassDelegate
                .notifyChildrenUsingCoordinatesWhilePlacing()
            val layer = layer
            if (layer != null) {
                layer.move(position)
            } else {
                wrappedBy?.invalidateLayer()
            }
            invalidateAlignmentLinesFromPositionChange()
            layoutNode.owner?.onLayoutChange(layoutNode)
        }
        this.zIndex = zIndex
    }

    /**
     * Draws the content of the LayoutNode
     */
    fun draw(canvas: Canvas) {
        val layer = layer
        if (layer != null) {
            layer.drawLayer(canvas)
        } else {
            val x = position.x.toFloat()
            val y = position.y.toFloat()
            canvas.translate(x, y)
            drawContainedDrawModifiers(canvas)
            canvas.translate(-x, -y)
        }
    }

    private fun drawContainedDrawModifiers(canvas: Canvas) {
        val head = head(Nodes.Draw)
        if (head == null) {
            performDraw(canvas)
        } else {
            val drawScope = layoutNode.mDrawScope
            drawScope.draw(canvas, size.toSize(), this, head)
        }
    }

    open fun performDraw(canvas: Canvas) {
        wrapped?.draw(canvas)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun onPlaced() {
        val lookahead = lookaheadDelegate
        if (lookahead != null) {
            visitNodes(Nodes.LayoutAware) {
                it.onLookaheadPlaced(lookahead.lookaheadLayoutCoordinates)
            }
        }
        visitNodes(Nodes.LayoutAware) {
            it.onPlaced(this)
        }
    }

    // implementation of draw block passed to the OwnedLayer
    @Suppress("LiftReturnOrAssignment")
    override fun invoke(canvas: Canvas) {
        if (layoutNode.isPlaced) {
            snapshotObserver.observeReads(this, onCommitAffectingLayer) {
                drawContainedDrawModifiers(canvas)
            }
            lastLayerDrawingWasSkipped = false
        } else {
            // The invalidation is requested even for nodes which are not placed. As we are not
            // going to display them we skip the drawing. It is safe to just draw nothing as the
            // layer will be invalidated again when the node will be finally placed.
            lastLayerDrawingWasSkipped = true
        }
    }

    fun updateLayerBlock(
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        forceLayerInvalidated: Boolean = false
    ) {
        val layerInvalidated = this.layerBlock !== layerBlock || forceLayerInvalidated
        this.layerBlock = layerBlock
        onLayerBlockUpdated(layerBlock, forceLayerInvalidated = layerInvalidated)
    }

    private fun onLayerBlockUpdated(
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        forceLayerInvalidated: Boolean = false
    ) {
        val layerInvalidated = this.layerBlock !== layerBlock || layerDensity != layoutNode
            .density || layerLayoutDirection != layoutNode.layoutDirection ||
            forceLayerInvalidated
        this.layerBlock = layerBlock
        this.layerDensity = layoutNode.density
        this.layerLayoutDirection = layoutNode.layoutDirection

        if (isAttached && layerBlock != null) {
            if (layer == null) {
                layer = layoutNode.requireOwner().createLayer(
                    this,
                    invalidateParentLayer
                ).apply {
                    resize(measuredSize)
                    move(position)
                }
                updateLayerParameters()
                layoutNode.innerLayerCoordinatorIsDirty = true
                invalidateParentLayer()
            } else if (layerInvalidated) {
                updateLayerParameters()
            }
        } else {
            layer?.let {
                it.destroy()
                layoutNode.innerLayerCoordinatorIsDirty = true
                invalidateParentLayer()
                if (isAttached) {
                    layoutNode.owner?.onLayoutChange(layoutNode)
                }
            }
            layer = null
            lastLayerDrawingWasSkipped = false
        }
    }

    private fun updateLayerParameters() {
        val layer = layer
        if (layer != null) {
            val layerBlock = requireNotNull(layerBlock)
            graphicsLayerScope.reset()
            graphicsLayerScope.graphicsDensity = layoutNode.density
            graphicsLayerScope.size = size.toSize()
            snapshotObserver.observeReads(this, onCommitAffectingLayerParams) {
                layerBlock.invoke(graphicsLayerScope)
            }
            val layerPositionalProperties = layerPositionalProperties
                ?: LayerPositionalProperties().also { layerPositionalProperties = it }
            layerPositionalProperties.copyFrom(graphicsLayerScope)
            layer.updateLayerProperties(
                scaleX = graphicsLayerScope.scaleX,
                scaleY = graphicsLayerScope.scaleY,
                alpha = graphicsLayerScope.alpha,
                translationX = graphicsLayerScope.translationX,
                translationY = graphicsLayerScope.translationY,
                shadowElevation = graphicsLayerScope.shadowElevation,
                ambientShadowColor = graphicsLayerScope.ambientShadowColor,
                spotShadowColor = graphicsLayerScope.spotShadowColor,
                rotationX = graphicsLayerScope.rotationX,
                rotationY = graphicsLayerScope.rotationY,
                rotationZ = graphicsLayerScope.rotationZ,
                cameraDistance = graphicsLayerScope.cameraDistance,
                transformOrigin = graphicsLayerScope.transformOrigin,
                shape = graphicsLayerScope.shape,
                clip = graphicsLayerScope.clip,
                renderEffect = graphicsLayerScope.renderEffect,
                compositingStrategy = graphicsLayerScope.compositingStrategy,
                layoutDirection = layoutNode.layoutDirection,
                density = layoutNode.density
            )
            isClipping = graphicsLayerScope.clip
        } else {
            require(layerBlock == null)
        }
        lastLayerAlpha = graphicsLayerScope.alpha
        layoutNode.owner?.onLayoutChange(layoutNode)
    }

    private val invalidateParentLayer: () -> Unit = {
        wrappedBy?.invalidateLayer()
    }

    /**
     * True when the last drawing of this layer didn't draw the real content as the LayoutNode
     * containing this layer was not placed by the parent.
     */
    internal var lastLayerDrawingWasSkipped = false
        private set

    var layer: OwnedLayer? = null
        private set

    override val isValidOwnerScope: Boolean
        get() = layer != null && isAttached

    val minimumTouchTargetSize: Size
        get() = with(layerDensity) { layoutNode.viewConfiguration.minimumTouchTargetSize.toSize() }

    /**
     * Executes a hit test for this [NodeCoordinator].
     *
     * @param hitTestSource The hit test specifics for pointer input or semantics
     * @param pointerPosition The tested pointer position, which is relative to
     * the [NodeCoordinator].
     * @param hitTestResult The parent [HitTestResult] that any hit should be added to.
     * @param isTouchEvent `true` if this is from a touch source. Touch sources allow for
     * minimum touch target. Semantics hit tests always treat hits as needing minimum touch target.
     * @param isInLayer `true` if the touch event is in the layer of this and all parents or `false`
     * if it is outside the layer, but within the minimum touch target of the edge of the layer.
     * This can only be `false` when [isTouchEvent] is `true` or else a layer miss means the event
     * will be clipped out.
     */
    fun <T : DelegatableNode> hitTest(
        hitTestSource: HitTestSource<T>,
        pointerPosition: Offset,
        hitTestResult: HitTestResult<T>,
        isTouchEvent: Boolean,
        isInLayer: Boolean
    ) {
        val head = headUnchecked(hitTestSource.entityType())
        if (!withinLayerBounds(pointerPosition)) {
            // This missed the clip, but if this layout is too small and this is within the
            // minimum touch target, we still consider it a hit.
            if (isTouchEvent) {
                val distanceFromEdge =
                    distanceInMinimumTouchTarget(pointerPosition, minimumTouchTargetSize)
                if (distanceFromEdge.isFinite() &&
                    hitTestResult.isHitInMinimumTouchTargetBetter(distanceFromEdge, false)
                ) {
                    head.hitNear(
                        hitTestSource,
                        pointerPosition,
                        hitTestResult,
                        isTouchEvent,
                        false,
                        distanceFromEdge
                    )
                } // else it is a complete miss.
            }
        } else if (head == null) {
            hitTestChild(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
        } else if (isPointerInBounds(pointerPosition)) {
            // A real hit
            head.hit(
                hitTestSource,
                pointerPosition,
                hitTestResult,
                isTouchEvent,
                isInLayer
            )
        } else {
            val distanceFromEdge = if (!isTouchEvent) Float.POSITIVE_INFINITY else {
                distanceInMinimumTouchTarget(pointerPosition, minimumTouchTargetSize)
            }

            if (distanceFromEdge.isFinite() &&
                hitTestResult.isHitInMinimumTouchTargetBetter(distanceFromEdge, isInLayer)
            ) {
                // Hit closer than existing handlers, so just record it
                head.hitNear(
                    hitTestSource,
                    pointerPosition,
                    hitTestResult,
                    isTouchEvent,
                    isInLayer,
                    distanceFromEdge
                )
            } else {
                head.speculativeHit(
                    hitTestSource,
                    pointerPosition,
                    hitTestResult,
                    isTouchEvent,
                    isInLayer,
                    distanceFromEdge
                )
            }
        }
    }

    /**
     * The [NodeCoordinator] had a hit in bounds and can record any children in the
     * [hitTestResult].
     */
    private fun <T : DelegatableNode> T?.hit(
        hitTestSource: HitTestSource<T>,
        pointerPosition: Offset,
        hitTestResult: HitTestResult<T>,
        isTouchEvent: Boolean,
        isInLayer: Boolean
    ) {
        if (this == null) {
            hitTestChild(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
        } else {
            hitTestResult.hit(this, isInLayer) {
                nextUncheckedUntil(hitTestSource.entityType(), Nodes.Layout)
                    .hit(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
            }
        }
    }

    /**
     * The [NodeCoordinator] had a hit [distanceFromEdge] from the bounds and it is within
     * the minimum touch target distance, so it should be recorded as such in the [hitTestResult].
     */
    private fun <T : DelegatableNode> T?.hitNear(
        hitTestSource: HitTestSource<T>,
        pointerPosition: Offset,
        hitTestResult: HitTestResult<T>,
        isTouchEvent: Boolean,
        isInLayer: Boolean,
        distanceFromEdge: Float
    ) {
        if (this == null) {
            hitTestChild(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
        } else {
            // Hit closer than existing handlers, so just record it
            hitTestResult.hitInMinimumTouchTarget(
                this,
                distanceFromEdge,
                isInLayer
            ) {
                nextUncheckedUntil(hitTestSource.entityType(), Nodes.Layout).hitNear(
                    hitTestSource,
                    pointerPosition,
                    hitTestResult,
                    isTouchEvent,
                    isInLayer,
                    distanceFromEdge
                )
            }
        }
    }

    /**
     * The [NodeCoordinator] had a miss, but it hasn't been clipped out. The child must be
     * checked to see if it hit.
     */
    private fun <T : DelegatableNode> T?.speculativeHit(
        hitTestSource: HitTestSource<T>,
        pointerPosition: Offset,
        hitTestResult: HitTestResult<T>,
        isTouchEvent: Boolean,
        isInLayer: Boolean,
        distanceFromEdge: Float
    ) {
        if (this == null) {
            hitTestChild(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
        } else if (hitTestSource.interceptOutOfBoundsChildEvents(this)) {
            // We only want to replace the existing touch target if there are better
            // hits in the children
            hitTestResult.speculativeHit(
                this,
                distanceFromEdge,
                isInLayer
            ) {
                nextUncheckedUntil(hitTestSource.entityType(), Nodes.Layout).speculativeHit(
                    hitTestSource,
                    pointerPosition,
                    hitTestResult,
                    isTouchEvent,
                    isInLayer,
                    distanceFromEdge
                )
            }
        } else {
            nextUncheckedUntil(hitTestSource.entityType(), Nodes.Layout).speculativeHit(
                hitTestSource,
                pointerPosition,
                hitTestResult,
                isTouchEvent,
                isInLayer,
                distanceFromEdge
            )
        }
    }

    /**
     * Do a [hitTest] on the children of this [NodeCoordinator].
     */
    open fun <T : DelegatableNode> hitTestChild(
        hitTestSource: HitTestSource<T>,
        pointerPosition: Offset,
        hitTestResult: HitTestResult<T>,
        isTouchEvent: Boolean,
        isInLayer: Boolean
    ) {
        // Also, keep looking to see if we also might hit any children.
        // This avoids checking layer bounds twice as when we call super.hitTest()
        val wrapped = wrapped
        if (wrapped != null) {
            val positionInWrapped = wrapped.fromParentPosition(pointerPosition)
            wrapped.hitTest(
                hitTestSource,
                positionInWrapped,
                hitTestResult,
                isTouchEvent,
                isInLayer
            )
        }
    }

    /**
     * Returns the bounds of this [NodeCoordinator], including the minimum touch target.
     */
    fun touchBoundsInRoot(): Rect {
        if (!isAttached) {
            return Rect.Zero
        }

        val root = findRootCoordinates()

        val bounds = rectCache
        val padding = calculateMinimumTouchTargetPadding(minimumTouchTargetSize)
        bounds.left = -padding.width
        bounds.top = -padding.height
        bounds.right = measuredWidth + padding.width
        bounds.bottom = measuredHeight + padding.height

        var coordinator: NodeCoordinator = this
        while (coordinator !== root) {
            coordinator.rectInParent(
                bounds,
                clipBounds = false,
                clipToMinimumTouchTargetSize = true
            )
            if (bounds.isEmpty) {
                return Rect.Zero
            }

            coordinator = coordinator.wrappedBy!!
        }
        return bounds.toRect()
    }

    override fun windowToLocal(relativeToWindow: Offset): Offset {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        val root = findRootCoordinates()
        val positionInRoot = layoutNode.requireOwner()
            .calculateLocalPosition(relativeToWindow) - root.positionInRoot()
        return localPositionOf(root, positionInRoot)
    }

    override fun localToWindow(relativeToLocal: Offset): Offset {
        val positionInRoot = localToRoot(relativeToLocal)
        val owner = layoutNode.requireOwner()
        return owner.calculatePositionInWindow(positionInRoot)
    }

    private fun LayoutCoordinates.toCoordinator() =
        (this as? LookaheadLayoutCoordinatesImpl)?.coordinator ?: this as NodeCoordinator

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset
    ): Offset {
        val nodeCoordinator = sourceCoordinates.toCoordinator()
        val commonAncestor = findCommonAncestor(nodeCoordinator)

        var position = relativeToSource
        var coordinator = nodeCoordinator
        while (coordinator !== commonAncestor) {
            position = coordinator.toParentPosition(position)
            coordinator = coordinator.wrappedBy!!
        }

        return ancestorToLocal(commonAncestor, position)
    }

    override fun transformFrom(sourceCoordinates: LayoutCoordinates, matrix: Matrix) {
        val coordinator = sourceCoordinates.toCoordinator()
        val commonAncestor = findCommonAncestor(coordinator)

        matrix.reset()
        // Transform from the source to the common ancestor
        coordinator.transformToAncestor(commonAncestor, matrix)
        // Transform from the common ancestor to this
        transformFromAncestor(commonAncestor, matrix)
    }

    private fun transformToAncestor(ancestor: NodeCoordinator, matrix: Matrix) {
        var wrapper = this
        while (wrapper != ancestor) {
            wrapper.layer?.transform(matrix)
            val position = wrapper.position
            if (position != IntOffset.Zero) {
                tmpMatrix.reset()
                tmpMatrix.translate(position.x.toFloat(), position.y.toFloat())
                matrix.timesAssign(tmpMatrix)
            }
            wrapper = wrapper.wrappedBy!!
        }
    }

    private fun transformFromAncestor(ancestor: NodeCoordinator, matrix: Matrix) {
        if (ancestor != this) {
            wrappedBy!!.transformFromAncestor(ancestor, matrix)
            if (position != IntOffset.Zero) {
                tmpMatrix.reset()
                tmpMatrix.translate(-position.x.toFloat(), -position.y.toFloat())
                matrix.timesAssign(tmpMatrix)
            }
            layer?.inverseTransform(matrix)
        }
    }

    override fun localBoundingBoxOf(
        sourceCoordinates: LayoutCoordinates,
        clipBounds: Boolean
    ): Rect {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        check(sourceCoordinates.isAttached) {
            "LayoutCoordinates $sourceCoordinates is not attached!"
        }
        val srcCoordinator = sourceCoordinates.toCoordinator()
        val commonAncestor = findCommonAncestor(srcCoordinator)

        val bounds = rectCache
        bounds.left = 0f
        bounds.top = 0f
        bounds.right = sourceCoordinates.size.width.toFloat()
        bounds.bottom = sourceCoordinates.size.height.toFloat()

        var coordinator = srcCoordinator
        while (coordinator !== commonAncestor) {
            coordinator.rectInParent(bounds, clipBounds)
            if (bounds.isEmpty) {
                return Rect.Zero
            }

            coordinator = coordinator.wrappedBy!!
        }

        ancestorToLocal(commonAncestor, bounds, clipBounds)
        return bounds.toRect()
    }

    private fun ancestorToLocal(ancestor: NodeCoordinator, offset: Offset): Offset {
        if (ancestor === this) {
            return offset
        }
        val wrappedBy = wrappedBy
        if (wrappedBy == null || ancestor == wrappedBy) {
            return fromParentPosition(offset)
        }
        return fromParentPosition(wrappedBy.ancestorToLocal(ancestor, offset))
    }

    private fun ancestorToLocal(
        ancestor: NodeCoordinator,
        rect: MutableRect,
        clipBounds: Boolean
    ) {
        if (ancestor === this) {
            return
        }
        wrappedBy?.ancestorToLocal(ancestor, rect, clipBounds)
        return fromParentRect(rect, clipBounds)
    }

    override fun localToRoot(relativeToLocal: Offset): Offset {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        var coordinator: NodeCoordinator? = this
        var position = relativeToLocal
        while (coordinator != null) {
            position = coordinator.toParentPosition(position)
            coordinator = coordinator.wrappedBy
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
     * [parentLayoutCoordinates] coordinate system.
     */
    open fun toParentPosition(position: Offset): Offset {
        val layer = layer
        val targetPosition = layer?.mapOffset(position, inverse = false) ?: position
        return targetPosition + this.position
    }

    /**
     * Converts [position] in the [parentLayoutCoordinates] coordinate system to a [Offset] in the
     * local coordinate system.
     */
    open fun fromParentPosition(position: Offset): Offset {
        val relativeToPosition = position - this.position
        val layer = layer
        return layer?.mapOffset(relativeToPosition, inverse = true)
            ?: relativeToPosition
    }

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
     * This will be called when the [LayoutNode] associated with this [NodeCoordinator] is
     * attached to the [Owner].
     */
    fun onLayoutNodeAttach() {
        onLayerBlockUpdated(layerBlock)
    }

    /**
     * This will be called when the [LayoutNode] associated with this [NodeCoordinator] is
     * released or when the [NodeCoordinator] is released (will not be used anymore).
     */
    fun onRelease() {
        released = true
        if (layer != null) {
            onLayerBlockUpdated(null)
        }
    }

    /**
     * Modifies bounds to be in the parent NodeCoordinator's coordinates, including clipping,
     * if [clipBounds] is true. If [clipToMinimumTouchTargetSize] is true and the layer clips,
     * then the clip bounds are extended to allow minimum touch target extended area.
     */
    internal fun rectInParent(
        bounds: MutableRect,
        clipBounds: Boolean,
        clipToMinimumTouchTargetSize: Boolean = false
    ) {
        val layer = layer
        if (layer != null) {
            if (isClipping) {
                if (clipToMinimumTouchTargetSize) {
                    val minTouch = minimumTouchTargetSize
                    val horz = minTouch.width / 2f
                    val vert = minTouch.height / 2f
                    bounds.intersect(
                        -horz, -vert, size.width.toFloat() + horz, size.height.toFloat() + vert
                    )
                } else if (clipBounds) {
                    bounds.intersect(0f, 0f, size.width.toFloat(), size.height.toFloat())
                }
                if (bounds.isEmpty) {
                    return
                }
            }
            layer.mapBounds(bounds, inverse = false)
        }

        val x = position.x
        bounds.left += x
        bounds.right += x

        val y = position.y
        bounds.top += y
        bounds.bottom += y
    }

    /**
     * Modifies bounds in the parent's coordinates to be in this NodeCoordinator's
     * coordinates, including clipping, if [clipBounds] is true.
     */
    private fun fromParentRect(bounds: MutableRect, clipBounds: Boolean) {
        val x = position.x
        bounds.left -= x
        bounds.right -= x

        val y = position.y
        bounds.top -= y
        bounds.bottom -= y

        val layer = layer
        if (layer != null) {
            layer.mapBounds(bounds, inverse = true)
            if (isClipping && clipBounds) {
                bounds.intersect(0f, 0f, size.width.toFloat(), size.height.toFloat())
                if (bounds.isEmpty) {
                    return
                }
            }
        }
    }

    protected fun withinLayerBounds(pointerPosition: Offset): Boolean {
        if (!pointerPosition.isFinite) {
            return false
        }
        val layer = layer
        return layer == null || !isClipping || layer.isInLayer(pointerPosition)
    }

    /**
     * Whether a pointer that is relative to the [NodeCoordinator] is in the bounds of this
     * NodeCoordinator.
     */
    protected fun isPointerInBounds(pointerPosition: Offset): Boolean {
        val x = pointerPosition.x
        val y = pointerPosition.y
        return x >= 0f && y >= 0f && x < measuredWidth && y < measuredHeight
    }

    /**
     * Invalidates the layer that this coordinator will draw into.
     */
    open fun invalidateLayer() {
        val layer = layer
        if (layer != null) {
            layer.invalidate()
        } else {
            wrappedBy?.invalidateLayer()
        }
    }

    /**
     * Send a request to bring a portion of this item into view. The portion that has to be
     * brought into view is specified as a rectangle where the coordinates are in the local
     * coordinates of that nodeCoordinator. This request is sent up the hierarchy to all parents
     * that have a [RelocationModifier][androidx.compose.ui.layout.RelocationModifier].
     */
    open suspend fun propagateRelocationRequest(rect: Rect) {
        val parent = wrappedBy ?: return

        // Translate this nodeCoordinator to the coordinate system of the parent.
        val boundingBoxInParentCoordinates = parent.localBoundingBoxOf(this, false)

        // Translate the rect to parent coordinates
        val rectInParentBounds = rect.translate(boundingBoxInParentCoordinates.topLeft)

        parent.propagateRelocationRequest(rectInParentBounds)
    }

    /**
     * Called when [LayoutNode.modifier] has changed and all the NodeCoordinators have been
     * configured.
     */
    open fun onLayoutModifierNodeChanged() {
        layer?.invalidate()
    }

    internal fun findCommonAncestor(other: NodeCoordinator): NodeCoordinator {
        var ancestor1 = other.layoutNode
        var ancestor2 = layoutNode
        if (ancestor1 === ancestor2) {
            val otherNode = other.tail
            // They are on the same node, but we don't know which is the deeper of the two
            tail.visitLocalParents(Nodes.Layout.mask) {
                if (it === otherNode) return other
            }
            return this
        }

        while (ancestor1.depth > ancestor2.depth) {
            ancestor1 = ancestor1.parent!!
        }

        while (ancestor2.depth > ancestor1.depth) {
            ancestor2 = ancestor2.parent!!
        }

        while (ancestor1 !== ancestor2) {
            val parent1 = ancestor1.parent
            val parent2 = ancestor2.parent
            if (parent1 == null || parent2 == null) {
                throw IllegalArgumentException("layouts are not part of the same hierarchy")
            }
            ancestor1 = parent1
            ancestor2 = parent2
        }

        return when {
            ancestor2 === layoutNode -> this
            ancestor1 === other.layoutNode -> other
            else -> ancestor1.innerCoordinator
        }
    }

    fun shouldSharePointerInputWithSiblings(): Boolean {
        val start = headNode(Nodes.PointerInput.includeSelfInTraversal) ?: return false
        start.visitLocalChildren(Nodes.PointerInput) {
            if (it.sharePointerInputWithSiblings()) return true
        }
        return false
    }

    private fun offsetFromEdge(pointerPosition: Offset): Offset {
        val x = pointerPosition.x
        val horizontal = maxOf(0f, if (x < 0) -x else x - measuredWidth)
        val y = pointerPosition.y
        val vertical = maxOf(0f, if (y < 0) -y else y - measuredHeight)

        return Offset(horizontal, vertical)
    }

    /**
     * Returns the additional amount on the horizontal and vertical dimensions that
     * this extends beyond [width] and [height] on all sides. This takes into account
     * [minimumTouchTargetSize] and [measuredSize] vs. [width] and [height].
     */
    protected fun calculateMinimumTouchTargetPadding(minimumTouchTargetSize: Size): Size {
        val widthDiff = minimumTouchTargetSize.width - measuredWidth.toFloat()
        val heightDiff = minimumTouchTargetSize.height - measuredHeight.toFloat()
        return Size(maxOf(0f, widthDiff / 2f), maxOf(0f, heightDiff / 2f))
    }

    /**
     * The distance within the [minimumTouchTargetSize] of [pointerPosition] to the layout
     * size. If [pointerPosition] isn't within [minimumTouchTargetSize], then
     * [Float.POSITIVE_INFINITY] is returned.
     */
    protected fun distanceInMinimumTouchTarget(
        pointerPosition: Offset,
        minimumTouchTargetSize: Size
    ): Float {
        if (measuredWidth >= minimumTouchTargetSize.width &&
            measuredHeight >= minimumTouchTargetSize.height
        ) {
            // this layout is big enough that it doesn't qualify for minimum touch targets
            return Float.POSITIVE_INFINITY
        }

        val (width, height) = calculateMinimumTouchTargetPadding(minimumTouchTargetSize)
        val offsetFromEdge = offsetFromEdge(pointerPosition)

        return if ((width > 0f || height > 0f) &&
            offsetFromEdge.x <= width && offsetFromEdge.y <= height
        ) {
            offsetFromEdge.getDistanceSquared()
        } else {
            Float.POSITIVE_INFINITY // miss
        }
    }

    /**
     * [LayoutNode.hitTest] and [LayoutNode.hitTestSemantics] are very similar, but the data
     * used in their implementations are different. This extracts the differences between the
     * two methods into a single interface.
     */
    internal interface HitTestSource<N : DelegatableNode> {
        /**
         * Returns the [NodeKind] for the hit test target.
         */
        fun entityType(): NodeKind<N>

        /**
         * Pointer input hit tests can intercept child hits when enabled. This returns `true`
         * if the modifier has requested intercepting.
         */
        fun interceptOutOfBoundsChildEvents(node: N): Boolean

        /**
         * Returns false if the parent layout node has a state that suppresses
         * hit testing of its children.
         */
        fun shouldHitTestChildren(parentLayoutNode: LayoutNode): Boolean

        /**
         * Calls a hit test on [layoutNode].
         */
        fun childHitTest(
            layoutNode: LayoutNode,
            pointerPosition: Offset,
            hitTestResult: HitTestResult<N>,
            isTouchEvent: Boolean,
            isInLayer: Boolean
        )
    }

    internal companion object {
        const val ExpectAttachedLayoutCoordinates = "LayoutCoordinate operations are only valid " +
            "when isAttached is true"
        const val UnmeasuredError = "Asking for measurement result of unmeasured layout modifier"
        private val onCommitAffectingLayerParams: (NodeCoordinator) -> Unit = { coordinator ->
            if (coordinator.isValidOwnerScope) {
                // coordinator.layerPositionalProperties should always be non-null here, but
                // we'll just be careful with a null check.
                val layerPositionalProperties = coordinator.layerPositionalProperties
                if (layerPositionalProperties == null) {
                    coordinator.updateLayerParameters()
                } else {
                    tmpLayerPositionalProperties.copyFrom(layerPositionalProperties)
                    coordinator.updateLayerParameters()
                    if (!tmpLayerPositionalProperties.hasSameValuesAs(layerPositionalProperties)) {
                        val layoutNode = coordinator.layoutNode
                        val layoutDelegate = layoutNode.layoutDelegate
                        if (layoutDelegate.childrenAccessingCoordinatesDuringPlacement > 0) {
                            if (layoutDelegate.coordinatesAccessedDuringPlacement) {
                                layoutNode.requestRelayout()
                            }
                            layoutDelegate.measurePassDelegate
                                .notifyChildrenUsingCoordinatesWhilePlacing()
                        }
                        layoutNode.owner?.requestOnPositionedCallback(layoutNode)
                    }
                }
            }
        }
        private val onCommitAffectingLayer: (NodeCoordinator) -> Unit = { coordinator ->
            coordinator.layer?.invalidate()
        }
        private val graphicsLayerScope = ReusableGraphicsLayerScope()
        private val tmpLayerPositionalProperties = LayerPositionalProperties()

        // Used for matrix calculations. It should not be used for anything that could lead to
        // reentrancy.
        private val tmpMatrix = Matrix()

        /**
         * Hit testing specifics for pointer input.
         */
        @OptIn(ExperimentalComposeUiApi::class)
        val PointerInputSource =
            object : HitTestSource<PointerInputModifierNode> {
                override fun entityType() = Nodes.PointerInput

                override fun interceptOutOfBoundsChildEvents(node: PointerInputModifierNode) =
                    node.interceptOutOfBoundsChildEvents()

                override fun shouldHitTestChildren(parentLayoutNode: LayoutNode) = true

                override fun childHitTest(
                    layoutNode: LayoutNode,
                    pointerPosition: Offset,
                    hitTestResult: HitTestResult<PointerInputModifierNode>,
                    isTouchEvent: Boolean,
                    isInLayer: Boolean
                ) = layoutNode.hitTest(pointerPosition, hitTestResult, isTouchEvent, isInLayer)
            }

        /**
         * Hit testing specifics for semantics.
         */
        val SemanticsSource =
            object : HitTestSource<SemanticsModifierNode> {
                override fun entityType() = Nodes.Semantics

                override fun interceptOutOfBoundsChildEvents(node: SemanticsModifierNode) = false

                override fun shouldHitTestChildren(parentLayoutNode: LayoutNode) =
                    parentLayoutNode.outerSemantics?.collapsedSemanticsConfiguration()
                        ?.isClearingSemantics != true

                override fun childHitTest(
                    layoutNode: LayoutNode,
                    pointerPosition: Offset,
                    hitTestResult: HitTestResult<SemanticsModifierNode>,
                    isTouchEvent: Boolean,
                    isInLayer: Boolean
                ) = layoutNode.hitTestSemantics(
                    pointerPosition,
                    hitTestResult,
                    isTouchEvent,
                    isInLayer
                )
            }
    }
}

/**
 * These are the components of a layer that changes the position and may lead
 * to an OnGloballyPositionedCallback.
 */
private class LayerPositionalProperties {
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f
    private var translationX: Float = 0f
    private var translationY: Float = 0f
    private var rotationX: Float = 0f
    private var rotationY: Float = 0f
    private var rotationZ: Float = 0f
    private var cameraDistance: Float = DefaultCameraDistance
    private var transformOrigin: TransformOrigin = TransformOrigin.Center

    fun copyFrom(other: LayerPositionalProperties) {
        scaleX = other.scaleX
        scaleY = other.scaleY
        translationX = other.translationX
        translationY = other.translationY
        rotationX = other.rotationX
        rotationY = other.rotationY
        rotationZ = other.rotationZ
        cameraDistance = other.cameraDistance
        transformOrigin = other.transformOrigin
    }

    fun copyFrom(scope: GraphicsLayerScope) {
        scaleX = scope.scaleX
        scaleY = scope.scaleY
        translationX = scope.translationX
        translationY = scope.translationY
        rotationX = scope.rotationX
        rotationY = scope.rotationY
        rotationZ = scope.rotationZ
        cameraDistance = scope.cameraDistance
        transformOrigin = scope.transformOrigin
    }

    fun hasSameValuesAs(other: LayerPositionalProperties): Boolean {
        return scaleX == other.scaleX &&
            scaleY == other.scaleY &&
            translationX == other.translationX &&
            translationY == other.translationY &&
            rotationX == other.rotationX &&
            rotationY == other.rotationY &&
            rotationZ == other.rotationZ &&
            cameraDistance == other.cameraDistance &&
            transformOrigin == other.transformOrigin
    }
}

private fun <T> DelegatableNode.nextUncheckedUntil(type: NodeKind<T>, stopType: NodeKind<*>): T? {
    val child = node.child ?: return null
    if (child.aggregateChildKindSet and type.mask == 0) return null
    var next: Modifier.Node? = child
    while (next != null) {
        val kindSet = next.kindSet
        if (kindSet and stopType.mask != 0) return null
        if (kindSet and type.mask != 0) {
            @Suppress("UNCHECKED_CAST")
            return next as? T
        }
        next = next.child
    }
    return null
}
