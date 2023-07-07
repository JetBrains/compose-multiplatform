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

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Helper method to determine if the appropriate [BlendMode] is supported on the given Android
 * API level this provides an opportunity for consumers to fallback on an alternative user
 * experience for devices that do not support the corresponding blend mode. Usages of [BlendMode]
 * types that are not supported will fallback onto the default of [BlendMode.SrcOver]
 */
actual fun BlendMode.isSupported(): Boolean {
    // All blend modes supported on Android Q /API level 29+
    // For older API levels we first check to see if we are consuming the default BlendMode
    // or SrcOver which is supported on all platforms
    // Otherwise we attempt to convert to the appropriate PorterDuff mode and we get
    // something other than PorterDuff.Mode.SRC_OVER (the default) then we support this BlendMode.
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
        this == BlendMode.SrcOver ||
        toPorterDuffMode() != android.graphics.PorterDuff.Mode.SRC_OVER
}

/**
 * Convert the [BlendMode] to the corresponding [android.graphics.PorterDuff.Mode] if it exists,
 * falling back on the default of [android.graphics.PorterDuff.Mode.SRC_OVER] for unsupported types
 */
internal fun BlendMode.toPorterDuffMode(): android.graphics.PorterDuff.Mode = when (this) {
    BlendMode.Clear -> android.graphics.PorterDuff.Mode.CLEAR
    BlendMode.Src -> android.graphics.PorterDuff.Mode.SRC
    BlendMode.Dst -> android.graphics.PorterDuff.Mode.DST
    BlendMode.SrcOver -> android.graphics.PorterDuff.Mode.SRC_OVER
    BlendMode.DstOver -> android.graphics.PorterDuff.Mode.DST_OVER
    BlendMode.SrcIn -> android.graphics.PorterDuff.Mode.SRC_IN
    BlendMode.DstIn -> android.graphics.PorterDuff.Mode.DST_IN
    BlendMode.SrcOut -> android.graphics.PorterDuff.Mode.SRC_OUT
    BlendMode.DstOut -> android.graphics.PorterDuff.Mode.DST_OUT
    BlendMode.SrcAtop -> android.graphics.PorterDuff.Mode.SRC_ATOP
    BlendMode.DstAtop -> android.graphics.PorterDuff.Mode.DST_ATOP
    BlendMode.Xor -> android.graphics.PorterDuff.Mode.XOR
    BlendMode.Plus -> android.graphics.PorterDuff.Mode.ADD
    BlendMode.Screen -> android.graphics.PorterDuff.Mode.SCREEN
    BlendMode.Overlay -> android.graphics.PorterDuff.Mode.OVERLAY
    BlendMode.Darken -> android.graphics.PorterDuff.Mode.DARKEN
    BlendMode.Lighten -> android.graphics.PorterDuff.Mode.LIGHTEN
    BlendMode.Modulate -> {
        // b/73224934 Android PorterDuff Multiply maps to Skia Modulate
        android.graphics.PorterDuff.Mode.MULTIPLY
    }
    // Always return SRC_OVER as the default if there is no valid alternative
    else -> android.graphics.PorterDuff.Mode.SRC_OVER
}

/**
 * Convert the compose [BlendMode] to the underlying Android platform [android.graphics.BlendMode]
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal fun BlendMode.toAndroidBlendMode(): android.graphics.BlendMode = when (this) {
    BlendMode.Clear -> android.graphics.BlendMode.CLEAR
    BlendMode.Src -> android.graphics.BlendMode.SRC
    BlendMode.Dst -> android.graphics.BlendMode.DST
    BlendMode.SrcOver -> android.graphics.BlendMode.SRC_OVER
    BlendMode.DstOver -> android.graphics.BlendMode.DST_OVER
    BlendMode.SrcIn -> android.graphics.BlendMode.SRC_IN
    BlendMode.DstIn -> android.graphics.BlendMode.DST_IN
    BlendMode.SrcOut -> android.graphics.BlendMode.SRC_OUT
    BlendMode.DstOut -> android.graphics.BlendMode.DST_OUT
    BlendMode.SrcAtop -> android.graphics.BlendMode.SRC_ATOP
    BlendMode.DstAtop -> android.graphics.BlendMode.DST_ATOP
    BlendMode.Xor -> android.graphics.BlendMode.XOR
    BlendMode.Plus -> android.graphics.BlendMode.PLUS
    BlendMode.Modulate -> android.graphics.BlendMode.MODULATE
    BlendMode.Screen -> android.graphics.BlendMode.SCREEN
    BlendMode.Overlay -> android.graphics.BlendMode.OVERLAY
    BlendMode.Darken -> android.graphics.BlendMode.DARKEN
    BlendMode.Lighten -> android.graphics.BlendMode.LIGHTEN
    BlendMode.ColorDodge -> android.graphics.BlendMode.COLOR_DODGE
    BlendMode.ColorBurn -> android.graphics.BlendMode.COLOR_BURN
    BlendMode.Hardlight -> android.graphics.BlendMode.HARD_LIGHT
    BlendMode.Softlight -> android.graphics.BlendMode.SOFT_LIGHT
    BlendMode.Difference -> android.graphics.BlendMode.DIFFERENCE
    BlendMode.Exclusion -> android.graphics.BlendMode.EXCLUSION
    BlendMode.Multiply -> android.graphics.BlendMode.MULTIPLY
    BlendMode.Hue -> android.graphics.BlendMode.HUE
    BlendMode.Saturation -> android.graphics.BlendMode.SATURATION
    BlendMode.Color -> android.graphics.BlendMode.COLOR
    BlendMode.Luminosity -> android.graphics.BlendMode.LUMINOSITY
    // Always return SRC_OVER as the default if there is no valid alternative
    else -> android.graphics.BlendMode.SRC_OVER
}
