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

import android.graphics.Shader
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

/**
 * Helper method to determine if the appropriate [TileMode] is supported on the given Android
 * API level this provides an opportunity for consumers to fallback on an alternative user
 * experience for devices that do not support the corresponding blend mode. Usages of [TileMode]
 * types that are not supported will fallback onto the default of [TileMode.Clamp]
 */
actual fun TileMode.isSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || this != TileMode.Decal

fun TileMode.toAndroidTileMode(): Shader.TileMode =
    when (this) {
        TileMode.Clamp -> Shader.TileMode.CLAMP
        TileMode.Repeated -> Shader.TileMode.REPEAT
        TileMode.Mirror -> Shader.TileMode.MIRROR
        TileMode.Decal ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                TileModeVerificationHelper.getFrameworkTileModeDecal()
            } else {
                Shader.TileMode.CLAMP
            }

        // Always fallback to TileMode.Clamp
        else -> Shader.TileMode.CLAMP
    }

fun Shader.TileMode.toComposeTileMode(): TileMode =
    when (this) {
        Shader.TileMode.CLAMP -> TileMode.Clamp
        Shader.TileMode.MIRROR -> TileMode.Mirror
        Shader.TileMode.REPEAT -> TileMode.Repeated
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && this == Shader.TileMode.DECAL) {
                TileModeVerificationHelper.getComposeTileModeDecal()
            } else {
                TileMode.Clamp
            }
        }
    }

@RequiresApi(Build.VERSION_CODES.S)
private object TileModeVerificationHelper {
    @DoNotInline
    fun getFrameworkTileModeDecal() = Shader.TileMode.DECAL

    @DoNotInline
    fun getComposeTileModeDecal() = TileMode.Decal
}