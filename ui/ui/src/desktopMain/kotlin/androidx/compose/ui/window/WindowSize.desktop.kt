/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

/**
 * Constructs an [WindowSize] from [width] and [height] [Dp] values.
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Use DpSize",
    replaceWith = ReplaceWith("DpSize(width, height)", "androidx.compose.ui.unit.DpSize")
)
fun WindowSize(
    /**
     * The width of the window in [Dp]. If it is [Dp.Unspecified] then the width of the window
     * will determined by the inner content.
     */
    width: Dp,

    /**
     * The height of the window in [Dp]. If it is [Dp.Unspecified] then the height of the window
     * will determined by the inner content.
     */
    height: Dp
) = WindowSize(packFloats(width.value, height.value))

/**
 * Size of the window or dialog in [Dp].
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
@Immutable
@Deprecated("Use DpSize", replaceWith = ReplaceWith("DpSize", "androidx.compose.ui.unit.DpSize"))
inline class WindowSize internal constructor(@PublishedApi internal val packedValue: Long) {
    /**
     * `true` if the window size has specified values
     *
     * `false` if the window size are not yet determined ([width] or [height] are [Dp.Unspecified])
     */
    val isSpecified: Boolean get() = width.isSpecified && height.isSpecified

    /**
     * The width of the window in [Dp]. If it is [Dp.Unspecified] then the width of the window
     * will determined by the inner content.
     */
    @Stable
    val width: Dp
        get() = unpackFloat1(packedValue).dp

    /**
     * The height of the window in [Dp]. If it is [Dp.Unspecified] then the height of the window
     * will determined by the inner content.
     */
    @Stable
    val height: Dp
        get() = unpackFloat2(packedValue).dp

    @Stable
    operator fun component1(): Dp = width

    @Stable
    operator fun component2(): Dp = height

    /**
     * Returns a copy of this [WindowSize] instance optionally overriding the
     * [width] or [height] parameter.
     */
    @Suppress("DEPRECATION")
    fun copy(
        width: Dp = this.width,
        height: Dp = this.height
    ): WindowSize = WindowSize(width, height)

    @Stable
    override fun toString() = "$width x $height"
}