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

import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * A layer returned by [Owner.createLayer] to separate drawn content.
 */
interface OwnedLayer {
    /**
     * The ID of the layer. This is used by tooling to match a layer to the associated
     * LayoutNode.
     */
    val layerId: Long

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
        clip: Boolean
    )

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
     * Modifies [matrix] to be the transform that this layer applies to its content.
     */
    fun getMatrix(matrix: Matrix)
}
