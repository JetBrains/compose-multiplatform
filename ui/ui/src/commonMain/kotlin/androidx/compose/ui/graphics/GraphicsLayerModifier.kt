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

package androidx.compose.ui.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Constraints

/**
 * A [Modifier.Element] that makes content draw into a draw layer. The draw layer can be
 * invalidated separately from parents. A [graphicsLayer] should be used when the content
 * updates independently from anything above it to minimize the invalidated content.
 *
 * [graphicsLayer] can also be used to apply effects to content, such as scaling ([scaleX], [scaleY]),
 * rotation ([rotationX], [rotationY], [rotationZ]), opacity ([alpha]), shadow
 * ([shadowElevation], [shape]), and clipping ([clip], [shape]).
 *
 * Note that if you provide a non-zero [shadowElevation] and if the passed [shape] is concave the
 * shadow will not be drawn on Android versions less than 10.
 *
 * Also note that alpha values less than 1.0f will have their contents implicitly clipped to their
 * bounds. This is because an intermediate compositing layer is created to render contents into
 * first before being drawn into the destination with the desired alpha.
 * This layer is sized to the bounds of the composable this modifier is configured on, and contents
 * outside of these bounds are omitted.
 *
 * If the layer parameters are backed by a [androidx.compose.runtime.State] or an animated value
 * prefer an overload with a lambda block on [GraphicsLayerScope] as reading a state inside the block
 * will only cause the layer properties update without triggering recomposition and relayout.
 *
 * @sample androidx.compose.ui.samples.ChangeOpacity
 *
 * @param scaleX see [GraphicsLayerScope.scaleX]
 * @param scaleY see [GraphicsLayerScope.scaleY]
 * @param alpha see [GraphicsLayerScope.alpha]
 * @param translationX see [GraphicsLayerScope.translationX]
 * @param translationY see [GraphicsLayerScope.translationY]
 * @param shadowElevation see [GraphicsLayerScope.shadowElevation]
 * @param rotationX see [GraphicsLayerScope.rotationX]
 * @param rotationY see [GraphicsLayerScope.rotationY]
 * @param rotationZ see [GraphicsLayerScope.rotationZ]
 * @param cameraDistance see [GraphicsLayerScope.cameraDistance]
 * @param transformOrigin see [GraphicsLayerScope.transformOrigin]
 * @param shape see [GraphicsLayerScope.shape]
 * @param clip see [GraphicsLayerScope.clip]
 */
@Deprecated(
    "Replace with graphicsLayer that consumes an optional RenderEffect parameter and " +
        "shadow color parameters",
    replaceWith = ReplaceWith(
        "Modifier.graphicsLayer(scaleX, scaleY, alpha, translationX, translationY, " +
            "shadowElevation, rotationX, rotationY, rotationZ, cameraDistance, transformOrigin, " +
            "shape, clip, null, DefaultShadowColor, DefaultShadowColor)",
        "androidx.compose.ui.graphics"
    ),
    level = DeprecationLevel.HIDDEN
)
@Stable
fun Modifier.graphicsLayer(
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f,
    translationX: Float = 0f,
    translationY: Float = 0f,
    shadowElevation: Float = 0f,
    rotationX: Float = 0f,
    rotationY: Float = 0f,
    rotationZ: Float = 0f,
    cameraDistance: Float = DefaultCameraDistance,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    shape: Shape = RectangleShape,
    clip: Boolean = false
) = graphicsLayer(
    scaleX = scaleX,
    scaleY = scaleY,
    alpha = alpha,
    translationX = translationX,
    translationY = translationY,
    shadowElevation = shadowElevation,
    rotationX = rotationX,
    rotationY = rotationY,
    rotationZ = rotationZ,
    cameraDistance = cameraDistance,
    transformOrigin = transformOrigin,
    shape = shape,
    clip = clip,
    renderEffect = null
)

