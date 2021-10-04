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

import org.jetbrains.skia.PathEffect as SkPathEffect

internal class SkiaBackedPathEffect(val nativePathEffect: SkPathEffect) : PathEffect

/**
 * Convert the [org.jetbrains.skia.PathEffect] instance into a Compose-compatible PathEffect
 */
fun SkPathEffect.asComposePathEffect(): PathEffect = SkiaBackedPathEffect(this)

/**
 * Obtain a reference to skia PathEffect type
 */
fun PathEffect.asSkiaPathEffect(): SkPathEffect =
    (this as SkiaBackedPathEffect).nativePathEffect

internal actual fun actualCornerPathEffect(radius: Float): PathEffect =
    SkiaBackedPathEffect(SkPathEffect.makeCorner(radius))

internal actual fun actualDashPathEffect(
    intervals: FloatArray,
    phase: Float
): PathEffect = SkiaBackedPathEffect(SkPathEffect.makeDash(intervals, phase))

internal actual fun actualChainPathEffect(outer: PathEffect, inner: PathEffect): PathEffect =
    SkiaBackedPathEffect(outer.asSkiaPathEffect().makeCompose(inner.asSkiaPathEffect()))

internal actual fun actualStampedPathEffect(
    shape: Path,
    advance: Float,
    phase: Float,
    style: StampedPathEffectStyle
): PathEffect =
    SkiaBackedPathEffect(
        SkPathEffect.makePath1D(
            shape.asSkiaPath(),
            advance,
            phase,
            style.toSkiaStampedPathEffectStyle()
        )
    )

internal fun StampedPathEffectStyle.toSkiaStampedPathEffectStyle(): SkPathEffect.Style =
    when (this) {
        StampedPathEffectStyle.Morph -> SkPathEffect.Style.MORPH
        StampedPathEffectStyle.Rotate -> SkPathEffect.Style.ROTATE
        StampedPathEffectStyle.Translate -> SkPathEffect.Style.TRANSLATE
        else -> SkPathEffect.Style.TRANSLATE
    }