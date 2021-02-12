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

import org.jetbrains.skija.PathEffect as SkijaPathEffect

internal class DesktopPathEffect(val nativePathEffect: SkijaPathEffect) : PathEffect

/**
 * Obtain a reference to the desktop PathEffect type
 */
fun PathEffect.asDesktopPathEffect(): SkijaPathEffect =
    (this as DesktopPathEffect).nativePathEffect

internal actual fun actualCornerPathEffect(radius: Float): PathEffect =
    DesktopPathEffect(SkijaPathEffect.makeCorner(radius))

internal actual fun actualDashPathEffect(
    intervals: FloatArray,
    phase: Float
): PathEffect = DesktopPathEffect(SkijaPathEffect.makeDash(intervals, phase))

internal actual fun actualChainPathEffect(outer: PathEffect, inner: PathEffect): PathEffect =
    DesktopPathEffect(outer.asDesktopPathEffect().makeCompose(inner.asDesktopPathEffect()))

internal actual fun actualStampedPathEffect(
    shape: Path,
    advance: Float,
    phase: Float,
    style: StampedPathEffectStyle
): PathEffect =
    DesktopPathEffect(
        SkijaPathEffect.makePath1D(
            shape.asDesktopPath(),
            advance,
            phase,
            style.toSkijaStampedPathEffectStyle()
        )
    )

internal fun StampedPathEffectStyle.toSkijaStampedPathEffectStyle(): SkijaPathEffect.Style =
    when (this) {
        StampedPathEffectStyle.Morph -> SkijaPathEffect.Style.MORPH
        StampedPathEffectStyle.Rotate -> SkijaPathEffect.Style.ROTATE
        StampedPathEffectStyle.Translate -> SkijaPathEffect.Style.TRANSLATE
    }