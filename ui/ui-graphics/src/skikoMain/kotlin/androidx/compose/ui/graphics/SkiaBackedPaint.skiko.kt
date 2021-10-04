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

import org.jetbrains.skia.PaintMode as SkPaintMode
import org.jetbrains.skia.PaintStrokeCap as SkPaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin as SkPaintStrokeJoin

actual typealias NativePaint = org.jetbrains.skia.Paint

actual fun Paint(): Paint = SkiaBackedPaint()

/**
 * Convert the [org.jetbrains.skia.Paint] instance into a Compose-compatible Paint
 */
fun org.jetbrains.skia.Paint.asComposePaint(): Paint = SkiaBackedPaint(this)

internal class SkiaBackedPaint(
    val skia: org.jetbrains.skia.Paint = org.jetbrains.skia.Paint()
) : Paint {
    override fun asFrameworkPaint(): NativePaint = skia

    override var alpha: Float
        get() = Color(skia.color).alpha
        set(value) {
            skia.color = Color(skia.color).copy(alpha = value).toArgb()
        }

    override var isAntiAlias: Boolean
        get() = skia.isAntiAlias
        set(value) {
            skia.isAntiAlias = value
        }

    override var color: Color
        get() = Color(skia.color)
        set(color) {
            skia.color = color.toArgb()
        }

    override var blendMode: BlendMode = BlendMode.SrcOver
        set(value) {
            skia.blendMode = value.toSkia()
            field = value
        }

    override var style: PaintingStyle = PaintingStyle.Fill
        set(value) {
            skia.mode = value.toSkia()
            field = value
        }

    override var strokeWidth: Float
        get() = skia.strokeWidth
        set(value) {
            skia.strokeWidth = value
        }

    override var strokeCap: StrokeCap = StrokeCap.Butt
        set(value) {
            skia.strokeCap = value.toSkia()
            field = value
        }

    override var strokeJoin: StrokeJoin = StrokeJoin.Round
        set(value) {
            skia.strokeJoin = value.toSkia()
            field = value
        }

    override var strokeMiterLimit: Float = 0f
        set(value) {
            skia.strokeMiter = value
            field = value
        }

    override var filterQuality: FilterQuality = FilterQuality.Medium

    override var shader: Shader? = null
        set(value) {
            skia.shader = value
            field = value
        }

    override var colorFilter: ColorFilter? = null
        set(value) {
            skia.colorFilter = value?.asSkiaColorFilter()
            field = value
        }

    override var pathEffect: PathEffect? = null
        set(value) {
            skia.pathEffect = (value as SkiaBackedPathEffect?)?.asSkiaPathEffect()
            field = value
        }

    private fun PaintingStyle.toSkia() = when (this) {
        PaintingStyle.Fill -> SkPaintMode.FILL
        PaintingStyle.Stroke -> SkPaintMode.STROKE
        else -> SkPaintMode.FILL
    }

    private fun StrokeCap.toSkia() = when (this) {
        StrokeCap.Butt -> SkPaintStrokeCap.BUTT
        StrokeCap.Round -> SkPaintStrokeCap.ROUND
        StrokeCap.Square -> SkPaintStrokeCap.SQUARE
        else -> SkPaintStrokeCap.BUTT
    }

    private fun StrokeJoin.toSkia() = when (this) {
        StrokeJoin.Miter -> SkPaintStrokeJoin.MITER
        StrokeJoin.Round -> SkPaintStrokeJoin.ROUND
        StrokeJoin.Bevel -> SkPaintStrokeJoin.BEVEL
        else -> SkPaintStrokeJoin.MITER
    }
}

actual fun BlendMode.isSupported(): Boolean = true