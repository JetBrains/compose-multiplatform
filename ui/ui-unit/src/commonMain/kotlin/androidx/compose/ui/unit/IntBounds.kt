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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect

/**
 * A four dimensional bounds holder defined by integer pixels.
 */
@Immutable
data class IntBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

@Stable
inline fun IntBounds(topLeft: IntOffset, size: IntSize) =
    IntBounds(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height
    )

/**
 * The width of this IntBounds in integer pixels.
 */
@Stable
inline val IntBounds.width: Int get() = right - left

/**
 * The height of this IntBounds in integer pixels.
 */
@Stable
inline val IntBounds.height: Int get() = bottom - top

/**
 * Returns the [IntOffset] of the center of the [IntBounds].
 */
@Stable
inline fun IntBounds.center(): IntOffset {
    return IntOffset((left + right) / 2, (top + bottom) / 2)
}

/**
 * Convert an [IntBounds] to an [IntSize].
 */
@Stable
fun IntBounds.toSize(): IntSize {
    return IntSize(width, height)
}

/**
 * Convert an [IntSize] to an [IntBounds]. The left and top are 0 and the right and bottom
 * are the width and height, respectively.
 */
@Stable
fun IntSize.toBounds(): IntBounds {
    return IntBounds(0, 0, width, height)
}

/**
 * Convert an [IntBounds] to a [Rect].
 */
@Stable
fun IntBounds.toRect(): Rect {
    return Rect(
        left.toFloat(),
        top.toFloat(),
        right.toFloat(),
        bottom.toFloat()
    )
}