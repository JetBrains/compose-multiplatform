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

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * A [Placeable] corresponds to a child layout that can be positioned by its
 * parent layout. Most [Placeable]s are the result of a [Measurable.measure] call.
 *
 * A `Placeable` should never be stored between measure calls.
 */
abstract class Placeable : Measured {
    /**
     * The width, in pixels, of the measured layout, as seen by the parent. This will usually
     * coincide with the measured width of the layout (aka the `width` value passed into
     * [MeasureScope.layout]), but can be different if the layout does not respect its
     * incoming constraints: in these cases the width will be coerced inside the min and
     * max width constraints - to access the actual width that the layout measured itself to,
     * use [measuredWidth].
     */
    var width: Int = 0
        private set

    /**
     * The height, in pixels, of the measured layout, as seen by the parent. This will usually
     * coincide with the measured height of the layout (aka the `height` value passed
     * into [MeasureScope.layout]), but can be different if the layout does not respect its
     * incoming constraints: in these cases the height will be coerced inside the min and
     * max height constraints - to access the actual height that the layout measured itself to,
     * use [measuredHeight].
     */
    var height: Int = 0
        private set

    /**
     * The measured width of the layout. This might not respect the measurement constraints.
     */
    override val measuredWidth: Int get() = measuredSize.width

    /**
     * The measured height of the layout. This might not respect the measurement constraints.
     */
    override val measuredHeight: Int get() = measuredSize.height

    /**
     * The measured size of this Placeable. This might not respect [measurementConstraints].
     */
    protected var measuredSize: IntSize = IntSize(0, 0)
        set(value) {
            if (field != value) {
                field = value
                recalculateWidthAndHeight()
            }
        }

    private fun recalculateWidthAndHeight() {
        width = measuredSize.width.coerceIn(
            measurementConstraints.minWidth,
            measurementConstraints.maxWidth
        )
        height = measuredSize.height.coerceIn(
            measurementConstraints.minHeight,
            measurementConstraints.maxHeight
        )
    }

