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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round

/**
 * A [Placeable] corresponds to a child layout that can be positioned by its
 * parent layout. Most [Placeable]s are the result of a [Measurable.measure] call.
 *
 * A `Placeable` should never be stored between measure calls.
 */
abstract class Placeable {
    /**
     * The width, in pixels, of the measured layout, as seen by the parent. This is usually the
     * `width` value passed into [MeasureScope.layout], but will be different if the layout does
     * not respect its incoming constraints, so the width will be coerced inside the min and
     * max width.
     */
    var width: Int = 0
        private set

    /**
     * The height, in pixels, of the measured layout, as seen by the parent. This is usually the
     * `height` value passed into [MeasureScope.layout], but can be different if the layout does
     * not respect its incoming constraints, so the height will be coerced inside the min and
     * max height.
     */
    var height: Int = 0
        private set

    /**
     * Returns the position of an [alignment line][AlignmentLine],
     * or `null` if the line is not provided.
     */
    abstract operator fun get(line: AlignmentLine): Int

    /**
     * The measured size of this Placeable. This might not respect [measurementConstraints].
     */
    protected var measuredSize: IntSize = IntSize(0, 0)
        set(value) {
            field = value
            width = value.width.coerceIn(
                measurementConstraints.minWidth,
                measurementConstraints.maxWidth
            )
            height = value.height.coerceIn(
                measurementConstraints.minHeight,
                measurementConstraints.maxHeight
            )
        }

    internal val measuredWidth get() = measuredSize.width

    internal val measuredHeight get() = measuredSize.height

    /**
     * Positions the [Placeable] at [position] in its parent's coordinate system.
     */
    protected abstract fun placeAt(position: IntOffset)

    /**
     * The constraints used for the measurement made to obtain this [Placeable].
     */
    protected var measurementConstraints: Constraints = Constraints()

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
         */
        fun Placeable.placeRelative(position: IntOffset) = placeAutoMirrored(position)

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system.
         * If the layout direction is right-to-left, the given [position] will be horizontally
         * mirrored so that the position of the [Placeable] implicitly reacts to RTL layout
         * direction contexts.
         * If this method is used outside the [MeasureScope.layout] positioning block, the
         * automatic position mirroring will not happen and the [Placeable] will be placed at the
         * given [position], similar to the [place] method.
         */
        fun Placeable.placeRelative(position: Offset) = placeAutoMirrored(position.round())

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system.
         * If the layout direction is right-to-left, the given position will be horizontally
         * mirrored so that the position of the [Placeable] implicitly reacts to RTL layout
         * direction contexts.
         * If this method is used outside the [MeasureScope.layout] positioning block, the
         * automatic position mirroring will not happen and the [Placeable] will be placed at the
         * given position, similar to the [place] method.
         */
        fun Placeable.placeRelative(x: Int, y: Int) = placeAutoMirrored(IntOffset(x, y))

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system.
         * Unlike [placeRelative], the given [position] will not implicitly react in RTL layout direction
         * contexts.
         */
        fun Placeable.place(position: Offset) = place(position.round())

        /**
         * Place a [Placeable] at [x], [y] in its parent's coordinate system.
         * Unlike [placeRelative], the given position will not implicitly react in RTL layout direction
         * contexts.
         */
        fun Placeable.place(x: Int, y: Int) = place(IntOffset(x, y))

        /**
         * Place a [Placeable] at [position] in its parent's coordinate system.
         * Unlike [placeRelative], the given [position] will not implicitly react in RTL layout direction
         * contexts.
         */
        fun Placeable.place(position: IntOffset) =
            placeAt(position + apparentToRealOffset)

        private fun Placeable.placeAutoMirrored(position: IntOffset) {
            if (parentLayoutDirection == LayoutDirection.Ltr || parentWidth == 0) {
                place(position)
            } else {
                place(IntOffset(parentWidth - measuredSize.width - position.x, position.y))
            }
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