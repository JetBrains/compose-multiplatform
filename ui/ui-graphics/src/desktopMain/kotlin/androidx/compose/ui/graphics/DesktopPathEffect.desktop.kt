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

import org.jetbrains.skia.PathEffect as SkiaPathEffect

internal class DesktopPathEffect(val nativePathEffect: SkiaPathEffect) : PathEffect

/**
 * Convert the [org.jetbrains.skia.PathEffect] instance into a Compose-compatible PathEffect
 */
fun SkiaPathEffect.asComposePathEffect(): PathEffect = DesktopPathEffect(this)

/**
 * Obtain a reference to the desktop PathEffect type
 */
@Deprecated("Use asSkiaPathEffect()", replaceWith = ReplaceWith("asSkiaPathEffect()"))
fun PathEffect.asDesktopPathEffect(): SkiaPathEffect = asSkiaPathEffect()

/**
 * Obtain a reference to the desktop PathEffect type
 */
fun PathEffect.asSkiaPathEffect(): SkiaPathEffect =
    (this as DesktopPathEffect).nativePathEffect

internal actual fun actualCornerPathEffect(radius: Float): PathEffect =
    DesktopPathEffect(SkiaPathEffect.makeCorner(radius))

internal actual fun actualDashPathEffect(
    intervals: FloatArray,
    phase: Float
): PathEffect = DesktopPathEffect(SkiaPathEffect.makeDash(intervals, phase))

internal actual fun actualChainPathEffect(outer: PathEffect, inner: PathEffect): PathEffect =
    DesktopPathEffect(outer.asSkiaPathEffect().makeCompose(inner.asSkiaPathEffect()))

internal actual fun actualStampedPathEffect(
    shape: Path,
    advance: Float,
    phase: Float,
    style: StampedPathEffectStyle
): PathEffect =
    DesktopPathEffect(
        SkiaPathEffect.makePath1D(
            shape.asSkiaPath(),
            advance,
            phase,
            style.toSkiaStampedPathEffectStyle()
        )
    )

internal fun StampedPathEffectStyle.toSkiaStampedPathEffectStyle(): SkiaPathEffect.Style =
    when (this) {
        StampedPathEffectStyle.Morph -> SkiaPathEffect.Style.MORPH
        StampedPathEffectStyle.Rotate -> SkiaPathEffect.Style.ROTATE
        StampedPathEffectStyle.Translate -> SkiaPathEffect.Style.TRANSLATE
        else -> SkiaPathEffect.Style.TRANSLATE
    }