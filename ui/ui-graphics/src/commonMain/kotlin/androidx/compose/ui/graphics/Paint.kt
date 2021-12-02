/*
 * Copyright 2018 The Android Open Source Project
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

/**
 * Default alpha value used on [Paint]. This value will draw source content fully opaque.
 */
const val DefaultAlpha: Float = 1.0f

expect class NativePaint

expect fun Paint(): Paint

interface Paint {
    fun asFrameworkPaint(): NativePaint

    /**
     * Configures the alpha value between 0f to 1f representing fully transparent to fully
     * opaque for the color drawn with this Paint
     */
    var alpha: Float

    /**
     * Whether to apply anti-aliasing to lines and images drawn on the
     * canvas.
     * Defaults to true.
     */
    var isAntiAlias: Boolean

    /**
     * The color to use when stroking or filling a shape.
     * Defaults to opaque black.
     * See also:
     * [style], which controls whether to stroke or fill (or both).
     * [colorFilter], which overrides [color].
     * [shader], which overrides [color] with more elaborate effects.
     * This color is not used when compositing. To colorize a layer, use [colorFilter].
     */
    var color: Color

    /**
     * A blend mode to apply when a shape is drawn or a layer is composited.
     * The source colors are from the shape being drawn (e.g. from
     * [Canvas.drawPath]) or layer being composited (the graphics that were drawn
     * between the [Canvas.saveLayer] and [Canvas.restore] calls), after applying
     * the [colorFilter], if any.
     * The destination colors are from the background onto which the shape or
     * layer is being composited.
     * Defaults to [BlendMode.SrcOver].
     * See also:
     * [Canvas.saveLayer], which uses its [Paint]'s [blendMode] to composite
     * the layer when [Canvas.restore] is called.
     * [BlendMode], which discusses the user of [Canvas.saveLayer] with [blendMode].
     */
    var blendMode: BlendMode

    /**
     * Whether to paint inside shapes, the edges of shapes, or both.
     * Defaults to [PaintingStyle.Fill].
     */
    var style: PaintingStyle

    /**
     * How wide to make edges drawn when [style] is set to
     * [PaintingStyle.Stroke]. The width is given in logical pixels measured in
     * the direction orthogonal to the direction of the path.
     * Defaults to 0.0, which correspond to a hairline width.
     */
    var strokeWidth: Float

    /**
     * The kind of finish to place on the end of lines drawn when
     * [style] is set to [PaintingStyle.Stroke].
     * Defaults to [StrokeCap.Butt], i.e. no caps.
     */
    var strokeCap: StrokeCap

    /**
     * The kind of finish to place on the joins between segments.
     * This applies to paths drawn when [style] is set to [PaintingStyle.Stroke],
     * It does not apply to points drawn as lines with [Canvas.drawPoints].
     * Defaults to [StrokeJoin.Miter], i.e. sharp corners. See also
     * [strokeMiterLimit] to control when miters are replaced by bevels.
     */
    var strokeJoin: StrokeJoin

    /**
     * The limit for miters to be drawn on segments when the join is set to
     * [StrokeJoin.Miter] and the [style] is set to [PaintingStyle.Stroke]. If
     * this limit is exceeded, then a [StrokeJoin.Bevel] join will be drawn
     * instead. This may cause some 'popping' of the corners of a path if the
     * angle between line segments is animated.
     * This limit is expressed as a limit on the length of the miter.
     * Defaults to 4.0.  Using zero as a limit will cause a [StrokeJoin.Bevel]
     * join to be used all the time.
     */
    var strokeMiterLimit: Float

    /**
     * Controls the performance vs quality trade-off to use when applying
     * when drawing images, as with [Canvas.drawImageRect]
     * Defaults to [FilterQuality.Low].
     */
    var filterQuality: FilterQuality

    /**
     * The shader to use when stroking or filling a shape.
     *
     * When this is null, the [color] is used instead.
     *
     * See also:
     * [LinearGradientShader], [RadialGradientShader], or [SweepGradientShader] shaders that
     * paint a color gradient.
     * [ImageShader], a shader that tiles an [ImageBitmap].
     * [colorFilter], which overrides [shader].
     * [color], which is used if [shader] and [colorFilter] are null.
     */
    var shader: Shader?

    /**
     *  A color filter to apply when a shape is drawn or when a layer is
     *  composited.
     *  See [ColorFilter] for details.
     *  When a shape is being drawn, [colorFilter] overrides [color] and [shader].
     */
    var colorFilter: ColorFilter?

    /**
     * Specifies the [PathEffect] applied to the geometry of the shape that is drawn
     */
    var pathEffect: PathEffect?
}
