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

package androidx.compose.ui.graphics

import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuffColorFilter
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

internal actual typealias NativeColorFilter = android.graphics.ColorFilter

/**
 * Obtain a [android.graphics.ColorFilter] instance from this [ColorFilter]
 */
fun ColorFilter.asAndroidColorFilter(): android.graphics.ColorFilter = nativeColorFilter

/**
 * Create a [ColorFilter] from the given [android.graphics.ColorFilter] instance
 */
fun android.graphics.ColorFilter.asComposeColorFilter(): ColorFilter = ColorFilter(this)

internal actual fun actualTintColorFilter(color: Color, blendMode: BlendMode): ColorFilter {
    val androidColorFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        BlendModeColorFilterHelper.BlendModeColorFilter(color, blendMode)
    } else {
        PorterDuffColorFilter(color.toArgb(), blendMode.toPorterDuffMode())
    }
    return ColorFilter(androidColorFilter)
}

internal actual fun actualColorMatrixColorFilter(colorMatrix: ColorMatrix): ColorFilter =
    ColorFilter(android.graphics.ColorMatrixColorFilter(colorMatrix.values))

internal actual fun actualLightingColorFilter(multiply: Color, add: Color): ColorFilter =
    ColorFilter(android.graphics.LightingColorFilter(multiply.toArgb(), add.toArgb()))

@RequiresApi(Build.VERSION_CODES.Q)
private object BlendModeColorFilterHelper {
    @DoNotInline
    fun BlendModeColorFilter(color: Color, blendMode: BlendMode): BlendModeColorFilter {
        return BlendModeColorFilter(color.toArgb(), blendMode.toAndroidBlendMode())
    }
}