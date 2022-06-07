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
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.layout.findRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.outerSemantics
import androidx.compose.ui.semantics.SemanticsEntity
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.minus
import androidx.compose.ui.unit.plus

/**
 * Measurable and Placeable type that has a position.
 */
internal abstract class LayoutNodeWrapper(
    internal val layoutNode: LayoutNode
) : Placeable(), Measurable, LayoutCoordinates, OwnerScope, (Canvas) -> Unit {
    internal open val wrapped: LayoutNodeWrapper? get() = null
    internal var wrappedBy: LayoutNodeWrapper? = null

    /**
     * The scope used to measure the wrapped. InnerPlaceables are using the MeasureScope
     * of the LayoutNode. For fewer allocations, everything else is reusing the measure scope of
     * their wrapped.
     */
    abstract val measureScope: MeasureScope

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

    private var _isAttached = false
    final override val isAttached: Boolean
        get() {
            if (_isAttached) {
                require(layoutNode.isAttached)
            }
            return _isAttached
        }

    private var _measureResult: MeasureResult? = null
    var measureResult: MeasureResult
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
                    if (wrapped?.layoutNode == layoutNode) {
                        layoutNode.parent?.onAlignmentsChanged()
                        // We might need to request remeasure or relayout for the parent in
                        // case they ask for the lines so we are the query owner, without
                        // marking dirty our alignment lines (because only the modifier's changed).
                        if (layoutNode.alignmentLines.usedDuringParentMeasurement) {
                            layoutNode.parent?.requestRemeasure()
                        } else if (layoutNode.alignmentLines.usedDuringParentLayout) {
                            layoutNode.parent?.requestRelayout()
                        }
                    } else {
                        // It means we are an InnerPlaceable.
                        layoutNode.onAlignmentsChanged()
                    }
                    layoutNode.alignmentLines.dirty = true

                    val oldLines = oldAlignmentLines
                        ?: (mutableMapOf<AlignmentLine, Int>().also { oldAlignmentLines = it })
                    oldLines.clear()
                    oldLines.putAll(value.alignmentLines)
                }
            }
        }

    private val hasMeasureResult: Boolean
        get() = _measureResult != null

    private var oldAlignmentLines: MutableMap<AlignmentLine, Int>? = null

    override val providedAlignmentLines: Set<AlignmentLine>
        get() {
            var set: MutableSet<AlignmentLine>? = null
            var wrapper: LayoutNodeWrapper? = this
            while (wrapper != null) {
                val alignmentLines = wrapper._measureResult?.alignmentLines
                if (alignmentLines?.isNotEmpty() == true) {
                    if (set == null) {
                        set = mutableSetOf()
                    }
                    set.addAll(alignmentLines.keys)
                }
                wrapper = wrapper.wrapped
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
        entities.forEach(EntityList.DrawEntityType) { it.onMeasureResultChanged() }
    }

    var position: IntOffset = IntOffset.Zero
        private set

    var zIndex: Float = 0f
        protected set

    override val parentData: Any?
        get() = entities.head(EntityList.ParentDataEntityType).parentData

    private val SimpleEntity<ParentDataModifier>?.parentData: Any?
        get() = if (this == null) {
            wrapped?.parentData
        } else {
            with(modifier) {
                /**
                 * ParentData provided through the parentData node will override the data provided
                 * through a modifier.
                 */
                measureScope.modifyParentData(next.parentData)
            }
        }

    final override val parentLayoutCoordinates: LayoutCoordinates?
        get() {
            check(isAttached) { ExpectAttachedLayoutCoordinates }
            return layoutNode.outerLayoutNodeWrapper.wrappedBy
        }

    final override val parentCoordinates: LayoutCoordinates?
        get() {
            check(isAttached) { ExpectAttachedLayoutCoordinates }
            return wrappedBy
        }

    // True when the wrapper is running its own placing block to obtain the position of the
    // wrapped, but is not interested in the position of the wrapped of the wrapped.
    var isShallowPlacing = false

    private var _rectCache: MutableRect? = null
    protected val rectCache: MutableRect
        get() = _rectCache ?: MutableRect(0f, 0f, 0f, 0f).also {
            _rectCache = it
        }

    private val snapshotObserver get() = layoutNode.requireOwner().snapshotObserver

    /**
     * All [LayoutNodeEntity] elements that are associated with this [LayoutNodeWrapper].
     */
    val entities = EntityList()

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
        if (entities.has(EntityList.RemeasureEntityType)) {
            Snapshot.withoutReadObservation {
                entities.forEach(EntityList.RemeasureEntityType) {
                    it.modifier.onRemeasured(measuredSize)
                }
            }
        }
    }

    abstract fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int

    final override fun get(alignmentLine: AlignmentLine): Int {
        if (!hasMeasureResult) return AlignmentLine.Unspecified
        val measuredPosition = calculateAlignmentLine(alignmentLine)
        if (measuredPosition == AlignmentLine.Unspecified) return AlignmentLine.Unspecified
        return measuredPosition + if (alignmentLine is VerticalAlignmentLine) {
            apparentToRealOffset.x
        } else {
            apparentToRealOffset.y
        }
    }

    /**
     * An initialization function that is called when the [LayoutNodeWrapper] is initially created,
     * and also called when the [LayoutNodeWrapper] is re-used.
     */
    open fun onInitialize() {
        layer?.invalidate()
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
            val layer = layer
            if (layer != null) {
                layer.move(position)
            } else {
                wrappedBy?.invalidateLayer()
            }
            if (wrapped?.layoutNode != layoutNode) {
                layoutNode.onAlignmentsChanged()
            } else {
                layoutNode.parent?.onAlignmentsChanged()
            }
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
        val head = entities.head(EntityList.DrawEntityType)
        if (head == null) {
            performDraw(canvas)
        } else {
            head.draw(canvas)
        }
    }

    open fun performDraw(canvas: Canvas) {
        wrapped?.draw(canvas)
    }

    fun onPlaced() {
        @OptIn(ExperimentalComposeUiApi::class)
        entities.forEach(EntityList.OnPlacedEntityType) {
            it.modifier.onPlaced(this)
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

    fun onLayerBlockUpdated(layerBlock: (GraphicsLayerScope.() -> Unit)?) {
        val layerInvalidated = this.layerBlock !== layerBlock || layerDensity != layoutNode
            .density || layerLayoutDirection != layoutNode.layoutDirection
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
                layoutNode.innerLayerWrapperIsDirty = true
                invalidateParentLayer()
            } else if (layerInvalidated) {
                updateLayerParameters()
            }
        } else {
            layer?.let {
                it.destroy()
                layoutNode.innerLayerWrapperIsDirty = true
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
            snapshotObserver.observeReads(this, onCommitAffectingLayerParams) {
                layerBlock.invoke(graphicsLayerScope)
            }
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

    override val isValid: Boolean
        get() = layer != null

    val minimumTouchTargetSize: Size
        get() = with(layerDensity) { layoutNode.viewConfiguration.minimumTouchTargetSize.toSize() }

    /**
     * Executes a hit test for this [LayoutNodeWrapper].
     *
     * @param hitTestSource The hit test specifics for pointer input or semantics
     * @param pointerPosition The tested pointer position, which is relative to
     * the [LayoutNodeWrapper].
     * @param hitTestResult The parent [HitTestResult] that any hit should be added to.
     * @param isTouchEvent `true` if this is from a touch source. Touch sources allow for
     * minimum touch target. Semantics hit tests always treat hits as needing minimum touch target.
     * @param isInLayer `true` if the touch event is in the layer of this and all parents or `false`
     * if it is outside the layer, but within the minimum touch target of the edge of the layer.
     * This can only be `false` when [isTouchEvent] is `true` or else a layer miss means the event
     * will be clipped out.
     */
    fun <T : LayoutNodeEntity<T, M>, C, M : Modifier> hitTest(
        hitTestSource: HitTestSource<T, C, M>,
        pointerPosition: Offset,
        hitTestResult: HitTestResult<C>,
        isTouchEvent: Boolean,
        isInLayer: Boolean
    ) {
        val head = entities.head(hitTestSource.entityType())
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
     * The [LayoutNodeWrapper] had a hit in bounds and can record any children in the
     * [hitTestResult].
     */
    private fun <T : LayoutNodeEntity<T, M>, C, M : Modifier> T?.hit(
        hitTestSource: HitTestSource<T, C, M>,
        pointerPosition: Offset,
        hitTestResult: HitTestResult<C>,
        isTouchEvent: Boolean,
        isInLayer: Boolean
    ) {
        if (this == null) {
            hitTestChild(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
        } else {
            hitTestResult.hit(hitTestSource.contentFrom(this), isInLayer) {
                next.hit(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
            }
        }
    }

    /**
     * The [LayoutNodeWrapper] had a hit [distanceFromEdge] from the bounds and it is within
     * the minimum touch target distance, so it should be recorded as such in the [hitTestResult].
     */
    private fun <T : LayoutNodeEntity<T, M>, C, M : Modifier> T?.hitNear(
        hitTestSource: HitTestSource<T, C, M>,
        pointerPosition: Offset,
        hitTestResult: HitTestResult<C>,
        isTouchEvent: Boolean,
        isInLayer: Boolean,
        distanceFromEdge: Float
    ) {
        if (this == null) {
            hitTestChild(hitTestSource, pointerPosition, hitTestResult, isTouchEvent, isInLayer)
        } else {
            // Hit closer than existing handlers, so just record it
            hitTestResult.hitInMinimumTouchTarget(
                hitTestSource.contentFrom(this),
                distanceFromEdge,
                isInLayer
            ) {
                next.hitNear(
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
     * The [LayoutNodeWrapper] had a miss, but it hasn't been clipped out. The child must be
     * checked to see if it hit.
     */
    private fun <T : LayoutNodeEntity<T, M>, C, M : Modifier> T?.speculativeHit(
        hitTestSource: HitTestSource<T, C, M>,
        pointerPosition: Offset,
        hitTestResult: HitTestResult<C>,
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
                hitTestSource.contentFrom(this),
                distanceFromEdge,
                isInLayer
            ) {
                next.speculativeHit(
                    hitTestSource,
                    pointerPosition,
                    hitTestResult,
                    isTouchEvent,
                    isInLayer,
                    distanceFromEdge
                )
            }
        } else {
            next.speculativeHit(
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
     * Do a [hitTest] on the children of this [LayoutNodeWrapper].
     */
    open fun <T : LayoutNodeEntity<T, M>, C, M : Modifier> hitTestChild(
        hitTestSource: HitTestSource<T, C, M>,
        pointerPosition: Offset,
        hitTestResult: HitTestResult<C>,
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
     * Returns the bounds of this [LayoutNodeWrapper], including the minimum touch target.
     */
    fun touchBoundsInRoot(): Rect {
        if (!isAttached) {
            return Rect.Zero
        }

        val root = findRoot()

        val bounds = rectCache
        val padding = calculateMinimumTouchTargetPadding(minimumTouchTargetSize)
        bounds.left = -padding.width
        bounds.top = -padding.height
        bounds.right = measuredWidth + padding.width
        bounds.bottom = measuredHeight + padding.height

        var wrapper: LayoutNodeWrapper = this
        while (wrapper !== root) {
            wrapper.rectInParent(bounds, clipBounds = false, clipToMinimumTouchTargetSize = true)
            if (bounds.isEmpty) {
                return Rect.Zero
            }

            wrapper = wrapper.wrappedBy!!
        }
        return bounds.toRect()
    }

    override fun windowToLocal(relativeToWindow: Offset): Offset {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        val root = findRoot()
        val positionInRoot = layoutNode.requireOwner()
            .calculateLocalPosition(relativeToWindow) - root.positionInRoot()
        return localPositionOf(root, positionInRoot)
    }

    override fun localToWindow(relativeToLocal: Offset): Offset {
        val positionInRoot = localToRoot(relativeToLocal)
        val owner = layoutNode.requireOwner()
        return owner.calculatePositionInWindow(positionInRoot)
    }

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset
    ): Offset {
        val layoutNodeWrapper = sourceCoordinates as LayoutNodeWrapper
        val commonAncestor = findCommonAncestor(sourceCoordinates)

        var position = relativeToSource
        var wrapper = layoutNodeWrapper
        while (wrapper !== commonAncestor) {
            position = wrapper.toParentPosition(position)
            wrapper = wrapper.wrappedBy!!
        }

        return ancestorToLocal(commonAncestor, position)
    }

    override fun localBoundingBoxOf(
        sourceCoordinates: LayoutCoordinates,
        clipBounds: Boolean
    ): Rect {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        check(sourceCoordinates.isAttached) {
            "LayoutCoordinates $sourceCoordinates is not attached!"
        }
        val layoutNodeWrapper = sourceCoordinates as LayoutNodeWrapper
        val commonAncestor = findCommonAncestor(sourceCoordinates)

        val bounds = rectCache
        bounds.left = 0f
        bounds.top = 0f
        bounds.right = sourceCoordinates.size.width.toFloat()
        bounds.bottom = sourceCoordinates.size.height.toFloat()

        var wrapper = layoutNodeWrapper
        while (wrapper !== commonAncestor) {
            wrapper.rectInParent(bounds, clipBounds)
            if (bounds.isEmpty) {
                return Rect.Zero
            }

            wrapper = wrapper.wrappedBy!!
        }

        ancestorToLocal(commonAncestor, bounds, clipBounds)
        return bounds.toRect()
    }

    private fun ancestorToLocal(ancestor: LayoutNodeWrapper, offset: Offset): Offset {
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
        ancestor: LayoutNodeWrapper,
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
        var wrapper: LayoutNodeWrapper? = this
        var position = relativeToLocal
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
        val relativeToWrapperPosition = position - this.position
        val layer = layer
        return layer?.mapOffset(relativeToWrapperPosition, inverse = true)
            ?: relativeToWrapperPosition
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
     * Attaches the [LayoutNodeWrapper] and its wrapped [LayoutNodeWrapper] to an active
     * LayoutNode.
     *
     * This will be called when the [LayoutNode] associated with this [LayoutNodeWrapper] is
     * attached to the [Owner].
     *
     * It is also called whenever the modifier chain is replaced and the [LayoutNodeWrapper]s are
     * recreated.
     */
    open fun attach() {
        _isAttached = true
        onLayerBlockUpdated(layerBlock)
        entities.forEach { it.onAttach() }
    }

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
    open fun detach() {
        entities.forEach { it.onDetach() }
        _isAttached = false
        onLayerBlockUpdated(layerBlock)
        // The layer has been removed and we need to invalidate the containing layer. We've lost
        // which layer contained this one, but all layers in this modifier chain will be invalidated
        // in onModifierChanged(). Therefore the only possible layer that won't automatically be
        // invalidated is the parent's layer. We'll invalidate it here:
        layoutNode.parent?.invalidateLayer()
    }

    /**
     * Modifies bounds to be in the parent LayoutNodeWrapper's coordinates, including clipping,
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
     * Modifies bounds in the parent's coordinates to be in this LayoutNodeWrapper's
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
     * Whether a pointer that is relative to the [LayoutNodeWrapper] is in the bounds of this
     * LayoutNodeWrapper.
     */
    protected fun isPointerInBounds(pointerPosition: Offset): Boolean {
        val x = pointerPosition.x
        val y = pointerPosition.y
        return x >= 0f && y >= 0f && x < measuredWidth && y < measuredHeight
    }

    /**
     * Invalidates the layer that this wrapper will draw into.
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
     * coordinates of that layoutNodeWrapper. This request is sent up the hierarchy to all parents
     * that have a [RelocationModifier][androidx.compose.ui.layout.RelocationModifier].
     */
    open suspend fun propagateRelocationRequest(rect: Rect) {
        val parent = wrappedBy ?: return

        // Translate this layoutNodeWrapper to the coordinate system of the parent.
        val boundingBoxInParentCoordinates = parent.localBoundingBoxOf(this, false)

        // Translate the rect to parent coordinates
        val rectInParentBounds = rect.translate(boundingBoxInParentCoordinates.topLeft)

        parent.propagateRelocationRequest(rectInParentBounds)
    }

    /**
     * Called when [LayoutNode.modifier] has changed and all the LayoutNodeWrappers have been
     * configured.
     */
    open fun onModifierChanged() {
        layer?.invalidate()
    }

    internal fun findCommonAncestor(other: LayoutNodeWrapper): LayoutNodeWrapper {
        var ancestor1 = other.layoutNode
        var ancestor2 = layoutNode
        if (ancestor1 === ancestor2) {
            // They are on the same node, but we don't know which is the deeper of the two
            val tooFar = layoutNode.outerLayoutNodeWrapper
            var tryMe = this
            while (tryMe !== tooFar && tryMe !== other) {
                tryMe = tryMe.wrappedBy!!
            }
            if (tryMe === other) {
                return other
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
            else -> ancestor1.innerLayoutNodeWrapper
        }
    }

    fun shouldSharePointerInputWithSiblings(): Boolean =
        entities.head(EntityList.PointerInputEntityType)
            ?.shouldSharePointerInputWithSiblings() == true ||
            wrapped?.shouldSharePointerInputWithSiblings() == true

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
            offsetFromEdge.x <= width && offsetFromEdge.y <= height) {
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
    internal interface HitTestSource<T : LayoutNodeEntity<T, M>, C, M : Modifier> {
        /**
         * Returns the [EntityList.EntityType] for the hit test target.
         */
        fun entityType(): EntityList.EntityType<T, M>

        /**
         * Returns the value used to store in [HitTestResult] for the given [LayoutNodeEntity].
         */
        fun contentFrom(entity: T): C

        /**
         * Pointer input hit tests can intercept child hits when enabled. This returns `true`
         * if the modifier has requested intercepting.
         */
        fun interceptOutOfBoundsChildEvents(entity: T): Boolean

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
            hitTestResult: HitTestResult<C>,
            isTouchEvent: Boolean,
            isInLayer: Boolean
        )
    }

    internal companion object {
        const val ExpectAttachedLayoutCoordinates = "LayoutCoordinate operations are only valid " +
            "when isAttached is true"
        const val UnmeasuredError = "Asking for measurement result of unmeasured layout modifier"
        private val onCommitAffectingLayerParams: (LayoutNodeWrapper) -> Unit = { wrapper ->
            if (wrapper.isValid) {
                wrapper.updateLayerParameters()
            }
        }
        private val onCommitAffectingLayer: (LayoutNodeWrapper) -> Unit = { wrapper ->
            wrapper.layer?.invalidate()
        }
        private val graphicsLayerScope = ReusableGraphicsLayerScope()

        /**
         * Hit testing specifics for pointer input.
         */
        val PointerInputSource =
            object : HitTestSource<PointerInputEntity, PointerInputFilter, PointerInputModifier> {
                override fun entityType() = EntityList.PointerInputEntityType

                @Suppress("ModifierFactoryReturnType", "ModifierFactoryExtensionFunction")
                override fun contentFrom(entity: PointerInputEntity) =
                    entity.modifier.pointerInputFilter

                override fun interceptOutOfBoundsChildEvents(entity: PointerInputEntity) =
                    entity.modifier.pointerInputFilter.interceptOutOfBoundsChildEvents

                override fun shouldHitTestChildren(parentLayoutNode: LayoutNode) = true

                override fun childHitTest(
                    layoutNode: LayoutNode,
                    pointerPosition: Offset,
                    hitTestResult: HitTestResult<PointerInputFilter>,
                    isTouchEvent: Boolean,
                    isInLayer: Boolean
                ) = layoutNode.hitTest(pointerPosition, hitTestResult, isTouchEvent, isInLayer)
            }

        /**
         * Hit testing specifics for semantics.
         */
        val SemanticsSource =
            object : HitTestSource<SemanticsEntity, SemanticsEntity, SemanticsModifier> {
                override fun entityType() = EntityList.SemanticsEntityType

                override fun contentFrom(entity: SemanticsEntity) = entity

                override fun interceptOutOfBoundsChildEvents(entity: SemanticsEntity) = false

                override fun shouldHitTestChildren(parentLayoutNode: LayoutNode) =
                    parentLayoutNode.outerSemantics?.collapsedSemanticsConfiguration()
                         ?.isClearingSemantics != true

                override fun childHitTest(
                    layoutNode: LayoutNode,
                    pointerPosition: Offset,
                    hitTestResult: HitTestResult<SemanticsEntity>,
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
