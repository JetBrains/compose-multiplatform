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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.requireCoordinator
import androidx.compose.ui.platform.InspectorInfo
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
    renderEffect = renderEffect,
    compositingStrategy = CompositingStrategy.Auto
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
@OptIn(ExperimentalComposeUiApi::class)
@Deprecated(
    "Replace with graphicsLayer that consumes a compositing strategy",
    replaceWith = ReplaceWith(
        "Modifier.graphicsLayer(scaleX, scaleY, alpha, translationX, translationY, " +
            "shadowElevation, rotationX, rotationY, rotationZ, cameraDistance, transformOrigin, " +
            "shape, clip, null, DefaultShadowColor, DefaultShadowColor, CompositingStrategy.Auto)",
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
    renderEffect: RenderEffect? = null,
    ambientShadowColor: Color = DefaultShadowColor,
    spotShadowColor: Color = DefaultShadowColor,
) = graphicsLayer(
    scaleX,
    scaleY,
    alpha,
    translationX,
    translationY,
    shadowElevation,
    rotationX,
    rotationY,
    rotationZ,
    cameraDistance,
    transformOrigin,
    shape,
    clip,
    renderEffect,
    ambientShadowColor,
    spotShadowColor,
    CompositingStrategy.Auto
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
 * [CompositingStrategy] determines whether or not the contents of this layer are rendered into
 * an offscreen buffer. This is useful in order to optimize alpha usages with
 * [CompositingStrategy.ModulateAlpha] which will skip the overhead of an offscreen buffer but can
 * generate different rendering results depending on whether or not the contents of the layer are
 * overlapping. Similarly leveraging [CompositingStrategy.Offscreen] is useful in situations where
 * creating an offscreen buffer is preferred usually in conjunction with [BlendMode] usage.
 *
 * Note that if you provide a non-zero [shadowElevation] and if the passed [shape] is concave the
 * shadow will not be drawn on Android versions less than 10.
 *
 * Also note that alpha values less than 1.0f will have their contents implicitly clipped to their
 * bounds unless [CompositingStrategy.ModulateAlpha] is specified.
 * This is because an intermediate compositing layer is created to render contents into
 * first before being drawn into the destination with the desired alpha.
 * This layer is sized to the bounds of the composable this modifier is configured on, and contents
 * outside of these bounds are omitted.
 *
 * If the layer parameters are backed by a [androidx.compose.runtime.State] or an animated value
 * prefer an overload with a lambda block on [GraphicsLayerScope] as reading a state inside the block
 * will only cause the layer properties update without triggering recomposition and relayout.
 *
 * @sample androidx.compose.ui.samples.ChangeOpacity
 * @sample androidx.compose.ui.samples.CompositingStrategyModulateAlpha
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
 * @param compositingStrategy see [GraphicsLayerScope.compositingStrategy]
 */
@OptIn(ExperimentalComposeUiApi::class)
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
    compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
) = this then GraphicsLayerModifierNodeElement(
    scaleX,
    scaleY,
    alpha,
    translationX,
    translationY,
    shadowElevation,
    rotationX,
    rotationY,
    rotationZ,
    cameraDistance,
    transformOrigin,
    shape,
    clip,
    renderEffect,
    ambientShadowColor,
    spotShadowColor,
    compositingStrategy
)

@ExperimentalComposeUiApi
private data class GraphicsLayerModifierNodeElement(
    val scaleX: Float,
    val scaleY: Float,
    val alpha: Float,
    val translationX: Float,
    val translationY: Float,
    val shadowElevation: Float,
    val rotationX: Float,
    val rotationY: Float,
    val rotationZ: Float,
    val cameraDistance: Float,
    val transformOrigin: TransformOrigin,
    val shape: Shape,
    val clip: Boolean,
    val renderEffect: RenderEffect?,
    val ambientShadowColor: Color,
    val spotShadowColor: Color,
    val compositingStrategy: CompositingStrategy
) : ModifierNodeElement<SimpleGraphicsLayerModifier>() {
    override fun create(): SimpleGraphicsLayerModifier {
        return SimpleGraphicsLayerModifier(
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
            renderEffect = renderEffect,
            ambientShadowColor = ambientShadowColor,
            spotShadowColor = spotShadowColor,
            compositingStrategy = compositingStrategy
        )
    }

    override fun update(node: SimpleGraphicsLayerModifier): SimpleGraphicsLayerModifier {
        node.scaleX = scaleX
        node.scaleY = scaleY
        node.alpha = alpha
        node.translationX = translationX
        node.translationY = translationY
        node.shadowElevation = shadowElevation
        node.rotationX = rotationX
        node.rotationY = rotationY
        node.rotationZ = rotationZ
        node.cameraDistance = cameraDistance
        node.transformOrigin = transformOrigin
        node.shape = shape
        node.clip = clip
        node.renderEffect = renderEffect
        node.ambientShadowColor = ambientShadowColor
        node.spotShadowColor = spotShadowColor
        node.compositingStrategy = compositingStrategy
        node.invalidateLayerBlock()

        return node
    }

    override fun InspectorInfo.inspectableProperties() {
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
        properties["compositingStrategy"] = compositingStrategy
    }
}

/**
 * A [Modifier.Node] that makes content draw into a draw layer. The draw layer can be
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
    this then BlockGraphicsLayerElement(block)

/**
 * Determines when to render the contents of a layer into an offscreen buffer before
 * being drawn to the destination.
 */
@Immutable
@kotlin.jvm.JvmInline
value class CompositingStrategy internal constructor(
    @Suppress("unused") private val value: Int
) {

    companion object {

        /**
         * Rendering to an offscreen buffer will be determined automatically by the rest of the
         * graphicsLayer parameters. This is the default behavior.
         * For example, whenever an alpha value less than 1.0f is provided on [Modifier.graphicsLayer],
         * a compositing layer is created automatically to first render the contents fully opaque,
         * then draw this offscreen buffer to the destination with the corresponding alpha. This is
         * necessary for correctness otherwise alpha applied to individual drawing instructions that
         * overlap will have a different result than expected. Additionally usage of [RenderEffect]
         * on the graphicsLayer will also render into an intermediate offscreen buffer before
         * being drawn into the destination.
         */
        val Auto = CompositingStrategy(0)

        /**
         * Rendering of content will always be rendered into an offscreen buffer first then drawn to
         * the destination regardless of the other parameters configured on the graphics
         * layer. This is useful for leveraging different blending algorithms for masking content.
         * For example, the contents can be drawn into this graphics layer and masked out by drawing
         * additional shapes with [BlendMode.Clear]
         */
        val Offscreen = CompositingStrategy(1)

        /**
         * Modulates alpha for each of the drawing instructions recorded within the graphicsLayer.
         * This avoids usage of an offscreen buffer for purposes of alpha rendering.
         * [ModulateAlpha] is more efficient than [Auto] in performance in scenarios where an alpha
         * value less than 1.0f is provided. Otherwise the performance is similar to that of [Auto].
         * However, this can provide different results than [Auto] if there is overlapping content
         * within the layer and alpha is applied. This should only be used if the contents of the layer
         * are known well in advance and are expected to not be overlapping.
         */
        val ModulateAlpha = CompositingStrategy(2)
    }
}

/**
 * A [Modifier.Element] that adds a draw layer such that tooling can identify an element
 * in the drawn image.
 */
@Stable
fun Modifier.toolingGraphicsLayer() =
    if (isDebugInspectorInfoEnabled) this.then(Modifier.graphicsLayer()) else this

@OptIn(ExperimentalComposeUiApi::class)
private data class BlockGraphicsLayerElement(
    val block: GraphicsLayerScope.() -> Unit
) : ModifierNodeElement<BlockGraphicsLayerModifier>() {
    override fun create() = BlockGraphicsLayerModifier(block)

    override fun update(node: BlockGraphicsLayerModifier) = node.apply {
        layerBlock = block
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "graphicsLayer"
        properties["block"] = block
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class BlockGraphicsLayerModifier(
    var layerBlock: GraphicsLayerScope.() -> Unit,
) : LayoutModifierNode, Modifier.Node() {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    override fun toString(): String =
        "BlockGraphicsLayerModifier(" +
            "block=$layerBlock)"
}

@OptIn(ExperimentalComposeUiApi::class)
private class SimpleGraphicsLayerModifier(
    var scaleX: Float,
    var scaleY: Float,
    var alpha: Float,
    var translationX: Float,
    var translationY: Float,
    var shadowElevation: Float,
    var rotationX: Float,
    var rotationY: Float,
    var rotationZ: Float,
    var cameraDistance: Float,
    var transformOrigin: TransformOrigin,
    var shape: Shape,
    var clip: Boolean,
    var renderEffect: RenderEffect?,
    var ambientShadowColor: Color,
    var spotShadowColor: Color,
    var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
) : LayoutModifierNode, Modifier.Node() {

    private var layerBlock: GraphicsLayerScope.() -> Unit = {
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
        compositingStrategy = this@SimpleGraphicsLayerModifier.compositingStrategy
    }

    fun invalidateLayerBlock() {
        requireCoordinator(Nodes.Layout).wrapped?.updateLayerBlock(
            this.layerBlock,
            forceLayerInvalidated = true
        )
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
            "spotShadowColor=$spotShadowColor, " +
            "compositingStrategy=$compositingStrategy)"
}