/**
 * A [Modifier.Element] that makes content draw into a draw layer. The draw layer can be
 * invalidated separately from parents. A [graphicsLayer] should be used when the content
 * updates independently from anything above it to minimize the invalidated content.
 *
 * [graphicsLayer] can also be used to apply effects to content, such as scaling ([scaleX], [scaleY]),
 * rotation ([rotationX], [rotationY], [rotationZ]), opacity ([alpha]), shadow
 * ([shadowElevation], [shape]), clipping ([clip], [shape]), as well as altering the result of the
 * layer with [RenderEffect].
 *
 * Note that if you provide a non-zero [shadowElevation] and if the passed [shape] is concave the
 * shadow will not be drawn on Android versions less than 10.
 *
 * Also note that alpha values less than 1.0f will have their contents implicitly clipped to their
 * bounds. This is because an intermediate compositing layer is created to render contents into
 * first before being drawn into the destination with the desired alpha.
 * This layer is sized to the bounds of the composable this modifier is configured on, and contents
 * outside of these bounds are omitted.
 *
 * If the layer parameters are backed by a [androidx.compose.runtime.State] or an animated value
 * prefer an overload with a lambda block on [GraphicsLayerScope] as reading a state inside the block
 * will only cause the layer properties update without triggering recomposition and relayout.
 *
 * @sample androidx.compose.ui.samples.ChangeOpacity
 *
 * @param scaleX see [GraphicsLayerScope.scaleX]
 * @param scaleY see [GraphicsLayerScope.scaleY]
 * @param alpha see [GraphicsLayerScope.alpha]
 * @param translationX see [GraphicsLayerScope.translationX]
 * @param translationY see [GraphicsLayerScope.translationY]
 * @param shadowElevation see [GraphicsLayerScope.shadowElevation]
 * @param rotationX see [GraphicsLayerScope.rotationX]
 * @param rotationY see [GraphicsLayerScope.rotationY]
 * @param rotationZ see [GraphicsLayerScope.rotationZ]
 * @param cameraDistance see [GraphicsLayerScope.cameraDistance]
 * @param transformOrigin see [GraphicsLayerScope.transformOrigin]
 * @param shape see [GraphicsLayerScope.shape]
 * @param clip see [GraphicsLayerScope.clip]
 * @param renderEffect see [GraphicsLayerScope.renderEffect]
 */
@Deprecated(
    "Replace with graphicsLayer that consumes shadow color parameters",
    replaceWith = ReplaceWith(
        "Modifier.graphicsLayer(scaleX, scaleY, alpha, translationX, translationY, " +
            "shadowElevation, rotationX, rotationY, rotationZ, cameraDistance, transformOrigin, " +
            "shape, clip, null, DefaultShadowColor, DefaultShadowColor)",
        "androidx.compose.ui.graphics"
    ),
    level = DeprecationLevel.HIDDEN
)
@Stable
fun Modifier.graphicsLayer(
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f,
    translationX: Float = 0f,
    translationY: Float = 0f,
    shadowElevation: Float = 0f,
    rotationX: Float = 0f,
    rotationY: Float = 0f,
    rotationZ: Float = 0f,
    cameraDistance: Float = DefaultCameraDistance,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    shape: Shape = RectangleShape,
    clip: Boolean = false,
    renderEffect: RenderEffect? = null
) = graphicsLayer(
    scaleX = scaleX,
    scaleY = scaleY,
    alpha = alpha,
    translationX = translationX,
    translationY = translationY,
    shadowElevation = shadowElevation,
    ambientShadowColor = DefaultShadowColor,
    spotShadowColor = DefaultShadowColor,
    rotationX = rotationX,
    rotationY = rotationY,
    rotationZ = rotationZ,
    cameraDistance = cameraDistance,
    transformOrigin = transformOrigin,
    shape = shape,
    clip = clip,
    renderEffect = renderEffect
)

/**
 * A [Modifier.Element] that makes content draw into a draw layer. The draw layer can be
 * invalidated separately from parents. A [graphicsLayer] should be used when the content
 * updates independently from anything above it to minimize the invalidated content.
 *
 * [graphicsLayer] can also be used to apply effects to content, such as scaling ([scaleX], [scaleY]),
 * rotation ([rotationX], [rotationY], [rotationZ]), opacity ([alpha]), shadow
 * ([shadowElevation], [shape]), clipping ([clip], [shape]), as well as altering the result of the
 * layer with [RenderEffect]. Shadow color and ambient colors can be modified by configuring the
 * [spotShadowColor] and [ambientShadowColor] respectively.
 *
 * Note that if you provide a non-zero [shadowElevation] and if the passed [shape] is concave the
 * shadow will not be drawn on Android versions less than 10.
 *
 * Also note that alpha values less than 1.0f will have their contents implicitly clipped to their
 * bounds. This is because an intermediate compositing layer is created to render contents into
 * first before being drawn into the destination with the desired alpha.
 * This layer is sized to the bounds of the composable this modifier is configured on, and contents
 * outside of these bounds are omitted.
 *
 * If the layer parameters are backed by a [androidx.compose.runtime.State] or an animated value
 * prefer an overload with a lambda block on [GraphicsLayerScope] as reading a state inside the block
 * will only cause the layer properties update without triggering recomposition and relayout.
 *
 * @sample androidx.compose.ui.samples.ChangeOpacity
 *
 * @param scaleX see [GraphicsLayerScope.scaleX]
 * @param scaleY see [GraphicsLayerScope.scaleY]
 * @param alpha see [GraphicsLayerScope.alpha]
 * @param translationX see [GraphicsLayerScope.translationX]
 * @param translationY see [GraphicsLayerScope.translationY]
 * @param shadowElevation see [GraphicsLayerScope.shadowElevation]
 * @param rotationX see [GraphicsLayerScope.rotationX]
 * @param rotationY see [GraphicsLayerScope.rotationY]
 * @param rotationZ see [GraphicsLayerScope.rotationZ]
 * @param cameraDistance see [GraphicsLayerScope.cameraDistance]
 * @param transformOrigin see [GraphicsLayerScope.transformOrigin]
 * @param shape see [GraphicsLayerScope.shape]
 * @param clip see [GraphicsLayerScope.clip]
 * @param renderEffect see [GraphicsLayerScope.renderEffect]
 * @param ambientShadowColor see [GraphicsLayerScope.ambientShadowColor]
 * @param spotShadowColor see [GraphicsLayerScope.spotShadowColor]
 */
