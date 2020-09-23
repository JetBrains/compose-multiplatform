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

package androidx.compose.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.util.annotation.FloatRange
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

/**
 * Constructs a [TransformOrigin] from the given fractional values from the Layer's
 * width and height
 */
@Suppress("NOTHING_TO_INLINE")
inline fun TransformOrigin(pivotFractionX: Float, pivotFractionY: Float): TransformOrigin =
    TransformOrigin(packFloats(pivotFractionX, pivotFractionY))

/**
 * A two-dimensional position represented as a fraction of the Layer's width and height
 */
@OptIn(ExperimentalUnsignedTypes::class)
@Immutable
inline class TransformOrigin(@PublishedApi internal val packedValue: Long) {

    /**
     * Return the position along the x-axis that should be used as the
     * origin for rotation and scale transformations. This is represented as a fraction
     * of the width of the content. A value of 0.5f represents the midpoint between the left
     * and right bounds of the content
     */
    val pivotFractionX: Float
        get() = unpackFloat1(packedValue)

    /**
     * Return the position along the y-axis that should be used as the
     * origin for rotation and scale transformations. This is represented as a fraction
     * of the height of the content. A value of 0.5f represents the midpoint between the top
     * and bottom bounds of the content
     */
    val pivotFractionY: Float
        get() = unpackFloat2(packedValue)

    /**
     * Returns a copy of this TransformOrigin instance optionally overriding the
     * pivotFractionX or pivotFractionY parameter
     */
    fun copy(
        pivotFractionX: Float = this.pivotFractionX,
        pivotFractionY: Float = this.pivotFractionY
    ) = TransformOrigin(pivotFractionX, pivotFractionY)

    companion object {

        /**
         * [TransformOrigin] constant to indicate that the center of the content should
         * be used for rotation and scale transformations
         */
        val Center = TransformOrigin(0.5f, 0.5f)
    }
}

/**
 * A [Modifier.Element] that makes content draw into a layer, allowing easily changing
 * properties of the drawn contents.
 *
 * @sample androidx.compose.ui.samples.AnimateFadeIn
 */
interface DrawLayerModifier : Modifier.Element {
    /**
     * The horizontal scale of the drawn area. This would typically default to `1`.
     */
    val scaleX: Float get() = 1f

    /**
     * The vertical scale of the drawn area. This would typically default to `1`.
     */
    val scaleY: Float get() = 1f

    /**
     * The alpha of the drawn area. Setting this to something other than `1`
     * will cause the drawn contents to be translucent and setting it to `0` will
     * cause it to be fully invisible.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val alpha: Float
        get() = 1f

    /**
     * Horizontal pixel offset of the layer relative to its left bound
     */
    val translationX: Float get() = 0f

    /**
     * Vertical pixel offset of the layer relative to its top bound
     */
    val translationY: Float get() = 0f

    /**
     * Sets the elevation for the shadow in pixels. With the [shadowElevation] > 0f and
     * [shape] set, a shadow is produced.
     */
    @get:FloatRange(from = 0.0, to = 3.4e38 /* POSITIVE_INFINITY */)
    val shadowElevation: Float
        get() = 0f

    /**
     * The rotation of the contents around the horizontal axis in degrees.
     */
    @get:FloatRange(from = 0.0, to = 360.0)
    val rotationX: Float
        get() = 0f

    /**
     * The rotation of the contents around the vertical axis in degrees.
     */
    @get:FloatRange(from = 0.0, to = 360.0)
    val rotationY: Float
        get() = 0f

    /**
     * The rotation of the contents around the Z axis in degrees.
     */
    @get:FloatRange(from = 0.0, to = 360.0)
    val rotationZ: Float
        get() = 0f

    /**
     * Offset percentage along the x and y axis for which contents are rotated and scaled.
     * The default value of 0.5f, 0.5f indicates the pivot point will be at the midpoint of the
     * left and right as well as the top and bottom bounds of the layer
     */
    val transformOrigin: TransformOrigin get() = TransformOrigin.Center

    /**
     * The [Shape] of the layer. When [shadowElevation] is non-zero a shadow is produced using
     * this [shape]. When [clip] is `true` contents will be clipped to this [shape].
     */
    val shape: Shape get() = RectangleShape

    /**
     * Set to `true` to clip the content to the [shape].
     */
    val clip: Boolean get() = false
}

private data class SimpleDrawLayerModifier(
    override val scaleX: Float,
    override val scaleY: Float,
    override val alpha: Float,
    override val translationX: Float,
    override val translationY: Float,
    override val shadowElevation: Float,
    override val rotationX: Float,
    override val rotationY: Float,
    override val rotationZ: Float,
    override val transformOrigin: TransformOrigin,
    override val shape: Shape,
    override val clip: Boolean
) : DrawLayerModifier, InspectableValue {
    override val nameFallback: String = "drawLayer"
    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(
            ValueElement("scaleX", scaleX),
            ValueElement("scaleY", scaleY),
            ValueElement("alpha", alpha),
            ValueElement("translationX", translationX),
            ValueElement("translationY", translationY),
            ValueElement("shadowElevation", shadowElevation),
            ValueElement("rotationX", rotationX),
            ValueElement("rotationY", rotationY),
            ValueElement("rotationZ", rotationZ),
            ValueElement("transformOrigin", transformOrigin),
            ValueElement("shape", shape),
            ValueElement("clip", clip)
        )
}

/**
 * Draw the content into a layer. This permits applying special effects and transformations:
 *
 * @sample androidx.compose.ui.samples.ChangeOpacity
 *
 * @param scaleX [DrawLayerModifier.scaleX]
 * @param scaleY [DrawLayerModifier.scaleY]
 * @param alpha [DrawLayerModifier.alpha]
 * @param shadowElevation [DrawLayerModifier.shadowElevation]
 * @param rotationX [DrawLayerModifier.rotationX]
 * @param rotationY [DrawLayerModifier.rotationY]
 * @param rotationZ [DrawLayerModifier.rotationZ]
 * @param shape [DrawLayerModifier.shape]
 * @param clip [DrawLayerModifier.clip]
 */
@Stable
fun Modifier.drawLayer(
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f,
    translationX: Float = 0f,
    translationY: Float = 0f,
    shadowElevation: Float = 0f,
    rotationX: Float = 0f,
    rotationY: Float = 0f,
    rotationZ: Float = 0f,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    shape: Shape = RectangleShape,
    clip: Boolean = false
) = this.then(
    SimpleDrawLayerModifier(
        scaleX = scaleX,
        scaleY = scaleY,
        alpha = alpha,
        translationX = translationX,
        translationY = translationY,
        shadowElevation = shadowElevation,
        rotationX = rotationX,
        rotationY = rotationY,
        rotationZ = rotationZ,
        transformOrigin = transformOrigin,
        shape = shape,
        clip = clip
    )
)