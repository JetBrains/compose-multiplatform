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

import org.jetbrains.skija.ColorFilter as SkijaColorFilter
import org.jetbrains.skija.FilterQuality as SkijaFilterQuality
import org.jetbrains.skija.PaintMode as SkijaPaintMode
import org.jetbrains.skija.PaintStrokeCap as SkijaPaintStrokeCap
import org.jetbrains.skija.PaintStrokeJoin as SkijaPaintStrokeJoin

actual typealias NativePaint = org.jetbrains.skija.Paint

actual fun Paint(): Paint = DesktopPaint()

class DesktopPaint : Paint {
    internal val skija = org.jetbrains.skija.Paint()

    constructor() {
        filterQuality = FilterQuality.Medium
    }

    override fun asFrameworkPaint(): NativePaint = skija

    override var alpha: Float
        get() = Color(skija.color).alpha
        set(value) {
            skija.color = Color(skija.color).copy(alpha = value).toArgb()
        }

    override var isAntiAlias: Boolean
        get() = skija.isAntiAlias
        set(value) {
            skija.isAntiAlias = value
        }

    override var color: Color
        get() = Color(skija.color)
        set(color) {
            skija.color = color.toArgb()
        }

    override var blendMode: BlendMode = BlendMode.SrcOver
        set(value) {
            skija.blendMode = value.toSkija()
            field = value
        }

    override var style: PaintingStyle = PaintingStyle.Fill
        set(value) {
            skija.mode = value.toSkija()
            field = value
        }

    override var strokeWidth: Float
        get() = skija.strokeWidth
        set(value) {
            skija.strokeWidth = value
        }

    override var strokeCap: StrokeCap = StrokeCap.Butt
        set(value) {
            skija.strokeCap = value.toSkija()
            field = value
        }

    override var strokeJoin: StrokeJoin = StrokeJoin.Round
        set(value) {
            skija.strokeJoin = value.toSkija()
            field = value
        }

    override var strokeMiterLimit: Float = 0f
        set(value) {
            skija.strokeMiter = value
            field = value
        }

    override var filterQuality: FilterQuality = FilterQuality.None
        set(value) {
            skija.filterQuality = value.toSkija()
            field = value
        }

    override var shader: Shader? = null
        set(value) {
            skija.shader = value
            field = value
        }

    override var colorFilter: ColorFilter? = null
        set(value) {
            skija.colorFilter = if (value != null) {
                SkijaColorFilter.makeBlend(
                    value.color.toArgb(),
                    value.blendMode.toSkija()
                )
            } else {
                null
            }
            field = value
        }

    override var nativePathEffect: NativePathEffect?
        get() = pathEffect?.asDesktopPathEffect()
        set(value) {
            pathEffect = if (value == null) {
                null
            } else {
                DesktopPathEffect(value)
            }
        }

    override var pathEffect: PathEffect? = null
        set(value) {
            skija.pathEffect = (value as DesktopPathEffect).asDesktopPathEffect()
            field = value
        }

    private fun PaintingStyle.toSkija() = when (this) {
        PaintingStyle.Fill -> SkijaPaintMode.FILL
        PaintingStyle.Stroke -> SkijaPaintMode.STROKE
    }

    private fun StrokeCap.toSkija() = when (this) {
        StrokeCap.Butt -> SkijaPaintStrokeCap.BUTT
        StrokeCap.Round -> SkijaPaintStrokeCap.ROUND
        StrokeCap.Square -> SkijaPaintStrokeCap.SQUARE
    }

    private fun StrokeJoin.toSkija() = when (this) {
        StrokeJoin.Miter -> SkijaPaintStrokeJoin.MITER
        StrokeJoin.Round -> SkijaPaintStrokeJoin.ROUND
        StrokeJoin.Bevel -> SkijaPaintStrokeJoin.BEVEL
    }

    private fun FilterQuality.toSkija() = when (this) {
        FilterQuality.None -> SkijaFilterQuality.NONE
        FilterQuality.Low -> SkijaFilterQuality.LOW
        FilterQuality.Medium -> SkijaFilterQuality.MEDIUM
        FilterQuality.High -> SkijaFilterQuality.HIGH
    }
}

actual fun BlendMode.isSupported(): Boolean = true