@Stable
fun Modifier.graphicsLayer(
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f,
    translationX: Float = 0f,
    translationY: Float = 0f,
    shadowElevation: Float = 0f,
    rotationX: Float = 0f,
    rotationY: Float = 0f,
    rotationZ: Float = 0f,
    cameraDistance: Float = DefaultCameraDistance,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    shape: Shape = RectangleShape,
    clip: Boolean = false,
    renderEffect: RenderEffect? = null,
    ambientShadowColor: Color = DefaultShadowColor,
    spotShadowColor: Color = DefaultShadowColor,
) = this.then(
    SimpleGraphicsLayerModifier(
        scaleX = scaleX,
        scaleY = scaleY,
        alpha = alpha,
        translationX = translationX,
        translationY = translationY,
        shadowElevation = shadowElevation,
        ambientShadowColor = ambientShadowColor,
        spotShadowColor = spotShadowColor,
        rotationX = rotationX,
        rotationY = rotationY,
        rotationZ = rotationZ,
        cameraDistance = cameraDistance,
        transformOrigin = transformOrigin,
        shape = shape,
        clip = clip,
        renderEffect = renderEffect,
        inspectorInfo = debugInspectorInfo {
            name = "graphicsLayer"
            properties["scaleX"] = scaleX
            properties["scaleY"] = scaleY
            properties["alpha"] = alpha
            properties["translationX"] = translationX
            properties["translationY"] = translationY
            properties["shadowElevation"] = shadowElevation
            properties["rotationX"] = rotationX
            properties["rotationY"] = rotationY
            properties["rotationZ"] = rotationZ
            properties["cameraDistance"] = cameraDistance
            properties["transformOrigin"] = transformOrigin
            properties["shape"] = shape
            properties["clip"] = clip
            properties["renderEffect"] = renderEffect
            properties["ambientShadowColor"] = ambientShadowColor
            properties["spotShadowColor"] = spotShadowColor
        }
    )
)

/**
 * A [Modifier.Element] that makes content draw into a draw layer. The draw layer can be
 * invalidated separately from parents. A [graphicsLayer] should be used when the content
 * updates independently from anything above it to minimize the invalidated content.
 *
 * [graphicsLayer] can be used to apply effects to content, such as scaling, rotation, opacity,
 * shadow, and clipping.
 * Prefer this version when you have layer properties backed by a
 * [androidx.compose.runtime.State] or an animated value as reading a state inside [block] will
 * only cause the layer properties update without triggering recomposition and relayout.
 *
 * @sample androidx.compose.ui.samples.AnimateFadeIn
 *
 * @param block block on [GraphicsLayerScope] where you define the layer properties.
 */
@Stable
fun Modifier.graphicsLayer(block: GraphicsLayerScope.() -> Unit): Modifier =
    this.then(
        BlockGraphicsLayerModifier(
            layerBlock = block,
            inspectorInfo = debugInspectorInfo {
                name = "graphicsLayer"
                properties["block"] = block
            }
        )
    )

/**
 * A [Modifier.Element] that adds a draw layer such that tooling can identify an element
 * in the drawn image.
 */
@Stable
fun Modifier.toolingGraphicsLayer() =
    if (isDebugInspectorInfoEnabled) this.then(Modifier.graphicsLayer()) else this

