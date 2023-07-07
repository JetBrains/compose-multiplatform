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

package androidx.compose.animation.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

private const val DpVisibilityThreshold = 0.1f
private const val PxVisibilityThreshold = 0.5f

private val rectVisibilityThreshold = Rect(
    PxVisibilityThreshold,
    PxVisibilityThreshold,
    PxVisibilityThreshold,
    PxVisibilityThreshold
)

/**
 * Visibility threshold for [IntOffset]. This defines the amount of value change that is
 * considered to be no longer visible. The animation system uses this to signal to some default
 * [spring] animations to stop when the value is close enough to the target.
 */
val IntOffset.Companion.VisibilityThreshold: IntOffset
    get() = IntOffset(1, 1)

/**
 * Visibility threshold for [Offset]. This defines the amount of value change that is
 * considered to be no longer visible. The animation system uses this to signal to some default
 * [spring] animations to stop when the value is close enough to the target.
 */
val Offset.Companion.VisibilityThreshold: Offset
    get() = Offset(PxVisibilityThreshold, PxVisibilityThreshold)

/**
 * Visibility threshold for [Int]. This defines the amount of value change that is
 * considered to be no longer visible. The animation system uses this to signal to some default
 * [spring] animations to stop when the value is close enough to the target.
 */
val Int.Companion.VisibilityThreshold: Int
    get() = 1

/**
 * Visibility threshold for [Dp]. This defines the amount of value change that is
 * considered to be no longer visible. The animation system uses this to signal to some default
 * [spring] animations to stop when the value is close enough to the target.
 */
val Dp.Companion.VisibilityThreshold: Dp
    get() = DpVisibilityThreshold.dp

/**
 * Visibility threshold for [DpOffset]. This defines the amount of value change that is
 * considered to be no longer visible. The animation system uses this to signal to some default
 * [spring] animations to stop when the value is close enough to the target.
 */
val DpOffset.Companion.VisibilityThreshold: DpOffset
    get() = DpOffset(Dp.VisibilityThreshold, Dp.VisibilityThreshold)

/**
 * Visibility threshold for [Size]. This defines the amount of value change that is
 * considered to be no longer visible. The animation system uses this to signal to some default
 * [spring] animations to stop when the value is close enough to the target.
 */
val Size.Companion.VisibilityThreshold: Size
    get() = Size(PxVisibilityThreshold, PxVisibilityThreshold)

/**
 * Visibility threshold for [IntSize]. This defines the amount of value change that is
 * considered to be no longer visible. The animation system uses this to signal to some default
 * [spring] animations to stop when the value is close enough to the target.
 */
val IntSize.Companion.VisibilityThreshold: IntSize
    get() = IntSize(1, 1)

/**
 * Visibility threshold for [Rect]. This defines the amount of value change that is
 * considered to be no longer visible. The animation system uses this to signal to some default
 * [spring] animations to stop when the value is close enough to the target.
 */
val Rect.Companion.VisibilityThreshold: Rect
    get() = rectVisibilityThreshold

// TODO: Add Dp.DefaultAnimation = spring<Dp>(visibilityThreshold = Dp.VisibilityThreshold)

internal val visibilityThresholdMap: Map<TwoWayConverter<*, *>, Float> = mapOf(
    Int.VectorConverter to 1f,
    IntSize.VectorConverter to 1f,
    IntOffset.VectorConverter to 1f,
    Float.VectorConverter to 0.01f,
    Rect.VectorConverter to PxVisibilityThreshold,
    Size.VectorConverter to PxVisibilityThreshold,
    Offset.VectorConverter to PxVisibilityThreshold,
    Dp.VectorConverter to DpVisibilityThreshold,
    DpOffset.VectorConverter to DpVisibilityThreshold
)