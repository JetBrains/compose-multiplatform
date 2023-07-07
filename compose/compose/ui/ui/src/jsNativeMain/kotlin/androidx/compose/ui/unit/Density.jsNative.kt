/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.unit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified

/**
 * Convert a [Offset] to a [DpOffset].
 */
@Stable
internal fun Offset.toDpOffset(density: Density): DpOffset = with(density) {
    if (isSpecified) {
        DpOffset(x.toDp(), y.toDp())
    } else {
        DpOffset.Unspecified
    }
}

/**
 * Convert a [DpOffset] to a [Offset].
 */
@Stable
internal fun DpOffset.toOffset(density: Density): Offset = with(density) {
    if (isSpecified) {
        Offset(x.toPx(), y.toPx())
    } else {
        Offset.Unspecified
    }
}

/**
 * Convert a [Rect] to a [DpRect].
 */
@Stable
internal fun Rect.toDpRect(density: Density): DpRect = with(density) {
    DpRect(
        origin = topLeft.toDpOffset(density),
        size = size.toDpSize()
    )
}