private class BlockGraphicsLayerModifier(
    private val layerBlock: GraphicsLayerScope.() -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BlockGraphicsLayerModifier) return false
        return layerBlock == other.layerBlock
    }

    override fun hashCode(): Int {
        return layerBlock.hashCode()
    }

    override fun toString(): String =
        "BlockGraphicsLayerModifier(" +
            "block=$layerBlock)"
}

private class SimpleGraphicsLayerModifier(
    private val scaleX: Float,
    private val scaleY: Float,
    private val alpha: Float,
    private val translationX: Float,
    private val translationY: Float,
    private val shadowElevation: Float,
    private val rotationX: Float,
    private val rotationY: Float,
    private val rotationZ: Float,
    private val cameraDistance: Float,
    private val transformOrigin: TransformOrigin,
    private val shape: Shape,
    private val clip: Boolean,
    private val renderEffect: RenderEffect?,
    private val ambientShadowColor: Color,
    private val spotShadowColor: Color,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        scaleX = this@SimpleGraphicsLayerModifier.scaleX
        scaleY = this@SimpleGraphicsLayerModifier.scaleY
        alpha = this@SimpleGraphicsLayerModifier.alpha
        translationX = this@SimpleGraphicsLayerModifier.translationX
        translationY = this@SimpleGraphicsLayerModifier.translationY
        shadowElevation = this@SimpleGraphicsLayerModifier.shadowElevation
        rotationX = this@SimpleGraphicsLayerModifier.rotationX
        rotationY = this@SimpleGraphicsLayerModifier.rotationY
        rotationZ = this@SimpleGraphicsLayerModifier.rotationZ
        cameraDistance = this@SimpleGraphicsLayerModifier.cameraDistance
        transformOrigin = this@SimpleGraphicsLayerModifier.transformOrigin
        shape = this@SimpleGraphicsLayerModifier.shape
        clip = this@SimpleGraphicsLayerModifier.clip
        renderEffect = this@SimpleGraphicsLayerModifier.renderEffect
        ambientShadowColor = this@SimpleGraphicsLayerModifier.ambientShadowColor
        spotShadowColor = this@SimpleGraphicsLayerModifier.spotShadowColor
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    override fun hashCode(): Int {
        var result = scaleX.hashCode()
        result = 31 * result + scaleY.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + translationX.hashCode()
        result = 31 * result + translationY.hashCode()
        result = 31 * result + shadowElevation.hashCode()
        result = 31 * result + rotationX.hashCode()
        result = 31 * result + rotationY.hashCode()
        result = 31 * result + rotationZ.hashCode()
        result = 31 * result + cameraDistance.hashCode()
        result = 31 * result + transformOrigin.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + clip.hashCode()
        result = 31 * result + renderEffect.hashCode()
        result = 31 * result + ambientShadowColor.hashCode()
        result = 31 * result + spotShadowColor.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? SimpleGraphicsLayerModifier ?: return false
        return scaleX == otherModifier.scaleX &&
            scaleY == otherModifier.scaleY &&
            alpha == otherModifier.alpha &&
            translationX == otherModifier.translationX &&
            translationY == otherModifier.translationY &&
            shadowElevation == otherModifier.shadowElevation &&
            rotationX == otherModifier.rotationX &&
            rotationY == otherModifier.rotationY &&
            rotationZ == otherModifier.rotationZ &&
            cameraDistance == otherModifier.cameraDistance &&
            transformOrigin == otherModifier.transformOrigin &&
            shape == otherModifier.shape &&
            clip == otherModifier.clip &&
            renderEffect == otherModifier.renderEffect &&
            ambientShadowColor == otherModifier.ambientShadowColor &&
            spotShadowColor == otherModifier.spotShadowColor
    }

    override fun toString(): String =
        "SimpleGraphicsLayerModifier(" +
            "scaleX=$scaleX, " +
            "scaleY=$scaleY, " +
            "alpha = $alpha, " +
            "translationX=$translationX, " +
            "translationY=$translationY, " +
            "shadowElevation=$shadowElevation, " +
            "rotationX=$rotationX, " +
            "rotationY=$rotationY, " +
            "rotationZ=$rotationZ, " +
            "cameraDistance=$cameraDistance, " +
            "transformOrigin=$transformOrigin, " +
            "shape=$shape, " +
            "clip=$clip, " +
            "renderEffect=$renderEffect, " +
            "ambientShadowColor=$ambientShadowColor, " +
            "spotShadowColor=$spotShadowColor)"
}
