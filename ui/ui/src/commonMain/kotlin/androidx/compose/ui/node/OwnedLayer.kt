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

package androidx.compose.ui.node

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * A layer returned by [Owner.createLayer] to separate drawn content.
 */
internal interface OwnedLayer {

    /**
     * Applies the new layer properties and causing this layer to be redrawn.
     */
    fun updateLayerProperties(
        scaleX: Float,
        scaleY: Float,
        alpha: Float,
        translationX: Float,
        translationY: Float,
        shadowElevation: Float,
        rotationX: Float,
        rotationY: Float,
        rotationZ: Float,
        cameraDistance: Float,
        transformOrigin: TransformOrigin,
        shape: Shape,
        clip: Boolean,
        renderEffect: RenderEffect?,
        ambientShadowColor: Color,
        spotShadowColor: Color,
        layoutDirection: LayoutDirection,
        density: Density
    )

    /**
     * Returns `false` if [position] is outside the clipped region or `true` if clipping
     * is disabled or it is within the clipped region.
     */
    fun isInLayer(position: Offset): Boolean

    /**
     * Changes the position of the layer contents.
     */
    fun move(position: IntOffset)

    /**
     * Changes the size of the layer's drawn area.
     */
    fun resize(size: IntSize)

    /**
     * Causes the layer to be drawn into [canvas]
     */
    fun drawLayer(canvas: Canvas)

    /**
     * Updates the drawing on the current canvas.
     */
    fun updateDisplayList()

    /**
     * Asks to the layer to redraw itself without forcing all of its parents to redraw.
     */
    fun invalidate()

    /**
     * Indicates that the layer is no longer needed.
     */
    fun destroy()

    /**
     * Transforms [point] to this layer's bounds, returning an [Offset] with the transformed x
     * and y values.
     *
     * @param point the [Offset] to transform to this layer's bounds
     * @param inverse whether to invert this layer's transform [Matrix] first, such as when
     * converting an offset in a parent layer to be in this layer's coordinates.
     */
    fun mapOffset(point: Offset, inverse: Boolean): Offset

    /**
     * Transforms the provided [rect] to this layer's bounds, then updates [rect] to match the
     * new bounds after the transform.
     *
     * @param rect the bounds to transform to this layer's bounds, and then mutate with the
     * resulting value
     * @param inverse whether to invert this layer's transform [Matrix] first, such as when
     * converting bounds in a parent layer to be in this layer's coordinates.
     */
    fun mapBounds(rect: MutableRect, inverse: Boolean)

    /**
     * Reuse this layer after it was [destroy]ed, setting the new
     * [drawBlock] and [invalidateParentLayer] values. The layer will be reinitialized
     * as new after this call.
     */
    fun reuseLayer(drawBlock: (Canvas) -> Unit, invalidateParentLayer: () -> Unit)
}
