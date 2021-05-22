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

import android.graphics.PathDashPathEffect

/**
 * Obtain a reference to the Android PathEffect type
 */
internal class AndroidPathEffect(val nativePathEffect: android.graphics.PathEffect) : PathEffect

fun PathEffect.asAndroidPathEffect(): android.graphics.PathEffect =
    (this as AndroidPathEffect).nativePathEffect

fun android.graphics.PathEffect.toComposePathEffect(): PathEffect = AndroidPathEffect(this)

internal actual fun actualCornerPathEffect(radius: Float): PathEffect =
    AndroidPathEffect(android.graphics.CornerPathEffect(radius))

internal actual fun actualDashPathEffect(intervals: FloatArray, phase: Float): PathEffect =
    AndroidPathEffect(android.graphics.DashPathEffect(intervals, phase))

internal actual fun actualChainPathEffect(outer: PathEffect, inner: PathEffect): PathEffect =
    AndroidPathEffect(
        android.graphics.ComposePathEffect(
            (outer as AndroidPathEffect).nativePathEffect,
            (inner as AndroidPathEffect).nativePathEffect
        )
    )

internal actual fun actualStampedPathEffect(
    shape: Path,
    advance: Float,
    phase: Float,
    style: StampedPathEffectStyle
): PathEffect =
    AndroidPathEffect(
        PathDashPathEffect(
            shape.asAndroidPath(),
            advance,
            phase,
            style.toAndroidPathDashPathEffectStyle()
        )
    )

internal fun StampedPathEffectStyle.toAndroidPathDashPathEffectStyle() =
    when (this) {
        StampedPathEffectStyle.Morph -> PathDashPathEffect.Style.MORPH
        StampedPathEffectStyle.Rotate -> PathDashPathEffect.Style.ROTATE
        StampedPathEffectStyle.Translate -> PathDashPathEffect.Style.TRANSLATE
        else -> PathDashPathEffect.Style.TRANSLATE
    }