    /**
     * Positions the [Placeable] at [position] in its parent's coordinate system.
     *
     * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
     * [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
     * have the same [zIndex] the order in which the items were placed is used.
     * @param layerBlock when non-null this [Placeable] should be placed with an introduced
     * graphic layer. You can configure any layer property available on [GraphicsLayerScope] via
     * this block. Also if the [Placeable] will be placed with a new [position] next time only the
     * graphic layer will be moved without requiring to redrawn the [Placeable] content.
     */
    protected abstract fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    )

    /**
     * The constraints used for the measurement made to obtain this [Placeable].
     */
    protected var measurementConstraints: Constraints = DefaultConstraints
        set(value) {
            if (field != value) {
                field = value
                recalculateWidthAndHeight()
            }
        }

    /**
     * The offset to be added to an apparent position assigned to this [Placeable] to make it real.
     * The real layout will be centered on the space assigned by the parent, which computed the
     * child's position only seeing its apparent size.
     */
    protected val apparentToRealOffset: IntOffset
        get() = IntOffset((width - measuredSize.width) / 2, (height - measuredSize.height) / 2)

    /**
     * Receiver scope that permits explicit placement of a [Placeable].
     *
     * While a [Placeable] may be placed at any time, this explicit receiver scope is used
     * to discourage placement outside of [MeasureScope.layout] positioning blocks.
     * This permits Compose UI to perform additional layout optimizations allowing repositioning
     * a [Placeable] without remeasuring its original [Measurable] if factors contributing to its
     * potential measurement have not changed.
     * The scope also allows automatic mirroring of children positions in RTL layout direction
     * contexts using the [placeRelative] methods available in the scope. If the automatic
     * mirroring is not desired, [place] should be used instead.
     */
    // TODO(b/150276678): using the PlacementScope to place outside the layout pass is not working.
    abstract class PlacementScope {
        /**
         * Keeps the parent layout node's width to make the automatic mirroring of the position
         * in RTL environment. If the value is zero, than the [Placeable] will be be placed to
         * the original position (position will not be mirrored).
         */
        protected abstract val parentWidth: Int

        /**
         * Keeps the layout direction of the parent of the placeable that is being places using
         * current [PlacementScope]. Used to support automatic position mirroring for convenient
         * RTL support in custom layouts.
         */
        protected abstract val parentLayoutDirection: LayoutDirection

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system.
         * If the layout direction is right-to-left, the given [position] will be horizontally
         * mirrored so that the position of the [Placeable] implicitly reacts to RTL layout
         * direction contexts.
         * If this method is used outside the [MeasureScope.layout] positioning block, the
         * automatic position mirroring will not happen and the [Placeable] will be placed at the
         * given [position], similar to the [place] method.
         *
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         * [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         * have the same [zIndex] the order in which the items were placed is used.
         */
        fun Placeable.placeRelative(position: IntOffset, zIndex: Float = 0f) =
            placeAutoMirrored(position, zIndex, null)

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system.
         * If the layout direction is right-to-left, the given position will be horizontally
         * mirrored so that the position of the [Placeable] implicitly reacts to RTL layout
         * direction contexts.
         * If this method is used outside the [MeasureScope.layout] positioning block, the
         * automatic position mirroring will not happen and the [Placeable] will be placed at the
         * given position, similar to the [place] method.
         *
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         * [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         * have the same [zIndex] the order in which the items were placed is used.
         */
        fun Placeable.placeRelative(x: Int, y: Int, zIndex: Float = 0f) =
            placeAutoMirrored(IntOffset(x, y), zIndex, null)

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system.
         * Unlike [placeRelative], the given position will not implicitly react in RTL layout direction
         * contexts.
         *
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         * [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         * have the same [zIndex] the order in which the items were placed is used.
         */
        fun Placeable.place(x: Int, y: Int, zIndex: Float = 0f) =
            placeApparentToRealOffset(IntOffset(x, y), zIndex, null)

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system.
         * Unlike [placeRelative], the given [position] will not implicitly react in RTL layout direction
         * contexts.
         *
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         * [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         * have the same [zIndex] the order in which the items were placed is used.
         */
        fun Placeable.place(position: IntOffset, zIndex: Float = 0f) =
            placeApparentToRealOffset(position, zIndex, null)

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system with an introduced
         * graphic layer.
         * If the layout direction is right-to-left, the given [position] will be horizontally
         * mirrored so that the position of the [Placeable] implicitly reacts to RTL layout
         * direction contexts.
         * If this method is used outside the [MeasureScope.layout] positioning block, the
         * automatic position mirroring will not happen and the [Placeable] will be placed at the
         * given [position], similar to the [place] method.
         *
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         * [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         * have the same [zIndex] the order in which the items were placed is used.
         * @param layerBlock You can configure any layer property available on [GraphicsLayerScope] via
         * this block. If the [Placeable] will be placed with a new [position] next time only the
         * graphic layer will be moved without requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeRelativeWithLayer(
            position: IntOffset,
            zIndex: Float = 0f,
            layerBlock: GraphicsLayerScope.() -> Unit = DefaultLayerBlock
        ) = placeAutoMirrored(position, zIndex, layerBlock)

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system with an introduced
         * graphic layer.
         * If the layout direction is right-to-left, the given position will be horizontally
         * mirrored so that the position of the [Placeable] implicitly reacts to RTL layout
         * direction contexts.
         * If this method is used outside the [MeasureScope.layout] positioning block, the
         * automatic position mirroring will not happen and the [Placeable] will be placed at the
         * given position, similar to the [place] method.
         *
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         * [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         * have the same [zIndex] the order in which the items were placed is used.
         * @param layerBlock You can configure any layer property available on [GraphicsLayerScope] via
         * this block. If the [Placeable] will be placed with a new [x] or [y] next time only the
         * graphic layer will be moved without requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeRelativeWithLayer(
            x: Int,
            y: Int,
            zIndex: Float = 0f,
            layerBlock: GraphicsLayerScope.() -> Unit = DefaultLayerBlock
        ) = placeAutoMirrored(IntOffset(x, y), zIndex, layerBlock)

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system with an introduced
         * graphic layer.
         * Unlike [placeRelative], the given position will not implicitly react in RTL layout direction
         * contexts.
         *
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         * [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         * have the same [zIndex] the order in which the items were placed is used.
         * @param layerBlock You can configure any layer property available on [GraphicsLayerScope] via
         * this block. If the [Placeable] will be placed with a new [x] or [y] next time only the
         * graphic layer will be moved without requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeWithLayer(
            x: Int,
            y: Int,
            zIndex: Float = 0f,
            layerBlock: GraphicsLayerScope.() -> Unit = DefaultLayerBlock
        ) = placeApparentToRealOffset(IntOffset(x, y), zIndex, layerBlock)

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system with an introduced
         * graphic layer.
         * Unlike [placeRelative], the given [position] will not implicitly react in RTL layout direction
         * contexts.
         *
         * @param zIndex controls the drawing order for the [Placeable]. A [Placeable] with larger
         * [zIndex] will be drawn on top of all the children with smaller [zIndex]. When children
         * have the same [zIndex] the order in which the items were placed is used.
         * @param layerBlock You can configure any layer property available on [GraphicsLayerScope] via
         * this block. If the [Placeable] will be placed with a new [position] next time only the
         * graphic layer will be moved without requiring to redrawn the [Placeable] content.
         */
        fun Placeable.placeWithLayer(
            position: IntOffset,
            zIndex: Float = 0f,
            layerBlock: GraphicsLayerScope.() -> Unit = DefaultLayerBlock
        ) = placeApparentToRealOffset(position, zIndex, layerBlock)

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun Placeable.placeAutoMirrored(
            position: IntOffset,
            zIndex: Float,
            noinline layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            if (parentLayoutDirection == LayoutDirection.Ltr || parentWidth == 0) {
                placeApparentToRealOffset(position, zIndex, layerBlock)
            } else {
                placeApparentToRealOffset(
                    IntOffset(parentWidth - measuredSize.width - position.x, position.y),
                    zIndex,
                    layerBlock
                )
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun Placeable.placeApparentToRealOffset(
            position: IntOffset,
            zIndex: Float,
            noinline layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            placeAt(position + apparentToRealOffset, zIndex, layerBlock)
        }

        internal companion object : PlacementScope() {
            override var parentLayoutDirection = LayoutDirection.Ltr
                private set
            override var parentWidth = 0
                private set

            inline fun executeWithRtlMirroringValues(
                parentWidth: Int,
                parentLayoutDirection: LayoutDirection,
                crossinline block: PlacementScope.() -> Unit
            ) {
                val previousParentWidth = Companion.parentWidth
                val previousParentLayoutDirection = Companion.parentLayoutDirection
                Companion.parentWidth = parentWidth
                Companion.parentLayoutDirection = parentLayoutDirection
                this.block()
                Companion.parentWidth = previousParentWidth
                Companion.parentLayoutDirection = previousParentLayoutDirection
            }
        }
    }
}

/**
 * Block on [GraphicsLayerScope] which applies the default layer parameters.
 */
private val DefaultLayerBlock: GraphicsLayerScope.() -> Unit = {}

private val DefaultConstraints = Constraints()